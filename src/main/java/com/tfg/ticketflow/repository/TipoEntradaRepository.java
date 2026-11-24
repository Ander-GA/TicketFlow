
package com.tfg.ticketflow.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfg.ticketflow.entity.Evento.EstadoEvento;
import com.tfg.ticketflow.entity.TipoEntrada;

public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {

    // Obtener los tipos de entrada de un evento
    List<TipoEntrada> findByEventoId(Long eventoId);

    @EntityGraph(attributePaths = "evento")
    List<TipoEntrada> findByEventoIdOrderByIdAsc(Long eventoId);

    @EntityGraph(attributePaths = "evento")
    List<TipoEntrada> findByEventoIdAndEventoEstadoAndActivoTrueOrderByIdAsc(Long eventoId, EstadoEvento estado);

    Optional<TipoEntrada> findByIdAndEventoId(Long id, Long eventoId);

    @EntityGraph(attributePaths = "evento")
    Optional<TipoEntrada> findByIdAndEventoIdAndEventoEstadoAndActivoTrue(
            Long id, Long eventoId, EstadoEvento estado);

    // Comprobar si ya existe un tipo de entrada con ese nombre
    boolean existsByEventoIdAndNombreIgnoreCase(Long eventoId, String nombre);

    boolean existsByEventoIdAndNombreIgnoreCaseAndIdNot(Long eventoId, String nombre, Long id);

    boolean existsByEventoIdAndActivoTrueAndCantidadTotalGreaterThan(Long eventoId, Integer cantidad);

    boolean existsByEventoIdAndFechaInicioVentaGreaterThanEqual(Long eventoId, LocalDateTime fecha);

    // Sumar las cantidades asignadas a los tipos de entrada
    @Query("""
            SELECT COALESCE(SUM(t.cantidadTotal), 0)
            FROM TipoEntrada t
            WHERE t.evento.id = :eventoId
            """)
    Long sumarCantidadTotal(@Param("eventoId") Long eventoId);
}
