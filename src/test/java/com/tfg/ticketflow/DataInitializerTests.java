package com.tfg.ticketflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.UsuarioRepository;

class DataInitializerTests {

    @Test
    void respetaLosUsuariosYEventosExistentesEnLaInicializacionLocal() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        EventoRepository eventos = mock(EventoRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Usuario organizador = new Usuario();
        when(usuarios.existsByEmail(anyString())).thenReturn(true);
        when(usuarios.findByEmail("organizador@ticketflow.com")).thenReturn(Optional.of(organizador));
        when(eventos.existsByTituloAndOrganizador(anyString(), eq(organizador))).thenReturn(true);

        new DataInitializer(usuarios, eventos, passwordEncoder).run();

        verify(usuarios, times(4)).existsByEmail(anyString());
        verify(eventos, times(5)).existsByTituloAndOrganizador(anyString(), eq(organizador));
        verify(usuarios, never()).save(any());
        verify(eventos, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
