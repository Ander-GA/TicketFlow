
package com.tfg.ticketflow;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.UsuarioRepository;

@Component
@Profile("local")
public class DataInitializer implements CommandLineRunner {

        private final UsuarioRepository usuarioRepository;
        private final EventoRepository eventoRepository;
        private final PasswordEncoder passwordEncoder;

        public DataInitializer(UsuarioRepository usuarioRepository, EventoRepository eventoRepository, PasswordEncoder passwordEncoder) {
                this.usuarioRepository = usuarioRepository;
                this.eventoRepository = eventoRepository;
                this.passwordEncoder = passwordEncoder;
        }

        @Override
        public void run(String... args) {

                // Crear usuarios de prueba
                crearUsuarioSiNoExiste(
                        "Administrador", "TicketFlow",
                        "admin@ticketflow.com", "TicketFlow123!",
                        Usuario.Rol.ADMIN, "600111111"
                );

                crearUsuarioSiNoExiste(
                        "Ander", "Gonzalez Alonso",
                        "ander@ticketflow.com", "TicketFlow123!",
                        Usuario.Rol.COMPRADOR, "600222222"
                );

                crearUsuarioSiNoExiste(
                        "Organizador", "Pruebas",
                        "organizador@ticketflow.com", "TicketFlow123!",
                        Usuario.Rol.ORGANIZADOR, null
                );

                crearUsuarioSiNoExiste(
                        "Validador", "Pruebas",
                        "validador@ticketflow.com", "TicketFlow123!",
                        Usuario.Rol.VALIDADOR, null
                );

                // Recuperar el organizador de prueba
                Usuario organizador = usuarioRepository.findByEmail("organizador@ticketflow.com")
                        .orElseThrow(() -> new IllegalStateException(
                                "No se ha encontrado el organizador de prueba."
                        ));

                // Crear eventos de prueba
                crearEventoSiNoExiste(
                        "Concierto de música en directo",
                        "Una noche de música en directo con diferentes artistas.",
                        LocalDateTime.now().plusDays(7).withHour(20).withMinute(0).withSecond(0).withNano(0),
                        null,
                        "Bilbao",
                        "Bilbao Arena",
                        null,
                        organizador,
                        Evento.EstadoEvento.PUBLICADO
                );

                crearEventoSiNoExiste(
                        "Festival de verano",
                        "Festival con música, actividades y actuaciones en directo.",
                        LocalDateTime.now().plusDays(14).withHour(18).withMinute(0).withSecond(0).withNano(0),
                        null,
                        "Donostia",
                        "Plaza de la Trinidad",
                        null,
                        organizador,
                        Evento.EstadoEvento.PUBLICADO
                );

                crearEventoSiNoExiste(
                        "Conferencia de tecnología",
                        "Encuentro dedicado a las nuevas tendencias tecnológicas.",
                        LocalDateTime.now().plusDays(21).withHour(10).withMinute(0).withSecond(0).withNano(0),
                        null,
                        "Madrid",
                        "Palacio de Congresos",
                        null,
                        organizador,
                        Evento.EstadoEvento.PUBLICADO
                );

                crearEventoSiNoExiste(
                        "Torneo de videojuegos",
                        "Competición presencial de videojuegos con diferentes categorías.",
                        LocalDateTime.now().plusDays(30).withHour(16).withMinute(0).withSecond(0).withNano(0),
                        null,
                        "Bilbao",
                        "BEC - Barakaldo",
                        null,
                        organizador,
                        Evento.EstadoEvento.PUBLICADO
                );

                crearEventoSiNoExiste(
                        "Evento en preparación",
                        "Este evento todavía no está disponible para el público.",
                        LocalDateTime.now().plusDays(10),
                        null,
                        "Bilbao",
                        "Lugar pendiente de confirmar",
                        null,
                        organizador,
                        Evento.EstadoEvento.BORRADOR
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

        private void crearEventoSiNoExiste(String titulo,
                String descripcion,
                LocalDateTime fechaInicio,
                LocalDateTime fechaFin,
                String ubicacion,
                String direccion,
                String imagen,
                Usuario organizador,
                Evento.EstadoEvento estado) {

                if (eventoRepository.existsByTituloAndOrganizador(titulo, organizador)) {
                        return;
                }

                Evento evento = new Evento(
                        titulo,
                        descripcion,
                        fechaInicio,
                        fechaFin,
                        ubicacion,
                        direccion,
                        imagen,
                        organizador
                );

                evento.setEstado(estado);

                eventoRepository.save(evento);
        }
}
