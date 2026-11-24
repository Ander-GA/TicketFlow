package com.tfg.ticketflow.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.TipoEntrada;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.TipoEntradaRepository;

@Service
public class TipoEntradaService {

    private final TipoEntradaRepository tipoEntradaRepository;
    private final EventoRepository eventoRepository;
    private final Clock clock;

    public TipoEntradaService(TipoEntradaRepository tipoEntradaRepository,
            EventoRepository eventoRepository, Clock reservasClock) {
        this.tipoEntradaRepository = tipoEntradaRepository;
        this.eventoRepository = eventoRepository;
        this.clock = reservasClock;
    }

    // Catálogo público: solo tipos activos de eventos publicados.
    @Transactional(readOnly = true)
    public List<TipoEntrada> obtenerTiposPublicos(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se ha encontrado el evento."));
        if (evento.getEstado() != Evento.EstadoEvento.PUBLICADO) {
            throw new RecursoNoEncontradoException("No se ha encontrado el evento publicado.");
        }
        return tipoEntradaRepository.findByEventoIdAndEventoEstadoAndActivoTrueOrderByIdAsc(
                eventoId, Evento.EstadoEvento.PUBLICADO);
    }

    // El organizador consulta todos sus tipos, también los desactivados.
    @Transactional(readOnly = true)
    public List<TipoEntrada> obtenerTiposDelOrganizador(Long eventoId, Usuario organizador) {
        eventoRepository.findByIdAndOrganizador(eventoId, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));
        return tipoEntradaRepository.findByEventoIdOrderByIdAsc(eventoId);
    }

    // Crear un tipo de entrada. Se bloquea el evento para serializar los cambios de aforo.
    @Transactional
    public TipoEntrada crearTipoEntrada(Long eventoId, Usuario organizador,
            String nombre, String descripcion, BigDecimal precio,
            Integer cantidadTotal, LocalDateTime fechaInicioVenta) {

        Evento evento = obtenerEventoParaModificar(eventoId, organizador);
        validarEventoEditable(evento);
        validarDatos(nombre, descripcion, precio, cantidadTotal, fechaInicioVenta, evento);
        String nombreNormalizado = nombre.trim();

        if (tipoEntradaRepository.existsByEventoIdAndNombreIgnoreCase(eventoId, nombreNormalizado)) {
            throw new ConflictoNegocioException("Ya existe un tipo de entrada con ese nombre.");
        }

        long asignadas = tipoEntradaRepository.sumarCantidadTotal(eventoId);
        validarLimiteAforo(evento, asignadas + cantidadTotal);

        TipoEntrada tipo = new TipoEntrada(nombreNormalizado, normalizarDescripcion(descripcion),
                precio, cantidadTotal, fechaInicioVenta, evento);
        return tipoEntradaRepository.save(tipo);
    }

    // Modificar los atributos de un tipo. Condiciones bloqueadas = atributos comerciales inmutables.
    @Transactional
    public TipoEntrada actualizarTipoEntrada(Long eventoId, Long tipoId, Usuario organizador,
            String nombre, String descripcion, BigDecimal precio,
            Integer cantidadTotal, LocalDateTime fechaInicioVenta) {

        Evento evento = obtenerEventoParaModificar(eventoId, organizador);
        validarEventoEditable(evento);
        TipoEntrada tipo = obtenerTipo(eventoId, tipoId);
        validarDatos(nombre, descripcion, precio, cantidadTotal, fechaInicioVenta, evento);

        String nombreNormalizado = nombre.trim();
        String descripcionNormalizada = normalizarDescripcion(descripcion);

        if (tipoEntradaRepository.existsByEventoIdAndNombreIgnoreCaseAndIdNot(
                eventoId, nombreNormalizado, tipoId)) {
            throw new ConflictoNegocioException("Ya existe un tipo de entrada con ese nombre.");
        }

        if (tipo.isCondicionesBloqueadas()) {
            boolean cambianCondiciones = !tipo.getNombre().equals(nombreNormalizado)
                    || !Objects.equals(tipo.getDescripcion(), descripcionNormalizada)
                    || tipo.getPrecio().compareTo(precio) != 0
                    || !Objects.equals(tipo.getFechaInicioVenta(), fechaInicioVenta);
            if (cambianCondiciones) {
                throw new ConflictoNegocioException(
                        "No se pueden modificar las condiciones de un tipo con reservas o compras registradas.");
            }
            // Hasta disponer del recuento de compromisos, prohibimos reducciones tras bloquear.
            if (cantidadTotal < tipo.getCantidadTotal()) {
                throw new ConflictoNegocioException(
                        "No se puede reducir el cupo de un tipo con condiciones bloqueadas.");
            }
        }

        long asignadas = tipoEntradaRepository.sumarCantidadTotal(eventoId);
        long nuevoTotal = asignadas - tipo.getCantidadTotal() + cantidadTotal;
        validarLimiteAforo(evento, nuevoTotal);

        tipo.setNombre(nombreNormalizado);
        tipo.setDescripcion(descripcionNormalizada);
        tipo.setPrecio(precio);
        tipo.setCantidadTotal(cantidadTotal);
        tipo.setFechaInicioVenta(fechaInicioVenta);

        return tipoEntradaRepository.save(tipo);
    }

    // Activar/desactivar no libera plazas, ni modifica entradas ya emitidas.
    @Transactional
    public TipoEntrada cambiarEstadoActivo(Long eventoId, Long tipoId,
            Usuario organizador, Boolean activo) {

        Evento evento = obtenerEventoParaModificar(eventoId, organizador);
        validarEventoEditable(evento);
        if (activo == null) {
            throw new IllegalArgumentException("Debes indicar el estado del tipo de entrada.");
        }
        TipoEntrada tipo = obtenerTipo(eventoId, tipoId);
        tipo.setActivo(activo);
        return tipoEntradaRepository.save(tipo);
    }

    // El borrado físico solo está permitido en BORRADOR y antes de cualquier reserva/compra.
    @Transactional
    public void eliminarTipoEntrada(Long eventoId, Long tipoId, Usuario organizador) {
        Evento evento = obtenerEventoParaModificar(eventoId, organizador);
        if (evento.getEstado() != Evento.EstadoEvento.BORRADOR) {
            throw new ConflictoNegocioException(
                    "Solo se pueden eliminar tipos de entrada de eventos en borrador.");
        }
        TipoEntrada tipo = obtenerTipo(eventoId, tipoId);
        if (tipo.isCondicionesBloqueadas()) {
            throw new ConflictoNegocioException(
                    "No se puede eliminar un tipo con reservas o compras registradas.");
        }
        tipoEntradaRepository.delete(tipo);
    }

    private Evento obtenerEventoParaModificar(Long eventoId, Usuario organizador) {
        return eventoRepository.buscarParaModificar(eventoId, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));
    }

    private TipoEntrada obtenerTipo(Long eventoId, Long tipoId) {
        return tipoEntradaRepository.findByIdAndEventoId(tipoId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el tipo de entrada de ese evento."));
    }

    private void validarEventoEditable(Evento evento) {
        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO
                || evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new ConflictoNegocioException(
                    "No se pueden modificar entradas de eventos cancelados o finalizados.");
        }
        if (evento.getEstado() == Evento.EstadoEvento.PUBLICADO
                && !clock.instant().isBefore(evento.getFechaInicio().atZone(clock.getZone()).toInstant())) {
            throw new ConflictoNegocioException(
                    "No se pueden modificar tipos de entrada de un evento ya comenzado.");
        }
    }

    private void validarDatos(String nombre, String descripcion, BigDecimal precio,
            Integer cantidadTotal, LocalDateTime fechaInicioVenta, Evento evento) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 100 caracteres.");
        }
        if (descripcion != null && descripcion.length() > 2000) {
            throw new IllegalArgumentException("La descripción no puede superar los 2000 caracteres.");
        }
        if (precio == null || precio.signum() < 0 || precio.scale() > 2 || precio.precision() - precio.scale() > 8) {
            throw new IllegalArgumentException("El precio debe ser no negativo y tener máximo 2 decimales.");
        }
        if (cantidadTotal == null || cantidadTotal <= 0) {
            throw new IllegalArgumentException("La cantidad de entradas debe ser mayor que cero.");
        }
        if (fechaInicioVenta != null && !fechaInicioVenta.isBefore(evento.getFechaInicio())) {
            throw new IllegalArgumentException("La venta debe comenzar antes del inicio del evento.");
        }
    }

    private void validarLimiteAforo(Evento evento, long cantidadAsignada) {
        if (evento.getAforoMaximo() == null || evento.getAforoMaximo() <= 0) {
            throw new ConflictoNegocioException("Configura un aforo válido antes de añadir tipos.");
        }
        if (cantidadAsignada > evento.getAforoMaximo()) {
            throw new ConflictoNegocioException("La suma de los tipos supera el aforo máximo del evento.");
        }
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }
}
