
package com.tfg.ticketflow.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.repository.EventoRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
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
    public Evento crearEvento(String titulo, String descripcion, LocalDateTime fechaInicio,
            LocalDateTime fechaFin, String ubicacion, String direccion, String imagen,
            Usuario organizador) {

        // Comprobar que la fecha de fin sea posterior a la fecha de inicio
        if (fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }

        Evento evento = new Evento(
                titulo,
                descripcion,
                fechaInicio,
                fechaFin,
                ubicacion,
                direccion,
                imagen,
                organizador
        );

        return eventoRepository.save(evento);
    }

    // Obtener todos los eventos publicados
    public List<Evento> obtenerEventosPublicados() {
        return eventoRepository.findByEstado(Evento.EstadoEvento.PUBLICADO);
    }

    // Obtener eventos publicados aplicando filtros opcionales
    public List<Evento> obtenerEventosPublicados(String titulo, String ubicacion) {

        // Normalizar los filtros recibidos
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

        // Comprobar que el evento existe y está publicado
        if (evento.isEmpty() || evento.get().getEstado() != Evento.EstadoEvento.PUBLICADO) {
            return Optional.empty();
        }

        return evento;
    }

    // Obtener todos los eventos de un organizador
    public List<Evento> obtenerEventosDelOrganizador(Usuario organizador) {
        return eventoRepository.findByOrganizador(organizador);
    }

    // Actualizar los datos de un evento
    public Evento actualizarEvento(Long id, Usuario organizador, String titulo,
            String descripcion, LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String ubicacion, String direccion, String imagen) {

        // Buscar el evento comprobando que pertenece al organizador
        Evento evento = eventoRepository.findByIdAndOrganizador(id, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // Comprobar que el evento se puede modificar
        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO
                || evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new ConflictoNegocioException(
                    "No se puede modificar un evento cancelado o finalizado.");
        }

        // Comprobar que la fecha de fin sea posterior a la fecha de inicio
        if (fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException(
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }

        // Actualizar los datos del evento
        evento.setTitulo(titulo.trim());
        evento.setDescripcion(descripcion.trim());
        evento.setFechaInicio(fechaInicio);
        evento.setFechaFin(fechaFin);
        evento.setUbicacion(ubicacion.trim());
        evento.setDireccion(direccion.trim());
        evento.setImagen(imagen);

        return eventoRepository.save(evento);
    }

    // Publicar un evento
    public Evento publicarEvento(Long id, Usuario organizador) {

        // Buscar el evento comprobando que pertenece al organizador
        Evento evento = eventoRepository.findByIdAndOrganizador(id, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // Comprobar que el evento está en borrador
        if (evento.getEstado() != Evento.EstadoEvento.BORRADOR) {
            throw new ConflictoNegocioException(
                    "Solo se pueden publicar eventos en borrador.");
        }

        // Comprobar que el evento todavía no ha comenzado
        if (!evento.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new ConflictoNegocioException(
                    "No se puede publicar un evento cuya fecha de inicio ya ha pasado.");
        }

        // Cambiar el estado del evento a publicado
        evento.setEstado(Evento.EstadoEvento.PUBLICADO);

        return eventoRepository.save(evento);
    }

    // Cancelar un evento
    public Evento cancelarEvento(Long id, Usuario organizador) {

        // Buscar el evento comprobando que pertenece al organizador
        Evento evento = eventoRepository.findByIdAndOrganizador(id, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // Comprobar que el evento no esté cancelado
        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO) {
            throw new ConflictoNegocioException(
                    "El evento ya está cancelado.");
        }

        // Comprobar que el evento no esté finalizado
        if (evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new ConflictoNegocioException(
                    "No se puede cancelar un evento finalizado.");
        }

        // Cambiar el estado del evento a cancelado
        evento.setEstado(Evento.EstadoEvento.CANCELADO);

        return eventoRepository.save(evento);
    }

    // Finalizar un evento
    public Evento finalizarEvento(Long id, Usuario organizador) {

        // Buscar el evento comprobando que pertenece al organizador
        Evento evento = eventoRepository.findByIdAndOrganizador(id, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // Comprobar que el evento está publicado
        if (evento.getEstado() != Evento.EstadoEvento.PUBLICADO) {
            throw new ConflictoNegocioException(
                    "Solo se pueden finalizar eventos publicados.");
        }

        // Determinar la fecha a partir de la cual se puede finalizar
        LocalDateTime fechaLimite = evento.getFechaFin() != null
                ? evento.getFechaFin()
                : evento.getFechaInicio();

        // Comprobar que el evento ha alcanzado esa fecha
        if (LocalDateTime.now().isBefore(fechaLimite)) {
            throw new ConflictoNegocioException(
                    "El evento todavía no puede finalizarse.");
        }

        // Cambiar el estado del evento a finalizado
        evento.setEstado(Evento.EstadoEvento.FINALIZADO);

        return eventoRepository.save(evento);
    }

    // Retirar un evento publicado
    public Evento retirarEvento(Long id, Usuario organizador) {

        // Buscar el evento comprobando que pertenece al organizador
        Evento evento = eventoRepository.findByIdAndOrganizador(id, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // Comprobar que el evento esté publicado
        if (evento.getEstado() != Evento.EstadoEvento.PUBLICADO) {
            throw new ConflictoNegocioException(
                    "Solo se pueden retirar eventos publicados.");
        }

        // Cambiar el estado del evento a borrador
        evento.setEstado(Evento.EstadoEvento.BORRADOR);

        return eventoRepository.save(evento);
    }
}
