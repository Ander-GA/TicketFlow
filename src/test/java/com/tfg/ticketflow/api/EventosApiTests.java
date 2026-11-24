package com.tfg.ticketflow.api;

import static com.tfg.ticketflow.entity.Evento.EstadoEvento.BORRADOR;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.CANCELADO;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.FINALIZADO;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.PUBLICADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.TipoEntrada;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.service.EventoService;
import com.tfg.ticketflow.service.TipoEntradaService;

class EventosApiTests extends ApiTestSupport {

    private static final String EMAIL = "organizador@example.test";
    @Autowired EventoService servicio;
    @Autowired TipoEntradaService servicioTipos;
    @Autowired TransactionTemplate transacciones;
    private Usuario organizador;
    private LocalDateTime inicio;

    @BeforeEach
    void prepararOrganizador() {
        organizador = guardarUsuario(EMAIL, Usuario.Rol.ORGANIZADOR);
        inicio = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void creaBorradorYValidaAforoYFechas() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        Map<String, Object> datos = datosEvento(inicio, 50);
        datos.remove("aforoMaximo");
        crear(token, datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.aforoMaximo").exists());
        datos.put("aforoMaximo", 0);
        crear(token, datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.aforoMaximo").exists());
        datos.put("aforoMaximo", 50);
        datos.put("fechaFin", inicio.minusHours(1).toString());
        crear(token, datos).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.general").exists());
        assertThat(eventos.count()).isZero();

        datos.put("fechaFin", inicio.plusHours(1).toString());
        crear(token, datos).andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Evento test"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.aforoMaximo").value(50));
        assertThat(eventos.findByOrganizador(organizador)).singleElement()
                .satisfies(evento -> assertThat(evento.getFechaFin()).isEqualTo(inicio.plusHours(1)));
    }

