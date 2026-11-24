
package com.tfg.ticketflow.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Reserva.EstadoReserva;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.ReservaRepository;
import com.tfg.ticketflow.repository.TipoEntradaRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final TipoEntradaRepository tipoEntradaRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public EventoService(
            EventoRepository eventoRepository,
            TipoEntradaRepository tipoEntradaRepository,
            ReservaRepository reservaRepository, Clock reservasClock) {

        this.eventoRepository = eventoRepository;
        this.tipoEntradaRepository = tipoEntradaRepository;
        this.reservaRepository = reservaRepository;
        this.clock = reservasClock;
    }

    // Buscar un evento por su identificador
    public Optional<Evento> buscarPorId(Long id) {
        return eventoRepository.findById(id);
    }

    // Obtener todos los eventos
    public List<Evento> obtenerTodos() {
        return eventoRepository.findAll();
    }

    // Crear un evento
    public Evento crearEvento(String titulo, String descripcion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String ubicacion, String direccion,
            Integer aforoMaximo, Usuario organizador) {
        return crearEvento(titulo, descripcion, fechaInicio, fechaFin,
                ubicacion, direccion, aforoMaximo, null, organizador);
    }

    public Evento crearEvento(String titulo, String descripcion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String ubicacion, String direccion, Integer aforoMaximo,
            LocalDateTime fechaInicioVentaGeneral, Usuario organizador) {

        validarFechas(fechaInicio, fechaFin, fechaInicioVentaGeneral);
        validarAforo(aforoMaximo);

        Evento evento = new Evento(
                titulo.trim(),
                descripcion.trim(),
                fechaInicio,
                fechaFin,
                ubicacion.trim(),
                direccion.trim(),
                null, // La imagen se sube por separado
                aforoMaximo,
                organizador
        );
        evento.setFechaInicioVentaGeneral(fechaInicioVentaGeneral);

        return eventoRepository.save(evento);
    }

    // Obtener todos los eventos publicados
    public List<Evento> obtenerEventosPublicados() {
        return eventoRepository.findByEstado(
                Evento.EstadoEvento.PUBLICADO);
    }

    // Obtener eventos publicados aplicando filtros opcionales
    public List<Evento> obtenerEventosPublicados(
            String titulo, String ubicacion) {

        String tituloFiltro = (titulo == null || titulo.isBlank())
                ? null : titulo.trim();

        String ubicacionFiltro = (ubicacion == null || ubicacion.isBlank())
                ? null : ubicacion.trim();

        return eventoRepository.buscarEventosFiltrados(
                Evento.EstadoEvento.PUBLICADO,
                tituloFiltro,
                ubicacionFiltro
        );
    }

    // Buscar un evento publicado por su identificador
    public Optional<Evento> buscarEventoPublicado(Long id) {

        Optional<Evento> evento = eventoRepository.findById(id);

        if (evento.isEmpty()
                || evento.get().getEstado()
                    != Evento.EstadoEvento.PUBLICADO) {
            return Optional.empty();
        }

        return evento;
    }

    // Obtener todos los eventos de un organizador
    public List<Evento> obtenerEventosDelOrganizador(Usuario organizador) {
        return eventoRepository.findByOrganizador(organizador);
    }

    // Actualizar los datos generales de un evento
    @Transactional
    public Evento actualizarEvento(Long id, Usuario organizador,
            String titulo, String descripcion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String ubicacion, String direccion,
            Integer aforoMaximo) {
        return actualizarEvento(id, organizador, titulo, descripcion,
                fechaInicio, fechaFin, ubicacion, direccion, aforoMaximo, null, false);
    }

    @Transactional
    public Evento actualizarEvento(Long id, Usuario organizador,
            String titulo, String descripcion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String ubicacion, String direccion, Integer aforoMaximo,
            LocalDateTime nuevaFechaVenta, boolean incluyeFechaVenta) {

        Evento evento =
                eventoRepository.buscarParaModificar(id, organizador)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No se ha encontrado el evento o no pertenece al organizador."));

        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO
                || evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new ConflictoNegocioException(
                    "No se puede modificar un evento cancelado o finalizado.");
        }

        if (evento.getEstado() == Evento.EstadoEvento.PUBLICADO
                && fechaInicio != null && !fechaInicio.equals(evento.getFechaInicio())
                && haLlegado(fechaInicio)) {
            throw new ConflictoNegocioException(
                    "La nueva fecha de inicio de un evento publicado debe ser futura.");
        }

        LocalDateTime fechaVentaActual = evento.getFechaInicioVentaGeneral();
        if (evento.getEstado() == Evento.EstadoEvento.PUBLICADO
                && incluyeFechaVenta && !Objects.equals(nuevaFechaVenta, fechaVentaActual)) {
            throw new ConflictoNegocioException(
                    "No se puede cambiar la fecha general de venta de un evento publicado.");
        }

        LocalDateTime fechaVenta = incluyeFechaVenta ? nuevaFechaVenta : fechaVentaActual;
        validarFechas(fechaInicio, fechaFin, fechaVenta);
        validarAforo(aforoMaximo);

        boolean cambiaFechaOUbicacion = !Objects.equals(fechaInicio, evento.getFechaInicio())
                || !Objects.equals(fechaFin, evento.getFechaFin())
                || !Objects.equals(ubicacion.trim(), evento.getUbicacion())
                || !Objects.equals(direccion.trim(), evento.getDireccion());
        if (cambiaFechaOUbicacion && hayCompromisos(id)) {
            throw new ConflictoNegocioException(
                    "No se pueden cambiar fechas ni ubicación con entradas comprometidas.");
        }

        validarFechasDeLosTipos(id, fechaInicio);

        // Evitar reducir el aforo por debajo de las plazas asignadas
        Long cantidadAsignada =
                tipoEntradaRepository.sumarCantidadTotal(id);

        if (aforoMaximo < cantidadAsignada) {
            throw new ConflictoNegocioException(
                    "El aforo no puede ser inferior a las entradas ya asignadas.");
        }

        evento.setTitulo(titulo.trim());
        evento.setDescripcion(descripcion.trim());
        evento.setFechaInicio(fechaInicio);
        evento.setFechaFin(fechaFin);
        evento.setUbicacion(ubicacion.trim());
        evento.setDireccion(direccion.trim());
        evento.setAforoMaximo(aforoMaximo);
        evento.setFechaInicioVentaGeneral(fechaVenta);

        // No modificamos evento.getImagen()

        return eventoRepository.save(evento);
    }

    // Publicar un evento
    @Transactional
    public Evento publicarEvento(Long id, Usuario organizador) {

        Evento evento =
                eventoRepository.buscarParaModificar(id, organizador)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No se ha encontrado el evento o no pertenece al organizador."));

        if (evento.getEstado() != Evento.EstadoEvento.BORRADOR) {
            throw new ConflictoNegocioException(
                    "Solo se pueden publicar eventos en borrador.");
        }

        if (haLlegado(evento.getFechaInicio())) {
            throw new ConflictoNegocioException(
                    "No se puede publicar un evento cuya fecha de inicio ya ha pasado.");
        }

        if (evento.getAforoMaximo() == null
                || evento.getAforoMaximo() <= 0) {
            throw new ConflictoNegocioException(
                    "No se puede publicar un evento sin un aforo máximo válido.");
        }

        validarFechas(evento.getFechaInicio(), evento.getFechaFin(), evento.getFechaInicioVentaGeneral());
        validarFechasDeLosTipos(id, evento.getFechaInicio());

        if (!tipoEntradaRepository.existsByEventoIdAndActivoTrueAndCantidadTotalGreaterThan(id, 0)) {
            throw new ConflictoNegocioException(
                    "Debes configurar al menos un tipo de entrada activo con plazas antes de publicar.");
        }

        if (tipoEntradaRepository.sumarCantidadTotal(id) > evento.getAforoMaximo()) {
            throw new ConflictoNegocioException("Las entradas asignadas superan el aforo.");
        }

        evento.setEstado(Evento.EstadoEvento.PUBLICADO);

        return eventoRepository.save(evento);
    }

    // Cancelar un evento
    @Transactional
    public Evento cancelarEvento(Long id, Usuario organizador) {

        Evento evento =
                eventoRepository.buscarParaModificar(id, organizador)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No se ha encontrado el evento o no pertenece al organizador."));

        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO) {
            throw new ConflictoNegocioException(
                    "El evento ya está cancelado.");
        }

        if (evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new ConflictoNegocioException(
                    "No se puede cancelar un evento finalizado.");
        }

        reservaRepository.cancelarActivasDelEvento(id, EstadoReserva.ACTIVA, EstadoReserva.CANCELADA, clock.instant());
        evento.setEstado(Evento.EstadoEvento.CANCELADO);

        return eventoRepository.save(evento);
    }

    // Finalizar un evento
    @Transactional
    public Evento finalizarEvento(Long id, Usuario organizador) {

        Evento evento =
                eventoRepository.buscarParaModificar(id, organizador)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No se ha encontrado el evento o no pertenece al organizador."));

        if (evento.getEstado() != Evento.EstadoEvento.PUBLICADO) {
            throw new ConflictoNegocioException(
                    "Solo se pueden finalizar eventos publicados.");
        }

        LocalDateTime fechaLimite = evento.getFechaFin() != null
                ? evento.getFechaFin()
                : evento.getFechaInicio();

        if (!haLlegado(fechaLimite)) {
            throw new ConflictoNegocioException(
                    "El evento todavía no puede finalizarse.");
        }

        evento.setEstado(Evento.EstadoEvento.FINALIZADO);

        return eventoRepository.save(evento);
    }

    // Retirar un evento publicado
    @Transactional
    public Evento retirarEvento(Long id, Usuario organizador) {

        Evento evento =
                eventoRepository.buscarParaModificar(id, organizador)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No se ha encontrado el evento o no pertenece al organizador."));

        if (evento.getEstado() != Evento.EstadoEvento.PUBLICADO) {
            throw new ConflictoNegocioException(
                    "Solo se pueden retirar eventos publicados.");
        }

        if (hayCompromisos(id)) {
            throw new ConflictoNegocioException(
                    "No se puede retirar el evento mientras tenga entradas comprometidas.");
        }

        evento.setEstado(Evento.EstadoEvento.BORRADOR);

        return eventoRepository.save(evento);
    }

    private boolean haLlegado(LocalDateTime fecha) {
        return !clock.instant().isBefore(fecha.atZone(clock.getZone()).toInstant());
    }

    private boolean hayCompromisos(Long id) {
        return reservaRepository.contarCompromisosPorEvento(id,
                EstadoReserva.ACTIVA, EstadoReserva.CONFIRMADA, clock.instant()) > 0;
    }

    private void validarFechas(LocalDateTime inicio, LocalDateTime fin, LocalDateTime fechaVenta) {
        if (inicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        }
        if (fin != null && !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (fechaVenta != null && !fechaVenta.isBefore(inicio)) {
            throw new IllegalArgumentException("La venta general debe comenzar antes del inicio del evento.");
        }
    }

    private void validarFechasDeLosTipos(Long eventoId, LocalDateTime inicio) {
        if (tipoEntradaRepository.existsByEventoIdAndFechaInicioVentaGreaterThanEqual(eventoId, inicio)) {
            throw new ConflictoNegocioException(
                    "La venta de todos los tipos de entrada debe comenzar antes del inicio del evento.");
        }
    }

    // Validar el aforo máximo
    private void validarAforo(Integer aforoMaximo) {

        if (aforoMaximo == null || aforoMaximo <= 0) {
            throw new IllegalArgumentException(
                    "El aforo máximo debe ser mayor que cero.");
        }
    }
}
