package com.tfg.ticketflow.dto.reserva;

import java.math.BigDecimal;
import java.time.Instant;

import com.tfg.ticketflow.entity.Reserva;
import com.tfg.ticketflow.entity.Reserva.EstadoReserva;

public class ReservaRespuestaDTO {

    private final Long id;
    private final Long eventoId;
    private final Long tipoEntradaId;
    private final String nombreTipoEntrada;
    private final Integer cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal importeTotal;
    private final Instant fechaCreacion;
    private final Instant fechaExpiracion;
    private final EstadoReserva estado;

    public ReservaRespuestaDTO(Reserva reserva, Instant ahora) {
        this.id = reserva.getId();
        this.eventoId = reserva.getEvento().getId();
        this.tipoEntradaId = reserva.getTipoEntrada().getId();
        this.nombreTipoEntrada = reserva.getTipoEntrada().getNombre();
        this.cantidad = reserva.getCantidad();
        this.precioUnitario = reserva.getPrecioUnitario();
        this.importeTotal = reserva.getImporteTotal();
        this.fechaCreacion = reserva.getFechaCreacion();
        this.fechaExpiracion = reserva.getFechaExpiracion();
        this.estado = reserva.getEstado() == EstadoReserva.ACTIVA
                && !reserva.getFechaExpiracion().isAfter(ahora)
                ? EstadoReserva.CADUCADA : reserva.getEstado();
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public Long getTipoEntradaId() { return tipoEntradaId; }
    public String getNombreTipoEntrada() { return nombreTipoEntrada; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getImporteTotal() { return importeTotal; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaExpiracion() { return fechaExpiracion; }
    public EstadoReserva getEstado() { return estado; }
}
