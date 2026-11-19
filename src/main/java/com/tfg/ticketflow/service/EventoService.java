package com.tfg.ticketflow.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
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
    public Evento crearEvento(String titulo, String descripcion, LocalDateTime fechaInicio, LocalDateTime fechaFin, String ubicacion, String direccion, String imagen, Usuario organizador) {

        Evento evento = new Evento(titulo, descripcion, fechaInicio, fechaFin, ubicacion, direccion, imagen, organizador);

        return eventoRepository.save(evento);
    }

    // Obtener todos los eventos publicados
    public List<Evento> obtenerEventosPublicados() {
        return eventoRepository.findByEstado(Evento.EstadoEvento.PUBLICADO);
    }
    
    // Buscar un evento publicado por su identificador
    public Optional<Evento> buscarEventoPublicado(Long id) {

        Optional<Evento> evento = eventoRepository.findById(id);

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
    public Evento actualizarEvento(Long id, Usuario organizador, String titulo, String descripcion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin, String ubicacion,
            String direccion, String imagen) {

        // Buscar el evento comprobando que pertenece al organizador
        Evento evento = eventoRepository.findByIdAndOrganizador(id, organizador)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // Comprobar que el evento se puede modificar
        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO
                || evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new IllegalArgumentException("No se puede modificar un evento cancelado o finalizado.");
        }

        // Comprobar que la fecha de fin no sea anterior a la fecha de inicio
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio.");
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

}