package com.tfg.ticketflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.ticketflow.dto.reserva.ReservaCreacionDTO;
import com.tfg.ticketflow.dto.reserva.ReservaRespuestaDTO;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.service.ReservaService;
import com.tfg.ticketflow.service.UsuarioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioService usuarioService;

    public ReservaController(ReservaService reservaService, UsuarioService usuarioService) {
        this.reservaService = reservaService;
        this.usuarioService = usuarioService;
    }

    // Reintentos con la misma clave devuelven la reserva original, sin ampliar el plazo.
    @PostMapping
    public ResponseEntity<ReservaRespuestaDTO> crearReserva(
            @Valid @RequestBody ReservaCreacionDTO datos, Authentication autenticacion) {
        ReservaRespuestaDTO respuesta = reservaService.crearReserva(
                datos.getEventoId(), datos.getTipoEntradaId(),
                datos.getCantidad(), datos.getClaveIdempotencia(), autenticacion.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaRespuestaDTO>> obtenerMisReservas(
            Authentication autenticacion) {
        return ResponseEntity.ok(reservaService.obtenerMisReservas(obtenerUsuario(autenticacion)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaRespuestaDTO> obtenerReserva(
            @PathVariable Long id, Authentication autenticacion) {
        return ResponseEntity.ok(reservaService.obtenerMiReserva(id, obtenerUsuario(autenticacion)));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaRespuestaDTO> cancelarReserva(
            @PathVariable Long id, Authentication autenticacion) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id, obtenerUsuario(autenticacion)));
    }

    private Usuario obtenerUsuario(Authentication autenticacion) {
        return usuarioService.buscarPorEmail(autenticacion.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el usuario."));
    }
}
