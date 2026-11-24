package com.tfg.ticketflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.ticketflow.dto.reserva.DisponibilidadTipoEntradaDTO;
import com.tfg.ticketflow.service.ReservaService;

@RestController
public class DisponibilidadController {

    private final ReservaService reservaService;

    public DisponibilidadController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // Solo expone inventario de modalidades activas de eventos publicados.
    @GetMapping("/api/v1/eventos/{eventoId}/tipos-entrada/{tipoId}/disponibilidad")
    public ResponseEntity<DisponibilidadTipoEntradaDTO> consultar(
            @PathVariable Long eventoId, @PathVariable Long tipoId) {
        return ResponseEntity.ok(reservaService.consultarDisponibilidad(eventoId, tipoId));
    }
}
