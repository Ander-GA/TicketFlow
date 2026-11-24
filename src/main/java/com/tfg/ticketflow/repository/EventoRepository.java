
package com.tfg.ticketflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;

import jakarta.persistence.LockModeType;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Buscar eventos por su estado
    List<Evento> findByEstado(Evento.EstadoEvento estado);

    // Comprobar si un organizador ya tiene un evento con ese título
    boolean existsByTituloAndOrganizador(String titulo, Usuario organizador);

    // Buscar un evento por título y organizador
    Optional<Evento> findByTituloAndOrganizador(String titulo, Usuario organizador);

    // Buscar todos los eventos de un organizador
    List<Evento> findByOrganizador(Usuario organizador);

    // Buscar un evento por su identificador y organizador
    Optional<Evento> findByIdAndOrganizador(Long id, Usuario organizador);

    // Buscar eventos publicados mediante filtros opcionales
    @Query("""
            SELECT e FROM Evento e
            WHERE e.estado = :estado
            AND (:titulo IS NULL OR LOWER(e.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
            AND (:ubicacion IS NULL OR LOWER(e.ubicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%')))
            """)
    List<Evento> buscarEventosFiltrados(
            @Param("estado") Evento.EstadoEvento estado,
            @Param("titulo") String titulo,
            @Param("ubicacion") String ubicacion);

    // Bloquear un evento durante una operación de modificación
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM Evento e
            WHERE e.id = :id AND e.organizador = :organizador
            """)
    Optional<Evento> buscarParaModificar(
            @Param("id") Long id,
            @Param("organizador") Usuario organizador);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> bloquearPorId(@Param("id") Long id);
}
