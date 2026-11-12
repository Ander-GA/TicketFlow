package com.tfg.ticketflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tfg.ticketflow.dto.usuario.AuthRespuestaDTO;
import com.tfg.ticketflow.dto.usuario.UsuarioLoginDTO;
import com.tfg.ticketflow.dto.usuario.UsuarioRespuestaDTO;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.service.AuthService;
import com.tfg.ticketflow.service.JwtService;
import com.tfg.ticketflow.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService,JwtService jwtService,UsuarioService usuarioService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRespuestaDTO> iniciarSesion(@Valid @RequestBody UsuarioLoginDTO datos) {
        Authentication autenticacion = authService.iniciarSesion(datos.getEmail(),datos.getPassword());
        Usuario usuario = usuarioService.buscarPorEmail(autenticacion.getName()).orElseThrow();
        String token = jwtService.generarToken(usuario);
        
        AuthRespuestaDTO respuesta = new AuthRespuestaDTO(token,new UsuarioRespuestaDTO(usuario));

        return ResponseEntity.ok(respuesta);
    }
}