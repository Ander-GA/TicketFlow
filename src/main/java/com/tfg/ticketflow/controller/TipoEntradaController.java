package com.tfg.ticketflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tfg.ticketflow.dto.tipoentrada.TipoEntradaActivoDTO;
import com.tfg.ticketflow.dto.tipoentrada.TipoEntradaActualizacionDTO;
import com.tfg.ticketflow.dto.tipoentrada.TipoEntradaCreacionDTO;
import com.tfg.ticketflow.dto.tipoentrada.TipoEntradaRespuestaDTO;
import com.tfg.ticketflow.entity.TipoEntrada;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.service.TipoEntradaService;
import com.tfg.ticketflow.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/tipos-entrada")
public class TipoEntradaController {

    private final TipoEntradaService tipoEntradaService;
    private final UsuarioService usuarioService;

    public TipoEntradaController(TipoEntradaService tipoEntradaService,
            UsuarioService usuarioService) {
        this.tipoEntradaService = tipoEntradaService;
        this.usuarioService = usuarioService;
    }

    // Consulta pública de tipos activos en eventos publicados.
    @GetMapping
    public ResponseEntity<List<TipoEntradaRespuestaDTO>> listarPublicos(
            @PathVariable Long eventoId) {
        List<TipoEntradaRespuestaDTO> respuesta = tipoEntradaService
                .obtenerTiposPublicos(eventoId).stream()
                .map(TipoEntradaRespuestaDTO::new).toList();
        return ResponseEntity.ok(respuesta);
    }

    // El propietario puede consultar también los desactivados.
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/gestion")
    public ResponseEntity<List<TipoEntradaRespuestaDTO>> listarParaGestion(
            @PathVariable Long eventoId, Authentication autenticacion) {
        Usuario organizador = obtenerOrganizador(autenticacion);
        List<TipoEntradaRespuestaDTO> respuesta = tipoEntradaService
                .obtenerTiposDelOrganizador(eventoId, organizador).stream()
                .map(TipoEntradaRespuestaDTO::new).toList();
        return ResponseEntity.ok(respuesta);
    }

    // Crear modalidad de entrada.
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<TipoEntradaRespuestaDTO> crearTipo(
            @PathVariable Long eventoId,
            @Valid @RequestBody TipoEntradaCreacionDTO datos,
            Authentication autenticacion) {
        Usuario organizador = obtenerOrganizador(autenticacion);
        TipoEntrada tipo = tipoEntradaService.crearTipoEntrada(
                eventoId, organizador, datos.getNombre(), datos.getDescripcion(),
                datos.getPrecio(), datos.getCantidadTotal(), datos.getFechaInicioVenta());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TipoEntradaRespuestaDTO(tipo));
    }

    // Modificar datos comerciales y cupo.
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{tipoId}")
    public ResponseEntity<TipoEntradaRespuestaDTO> actualizarTipo(
            @PathVariable Long eventoId, @PathVariable Long tipoId,
            @Valid @RequestBody TipoEntradaActualizacionDTO datos,
            Authentication autenticacion) {
        Usuario organizador = obtenerOrganizador(autenticacion);
        TipoEntrada tipo = tipoEntradaService.actualizarTipoEntrada(
                eventoId, tipoId, organizador, datos.getNombre(),
                datos.getDescripcion(), datos.getPrecio(),
                datos.getCantidadTotal(), datos.getFechaInicioVenta());
        return ResponseEntity.ok(new TipoEntradaRespuestaDTO(tipo));
    }

    // Activar o desactivar sin liberar el cupo asignado.
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{tipoId}/activo")
    public ResponseEntity<TipoEntradaRespuestaDTO> cambiarActivo(
            @PathVariable Long eventoId, @PathVariable Long tipoId,
            @Valid @RequestBody TipoEntradaActivoDTO datos,
            Authentication autenticacion) {
        Usuario organizador = obtenerOrganizador(autenticacion);
        TipoEntrada tipo = tipoEntradaService.cambiarEstadoActivo(
                eventoId, tipoId, organizador, datos.getActivo());
        return ResponseEntity.ok(new TipoEntradaRespuestaDTO(tipo));
    }

    // Eliminar físicamente solo mientras el evento esté en borrador y sin compromisos.
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{tipoId}")
    public ResponseEntity<Void> eliminarTipo(
            @PathVariable Long eventoId, @PathVariable Long tipoId,
            Authentication autenticacion) {
        Usuario organizador = obtenerOrganizador(autenticacion);
        tipoEntradaService.eliminarTipoEntrada(eventoId, tipoId, organizador);
        return ResponseEntity.noContent().build();
    }

    private Usuario obtenerOrganizador(Authentication autenticacion) {
        return usuarioService.buscarPorEmail(autenticacion.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el usuario."));
    }
}