    @Test
    void elCatalogoExcluyeEstadosNoPublicosYCombinaFiltros() throws Exception {
        Evento coincidencia = evento("Concierto acústico", "Bilbao", PUBLICADO, 50);
        evento("Teatro", "Bilbao", PUBLICADO, 50);
        evento("Concierto de verano", "Madrid", PUBLICADO, 50);
        for (Evento.EstadoEvento estado : List.of(BORRADOR, CANCELADO, FINALIZADO)) {
            Evento oculto = evento("Concierto oculto " + estado, "Bilbao", estado, 50);
            mvc.perform(get(EVENTOS + "/{id}", oculto.getId())).andExpect(status().isNotFound());
        }

        mvc.perform(get(EVENTOS)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(EVENTOS).param("titulo", " CONCIERTO ").param("ubicacion", " bilBAo "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(coincidencia.getId().intValue()));
        mvc.perform(get(EVENTOS + "/{id}", coincidencia.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
    }

    @Test
    void elOrganizadorSoloGestionaEventosPropios() throws Exception {
        Evento propio = evento("Propio", "Bilbao", BORRADOR, 50);
        Usuario otro = guardarUsuario("otro@example.test", Usuario.Rol.ORGANIZADOR);
        Evento ajeno = guardarEvento(otro, "Ajeno", "Bilbao", BORRADOR, inicio, null, 50);
        String token = iniciarSesion(EMAIL, PASSWORD);

        mvc.perform(get(EVENTOS + "/mis-eventos")).andExpect(status().isUnauthorized());
        mvc.perform(get(EVENTOS + "/mis-eventos").header(AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propio.getId().intValue()));
        editar(token, ajeno, datosEvento(inicio, 100)).andExpect(status().isNotFound());
        cambiarEstado(token, ajeno, "publicar").andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = Usuario.Rol.class, names = {"ADMIN", "COMPRADOR", "VALIDADOR"})
    void otrosRolesNoPuedenGestionarEventos(Usuario.Rol rol) throws Exception {
        String email = "otro@example.test";
        guardarUsuario(email, rol);
        String token = iniciarSesion(email, PASSWORD);
        Evento evento = evento("Protegido", "Bilbao", BORRADOR, 50);

        crear(token, datosEvento(inicio, 50)).andExpect(status().isForbidden());
        mvc.perform(get(EVENTOS + "/mis-eventos").header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        editar(token, evento, datosEvento(inicio, 100)).andExpect(status().isForbidden());
        for (String accion : List.of("publicar", "retirar", "cancelar", "finalizar")) {
            cambiarEstado(token, evento, accion).andExpect(status().isForbidden());
        }
    }

    @Test
    void validaTransicionesYVisibilidadAlPublicarRetirarYCancelar() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        Evento borrador = evento("Transiciones", "Bilbao", BORRADOR, 50);
        guardarTipoGeneral(borrador);
        cambiarEstado(token, borrador, "retirar").andExpect(status().isConflict());
        cambiarEstado(token, borrador, "finalizar").andExpect(status().isConflict());
        cambiarEstado(token, borrador, "publicar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
        cambiarEstado(token, borrador, "publicar").andExpect(status().isConflict());
        cambiarEstado(token, borrador, "finalizar").andExpect(status().isConflict());
        cambiarEstado(token, borrador, "retirar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
        mvc.perform(get(EVENTOS + "/{id}", borrador.getId())).andExpect(status().isNotFound());
        cambiarEstado(token, borrador, "publicar").andExpect(status().isOk());
        cambiarEstado(token, borrador, "cancelar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        cambiarEstado(token, borrador, "cancelar").andExpect(status().isConflict());
        cambiarEstado(token, borrador, "publicar").andExpect(status().isConflict());
        editar(token, borrador, datosEvento(inicio, 70)).andExpect(status().isConflict());
        mvc.perform(get(EVENTOS + "/{id}", borrador.getId())).andExpect(status().isNotFound());
    }

    @Test
    void publicarRequiereInicioFuturoYAforoEnEventosAntiguos() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        Evento pasado = guardarEvento(organizador, "Pasado", "Bilbao", BORRADOR,
                inicio.minusDays(3), null, 50);
        Evento sinAforo = evento("Sin aforo", "Bilbao", BORRADOR, null);
        guardarTipoGeneral(pasado);
        guardarTipoGeneral(sinAforo);
        cambiarEstado(token, pasado, "publicar").andExpect(status().isConflict());
        cambiarEstado(token, sinAforo, "publicar").andExpect(status().isConflict());
        editar(token, sinAforo, datosEvento(inicio, 50)).andExpect(status().isOk());
        cambiarEstado(token, sinAforo, "publicar").andExpect(status().isOk());
    }

    @Test
    void soloFinalizaAlLlegarALaFechaLimite() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        LocalDateTime pasado = inicio.minusDays(3);
        Evento enCurso = guardarEvento(organizador, "En curso", "Bilbao", PUBLICADO, pasado, inicio, 50);
        Evento terminado = guardarEvento(organizador, "Terminado", "Bilbao", PUBLICADO,
                pasado, pasado.plusHours(1), 50);
        Evento sinFin = guardarEvento(organizador, "Sin fin", "Bilbao", PUBLICADO, pasado, null, 50);

        cambiarEstado(token, enCurso, "finalizar").andExpect(status().isConflict());
        cambiarEstado(token, terminado, "finalizar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
        cambiarEstado(token, sinFin, "finalizar").andExpect(status().isOk());
        cambiarEstado(token, terminado, "cancelar").andExpect(status().isConflict());
        editar(token, terminado, datosEvento(inicio, 70)).andExpect(status().isConflict());
    }

    @Test
    void elAforoNoPuedeBajarDeLasPlazasAsignadas() throws Exception {
        String token = iniciarSesion(EMAIL, PASSWORD);
        Evento evento = evento("Con plazas", "Bilbao", PUBLICADO, 100);
        tiposEntrada.save(new TipoEntrada("General", null, BigDecimal.TEN, 30, evento));
        TipoEntrada inactiva = new TipoEntrada("VIP", null, BigDecimal.TEN, 20, evento);
        inactiva.setActivo(false);
        tiposEntrada.save(inactiva);
        Evento otro = evento("Otro", "Bilbao", BORRADOR, 100);
        tiposEntrada.save(new TipoEntrada("General", null, BigDecimal.TEN, 60, otro));

        editar(token, evento, datosEvento(inicio, 49)).andExpect(status().isConflict());
        assertThat(eventos.findById(evento.getId()).orElseThrow().getAforoMaximo()).isEqualTo(100);
        editar(token, evento, datosEvento(inicio, 50)).andExpect(status().isOk())
                .andExpect(jsonPath("$.aforoMaximo").value(50));
        editar(token, evento, datosEvento(inicio.minusDays(3), 50)).andExpect(status().isConflict());
        assertThat(eventos.findById(evento.getId()).orElseThrow().getFechaInicio()).isEqualTo(inicio);
    }

    @Test
    void dosEscriturasConcurrentesRevalidanElEstadoTrasLiberarElBloqueo() throws Exception {
        Evento evento = evento("Concurrente", "Bilbao", BORRADOR, 50);
        guardarTipoGeneral(evento);
        Long id = evento.getId();
        CountDownLatch canceladoSinConfirmar = new CountDownLatch(1);
        CountDownLatch confirmarCancelacion = new CountDownLatch(1);
        CountDownLatch comienzaPublicacion = new CountDownLatch(1);

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var cancelacion = ejecutor.submit(() -> transacciones.executeWithoutResult(estado -> {
                servicio.cancelarEvento(id, organizador);
                canceladoSinConfirmar.countDown();
                esperar(confirmarCancelacion);
            }));

            try {
                esperar(canceladoSinConfirmar);
                var publicacion = ejecutor.submit(() -> {
                    comienzaPublicacion.countDown();
                    return servicio.publicarEvento(id, organizador);
                });
                esperar(comienzaPublicacion);
                assertThatThrownBy(() -> publicacion.get(250, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);

                confirmarCancelacion.countDown();
                cancelacion.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> publicacion.get(5, TimeUnit.SECONDS))
                        .isInstanceOf(ExecutionException.class)
                        .hasCauseInstanceOf(ConflictoNegocioException.class);
            } finally {
                confirmarCancelacion.countDown();
            }
        }

        assertThat(eventos.findById(id).orElseThrow().getEstado()).isEqualTo(CANCELADO);
    }

    @Test
    void reducirAforoRevalidaLasPlazasQueOtroCambioAcabaDeAsignar() throws Exception {
        Long id = evento("Aforo concurrente", "Bilbao", BORRADOR, 100).getId();
        CountDownLatch tipoSinConfirmar = new CountDownLatch(1);
        CountDownLatch confirmarTipo = new CountDownLatch(1);
        CountDownLatch comienzaEdicion = new CountDownLatch(1);

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var altaTipo = ejecutor.submit(() -> transacciones.executeWithoutResult(estado -> {
                servicioTipos.crearTipoEntrada(id, organizador, "General", null, BigDecimal.TEN, 60, null);
                tipoSinConfirmar.countDown();
                esperar(confirmarTipo);
            }));

            try {
                esperar(tipoSinConfirmar);
                var edicion = ejecutor.submit(() -> {
                    comienzaEdicion.countDown();
                    return servicio.actualizarEvento(id, organizador, "Aforo concurrente", "Descripción",
                            inicio, null, "Bilbao", "Dirección", 50);
                });
                esperar(comienzaEdicion);
                assertThatThrownBy(() -> edicion.get(250, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);

                confirmarTipo.countDown();
                altaTipo.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> edicion.get(5, TimeUnit.SECONDS))
                        .isInstanceOf(ExecutionException.class)
                        .hasCauseInstanceOf(ConflictoNegocioException.class);
            } finally {
                confirmarTipo.countDown();
            }
        }

        assertThat(tiposEntrada.sumarCantidadTotal(id)).isEqualTo(60L);
        assertThat(eventos.findById(id).orElseThrow().getAforoMaximo()).isEqualTo(100);
    }

    @Test
    void tiposDeEntradaRechazanNombresDuplicadosEnElMismoEvento() {
        Evento evento = evento("Con tipos", "Bilbao", BORRADOR, 100);
        Evento otro = evento("Otro con tipos", "Bilbao", BORRADOR, 100);
        tiposEntrada.saveAndFlush(new TipoEntrada("General", null, BigDecimal.TEN, 30, evento));
        tiposEntrada.saveAndFlush(new TipoEntrada("General", null, BigDecimal.TEN, 20, otro));

        assertThatThrownBy(() -> tiposEntrada.saveAndFlush(
                new TipoEntrada("General", null, BigDecimal.TEN, 20, evento)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(tiposEntrada.findByEventoId(evento.getId())).hasSize(1);
    }

    @Test
    void editarUnTipoDeEntradaConUnaVersionAntiguaNoSobrescribeLosCambios() {
        Evento evento = evento("Con tipos", "Bilbao", BORRADOR, 100);
        Long id = tiposEntrada.saveAndFlush(new TipoEntrada("General", null, BigDecimal.TEN, 30, evento)).getId();
        TipoEntrada primeraCopia = tiposEntrada.findById(id).orElseThrow();
        TipoEntrada segundaCopia = tiposEntrada.findById(id).orElseThrow();

        primeraCopia.setCantidadTotal(40);
        tiposEntrada.saveAndFlush(primeraCopia);
        segundaCopia.setCantidadTotal(50);

        assertThatThrownBy(() -> tiposEntrada.saveAndFlush(segundaCopia))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(tiposEntrada.sumarCantidadTotal(evento.getId())).isEqualTo(40L);
    }

    private void esperar(CountDownLatch señal) {
        try {
            assertThat(señal.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Espera interrumpida", ex);
        }
    }

    private Evento evento(String titulo, String ubicacion, Evento.EstadoEvento estado, Integer aforo) {
        return guardarEvento(organizador, titulo, ubicacion, estado, inicio, null, aforo);
    }

    private void guardarTipoGeneral(Evento evento) {
        tiposEntrada.save(new TipoEntrada("General", null, BigDecimal.TEN, 10, evento));
    }

    private Map<String, Object> datosEvento(LocalDateTime fechaInicio, int aforo) {
        return new LinkedHashMap<>(Map.of("titulo", "  Evento test  ", "descripcion", "Descripción",
                "ubicacion", "Bilbao", "direccion", "Dirección", "fechaInicio", fechaInicio.toString(),
                "aforoMaximo", aforo));
    }

    private ResultActions crear(String token, Map<String, Object> datos) throws Exception {
        return mvc.perform(post(EVENTOS).header(AUTHORIZATION, token).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(datos)));
    }

    private ResultActions editar(String token, Evento evento, Map<String, Object> datos) throws Exception {
        return mvc.perform(put(EVENTOS + "/{id}", evento.getId()).header(AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(json.writeValueAsBytes(datos)));
    }

    private ResultActions cambiarEstado(String token, Evento evento, String accion) throws Exception {
        return mvc.perform(patch(EVENTOS + "/{id}/{accion}", evento.getId(), accion).header(AUTHORIZATION, token));
    }
}
