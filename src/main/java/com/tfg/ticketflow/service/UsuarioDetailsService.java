package com.tfg.ticketflow.service;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("No existe ningún usuario con ese email."));

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().name())
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}