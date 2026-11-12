package com.tfg.ticketflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/eventos/**").permitAll()
                // Registro de usuarios
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                // Swagger
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                .anyRequest().authenticated()
            )

            // Desactivamos el formulario de login automatico
            .formLogin(AbstractHttpConfigurer::disable)

            // Desactivamos tambien la autenticacion HTTP Basic
            .httpBasic(AbstractHttpConfigurer::disable)
            
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/usuarios/registro")
                .ignoringRequestMatchers("/api/v1/auth/login")
            )
            // Las peticiones no autenticadas recibiran un error 401
            // en lugar de una redireccion al formulario de login
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {}));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");

            return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        });

        return converter;
    }
}