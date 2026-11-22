
package com.tfg.ticketflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.ticketflow.dto.evento.EventoActualizacionDTO;
import com.tfg.ticketflow.dto.evento.EventoCreacionDTO;
import com.tfg.ticketflow.dto.evento.EventoRespuestaDTO;
import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.service.EventoService;
import com.tfg.ticketflow.service.UsuarioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/eventos")
public class EventoController {

        private final EventoService eventoService;
        private final UsuarioService usuarioService;

        public EventoController(EventoService eventoService, UsuarioService usuarioService) {
                this.eventoService = eventoService;
                this.usuarioService = usuarioService;
        }

        // Crear un evento
        @SecurityRequirement(name = "bearerAuth")
        @PostMapping
        public ResponseEntity<EventoRespuestaDTO> crearEvento(@Valid @RequestBody EventoCreacionDTO datos, Authentication autenticacion) {

                // Recuperamos el organizador a partir del usuario autenticado
                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                // Creamos el evento
                Evento evento = eventoService.crearEvento(
                        datos.getTitulo(),
                        datos.getDescripcion(),
                        datos.getFechaInicio(),
                        datos.getFechaFin(),
                        datos.getUbicacion(),
                        datos.getDireccion(),
                        datos.getImagen(),
                        organizador
                );

                // Convertimos la entidad en DTO de respuesta
                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        }

        // Obtener eventos publicados aplicando filtros opcionales
        @GetMapping
        public ResponseEntity<List<EventoRespuestaDTO>> obtenerEventosPublicados(
                @RequestParam(required = false) String titulo,
                @RequestParam(required = false) String ubicacion) {

                List<EventoRespuestaDTO> eventos = eventoService.obtenerEventosPublicados(titulo, ubicacion)
                        .stream()
                        .map(EventoRespuestaDTO::new)
                        .toList();

                return ResponseEntity.ok(eventos);
        }

    // Obtener un evento publicado por su identificador
        @GetMapping("/{id}")
        public ResponseEntity<EventoRespuestaDTO> obtenerEvento(@PathVariable Long id) {

                Evento evento = eventoService.buscarEventoPublicado(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "No se ha encontrado el evento."));

                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.ok(respuesta);
        }
        // Obtener todos los eventos del organizador autenticado
        @SecurityRequirement(name = "bearerAuth")
        @GetMapping("/mis-eventos")
        public ResponseEntity<List<EventoRespuestaDTO>> obtenerMisEventos(Authentication autenticacion) {

                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                List<EventoRespuestaDTO> eventos = eventoService.obtenerEventosDelOrganizador(organizador)
                        .stream()
                        .map(EventoRespuestaDTO::new)
                        .toList();

                return ResponseEntity.ok(eventos);
        }

        // Modificar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PutMapping("/{id}")
        public ResponseEntity<EventoRespuestaDTO> actualizarEvento(@PathVariable Long id, @Valid @RequestBody EventoActualizacionDTO datos, Authentication autenticacion) {

                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                Evento evento = eventoService.actualizarEvento(
                        id,
                        organizador,
                        datos.getTitulo(),
                        datos.getDescripcion(),
                        datos.getFechaInicio(),
                        datos.getFechaFin(),
                        datos.getUbicacion(),
                        datos.getDireccion(),
                        datos.getImagen()
                );

                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.ok(respuesta);
        }

        // Publicar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/publicar")
        public ResponseEntity<EventoRespuestaDTO> publicarEvento(@PathVariable Long id, Authentication autenticacion) {

                // Recuperar el organizador autenticado
                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                // Publicar el evento
                Evento evento = eventoService.publicarEvento(id, organizador);

                // Convertir la entidad en DTO de respuesta
                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.ok(respuesta);
        }

        // Cancelar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/cancelar")
        public ResponseEntity<EventoRespuestaDTO> cancelarEvento(@PathVariable Long id, Authentication autenticacion) {

                // Recuperar el organizador autenticado
                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                // Cancelar el evento
                Evento evento = eventoService.cancelarEvento(id, organizador);

                // Convertir la entidad en DTO de respuesta
                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.ok(respuesta);
        }

        // Finalizar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/finalizar")
        public ResponseEntity<EventoRespuestaDTO> finalizarEvento(@PathVariable Long id, Authentication autenticacion) {

                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                Evento evento = eventoService.finalizarEvento(id, organizador);

                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.ok(respuesta);
        }

        // Retirar un evento publicado
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/retirar")
        public ResponseEntity<EventoRespuestaDTO> retirarEvento(@PathVariable Long id, Authentication autenticacion) {

                Usuario organizador = usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario."));

                Evento evento = eventoService.retirarEvento(id, organizador);

                EventoRespuestaDTO respuesta = new EventoRespuestaDTO(evento);

                return ResponseEntity.ok(respuesta);
        }
}
