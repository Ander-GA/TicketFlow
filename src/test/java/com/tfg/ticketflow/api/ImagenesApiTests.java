package com.tfg.ticketflow.api;

import static com.tfg.ticketflow.entity.Evento.EstadoEvento.BORRADOR;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.CANCELADO;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.FINALIZADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.service.ImagenEventoService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImagenesApiTests extends ApiTestSupport {

    private static final String EMAIL = "portadas@example.test";
    private static final String IMAGENES = "/api/v1/imagenes/eventos/";
    private static final int MEGABYTE = 1024 * 1024;

    @TempDir static Path directorio;
    @LocalServerPort int puerto;
    @Autowired ImagenEventoService imagenes;
    @Autowired TransactionTemplate transacciones;

    private Usuario organizador;
    private Evento evento;

    @DynamicPropertySource
    static void configurarAlmacenamiento(DynamicPropertyRegistry propiedades) {
        propiedades.add("ticketflow.imagenes.directorio", () -> directorio.toString());
    }

    @BeforeEach
    void prepararEvento() {
        organizador = guardarUsuario(EMAIL, Usuario.Rol.ORGANIZADOR);
        evento = guardarEvento(organizador, "Con portada", "Bilbao", BORRADOR,
                LocalDateTime.now().plusDays(1), null, 100);
    }

    @Test
    void subeDescargaYSustituyeUnaPortadaPublicaDeUnBorrador() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        String urlPng = subirYLeerUrl(token, imagen("png", 8, 6));
        assertThat(urlPng).startsWith(IMAGENES).endsWith(".png");
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isEqualTo(urlPng);
        comprobarDescarga(urlPng, "image/png", 8, 6);
        mvc.perform(get(EVENTOS + "/{id}", evento.getId())).andExpect(status().isNotFound());

        String urlJpg = subirYLeerUrl(token, imagen("jpg", 5, 7));
        assertThat(urlJpg).startsWith(IMAGENES).endsWith(".jpg").isNotEqualTo(urlPng);
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isEqualTo(urlJpg);
        comprobarDescarga(urlJpg, "image/jpeg", 5, 7);
        mvc.perform(get(urlPng)).andExpect(status().isNotFound());
        assertThat(Files.exists(archivoLocal(urlPng))).isFalse();
    }

    @Test
    void soloElOrganizadorPropietarioPuedeSubirUnaPortada() throws Exception {
        MockMultipartFile archivo = imagen("png", 4, 4);
        mvc.perform(multipart(EVENTOS + "/{id}/imagen", evento.getId()).file(archivo))
                .andExpect(status().isUnauthorized());

        String comprador = "comprador@example.test";
        guardarUsuario(comprador, Usuario.Rol.COMPRADOR);
        subir(iniciarSesion(comprador, PASSWORD), archivo).andExpect(status().isForbidden());

        String otro = "otro-organizador@example.test";
        guardarUsuario(otro, Usuario.Rol.ORGANIZADOR);
        subir(iniciarSesion(otro, PASSWORD), archivo).andExpect(status().isNotFound());
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isNull();
    }

    @Test
    void rechazaCambiarPortadasDeEventosCanceladosOFinalizados() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        MockMultipartFile archivo = imagen("png", 4, 4);
        for (Evento.EstadoEvento estado : List.of(CANCELADO, FINALIZADO)) {
            evento.setEstado(estado);
            eventos.save(evento);
            subir(token, archivo).andExpect(status().isConflict());
        }
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isNull();
    }

    @Test
    void rechazaContenidoInvalidoFormatoNoAdmitidoYDimensionesExcesivas() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        List<MockMultipartFile> archivosInvalidos = List.of(
                new MockMultipartFile("archivo", "vacio.png", "image/png", new byte[0]),
                new MockMultipartFile("archivo", "falso.png", "image/png", new byte[] {1, 2, 3}),
                imagen("gif", 4, 4), imagen("png", 4001, 1), imagen("png", 4000, 3001));
        for (MockMultipartFile archivo : archivosInvalidos) {
            subir(token, archivo).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.general").exists());
        }
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isNull();
    }

    @Test
    void rechazaCon413CuandoElServidorRecibeUnaImagenDeMasDe5Mb() throws Exception {
        String boundary = "TicketflowTestBoundary";
        String cabecera = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"archivo\"; filename=\"grande.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto
                        + EVENTOS + "/" + evento.getId() + "/imagen"))
                .timeout(Duration.ofSeconds(10))
                .header(AUTHORIZATION, iniciarSesion(EMAIL, PASSWORD))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(
                        cabecera.getBytes(StandardCharsets.US_ASCII), new byte[5 * MEGABYTE + 1],
                        ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII))))
                .build();

        try (HttpClient cliente = HttpClient.newHttpClient()) {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            assertThat(respuesta.statusCode()).isEqualTo(413);
            assertThat(json.readTree(respuesta.body()).path("errores").path("general").stringValue()).isNotBlank();
        }
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isNull();
    }

    @Test
    void unaSustitucionRevertidaConservaLaPortadaAnteriorYEliminaLaNueva() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        String anterior = subirYLeerUrl(token, imagen("png", 4, 4));
        MockMultipartFile sustituta = imagen("png", 6, 6);

        String nueva = transacciones.execute(estado -> {
            String url = imagenes.subirImagen(evento.getId(), organizador, sustituta).getImagen();
            assertThat(Files.exists(archivoLocal(url))).isTrue();
            assertThat(Files.exists(archivoLocal(anterior))).isTrue();
            estado.setRollbackOnly();
            return url;
        });

        assertThat(nueva).isNotEqualTo(anterior);
        assertThat(eventos.findById(evento.getId()).orElseThrow().getImagen()).isEqualTo(anterior);
        assertThat(Files.exists(archivoLocal(nueva))).isFalse();
        comprobarDescarga(anterior, "image/png", 4, 4);
    }

    @Test
    void rechazaNombresInvalidosYArchivosInexistentes() throws Exception {
        mvc.perform(get(IMAGENES + "portada.png")).andExpect(status().isNotFound());
        mvc.perform(get(IMAGENES + UUID.randomUUID() + ".jpg")).andExpect(status().isNotFound());
    }

    private ResultActions subir(String token, MockMultipartFile archivo) throws Exception {
        return mvc.perform(multipart(EVENTOS + "/{id}/imagen", evento.getId())
                .file(archivo).header(AUTHORIZATION, token));
    }

    private String subirYLeerUrl(String token, MockMultipartFile archivo) throws Exception {
        byte[] respuesta = subir(token, archivo).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsByteArray();
        return json.readTree(respuesta).path("imagen").stringValue();
    }

    private void comprobarDescarga(String url, String tipo, int ancho, int alto) throws Exception {
        byte[] respuesta = mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType(tipo)).andReturn().getResponse().getContentAsByteArray();
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(respuesta));
        assertThat(imagen.getWidth()).isEqualTo(ancho);
        assertThat(imagen.getHeight()).isEqualTo(alto);
    }

    private Path archivoLocal(String url) {
        return directorio.resolve(url.substring(IMAGENES.length()));
    }

    private MockMultipartFile imagen(String formato, int ancho, int alto) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream contenido = new ByteArrayOutputStream();
        assertThat(ImageIO.write(imagen, formato, contenido)).isTrue();
        return new MockMultipartFile("archivo", "portada.bin", "application/octet-stream", contenido.toByteArray());
    }
}
