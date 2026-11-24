package com.tfg.ticketflow.dto.reserva;

import java.time.LocalDateTime;

public class DisponibilidadTipoEntradaDTO {

    private final Long eventoId;
    private final Long tipoEntradaId;
    private final Integer cantidadTotal;
    private final Long plazasComprometidas;
    private final Long plazasDisponibles;
    private final LocalDateTime fechaInicioVenta;
    private final boolean ventaAbierta;

    public DisponibilidadTipoEntradaDTO(Long eventoId, Long tipoEntradaId,
            Integer cantidadTotal, Long plazasComprometidas,
            Long plazasDisponibles, LocalDateTime fechaInicioVenta,
            boolean ventaAbierta) {
        this.eventoId = eventoId;
        this.tipoEntradaId = tipoEntradaId;
        this.cantidadTotal = cantidadTotal;
        this.plazasComprometidas = plazasComprometidas;
        this.plazasDisponibles = plazasDisponibles;
        this.fechaInicioVenta = fechaInicioVenta;
        this.ventaAbierta = ventaAbierta;
    }

    public Long getEventoId() { return eventoId; }
    public Long getTipoEntradaId() { return tipoEntradaId; }
    public Integer getCantidadTotal() { return cantidadTotal; }
    public Long getPlazasComprometidas() { return plazasComprometidas; }
    public Long getPlazasDisponibles() { return plazasDisponibles; }
    public LocalDateTime getFechaInicioVenta() { return fechaInicioVenta; }
    public boolean isVentaAbierta() { return ventaAbierta; }
}
