
package com.tfg.ticketflow.dto.evento;

import java.time.LocalDateTime;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Evento.EstadoEvento;

public class EventoRespuestaDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String ubicacion;
    private String direccion;
    private String imagen;
    private Integer aforoMaximo;
    private LocalDateTime fechaCreacion;
    private EstadoEvento estado;
    private Long organizadorId;
    private String organizadorNombre;

    public EventoRespuestaDTO(Evento evento) {
        this.id = evento.getId();
        this.titulo = evento.getTitulo();
        this.descripcion = evento.getDescripcion();
        this.fechaInicio = evento.getFechaInicio();
        this.fechaFin = evento.getFechaFin();
        this.ubicacion = evento.getUbicacion();
        this.direccion = evento.getDireccion();
        this.imagen = evento.getImagen();
        this.aforoMaximo = evento.getAforoMaximo();
        this.fechaCreacion = evento.getFechaCreacion();
        this.estado = evento.getEstado();
        this.organizadorId = evento.getOrganizador().getId();
        this.organizadorNombre = evento.getOrganizador().getNombre();
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

    public Long getOrganizadorId() {
        return organizadorId;
    }

    public void setOrganizadorId(Long organizadorId) {
        this.organizadorId = organizadorId;
    }

    public String getOrganizadorNombre() {
        return organizadorNombre;
    }

    public void setOrganizadorNombre(String organizadorNombre) {
        this.organizadorNombre = organizadorNombre;
    }
}
