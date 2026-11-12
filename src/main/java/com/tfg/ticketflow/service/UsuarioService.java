
package com.tfg.ticketflow.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public Usuario registrarUsuario(String nombre, String apellidos, String email,
            String password, String telefono) {

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese email.");
        }

        String passwordHash = passwordEncoder.encode(password);

        // Si no se proporciona teléfono, guardamos null.
        String telefonoNormalizado = telefono == null || telefono.isBlank()
                ? null
                : telefono.trim();

        Usuario usuario = new Usuario(
                nombre,
                apellidos,
                emailNormalizado,
                passwordHash,
                Usuario.Rol.COMPRADOR,
                telefonoNormalizado
        );

        return usuarioRepository.save(usuario);
    }
    // Actualizar los datos personales del usuario
    public Usuario actualizarPerfil(Usuario usuario, String nombre, String apellidos, String telefono) {

        usuario.setNombre(nombre.trim());
        usuario.setApellidos(apellidos.trim());

        // Si no se proporciona teléfono, guardamos null.
        String telefonoNormalizado = telefono == null || telefono.isBlank()
                ? null
                : telefono.trim();

        usuario.setTelefono(telefonoNormalizado);

        return usuarioRepository.save(usuario);
    }

    // Cambiar la contraseña del usuario
    public void cambiarPassword(Usuario usuario, String passwordActual, String passwordNueva) {

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
        }

        String nuevoPasswordHash = passwordEncoder.encode(passwordNueva);

        usuario.setPasswordHash(nuevoPasswordHash);

        usuarioRepository.save(usuario);
    }
}
