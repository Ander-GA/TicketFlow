
package com.tfg.ticketflow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.repository.UsuarioRepository;

@Component
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        crearUsuarioSiNoExiste(
                "Administrador",
                "TicketFlow",
                "admin@ticketflow.com",
                "TicketFlow123!",
                Usuario.Rol.ADMIN,
                "600111111"
        );

        crearUsuarioSiNoExiste(
                "Ander",
                "Gonzalez Alonso",
                "ander@ticketflow.com",
                "TicketFlow123!",
                Usuario.Rol.COMPRADOR,
                "600222222"
        );

        crearUsuarioSiNoExiste(
                "Organizador",
                "Pruebas",
                "organizador@ticketflow.com",
                "TicketFlow123!",
                Usuario.Rol.ORGANIZADOR,
                null
        );

        crearUsuarioSiNoExiste(
                "Validador",
                "Pruebas",
                "validador@ticketflow.com",
                "TicketFlow123!",
                Usuario.Rol.VALIDADOR,
                null
        );
    }

    private void crearUsuarioSiNoExiste(String nombre,
                                        String apellidos,
                                        String email,
                                        String password,
                                        Usuario.Rol rol,
                                        String telefono) {

        if (usuarioRepository.existsByEmail(email)) {
            return;
        }

        String passwordHash = passwordEncoder.encode(password);

        Usuario usuario = new Usuario(
                nombre,
                apellidos,
                email,
                passwordHash,
                rol,
                telefono
        );

        // Los usuarios de prueba tendrán el correo verificado.
        usuario.setEmailVerificado(true);

        usuarioRepository.save(usuario);
    }
}
