package com.tfg.ticketflow.api;

import static com.tfg.ticketflow.entity.Evento.EstadoEvento.BORRADOR;
import static com.tfg.ticketflow.entity.Evento.EstadoEvento.PUBLICADO;
import static com.tfg.ticketflow.entity.Reserva.EstadoReserva.ACTIVA;
import static com.tfg.ticketflow.entity.Reserva.EstadoReserva.CADUCADA;
import static com.tfg.ticketflow.entity.Reserva.EstadoReserva.CANCELADA;
import static com.tfg.ticketflow.entity.Reserva.EstadoReserva.CONFIRMADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.tfg.ticketflow.dto.reserva.ReservaRespuestaDTO;
import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Reserva;
import com.tfg.ticketflow.entity.Reserva.EstadoReserva;
import com.tfg.ticketflow.entity.TipoEntrada;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.service.EventoService;
import com.tfg.ticketflow.service.ReservaCaducidadScheduler;
import com.tfg.ticketflow.service.ReservaService;

class ReservasApiTests extends ApiTestSupport {

    private static final String RUTA = "/api/v1/reservas";
    private static final String EMAIL = "cliente-reservas@example.test";
    private static final String ORGANIZADOR = "organizador-reservas@example.test";
    private static final String CLAVE = "reserva-pruebas-01";
    private static final Instant AHORA = Instant.parse("2027-02-15T12:00:00.123456Z");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDateTime INICIO = LocalDateTime.ofInstant(AHORA, MADRID).plusDays(3).withNano(0);

    @MockitoBean(name = "reservasClock") Clock reloj;
    @Autowired ReservaService servicio;
    @Autowired EventoService servicioEventos;
    @Autowired ReservaCaducidadScheduler limpieza;
    @Autowired TransactionTemplate transacciones;

    private Usuario organizador;
    private Usuario comprador;
    private String token;
    private Evento evento;
    private TipoEntrada tipo;

    @BeforeEach
    void prepararReserva() throws Exception {
        when(reloj.getZone()).thenReturn(MADRID);
        avanzarA(AHORA);
        organizador = guardarUsuario(ORGANIZADOR, Usuario.Rol.ORGANIZADOR);
        comprador = guardarUsuario(EMAIL, Usuario.Rol.COMPRADOR);
        token = iniciarSesion(EMAIL, PASSWORD);
        evento = guardarEvento(organizador, "Reservable", "Bilbao", PUBLICADO, INICIO, null, 10);
        tipo = guardarTipo(evento, "General", 2, new BigDecimal("12.50"));
    }

    @Test
    void creaCongelaElPrecioYReutilizaLaClaveSinAmpliarElPlazo() throws Exception {
        long id = leerId(reservar(token, evento, tipo, 2, CLAVE)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.precioUnitario").value(12.5))
                .andExpect(jsonPath("$.importeTotal").value(25.0))
                .andExpect(jsonPath("$.fechaCreacion").value(AHORA.toString()))
                .andExpect(jsonPath("$.fechaExpiracion").value(AHORA.plusSeconds(600).toString())));
        avanzarA(AHORA.plusSeconds(120));
        reservar(token, evento, tipo, 2, CLAVE).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.fechaExpiracion").value(AHORA.plusSeconds(600).toString()));
        reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isConflict());
        assertThat(reservas.count()).isOne();
        assertThat(tiposEntrada.findById(tipo.getId()).orElseThrow().isCondicionesBloqueadas()).isTrue();
        mvc.perform(get(RUTA + "/mis-reservas").header(AUTHORIZATION, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value((int) id));
    }

    @Test
    void cancelarEsIdempotenteYLiberaPlazas() throws Exception {
        String segundo = tokenDe("segundo@example.test");
        String tercero = tokenDe("tercero@example.test");
        long primera = leerId(reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated()));
        reservar(segundo, evento, tipo, 1, CLAVE).andExpect(status().isCreated());
        reservar(tercero, evento, tipo, 1, CLAVE).andExpect(status().isConflict());
        disponibilidad(evento, tipo).andExpect(status().isOk())
                .andExpect(jsonPath("$.plazasComprometidas").value(2))
                .andExpect(jsonPath("$.plazasDisponibles").value(0));

