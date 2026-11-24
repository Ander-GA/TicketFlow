
package com.tfg.ticketflow.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    @Column(length = 255)
    private String ubicacion;

    @Column(length = 255)
    private String direccion;

    @Column(length = 500)
    private String imagen;

    // Temporalmente nullable por los eventos existentes en MySQL
    @Column(name = "aforo_maximo")
    private Integer aforoMaximo;
    private LocalDateTime fechaInicioVentaGeneral;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    public enum EstadoEvento {
        BORRADOR,
        PUBLICADO,
        CANCELADO,
        FINALIZADO
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    public Evento() {
    }

    public Evento(String titulo, String descripcion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String ubicacion, String direccion, String imagen,
            Integer aforoMaximo, Usuario organizador) {

        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.ubicacion = ubicacion;
        this.direccion = direccion;
        this.imagen = imagen;
        this.aforoMaximo = aforoMaximo;
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoEvento.BORRADOR;
        this.organizador = organizador;
    }

    // Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Integer getAforoMaximo() {
        return aforoMaximo;
    }

    public void setAforoMaximo(Integer aforoMaximo) {
        this.aforoMaximo = aforoMaximo;
    }

    public LocalDateTime getFechaInicioVentaGeneral() {
        return fechaInicioVentaGeneral;
    }

    public void setFechaInicioVentaGeneral(LocalDateTime fechaInicioVentaGeneral) {
        this.fechaInicioVentaGeneral = fechaInicioVentaGeneral;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public void setEstado(EstadoEvento estado) {
        this.estado = estado;
    }

    public Usuario getOrganizador() {
        return organizador;
    }

    public void setOrganizador(Usuario organizador) {
        this.organizador = organizador;
    }
}
