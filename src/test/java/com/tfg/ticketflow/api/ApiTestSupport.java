package com.tfg.ticketflow.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.TipoEntradaRepository;
import com.tfg.ticketflow.repository.UsuarioRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
abstract class ApiTestSupport {

    static final String LOGIN = "/api/v1/auth/login";
    static final String EVENTOS = "/api/v1/eventos";
    static final String PASSWORD = "TicketflowPruebas123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired EventoRepository eventos;
    @Autowired TipoEntradaRepository tiposEntrada;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void limpiarBaseDePruebas() {
        tiposEntrada.deleteAllInBatch();
        eventos.deleteAllInBatch();
        usuarios.deleteAllInBatch();
    }

    Usuario guardarUsuario(String email, Usuario.Rol rol) {
        return usuarios.save(new Usuario("Usuario", "De pruebas", email,
                passwordEncoder.encode(PASSWORD), rol, null));
    }

    Evento guardarEvento(Usuario organizador, String titulo, String ubicacion,
            Evento.EstadoEvento estado, LocalDateTime inicio, LocalDateTime fin, Integer aforo) {
        Evento evento = new Evento(titulo, "Descripción", inicio, fin, ubicacion, "Dirección",
                null, aforo, organizador);
        evento.setEstado(estado);
        return eventos.save(evento);
    }

    String iniciarSesion(String email, String password) throws Exception {
        byte[] respuesta = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return "Bearer " + json.readTree(respuesta).get("accessToken").stringValue();
    }
}
