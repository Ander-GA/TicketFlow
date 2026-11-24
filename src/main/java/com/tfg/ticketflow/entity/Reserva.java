package com.tfg.ticketflow.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "reservas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reserva_usuario_idempotencia",
                columnNames = { "usuario_id", "clave_idempotencia_hash" }),
        indexes = {
                @Index(name = "idx_reserva_tipo_estado_expiracion",
                        columnList = "tipo_entrada_id, estado, fecha_expiracion"),
                @Index(name = "idx_reserva_evento_estado",
                        columnList = "evento_id, estado"),
                @Index(name = "idx_reserva_estado_expiracion_evento",
                        columnList = "estado, fecha_expiracion, evento_id")
        })
public class Reserva {

    public enum EstadoReserva {
        ACTIVA, CADUCADA, CANCELADA, CONFIRMADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_entrada_id", nullable = false)
    private TipoEntrada tipoEntrada;

    @Column(nullable = false)
    private Integer cantidad;

    // Instant evita errores por cambios de horario de verano en reservas de 10 minutos.
    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    // Precio congelado para el siguiente módulo de compras simuladas.
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "importe_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;

    // SHA-256 mantiene la comparación exacta con cualquier colación de la base de datos.
    @Column(name = "clave_idempotencia_hash", nullable = false, length = 64)
    private String claveIdempotenciaHash;

    public Reserva() {
    }

    public Reserva(Usuario usuario, Evento evento, TipoEntrada tipoEntrada,
            Integer cantidad, Instant fechaCreacion, Instant fechaExpiracion,
            BigDecimal precioUnitario, String claveIdempotenciaHash) {
        this.usuario = usuario;
        this.evento = evento;
        this.tipoEntrada = tipoEntrada;
        this.cantidad = cantidad;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.precioUnitario = precioUnitario;
        this.importeTotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        this.claveIdempotenciaHash = claveIdempotenciaHash;
        this.estado = EstadoReserva.ACTIVA;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Evento getEvento() { return evento; }
    public TipoEntrada getTipoEntrada() { return tipoEntrada; }
    public Integer getCantidad() { return cantidad; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaExpiracion() { return fechaExpiracion; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getImporteTotal() { return importeTotal; }
}
