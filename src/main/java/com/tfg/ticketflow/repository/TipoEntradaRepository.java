
package com.tfg.ticketflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfg.ticketflow.entity.TipoEntrada;

public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {

    // Obtener los tipos de entrada de un evento
    List<TipoEntrada> findByEventoId(Long eventoId);

    // Comprobar si ya existe un tipo de entrada con ese nombre
    boolean existsByEventoIdAndNombreIgnoreCase(Long eventoId, String nombre);

    // Sumar las cantidades asignadas a los tipos de entrada
    @Query("""
            SELECT COALESCE(SUM(t.cantidadTotal), 0)
            FROM TipoEntrada t
            WHERE t.evento.id = :eventoId
            """)
    Long sumarCantidadTotal(@Param("eventoId") Long eventoId);
}
