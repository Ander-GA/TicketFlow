
package com.tfg.ticketflow.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tipos_entrada",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tipo_entrada_evento_nombre",
        columnNames = {"evento_id", "nombre"}
    )
)
public class TipoEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer cantidadTotal;

    @Column(nullable = false)
    private boolean activo;

    private LocalDateTime fechaInicioVenta;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean condicionesBloqueadas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Version
    private Long version;

    public TipoEntrada() {
    }

    public TipoEntrada(String nombre, String descripcion, BigDecimal precio,
            Integer cantidadTotal, Evento evento) {
        this(nombre, descripcion, precio, cantidadTotal, null, evento);
    }

    public TipoEntrada(String nombre, String descripcion, BigDecimal precio,
            Integer cantidadTotal, LocalDateTime fechaInicioVenta, Evento evento) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidadTotal = cantidadTotal;
        this.fechaInicioVenta = fechaInicioVenta;
        this.activo = true;
        this.evento = evento;
    }

    // Getters y setters

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getCantidadTotal() {
        return cantidadTotal;
    }

    public void setCantidadTotal(Integer cantidadTotal) {
        this.cantidadTotal = cantidadTotal;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaInicioVenta() {
        return fechaInicioVenta;
    }

    public void setFechaInicioVenta(LocalDateTime fechaInicioVenta) {
        this.fechaInicioVenta = fechaInicioVenta;
    }

    public boolean isCondicionesBloqueadas() {
        return condicionesBloqueadas;
    }

    public void bloquearCondiciones() {
        this.condicionesBloqueadas = true;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Long getVersion() {
        return version;
    }
}
