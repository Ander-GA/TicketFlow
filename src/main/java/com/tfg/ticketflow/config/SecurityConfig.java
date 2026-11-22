
package com.tfg.ticketflow.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos del frontend
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()

                // Gestión de eventos
                .requestMatchers(HttpMethod.GET, "/api/v1/eventos/mis-eventos").hasRole("ORGANIZADOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/eventos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/eventos").hasRole("ORGANIZADOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/eventos/**").hasRole("ORGANIZADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/eventos/*/publicar").hasRole("ORGANIZADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/eventos/*/cancelar").hasRole("ORGANIZADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/eventos/*/finalizar").hasRole("ORGANIZADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/eventos/*/retirar").hasRole("ORGANIZADOR")

                // Registro e inicio de sesión
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                // Swagger
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // El resto de peticiones requieren autenticación
                .anyRequest().authenticated()
            )

            // Desactivamos el formulario de login automático
            .formLogin(AbstractHttpConfigurer::disable)

            // Desactivamos también la autenticación HTTP Basic
            .httpBasic(AbstractHttpConfigurer::disable)

            // Desactivamos CSRF porque utilizamos autenticación mediante JWT
            .csrf(AbstractHttpConfigurer::disable)

            // No utilizamos sesiones HTTP para mantener la autenticación
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Las peticiones no autenticadas recibirán un error 401
            // en lugar de una redirección al formulario de login
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )

            // Configuramos la autenticación mediante JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

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
