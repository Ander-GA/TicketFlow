package com.tfg.ticketflow.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfg.ticketflow.entity.Reserva;
import com.tfg.ticketflow.entity.Reserva.EstadoReserva;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @EntityGraph(attributePaths = { "evento", "tipoEntrada" })
    Optional<Reserva> findByUsuarioIdAndClaveIdempotenciaHash(Long usuarioId, String claveIdempotenciaHash);

    @EntityGraph(attributePaths = { "evento", "tipoEntrada" })
    List<Reserva> findByUsuarioIdOrderByFechaCreacionDescIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = { "evento", "tipoEntrada" })
    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);

    // Primera consulta para conocer el evento antes de bloquearlo al cancelar.
    @Query("SELECT r.evento.id FROM Reserva r WHERE r.id = :reservaId AND r.usuario.id = :usuarioId")
    Optional<Long> obtenerEventoIdDeReservaPropia(
            @Param("reservaId") Long reservaId, @Param("usuarioId") Long usuarioId);

    // Lectura actual con bloqueo tras bloquear el evento; evita modificar un estado obsoleto.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :reservaId AND r.usuario.id = :usuarioId")
    Optional<Reserva> bloquearReservaPropia(
            @Param("reservaId") Long reservaId, @Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.usuario.id = :usuarioId AND r.tipoEntrada.id = :tipoId
              AND r.estado = :activa AND r.fechaExpiracion > :ahora
            """)
    long contarActivasDelUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("tipoId") Long tipoId,
            @Param("activa") EstadoReserva activa,
            @Param("ahora") Instant ahora);

    // Las CONFIRMADAS consumen cupo permanentemente; las ACTIVA solo hasta expirar.
    @Query("""
            SELECT COALESCE(SUM(r.cantidad), 0) FROM Reserva r
            WHERE r.tipoEntrada.id = :tipoId
              AND (r.estado = :confirmada OR
                   (r.estado = :activa AND r.fechaExpiracion > :ahora))
            """)
    Long sumarComprometidasPorTipo(
            @Param("tipoId") Long tipoId,
            @Param("activa") EstadoReserva activa,
            @Param("confirmada") EstadoReserva confirmada,
            @Param("ahora") Instant ahora);

    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.evento.id = :eventoId
              AND (r.estado = :confirmada OR
                   (r.estado = :activa AND r.fechaExpiracion > :ahora))
            """)
    long contarCompromisosPorEvento(
            @Param("eventoId") Long eventoId,
            @Param("activa") EstadoReserva activa,
            @Param("confirmada") EstadoReserva confirmada,
            @Param("ahora") Instant ahora);

    // Identificar eventos con reservas caducadas para su limpieza periódica.
    @Query("""
            SELECT DISTINCT r.evento.id FROM Reserva r
            WHERE r.estado = :activa AND r.fechaExpiracion <= :ahora
            """)
    List<Long> buscarEventosConReservasVencidas(
            @Param("activa") EstadoReserva activa,
            @Param("ahora") Instant ahora);

    // Solo se ejecuta bajo el bloqueo pesimista del evento.
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Reserva r SET r.estado = :caducada
            WHERE r.evento.id = :eventoId AND r.estado = :activa
              AND r.fechaExpiracion <= :ahora
            """)
    int caducarVencidasDelEvento(
            @Param("eventoId") Long eventoId,
            @Param("activa") EstadoReserva activa,
            @Param("caducada") EstadoReserva caducada,
            @Param("ahora") Instant ahora);

    // Solo se ejecuta bajo el bloqueo pesimista del evento.
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Reserva r SET r.estado = :cancelada
            WHERE r.evento.id = :eventoId AND r.estado = :activa
              AND r.fechaExpiracion > :ahora
            """)
    int cancelarActivasDelEvento(
            @Param("eventoId") Long eventoId,
            @Param("activa") EstadoReserva activa,
            @Param("cancelada") EstadoReserva cancelada,
            @Param("ahora") Instant ahora);
}
