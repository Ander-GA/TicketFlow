package com.tfg.ticketflow.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.ticketflow.dto.reserva.DisponibilidadTipoEntradaDTO;
import com.tfg.ticketflow.dto.reserva.ReservaRespuestaDTO;
import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Reserva;
import com.tfg.ticketflow.entity.Reserva.EstadoReserva;
import com.tfg.ticketflow.entity.TipoEntrada;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.ReservaRepository;
import com.tfg.ticketflow.repository.TipoEntradaRepository;
import com.tfg.ticketflow.repository.UsuarioRepository;

@Service
public class ReservaService {

    private static final Duration DURACION_RESERVA = Duration.ofMinutes(10);

    private final EventoRepository eventoRepository;
    private final TipoEntradaRepository tipoEntradaRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public ReservaService(EventoRepository eventoRepository,
            TipoEntradaRepository tipoEntradaRepository,
            ReservaRepository reservaRepository,
            UsuarioRepository usuarioRepository,
            Clock reservasClock) {
        this.eventoRepository = eventoRepository;
        this.tipoEntradaRepository = tipoEntradaRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = reservasClock;
    }

    // Crear una reserva. Se bloquea el evento ANTES de consultar reservas o tipos
    // para que dos transacciones no consuman simultáneamente la última plaza.
    @Transactional
    public ReservaRespuestaDTO crearReserva(Long eventoId, Long tipoId,
            Integer cantidad, String claveIdempotencia, String email) {

        validarSolicitud(eventoId, tipoId, cantidad, claveIdempotencia);
        String claveHash = hashClave(claveIdempotencia);

        Evento evento = eventoRepository.bloquearPorId(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento."));

        // Segundo bloqueo: usuario. Así una misma clave no se ejecuta a la vez
        // en dos eventos diferentes. Orden obligatorio: evento -> usuario.
        Usuario usuario = usuarioRepository.findBloqueadoByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el usuario."));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ConflictoNegocioException("El usuario no puede realizar reservas.");
        }

        Instant ahora = clock.instant();

        // Un reintento devuelve la misma reserva sin prolongar sus 10 minutos.
        Reserva anterior = reservaRepository
                .findByUsuarioIdAndClaveIdempotenciaHash(usuario.getId(), claveHash)
                .orElse(null);
        if (anterior != null) {
            if (!anterior.getEvento().getId().equals(eventoId)
                    || !anterior.getTipoEntrada().getId().equals(tipoId)
                    || !anterior.getCantidad().equals(cantidad)) {
                throw new ConflictoNegocioException(
                        "La clave de idempotencia ya se utilizó para otra solicitud.");
            }
            return new ReservaRespuestaDTO(anterior, ahora);
        }

        TipoEntrada tipo = tipoEntradaRepository.findByIdAndEventoId(tipoId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el tipo de entrada del evento."));

        comprobarVenta(evento, tipo, ahora);

        // Una reserva activa por usuario y modalidad. Se admiten modalidades distintas.
        if (reservaRepository.contarActivasDelUsuario(usuario.getId(), tipoId,
                EstadoReserva.ACTIVA, ahora) > 0) {
            throw new ConflictoNegocioException(
                    "Ya tienes una reserva activa para este tipo de entrada.");
        }

        long comprometidas = contarComprometidas(tipoId, ahora);
        long disponibles = (long) tipo.getCantidadTotal() - comprometidas;
        if (disponibles < cantidad) {
            throw new ConflictoNegocioException(
                    "No quedan suficientes entradas disponibles para esta modalidad.");
        }

        Reserva reserva = new Reserva(usuario, evento, tipo, cantidad, ahora,
                ahora.plus(DURACION_RESERVA), tipo.getPrecio(), claveHash);
        reserva = reservaRepository.save(reserva);

        // Desde la primera reserva ya no se pueden cambiar las condiciones
        // comerciales originales, incluso si posteriormente caduca.
        tipo.bloquearCondiciones();

        return new ReservaRespuestaDTO(reserva, ahora);
    }

    @Transactional(readOnly = true)
    public List<ReservaRespuestaDTO> obtenerMisReservas(Usuario usuario) {
        Instant ahora = clock.instant();
        return reservaRepository.findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuario.getId())
                .stream().map(r -> new ReservaRespuestaDTO(r, ahora)).toList();
    }