        cancelar(token, primera).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        cancelar(token, primera).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        reservar(tercero, evento, tipo, 1, CLAVE).andExpect(status().isCreated());
    }

    @Test
    void liberaStockEnElInstanteExactoYLaLimpiezaConservaOtrosEstados() throws Exception {
        long activa = leerId(reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated()));
        Reserva confirmada = guardarReservaHistorica(CONFIRMADA, "confirmada");
        Reserva cancelada = guardarReservaHistorica(CANCELADA, "cancelada");
        avanzarA(AHORA.plusSeconds(600).minusNanos(1_000));
        disponibilidad(evento, tipo).andExpect(status().isOk())
                .andExpect(jsonPath("$.plazasDisponibles").value(0));

        avanzarA(AHORA.plusSeconds(600));
        disponibilidad(evento, tipo).andExpect(status().isOk())
                .andExpect(jsonPath("$.plazasDisponibles").value(1));
        cancelar(token, activa).andExpect(status().isConflict());
        reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value((int) activa))
                .andExpect(jsonPath("$.estado").value("CADUCADA"));
        assertThat(reservas.findById(activa).orElseThrow().getEstado()).isEqualTo(ACTIVA);

        limpieza.caducarReservas();
        assertThat(reservas.findById(activa).orElseThrow().getEstado()).isEqualTo(CADUCADA);
        assertThat(reservas.findById(confirmada.getId()).orElseThrow().getEstado()).isEqualTo(CONFIRMADA);
        assertThat(reservas.findById(cancelada.getId()).orElseThrow().getEstado()).isEqualTo(CANCELADA);
        reservar(token, evento, tipo, 1, "reserva-clave-nueva").andExpect(status().isCreated());
    }

    @Test
    void distingueClavesPorMayusculasYLimitaUnaActivaPorTipo() throws Exception {
        TipoEntrada vip = guardarTipo(evento, "VIP", 2, BigDecimal.ZERO);
        reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated());
        reservar(token, evento, tipo, 1, "reserva-clave-nueva").andExpect(status().isConflict());
        reservar(token, evento, vip, 1, "Reserva-pruebas-01").andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioUnitario").value(0)).andExpect(jsonPath("$.importeTotal").value(0));
        assertThat(reservas.count()).isEqualTo(2);
    }

    @Test
    void validaVentanasDeVentaYEstadosDelEventoYDelTipo() throws Exception {
        LocalDateTime horaLocal = LocalDateTime.ofInstant(AHORA, MADRID);
        evento.setFechaInicioVentaGeneral(horaLocal.plusHours(1));
        eventos.save(evento);
        TipoEntrada anticipada = guardarTipo(evento, "Anticipada", 1, BigDecimal.TEN);
        anticipada.setFechaInicioVenta(horaLocal.minusHours(1));
        tiposEntrada.save(anticipada);

        disponibilidad(evento, tipo).andExpect(status().isOk()).andExpect(jsonPath("$.ventaAbierta").value(false));
        reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isConflict());
        reservar(token, evento, anticipada, 1, CLAVE).andExpect(status().isCreated());
        tipo.setActivo(false);
        tiposEntrada.save(tipo);
        disponibilidad(evento, tipo).andExpect(status().isNotFound());
        reservar(token, evento, tipo, 1, "reserva-clave-nueva").andExpect(status().isConflict());

        evento.setEstado(BORRADOR);
        eventos.save(evento);
        disponibilidad(evento, anticipada).andExpect(status().isNotFound());
        reservar(token, evento, anticipada, 1, "reserva-clave-nueva").andExpect(status().isConflict());
        evento.setEstado(PUBLICADO);
        eventos.save(evento);
        avanzarA(INICIO.atZone(MADRID).toInstant());
        disponibilidad(evento, anticipada).andExpect(status().isOk()).andExpect(jsonPath("$.ventaAbierta").value(false));
        reservar(token, evento, anticipada, 1, "reserva-clave-nueva").andExpect(status().isConflict());
    }

    @Test
    void validaTitularJwtDatosDeEntradaYQueLaCuentaSigaActiva() throws Exception {
        String ajeno = tokenDe("ajeno-reservas@example.test");
        long id = leerId(reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated()));
        mvc.perform(get(RUTA + "/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA + "/{id}", id).header(AUTHORIZATION, ajeno)).andExpect(status().isNotFound());
        cancelar(ajeno, id).andExpect(status().isNotFound());
        mvc.perform(get(RUTA + "/mis-reservas").header(AUTHORIZATION, ajeno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        reservar(token, evento, tipo, 0, CLAVE).andExpect(status().isBadRequest());
        reservar(token, evento, tipo, 11, CLAVE).andExpect(status().isBadRequest());
        reservar(token, evento, tipo, 1, "corta").andExpect(status().isBadRequest());
        reservar(token, evento, tipo, 1, " ").andExpect(status().isBadRequest());
        reservar(token, otroEvento(), tipo, 1, "reserva-clave-nueva").andExpect(status().isNotFound());
        comprador.setActivo(false);
        usuarios.save(comprador);
        reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isConflict());
    }

    @Test
    void protegeLasCondicionesYElEventoYLaCancelacionDelEventoLiberaLasReservas() throws Exception {
        String organizadorToken = iniciarSesion(ORGANIZADOR, PASSWORD);
        long id = leerId(reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated()));
        String rutaEvento = EVENTOS + "/" + evento.getId();
        String rutaTipo = rutaEvento + "/tipos-entrada/" + tipo.getId();

        mvc.perform(put(rutaTipo).header(AUTHORIZATION, organizadorToken).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("nombre", "General", "precio", 10, "cantidadTotal", 2))))
                .andExpect(status().isConflict());
        mvc.perform(patch(rutaEvento + "/retirar").header(AUTHORIZATION, organizadorToken))
                .andExpect(status().isConflict());
        editarEvento(organizadorToken, "Madrid").andExpect(status().isConflict());
        editarEvento(organizadorToken, "Bilbao").andExpect(status().isOk());
        mvc.perform(patch(rutaEvento + "/cancelar").header(AUTHORIZATION, organizadorToken))
                .andExpect(status().isOk());

        assertThat(reservas.findById(id).orElseThrow().getEstado()).isEqualTo(CANCELADA);
        assertThat(reservas.sumarComprometidasPorTipo(tipo.getId(), ACTIVA, CONFIRMADA, AHORA)).isZero();
        disponibilidad(evento, tipo).andExpect(status().isNotFound());
    }

    @Test
    void losDiezMinutosSeMantienenAlRepetirseLaHoraLocalEnOtono() throws Exception {
        evento.setFechaInicio(LocalDateTime.parse("2027-10-31T04:00:00"));
        evento.setFechaInicioVentaGeneral(LocalDateTime.parse("2027-10-31T02:50:00"));
        eventos.save(evento);
        avanzarA(Instant.parse("2027-10-31T00:55:00Z"));
        long id = leerId(reservar(token, evento, tipo, 1, CLAVE).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaExpiracion").value("2027-10-31T01:05:00Z")));
        assertThat(reservas.findById(id).orElseThrow().getFechaExpiracion())
                .isEqualTo(Instant.parse("2027-10-31T01:05:00Z"));
        avanzarA(Instant.parse("2027-10-31T01:05:00Z"));
        disponibilidad(evento, tipo).andExpect(status().isOk())
                .andExpect(jsonPath("$.ventaAbierta").value(true)).andExpect(jsonPath("$.plazasDisponibles").value(2));
    }

    @Test
    void conVeinteSolicitudesConcurrentesSoloUnaConsumeLaUltimaPlaza() throws Exception {
        TipoEntrada ultima = guardarTipo(evento, "Última", 1, BigDecimal.TEN);
        List<Callable<ReservaRespuestaDTO>> solicitudes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String email = "cliente-concurrente-" + i + "@example.test";
            usuarios.save(new Usuario("Cliente", "Concurrente", email, comprador.getPasswordHash(),
                    Usuario.Rol.COMPRADOR, null));
            solicitudes.add(() -> servicio.crearReserva(evento.getId(), ultima.getId(), 1, CLAVE, email));
        }
        assertThat(ejecutarEnParalelo(solicitudes)).hasSize(1);
        assertThat(reservas.sumarComprometidasPorTipo(ultima.getId(), ACTIVA, CONFIRMADA, AHORA)).isEqualTo(1L);
    }

    @Test
    void losReintentosConcurrentesSeSerializanInclusoEntreEventosDistintos() throws Exception {
        Callable<ReservaRespuestaDTO> crear = () -> servicio.crearReserva(evento.getId(), tipo.getId(), 1, CLAVE, EMAIL);
        assertThat(ejecutarEnParalelo(List.of(crear, crear))).hasSize(2)
                .extracting(ReservaRespuestaDTO::getId).containsOnly(reservas.findAll().getFirst().getId());
        assertThat(reservas.count()).isOne();

        Evento otro = otroEvento();
        TipoEntrada otroTipo = guardarTipo(otro, "General", 2, BigDecimal.TEN);
        TipoEntrada vip = guardarTipo(evento, "VIP", 2, BigDecimal.TEN);
        String claveNueva = "misma-clave-en-dos-eventos";
        assertThat(ejecutarEnParalelo(List.of(
                () -> servicio.crearReserva(evento.getId(), vip.getId(), 1, claveNueva, EMAIL),
                () -> servicio.crearReserva(otro.getId(), otroTipo.getId(), 1, claveNueva, EMAIL))))
                .hasSize(1);
        assertThat(reservas.count()).isEqualTo(2);
    }

    @Test
    void unaReservaYUnaReduccionDeAforoSeSerializanSobreElMismoEvento() throws Exception {
        CountDownLatch reservaSinConfirmar = new CountDownLatch(1);
        CountDownLatch confirmar = new CountDownLatch(1);
        CountDownLatch empiezaEdicion = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var reserva = ejecutor.submit(() -> transacciones.executeWithoutResult(estado -> {
                servicio.crearReserva(evento.getId(), tipo.getId(), 2, CLAVE, EMAIL);
                reservaSinConfirmar.countDown();
                try {
                    assertThat(confirmar.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Espera interrumpida", ex);
                }
            }));

            try {
                assertThat(reservaSinConfirmar.await(5, TimeUnit.SECONDS)).isTrue();
                var edicion = ejecutor.submit(() -> {
                    empiezaEdicion.countDown();
                    return servicioEventos.actualizarEvento(evento.getId(), organizador, "Reservable", "Descripción",
                            INICIO, null, "Bilbao", "Dirección", 1);
                });
                assertThat(empiezaEdicion.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> edicion.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

                confirmar.countDown();
                reserva.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> edicion.get(5, TimeUnit.SECONDS))
                        .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(ConflictoNegocioException.class);
            } finally {
                confirmar.countDown();
            }
        }
        assertThat(reservas.sumarComprometidasPorTipo(tipo.getId(), ACTIVA, CONFIRMADA, AHORA)).isEqualTo(2L);
        assertThat(eventos.findById(evento.getId()).orElseThrow().getAforoMaximo()).isEqualTo(10);
    }

    @Test
    void unaTransaccionRevertidaNoDejaReservaNiBloqueaLasCondiciones() {
        transacciones.executeWithoutResult(estado -> {
            servicio.crearReserva(evento.getId(), tipo.getId(), 1, CLAVE, EMAIL);
            assertThat(reservas.count()).isOne();
            estado.setRollbackOnly();
        });
        assertThat(reservas.count()).isZero();
        assertThat(tiposEntrada.findById(tipo.getId()).orElseThrow().isCondicionesBloqueadas()).isFalse();
    }

    private ResultadoConcurrente iniciar(Callable<ReservaRespuestaDTO> solicitud) throws Exception {
        try {
            return new ResultadoConcurrente(solicitud.call());
        } catch (ConflictoNegocioException ex) {
            return new ResultadoConcurrente(null);
        }
    }

    private List<ReservaRespuestaDTO> ejecutarEnParalelo(List<Callable<ReservaRespuestaDTO>> solicitudes)
            throws Exception {
        CountDownLatch listos = new CountDownLatch(solicitudes.size());
        CountDownLatch comenzar = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(solicitudes.size())) {
            var futuros = solicitudes.stream().map(solicitud -> ejecutor.submit(() -> {
                listos.countDown();
                assertThat(comenzar.await(5, TimeUnit.SECONDS)).isTrue();
                return iniciar(solicitud);
            })).toList();
            assertThat(listos.await(5, TimeUnit.SECONDS)).isTrue();
            comenzar.countDown();
            List<ReservaRespuestaDTO> exitosas = new ArrayList<>();
            for (var futuro : futuros) {
                try {
                    var resultado = futuro.get(10, TimeUnit.SECONDS);
                    if (resultado.reserva() != null) {
                        exitosas.add(resultado.reserva());
                    }
                } catch (ExecutionException ex) {
                    throw new AssertionError("Falló una escritura concurrente", ex.getCause());
                }
            }
            return exitosas;
        }
    }

    private record ResultadoConcurrente(ReservaRespuestaDTO reserva) { }

    private void avanzarA(Instant instante) {
        when(reloj.instant()).thenReturn(instante);
    }

    private Evento otroEvento() {
        return guardarEvento(organizador, "Otro", "Bilbao", PUBLICADO, INICIO, null, 10);
    }

    private TipoEntrada guardarTipo(Evento paraEvento, String nombre, int cantidad, BigDecimal precio) {
        return tiposEntrada.save(new TipoEntrada(nombre, null, precio, cantidad, paraEvento));
    }

    private Reserva guardarReservaHistorica(EstadoReserva estado, String clave) {
        Reserva reserva = new Reserva(comprador, evento, tipo, 1, AHORA.minusSeconds(600), AHORA,
                tipo.getPrecio(), clave);
        reserva.setEstado(estado);
        return reservas.save(reserva);
    }

    private String tokenDe(String email) throws Exception {
        guardarUsuario(email, Usuario.Rol.COMPRADOR);
        return iniciarSesion(email, PASSWORD);
    }

    private long leerId(ResultActions respuesta) {
        return json.readTree(respuesta.andReturn().getResponse().getContentAsByteArray()).get("id").asLong();
    }

    private ResultActions reservar(String auth, Evento paraEvento, TipoEntrada modalidad, int cantidad, String clave)
            throws Exception {
        return mvc.perform(post(RUTA).header(AUTHORIZATION, auth).contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("eventoId", paraEvento.getId(), "tipoEntradaId", modalidad.getId(),
                        "cantidad", cantidad, "claveIdempotencia", clave))));
    }

    private ResultActions cancelar(String auth, long id) throws Exception {
        return mvc.perform(patch(RUTA + "/{id}/cancelar", id).header(AUTHORIZATION, auth));
    }

    private ResultActions disponibilidad(Evento paraEvento, TipoEntrada modalidad) throws Exception {
        return mvc.perform(get(EVENTOS + "/{evento}/tipos-entrada/{tipo}/disponibilidad",
                paraEvento.getId(), modalidad.getId()));
    }

    private ResultActions editarEvento(String auth, String ubicacion) throws Exception {
        return mvc.perform(put(EVENTOS + "/{id}", evento.getId()).header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("titulo", "Nuevo título",
                        "descripcion", "Descripción", "fechaInicio", INICIO.toString(), "ubicacion", ubicacion,
                        "direccion", "Dirección", "aforoMaximo", 10))));
    }
}
