package com.tfg.ticketflow.dto.reserva;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReservaCreacionDTO {

    // Se incluye eventoId para bloquear la fila del evento ANTES de consultar inventario.
    @NotNull(message = "El evento es obligatorio")
    private Long eventoId;

    @NotNull(message = "El tipo de entrada es obligatorio")
    private Long tipoEntradaId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Max(value = 10, message = "No se permiten más de 10 entradas por reserva")
    private Integer cantidad;

    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Size(min = 16, max = 80, message = "La clave de idempotencia debe tener entre 16 y 80 caracteres")
    private String claveIdempotencia;

    public ReservaCreacionDTO() {
    }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public Long getTipoEntradaId() { return tipoEntradaId; }
    public void setTipoEntradaId(Long tipoEntradaId) { this.tipoEntradaId = tipoEntradaId; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
}
