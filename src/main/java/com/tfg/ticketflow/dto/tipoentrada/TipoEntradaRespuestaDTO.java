package com.tfg.ticketflow.dto.tipoentrada;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tfg.ticketflow.entity.TipoEntrada;

public class TipoEntradaRespuestaDTO {
    private Long id;
    private Long eventoId;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer cantidadTotal;
    private boolean activo;
    private LocalDateTime fechaInicioVenta;
    private LocalDateTime fechaInicioVentaEfectiva;
    private boolean condicionesBloqueadas;

    public TipoEntradaRespuestaDTO(TipoEntrada tipo) {
        this.id = tipo.getId();
        this.eventoId = tipo.getEvento().getId();
        this.nombre = tipo.getNombre();
        this.descripcion = tipo.getDescripcion();
        this.precio = tipo.getPrecio();
        this.cantidadTotal = tipo.getCantidadTotal();
        this.activo = tipo.isActivo();
        this.fechaInicioVenta = tipo.getFechaInicioVenta();
        this.fechaInicioVentaEfectiva = tipo.getFechaInicioVenta() != null
                ? tipo.getFechaInicioVenta()
                : tipo.getEvento().getFechaInicioVentaGeneral();
        this.condicionesBloqueadas = tipo.isCondicionesBloqueadas();
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getCantidadTotal() { return cantidadTotal; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaInicioVenta() { return fechaInicioVenta; }
    public LocalDateTime getFechaInicioVentaEfectiva() { return fechaInicioVentaEfectiva; }
    public boolean isCondicionesBloqueadas() { return condicionesBloqueadas; }
}
