package com.tfg.ticketflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.TipoEntradaRepository;

class EventoServiceTests {

    private static final long EVENTO_ID = 1L;

    private final EventoRepository eventos = mock(EventoRepository.class);
    private final TipoEntradaRepository tiposEntrada = mock(TipoEntradaRepository.class);
    private final EventoService servicio = new EventoService(eventos, tiposEntrada);
    private final Usuario organizador = new Usuario();
    private final Evento evento = new Evento();

    @BeforeEach
    void prepararEventoPublicado() {
        evento.setEstado(Evento.EstadoEvento.PUBLICADO);
        when(eventos.buscarParaModificar(EVENTO_ID, organizador)).thenReturn(Optional.of(evento));
    }

    @Test
    void rechazaMoverUnEventoPublicadoAlPasado() {
        LocalDateTime inicioOriginal = LocalDateTime.now().plusDays(1);
        evento.setFechaInicio(inicioOriginal);

        assertThrows(ConflictoNegocioException.class,
                () -> actualizarConInicio(LocalDateTime.now().minusDays(1)));

        assertEquals(inicioOriginal, evento.getFechaInicio());
        verify(eventos).buscarParaModificar(EVENTO_ID, organizador);
        verifyNoMoreInteractions(eventos);
        verifyNoInteractions(tiposEntrada);
    }

    @Test
    void permiteCompletarElAforoSinCambiarUnInicioPasado() {
        LocalDateTime inicioOriginal = LocalDateTime.now().minusDays(1);
        evento.setFechaInicio(inicioOriginal);
        when(tiposEntrada.sumarCantidadTotal(EVENTO_ID)).thenReturn(10L);

        actualizarConInicio(inicioOriginal);

        assertEquals(inicioOriginal, evento.getFechaInicio());
        assertEquals(100, evento.getAforoMaximo());
        assertEquals(Evento.EstadoEvento.PUBLICADO, evento.getEstado());
        verify(eventos).save(evento);
    }

    @Test
    void permiteReprogramarUnEventoPublicadoAUnaFechaFutura() {
        evento.setFechaInicio(LocalDateTime.now().plusDays(1));
        LocalDateTime nuevoInicio = LocalDateTime.now().plusDays(2);
        when(tiposEntrada.sumarCantidadTotal(EVENTO_ID)).thenReturn(10L);

        actualizarConInicio(nuevoInicio);

        assertEquals(nuevoInicio, evento.getFechaInicio());
        assertEquals(Evento.EstadoEvento.PUBLICADO, evento.getEstado());
        verify(eventos).save(evento);
    }

    private void actualizarConInicio(LocalDateTime fechaInicio) {
        servicio.actualizarEvento(EVENTO_ID, organizador, "Evento", "Descripción",
                fechaInicio, null, "Recinto", "Dirección", 100);
    }
}