    @Transactional(readOnly = true)
    public ReservaRespuestaDTO obtenerMiReserva(Long reservaId, Usuario usuario) {
        Reserva reserva = reservaRepository.findByIdAndUsuarioId(reservaId, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado la reserva."));
        return new ReservaRespuestaDTO(reserva, clock.instant());
    }

    // Se usa el mismo orden de bloqueo: primero evento y después reserva.
    @Transactional
    public ReservaRespuestaDTO cancelarReserva(Long reservaId, Usuario usuario) {
        Long eventoId = reservaRepository
                .obtenerEventoIdDeReservaPropia(reservaId, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado la reserva."));

        eventoRepository.bloquearPorId(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento."));

        Reserva reserva = reservaRepository.bloquearReservaPropia(reservaId, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado la reserva."));
        Instant ahora = clock.instant();

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return new ReservaRespuestaDTO(reserva, ahora);
        }
        if (reserva.getEstado() != EstadoReserva.ACTIVA
                || !reserva.getFechaExpiracion().isAfter(ahora)) {
            throw new ConflictoNegocioException(
                    "La reserva ya no está activa y no se puede cancelar.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return new ReservaRespuestaDTO(reservaRepository.save(reserva), ahora);
    }

    // Dato informativo: la reserva vuelve a comprobar el inventario bajo bloqueo.
    @Transactional(readOnly = true)
    public DisponibilidadTipoEntradaDTO consultarDisponibilidad(Long eventoId, Long tipoId) {
        TipoEntrada tipo = tipoEntradaRepository.findByIdAndEventoIdAndEventoEstadoAndActivoTrue(
                tipoId, eventoId, Evento.EstadoEvento.PUBLICADO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el tipo de entrada activo del evento publicado."));
        Evento evento = tipo.getEvento();

        Instant ahora = clock.instant();
        LocalDateTime apertura = obtenerFechaApertura(tipo, evento);
        long comprometidas = contarComprometidas(tipoId, ahora);
        long disponibles = Math.max(0L, (long) tipo.getCantidadTotal() - comprometidas);
        boolean ventaAbierta = ahora.isBefore(instanteEvento(evento.getFechaInicio()))
                && (apertura == null || !ahora.isBefore(instanteEvento(apertura)));

        return new DisponibilidadTipoEntradaDTO(eventoId, tipoId,
                tipo.getCantidadTotal(), comprometidas, disponibles,
                apertura, ventaAbierta);
    }

    private long contarComprometidas(Long tipoId, Instant ahora) {
        return reservaRepository.sumarComprometidasPorTipo(tipoId,
                EstadoReserva.ACTIVA, EstadoReserva.CONFIRMADA, ahora);
    }

    private void comprobarVenta(Evento evento, TipoEntrada tipo, Instant ahora) {
        if (evento.getEstado() != Evento.EstadoEvento.PUBLICADO) {
            throw new ConflictoNegocioException("El evento no está publicado.");
        }
        if (!tipo.isActivo()) {
            throw new ConflictoNegocioException("Este tipo de entrada no está a la venta.");
        }
        if (!ahora.isBefore(instanteEvento(evento.getFechaInicio()))) {
            throw new ConflictoNegocioException("El evento ya ha comenzado.");
        }
        LocalDateTime apertura = obtenerFechaApertura(tipo, evento);
        if (apertura != null && ahora.isBefore(instanteEvento(apertura))) {
            throw new ConflictoNegocioException("La venta de este tipo de entrada todavía no ha comenzado.");
        }
    }

    private LocalDateTime obtenerFechaApertura(TipoEntrada tipo, Evento evento) {
        return tipo.getFechaInicioVenta() != null
                ? tipo.getFechaInicioVenta() : evento.getFechaInicioVentaGeneral();
    }

    private Instant instanteEvento(LocalDateTime fecha) {
        return fecha.atZone(clock.getZone()).toInstant();
    }

    private void validarSolicitud(Long eventoId, Long tipoId, Integer cantidad,
            String claveIdempotencia) {
        if (eventoId == null || eventoId <= 0 || tipoId == null || tipoId <= 0) {
            throw new IllegalArgumentException("El evento y el tipo de entrada son obligatorios.");
        }
        if (cantidad == null || cantidad < 1 || cantidad > 10) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 10.");
        }
        if (claveIdempotencia == null || claveIdempotencia.isBlank()
                || claveIdempotencia.length() < 16 || claveIdempotencia.length() > 80) {
            throw new IllegalArgumentException("La clave de idempotencia debe tener entre 16 y 80 caracteres.");
        }
    }

    private String hashClave(String clave) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }
}
