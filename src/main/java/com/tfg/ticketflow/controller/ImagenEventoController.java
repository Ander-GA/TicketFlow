
package com.tfg.ticketflow.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tfg.ticketflow.dto.evento.EventoRespuestaDTO;
import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.service.ImagenEventoService;
import com.tfg.ticketflow.service.UsuarioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImagenEventoController {

        private final ImagenEventoService imagenEventoService;
        private final UsuarioService usuarioService;

        public ImagenEventoController(
                ImagenEventoService imagenEventoService,
                UsuarioService usuarioService) {

                this.imagenEventoService = imagenEventoService;
                this.usuarioService = usuarioService;
        }

        // Subir o sustituir la imagen de un evento
        @SecurityRequirement(name = "bearerAuth")
        @PostMapping(
                value = "/api/v1/eventos/{id}/imagen",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        public ResponseEntity<EventoRespuestaDTO> subirImagen(
                @PathVariable Long id,
                @RequestPart("archivo") MultipartFile archivo,
                Authentication autenticacion) {

                Usuario organizador =
                        usuarioService.buscarPorEmail(autenticacion.getName())
                                .orElseThrow(() ->
                                        new RecursoNoEncontradoException(
                                                "No se ha encontrado el usuario."));

                Evento evento = imagenEventoService.subirImagen(
                        id, organizador, archivo);

                return ResponseEntity.ok(new EventoRespuestaDTO(evento));
        }

        // Consultar públicamente una imagen
        @GetMapping("/api/v1/imagenes/eventos/{nombre}")
        public ResponseEntity<Resource> obtenerImagen(
                @PathVariable String nombre) {

                Resource imagen = imagenEventoService.obtenerImagen(nombre);

                MediaType tipoContenido = nombre.endsWith(".png")
                        ? MediaType.IMAGE_PNG
                        : MediaType.IMAGE_JPEG;

                return ResponseEntity.ok()
                        .contentType(tipoContenido)
                        .body(imagen);
        }
}
