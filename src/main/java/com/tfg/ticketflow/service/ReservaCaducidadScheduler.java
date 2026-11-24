package com.tfg.ticketflow.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tfg.ticketflow.entity.Reserva.EstadoReserva;
import com.tfg.ticketflow.repository.ReservaRepository;

@Component
public class ReservaCaducidadScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ReservaCaducidadScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaCaducidadService caducidadService;
    private final Clock clock;

    public ReservaCaducidadScheduler(ReservaRepository reservaRepository,
            ReservaCaducidadService caducidadService, Clock reservasClock) {
        this.reservaRepository = reservaRepository;
        this.caducidadService = caducidadService;
        this.clock = reservasClock;
    }

    // La liberación de aforo NO depende del scheduler: las consultas ya ignoran
    // reservas vencidas. Este proceso mantiene el estado histórico en MySQL.
    @Scheduled(fixedDelayString = "${ticketflow.reservas.limpieza-ms:30000}",
            initialDelayString = "${ticketflow.reservas.limpieza-ms:30000}")
    public void caducarReservas() {
        Instant ahora = clock.instant();
        List<Long> eventos = reservaRepository.buscarEventosConReservasVencidas(
                EstadoReserva.ACTIVA, ahora);

        for (Long eventoId : eventos) {
            try {
                caducidadService.caducarReservasDelEvento(eventoId, ahora);
            } catch (RuntimeException ex) {
                log.warn("Error al caducar reservas del evento {}", eventoId, ex);
            }
        }
    }
}
