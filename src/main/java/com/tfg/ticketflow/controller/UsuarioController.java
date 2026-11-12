package com.tfg.ticketflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tfg.ticketflow.dto.usuario.UsuarioCambioPasswordDTO;
import com.tfg.ticketflow.dto.usuario.UsuarioPerfilActualizacionDTO;
import com.tfg.ticketflow.dto.usuario.UsuarioRegistroDTO;
import com.tfg.ticketflow.dto.usuario.UsuarioRespuestaDTO;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.service.UsuarioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioRespuestaDTO> registrarUsuario(@Valid @RequestBody UsuarioRegistroDTO datos) {

        Usuario usuario = usuarioService.registrarUsuario(
            datos.getNombre(),
            datos.getApellidos(),
            datos.getEmail(),
            datos.getPassword(),
            datos.getTelefono()
    );
        UsuarioRespuestaDTO respuesta = new UsuarioRespuestaDTO(usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioRespuestaDTO> obtenerPerfil(Authentication autenticacion) {

        Usuario usuario = usuarioService.buscarPorEmail(
                autenticacion.getName()
        ).orElseThrow();

        return ResponseEntity.ok(new UsuarioRespuestaDTO(usuario));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/perfil")
    public ResponseEntity<UsuarioRespuestaDTO> actualizarPerfil(
            Authentication autenticacion,
            @Valid @RequestBody UsuarioPerfilActualizacionDTO datos) {

        Usuario usuario = usuarioService.buscarPorEmail(
                autenticacion.getName()
        ).orElseThrow();

        Usuario usuarioActualizado = usuarioService.actualizarPerfil(
                usuario,
                datos.getNombre(),
                datos.getApellidos(),
                datos.getTelefono()
        );

        return ResponseEntity.ok(new UsuarioRespuestaDTO(usuarioActualizado));
    }
    
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/perfil/password")
    public ResponseEntity<Void> cambiarPassword(
            Authentication autenticacion,
            @Valid @RequestBody UsuarioCambioPasswordDTO datos) {

        Usuario usuario = usuarioService.buscarPorEmail(
                autenticacion.getName()
        ).orElseThrow();

        usuarioService.cambiarPassword(
                usuario,
                datos.getPasswordActual(),
                datos.getPasswordNueva()
        );

        return ResponseEntity.noContent().build();
    }
}