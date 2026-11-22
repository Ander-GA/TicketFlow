
package com.tfg.ticketflow.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Gestionar los errores de validación de los DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> gestionarErroresValidacion(MethodArgumentNotValidException ex) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Hay errores en los datos introducidos.");
        respuesta.put("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // Gestionar los argumentos incorrectos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> gestionarArgumentosInvalidos(IllegalArgumentException ex) {
        return crearRespuestaError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Gestionar los recursos que no existen
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<?> gestionarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return crearRespuestaError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Gestionar los conflictos con las reglas de negocio
    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<?> gestionarConflictos(ConflictoNegocioException ex) {
        return crearRespuestaError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Gestionar los errores de autenticación
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> gestionarAutenticacionFallida(AuthenticationException ex) {

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Correo o contraseña incorrectos.");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
    }

    // Construir una respuesta de error con un formato común
    private ResponseEntity<?> crearRespuestaError(HttpStatus estado, String detalle) {

        Map<String, String> errores = new LinkedHashMap<>();
        errores.put("general", detalle);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "No se ha podido completar la operación.");
        respuesta.put("errores", errores);

        return ResponseEntity.status(estado).body(respuesta);
    }
}
