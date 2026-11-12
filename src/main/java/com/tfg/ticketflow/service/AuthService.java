package com.tfg.ticketflow.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public Authentication iniciarSesion(String email, String password) {
        Authentication autenticacion =new UsernamePasswordAuthenticationToken(email, password);
        return authenticationManager.authenticate(autenticacion);
    }
}