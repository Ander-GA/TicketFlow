package com.tfg.ticketflow.api;

import static com.tfg.ticketflow.entity.Evento.EstadoEvento.BORRADOR;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.PUBLICADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.TipoEntrada;
import com.tfg.ticketflow.entity.Usuario;

class TiposEntradaApiTests extends ApiTestSupport {

    private static final String EMAIL = "tipos@example.test";
    private Usuario organizador;
    private String token;
    private LocalDateTime inicio;
    private Evento evento;
    private String ruta;

    @BeforeEach
    void prepararEvento() throws Exception {
        organizador = guardarUsuario(EMAIL, Usuario.Rol.ORGANIZADOR);
        token = iniciarSesion(EMAIL, PASSWORD);
        inicio = LocalDateTime.now().plusDays(7).withSecond(30).withNano(0);
        evento = guardarEvento(organizador, "Tipos", "Bilbao", BORRADOR, inicio, null, 100);
        ruta = rutaTipos(evento);
    }

    @Test
    void publicaSoloConUnTipoActivoYExponeSoloLosTiposActivos() throws Exception {
        publicar().andExpect(status().isConflict());
        mvc.perform(get(ruta)).andExpect(status().isNotFound());
        long general = crearTipo("General", 40);
        cambiarActivo(general, false).andExpect(status().isOk());
        publicar().andExpect(status().isConflict());
        cambiarActivo(general, true).andExpect(status().isOk());
        long vip = crearTipo("VIP", 20);
        publicar().andExpect(status().isOk());
        cambiarActivo(vip, false).andExpect(status().isOk());

        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) general));
        mvc.perform(get(ruta + "/gestion").header(AUTHORIZATION, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].activo").value(false));
        borrar(vip).andExpect(status().isConflict());
        mvc.perform(patch(EVENTOS + "/{id}/retirar", evento.getId()).header(AUTHORIZATION, token))
                .andExpect(status().isOk());
        mvc.perform(get(ruta)).andExpect(status().isNotFound());
        borrar(vip).andExpect(status().isNoContent());
    }

    @Test
    void limitaElAforoIncluidosLosTiposInactivosYRechazaNombresDuplicados() throws Exception {
        long general = crearTipo(" General ", 60);
        long vip = crearTipo("VIP", 40);
        cambiarActivo(general, false).andExpect(status().isOk());

        crear(datosTipo("general", 1)).andExpect(status().isConflict());
        crear(datosTipo("Otra", 1)).andExpect(status().isConflict());
        editar(vip, datosTipo("GENERAL", 40)).andExpect(status().isConflict());
        editar(vip, datosTipo("VIP", 41)).andExpect(status().isConflict());
        editar(vip, datosTipo("VIP", 30)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadTotal").value(30));
        assertThat(tiposEntrada.sumarCantidadTotal(evento.getId())).isEqualTo(90L);
        crearTipo("Otra", 10);
        borrar(general).andExpect(status().isNoContent());
        assertThat(tiposEntrada.sumarCantidadTotal(evento.getId())).isEqualTo(40L);
    }

    @Test
    void validaPrecioCantidadNombreYFechaEspecifica() throws Exception {
        Map<String, Object> datos = datosTipo("  ", 0);
        datos.put("precio", new BigDecimal("-0.01"));
        crear(datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.cantidadTotal").exists())
                .andExpect(jsonPath("$.errores.precio").exists());

        datos = datosTipo("Gratuita", 10);
        datos.put("precio", new BigDecimal("0.001"));
        crear(datos).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.precio").exists());
        datos.put("precio", new BigDecimal("100000000"));
        crear(datos).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.precio").exists());
        datos.put("precio", BigDecimal.ZERO);
        datos.put("fechaInicioVenta", inicio.toString());
        crear(datos).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.general").exists());

        datos.remove("fechaInicioVenta");
        crear(datos).andExpect(status().isCreated()).andExpect(jsonPath("$.precio").value(0));
    }

    @Test
    void validaRolesPropietarioYQueElTipoPerteneceAlEvento() throws Exception {
        long id = crearTipo("General", 20);
        String gestion = ruta + "/gestion";
        mvc.perform(get(gestion)).andExpect(status().isUnauthorized());
        mvc.perform(post(ruta)).andExpect(status().isUnauthorized());

        guardarUsuario("comprador-tipos@example.test", Usuario.Rol.COMPRADOR);
        String comprador = iniciarSesion("comprador-tipos@example.test", PASSWORD);
        mvc.perform(get(gestion).header(AUTHORIZATION, comprador)).andExpect(status().isForbidden());
        mvc.perform(post(ruta).header(AUTHORIZATION, comprador)).andExpect(status().isForbidden());
        mvc.perform(put(ruta + "/{id}", id).header(AUTHORIZATION, comprador)).andExpect(status().isForbidden());
        mvc.perform(patch(ruta + "/{id}/activo", id).header(AUTHORIZATION, comprador)).andExpect(status().isForbidden());
        mvc.perform(delete(ruta + "/{id}", id).header(AUTHORIZATION, comprador)).andExpect(status().isForbidden());

        guardarUsuario("ajeno-tipos@example.test", Usuario.Rol.ORGANIZADOR);
        String ajeno = iniciarSesion("ajeno-tipos@example.test", PASSWORD);
        mvc.perform(get(gestion).header(AUTHORIZATION, ajeno)).andExpect(status().isNotFound());
        mvc.perform(post(ruta).header(AUTHORIZATION, ajeno).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(datosTipo("Ajeno", 20)))).andExpect(status().isNotFound());
        mvc.perform(delete(ruta + "/{id}", id).header(AUTHORIZATION, ajeno)).andExpect(status().isNotFound());

        Evento otro = guardarEvento(organizador, "Otro", "Bilbao", BORRADOR, inicio, null, 100);
        mvc.perform(delete(rutaTipos(otro) + "/{id}", id).header(AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void heredaOAnticipaLaVentaGeneralYMantieneCompatibilidadAlEditar() throws Exception {
        LocalDateTime general = inicio.minusDays(2);
        LocalDateTime anticipada = general.minusDays(1);
        Map<String, Object> eventoDatos = datosEvento(inicio);
        eventoDatos.put("fechaInicioVentaGeneral", inicio.toString());
        mvc.perform(post(EVENTOS).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(eventoDatos))).andExpect(status().isBadRequest());
        eventoDatos.put("fechaInicioVentaGeneral", general.toString());
        byte[] respuesta = mvc.perform(post(EVENTOS).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(json.writeValueAsBytes(eventoDatos))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaInicioVentaGeneral").value(general.toString())).andReturn()
                .getResponse().getContentAsByteArray();
        evento = eventos.findById(json.readTree(respuesta).get("id").asLong()).orElseThrow();
        ruta = rutaTipos(evento);
        crear(datosTipo("General", 40)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaInicioVentaEfectiva").value(general.toString()));
        Map<String, Object> vip = datosTipo("Anticipada", 20);
        vip.put("fechaInicioVenta", anticipada.toString());
        crear(vip).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaInicioVentaEfectiva").value(anticipada.toString()));
        publicar().andExpect(status().isOk());

        actualizarEvento(datosEvento(inicio)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicioVentaGeneral").value(general.toString()));
        eventoDatos.put("fechaInicioVentaGeneral", null);
        actualizarEvento(eventoDatos).andExpect(status().isConflict());
        assertThat(eventos.findById(evento.getId()).orElseThrow().getFechaInicioVentaGeneral()).isEqualTo(general);
    }

    @Test
    void noSePuedeAdelantarElEventoHastaUnaAperturaEspecifica() throws Exception {
        LocalDateTime apertura = inicio.minusDays(1);
        Map<String, Object> tipo = datosTipo("General", 30);
        tipo.put("fechaInicioVenta", apertura.toString());
        crear(tipo).andExpect(status().isCreated());
        actualizarEvento(datosEvento(apertura)).andExpect(status().isConflict());
        assertThat(eventos.findById(evento.getId()).orElseThrow().getFechaInicio()).isEqualTo(inicio);
    }

    @Test
    void bloqueaCambiosDeTiposCuandoElEventoPublicadoYaHaComenzado() throws Exception {
        evento.setEstado(PUBLICADO);
        evento.setFechaInicio(LocalDateTime.now().minusDays(1));
        eventos.save(evento);
        long id = tiposEntrada.save(new TipoEntrada("General", null, BigDecimal.TEN, 20, evento)).getId();
        crear(datosTipo("Otro", 10)).andExpect(status().isConflict());
        editar(id, datosTipo("General", 30)).andExpect(status().isConflict());
        cambiarActivo(id, false).andExpect(status().isConflict());
    }

    @Test
    void lasCondicionesBloqueadasProtegenLosDatosComercialesYElCupo() throws Exception {
        TipoEntrada tipo = new TipoEntrada("General", null, BigDecimal.TEN, 20, evento);
        tipo.bloquearCondiciones();
        long id = tiposEntrada.save(tipo).getId();

        editar(id, datosTipo("Modificada", 20)).andExpect(status().isConflict());
        editar(id, datosTipo("General", 19)).andExpect(status().isConflict());
        editar(id, datosTipo("General", 21)).andExpect(status().isOk());
        borrar(id).andExpect(status().isConflict());
    }

    private String rutaTipos(Evento evento) {
        return EVENTOS + "/" + evento.getId() + "/tipos-entrada";
    }

    private long crearTipo(String nombre, int cantidad) throws Exception {
        byte[] respuesta = crear(datosTipo(nombre, cantidad)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();
        return json.readTree(respuesta).get("id").asLong();
    }

    private Map<String, Object> datosTipo(String nombre, int cantidad) {
        return new LinkedHashMap<>(Map.of("nombre", nombre, "precio", BigDecimal.TEN, "cantidadTotal", cantidad));
    }

    private Map<String, Object> datosEvento(LocalDateTime inicio) {
        return new LinkedHashMap<>(Map.of("titulo", "Tipos", "descripcion", "Descripción", "ubicacion", "Bilbao",
                "direccion", "Dirección", "fechaInicio", inicio.toString(), "aforoMaximo", 100));
    }

    private ResultActions crear(Map<String, Object> datos) throws Exception {
        return mvc.perform(post(ruta).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(datos)));
    }

    private ResultActions editar(long tipoId, Map<String, Object> datos) throws Exception {
        return mvc.perform(put(ruta + "/{id}", tipoId).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(datos)));
    }

    private ResultActions cambiarActivo(long tipoId, boolean activo) throws Exception {
        return mvc.perform(patch(ruta + "/{id}/activo", tipoId).header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("activo", activo))));
    }

    private ResultActions borrar(long tipoId) throws Exception {
        return mvc.perform(delete(ruta + "/{id}", tipoId).header(AUTHORIZATION, token));
    }

    private ResultActions publicar() throws Exception {
        return mvc.perform(patch(EVENTOS + "/{id}/publicar", evento.getId()).header(AUTHORIZATION, token));
    }

    private ResultActions actualizarEvento(Map<String, Object> datos) throws Exception {
        return mvc.perform(put(EVENTOS + "/{id}", evento.getId()).header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(json.writeValueAsBytes(datos)));
    }
}
