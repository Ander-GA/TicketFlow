package com.tfg.ticketflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // Acceso publico a la pagina principal y errores
                .requestMatchers("/", "/error").permitAll()

                // Consulta publica de eventos
                .requestMatchers(HttpMethod.GET, "/api/v1/eventos", "/api/v1/eventos/**").permitAll()

                // El resto de operaciones requieren autenticacion
                .anyRequest().authenticated()
            )

            // Desactivamos el formulario de login automatico
            .formLogin(AbstractHttpConfigurer::disable)

            // Desactivamos tambien la autenticacion HTTP Basic
            .httpBasic(AbstractHttpConfigurer::disable)

            // Las peticiones no autenticadas recibiran un error 401
            // en lugar de una redireccion al formulario de login
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            );

        return http.build();
    }
}