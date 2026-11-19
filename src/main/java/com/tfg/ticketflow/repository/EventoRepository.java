
package com.tfg.ticketflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Buscar eventos por su estado
    List<Evento> findByEstado(Evento.EstadoEvento estado);

    // Comprobar si un organizador ya tiene un evento con ese título
    boolean existsByTituloAndOrganizador(String titulo, Usuario organizador);

    // Buscar todos los eventos de un organizador
    List<Evento> findByOrganizador(Usuario organizador);

    // Buscar un evento por su identificador y organizador
    Optional<Evento> findByIdAndOrganizador(Long id, Usuario organizador);
}
