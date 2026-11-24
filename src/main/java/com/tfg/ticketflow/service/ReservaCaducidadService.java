package com.tfg.ticketflow.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.ticketflow.entity.Reserva.EstadoReserva;
import com.tfg.ticketflow.repository.EventoRepository;
import com.tfg.ticketflow.repository.ReservaRepository;

@Service
public class ReservaCaducidadService {

    private final EventoRepository eventoRepository;
    private final ReservaRepository reservaRepository;

    public ReservaCaducidadService(EventoRepository eventoRepository,
            ReservaRepository reservaRepository) {
        this.eventoRepository = eventoRepository;
        this.reservaRepository = reservaRepository;
    }

    // Misma disciplina de bloqueo que la creación/cancelación de reservas.
    @Transactional
    public int caducarReservasDelEvento(Long eventoId, Instant ahora) {
        if (eventoRepository.bloquearPorId(eventoId).isEmpty()) {
            return 0;
        }
        return reservaRepository.caducarVencidasDelEvento(
                eventoId, EstadoReserva.ACTIVA, EstadoReserva.CADUCADA, ahora);
    }
}
