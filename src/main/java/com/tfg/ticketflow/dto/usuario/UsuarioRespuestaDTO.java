
package com.tfg.ticketflow.dto.usuario;

import com.tfg.ticketflow.entity.Usuario;
import java.time.LocalDateTime;

public class UsuarioRespuestaDTO {

    private Long id;
    private String nombre;
    private String apellidos;
    private String email;
    private String telefono;
    private Usuario.Rol rol;
    private LocalDateTime fechaRegistro;

    public UsuarioRespuestaDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.apellidos = usuario.getApellidos();
        this.email = usuario.getEmail();
        this.telefono = usuario.getTelefono();
        this.rol = usuario.getRol();
        this.fechaRegistro = usuario.getFechaRegistro();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public Usuario.Rol getRol() {
        return rol;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
