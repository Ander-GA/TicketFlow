package com.tfg.ticketflow.dto.tipoentrada;

import jakarta.validation.constraints.NotNull;

public class TipoEntradaActivoDTO {
    @NotNull(message = "Debes indicar si el tipo de entrada está activo")
    private Boolean activo;

    public TipoEntradaActivoDTO() { }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
