
package com.tfg.ticketflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tfg.ticketflow.dto.evento.EventoActualizacionDTO;
import com.tfg.ticketflow.dto.evento.EventoCreacionDTO;
import com.tfg.ticketflow.dto.evento.EventoRespuestaDTO;
import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.service.EventoService;
import com.tfg.ticketflow.service.UsuarioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/eventos")
public class EventoController {

        private final EventoService eventoService;
        private final UsuarioService usuarioService;

        public EventoController(
                EventoService eventoService,
                UsuarioService usuarioService) {

                this.eventoService = eventoService;
                this.usuarioService = usuarioService;
        }

        // Crear un evento
        @SecurityRequirement(name = "bearerAuth")
        @PostMapping
        public ResponseEntity<EventoRespuestaDTO> crearEvento(
                @Valid @RequestBody EventoCreacionDTO datos,
                Authentication autenticacion) {

                Usuario organizador =
                        usuarioService.buscarPorEmail(autenticacion.getName())
                                .orElseThrow(() ->
                                        new RecursoNoEncontradoException(
                                                "No se ha encontrado el usuario."));

                Evento evento = eventoService.crearEvento(
                        datos.getTitulo(),
                        datos.getDescripcion(),
                        datos.getFechaInicio(),
                        datos.getFechaFin(),
                        datos.getUbicacion(),
                        datos.getDireccion(),
                        datos.getAforoMaximo(),
                        organizador
                );

                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new EventoRespuestaDTO(evento));
        }

        // Consultar los eventos publicados
        @GetMapping
        public ResponseEntity<List<EventoRespuestaDTO>> obtenerEventosPublicados(
                @RequestParam(required = false) String titulo,
                @RequestParam(required = false) String ubicacion) {

                List<EventoRespuestaDTO> eventos =
                        eventoService.obtenerEventosPublicados(titulo, ubicacion)
                                .stream()
                                .map(EventoRespuestaDTO::new)
                                .toList();

                return ResponseEntity.ok(eventos);
        }

        // Consultar el detalle público de un evento
        @GetMapping("/{id}")
        public ResponseEntity<EventoRespuestaDTO> obtenerEvento(
                @PathVariable Long id) {

                Evento evento = eventoService.buscarEventoPublicado(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No se ha encontrado el evento."));

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Consultar los eventos del organizador
        @SecurityRequirement(name = "bearerAuth")
        @GetMapping("/mis-eventos")
        public ResponseEntity<List<EventoRespuestaDTO>> obtenerMisEventos(
                Authentication autenticacion) {

                Usuario organizador =
                        usuarioService.buscarPorEmail(autenticacion.getName())
                                .orElseThrow(() ->
                                        new RecursoNoEncontradoException(
                                                "No se ha encontrado el usuario."));

                List<EventoRespuestaDTO> eventos =
                        eventoService.obtenerEventosDelOrganizador(organizador)
                                .stream()
                                .map(EventoRespuestaDTO::new)
                                .toList();

                return ResponseEntity.ok(eventos);
        }

        // Actualizar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PutMapping("/{id}")
        public ResponseEntity<EventoRespuestaDTO> actualizarEvento(
                @PathVariable Long id,
                @Valid @RequestBody EventoActualizacionDTO datos,
                Authentication autenticacion) {

                Usuario organizador =
                        usuarioService.buscarPorEmail(autenticacion.getName())
                                .orElseThrow(() ->
                                        new RecursoNoEncontradoException(
                                                "No se ha encontrado el usuario."));

                Evento evento = eventoService.actualizarEvento(
                        id,
                        organizador,
                        datos.getTitulo(),
                        datos.getDescripcion(),
                        datos.getFechaInicio(),
                        datos.getFechaFin(),
                        datos.getUbicacion(),
                        datos.getDireccion(),
                        datos.getAforoMaximo()
                );

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Publicar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/publicar")
        public ResponseEntity<EventoRespuestaDTO> publicarEvento(
                @PathVariable Long id,
                Authentication autenticacion) {

                Usuario organizador =
                        obtenerOrganizador(autenticacion);

                Evento evento =
                        eventoService.publicarEvento(id, organizador);

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Cancelar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/cancelar")
        public ResponseEntity<EventoRespuestaDTO> cancelarEvento(
                @PathVariable Long id,
                Authentication autenticacion) {

                Usuario organizador =
                        obtenerOrganizador(autenticacion);

                Evento evento =
                        eventoService.cancelarEvento(id, organizador);

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Finalizar un evento
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/finalizar")
        public ResponseEntity<EventoRespuestaDTO> finalizarEvento(
                @PathVariable Long id,
                Authentication autenticacion) {

                Usuario organizador =
                        obtenerOrganizador(autenticacion);

                Evento evento =
                        eventoService.finalizarEvento(id, organizador);

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Retirar un evento publicado
        @SecurityRequirement(name = "bearerAuth")
        @PatchMapping("/{id}/retirar")
        public ResponseEntity<EventoRespuestaDTO> retirarEvento(
                @PathVariable Long id,
                Authentication autenticacion) {

                Usuario organizador =
                        obtenerOrganizador(autenticacion);

                Evento evento =
                        eventoService.retirarEvento(id, organizador);

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Recuperar el organizador autenticado
        private Usuario obtenerOrganizador(Authentication autenticacion) {

                return usuarioService.buscarPorEmail(autenticacion.getName())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No se ha encontrado el usuario."));
        }
}
