package com.tfg.ticketflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.ResultActions;

import com.tfg.ticketflow.entity.Usuario;

class UsuariosApiTests extends ApiTestSupport {

    private static final String REGISTRO = "/api/v1/usuarios/registro";
    private static final String PERFIL = "/api/v1/usuarios/perfil";
    private static final String EMAIL = "comprador@example.test";

    @Autowired JwtDecoder jwtDecoder;
    @Autowired JwtEncoder jwtEncoder;

    @Test
    void registraComoCompradorYEmiteUnJwtValido() throws Exception {
        mvc.perform(post(REGISTRO).contentType(APPLICATION_JSON)
                        .content(datosRegistro("COMPRADOR@EXAMPLE.TEST", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.rol").value("COMPRADOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Usuario guardado = usuarios.findByEmail(EMAIL).orElseThrow();
        assertThat(guardado.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, guardado.getPasswordHash())).isTrue();

        String autorizacion = iniciarSesion(EMAIL, PASSWORD);
        var token = jwtDecoder.decode(autorizacion.substring("Bearer ".length()));
        assertThat(token.getSubject()).isEqualTo(EMAIL);
        assertThat(token.getClaimAsString("rol")).isEqualTo("COMPRADOR");
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));

        mvc.perform(get(PERFIL).header(AUTHORIZATION, autorizacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guardado.getId().intValue()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rechazaElRegistroInvalidoYElCorreoDuplicado() throws Exception {
        mvc.perform(post(REGISTRO).contentType(APPLICATION_JSON)
                        .content(datosRegistro("correo-invalido", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
        assertThat(usuarios.count()).isZero();

        guardarUsuario(EMAIL, Usuario.Rol.COMPRADOR);
        mvc.perform(post(REGISTRO).contentType(APPLICATION_JSON)
                        .content(datosRegistro("COMPRADOR@EXAMPLE.TEST", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.general").exists());
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void protegeElPerfilYRechazaCredencialesInvalidasOCuentasInactivas() throws Exception {
        mvc.perform(get(PERFIL)).andExpect(status().isUnauthorized());
        mvc.perform(get(PERFIL).header(AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());

        Usuario comprador = guardarUsuario(EMAIL, Usuario.Rol.COMPRADOR);
        comprobarLoginRechazado("PasswordIncorrecta123!");
        comprador.setActivo(false);
        usuarios.save(comprador);
        comprobarLoginRechazado(PASSWORD);
    }

    @Test
    void rechazaUnJwtExpiradoAunqueLaFirmaSeaValida() throws Exception {
        guardarUsuario(EMAIL, Usuario.Rol.COMPRADOR);
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(EMAIL).claim("rol", "COMPRADOR")
                .issuedAt(ahora.minus(Duration.ofHours(1)))
                .expiresAt(ahora.minus(Duration.ofMinutes(30))).build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        mvc.perform(get(PERFIL).header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validaYActualizaElPerfilPropio() throws Exception {
        guardarUsuario(EMAIL, Usuario.Rol.COMPRADOR);
        String autorizacion = iniciarSesion(EMAIL, PASSWORD);
        mvc.perform(put(PERFIL).header(AUTHORIZATION, autorizacion).contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());

        mvc.perform(put(PERFIL).header(AUTHORIZATION, autorizacion).contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("nombre", "  Ana  ",
                                "apellidos", "  Ruiz ", "telefono", " 600123456 "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellidos").value("Ruiz"))
                .andExpect(jsonPath("$.telefono").value("600123456"));
        assertThat(usuarios.findByEmail(EMAIL).orElseThrow().getNombre()).isEqualTo("Ana");
    }

    @Test
    void cambiaLaPasswordSoloConLaActualCorrectaYUnaDistinta() throws Exception {
        guardarUsuario(EMAIL, Usuario.Rol.COMPRADOR);
        String autorizacion = iniciarSesion(EMAIL, PASSWORD);
        String nueva = "PasswordNueva123!";

        cambiarPassword(autorizacion, "PasswordIncorrecta123!", nueva).andExpect(status().isBadRequest());
        cambiarPassword(autorizacion, PASSWORD, PASSWORD).andExpect(status().isBadRequest());
        cambiarPassword(autorizacion, PASSWORD, "corta").andExpect(status().isBadRequest());
        cambiarPassword(autorizacion, PASSWORD, nueva).andExpect(status().isNoContent());

        comprobarLoginRechazado(PASSWORD);
        assertThat(iniciarSesion(EMAIL, nueva)).startsWith("Bearer ");
    }

    private byte[] datosRegistro(String email, String password) {
        return json.writeValueAsBytes(Map.of("nombre", "Ana", "apellidos", "Ruiz", "email", email,
                "password", password, "rol", "ADMIN"));
    }

    private void comprobarLoginRechazado(String password) throws Exception {
        mvc.perform(post(LOGIN).contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", EMAIL, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    private ResultActions cambiarPassword(
            String autorizacion, String actual, String nueva) throws Exception {
        return mvc.perform(put(PERFIL + "/password").header(AUTHORIZATION, autorizacion)
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("passwordActual", actual, "passwordNueva", nueva))));
    }
}
