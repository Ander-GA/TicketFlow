
package com.tfg.ticketflow.exception;

import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Errores de validación de los DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> gestionarErroresValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(
                        error.getField(),
                        error.getDefaultMessage())
        );

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Hay errores en los datos introducidos.");
        respuesta.put("errores", errores);

        return ResponseEntity.badRequest().body(respuesta);
    }

    // Argumentos inválidos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> gestionarArgumentosInvalidos(
            IllegalArgumentException ex) {

        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "No se ha podido completar la operación.",
                ex.getMessage()
        );
    }

    // Recurso inexistente
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<?> gestionarRecursoNoEncontrado(
            RecursoNoEncontradoException ex) {

        return crearRespuesta(
                HttpStatus.NOT_FOUND,
                "No se ha encontrado el recurso.",
                ex.getMessage()
        );
    }

    // Reglas de negocio incumplidas
    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<?> gestionarConflicto(
            ConflictoNegocioException ex) {

        return crearRespuesta(
                HttpStatus.CONFLICT,
                "No se ha podido completar la operación.",
                ex.getMessage()
        );
    }

    // Archivo demasiado grande
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> gestionarArchivoDemasiadoGrande(
            MaxUploadSizeExceededException ex) {

        return crearRespuesta(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "No se ha podido subir la imagen.",
                "La imagen supera el tamaño máximo permitido."
        );
    }

    // Errores del sistema de almacenamiento
    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<?> gestionarErrorAlmacenamiento(
            UncheckedIOException ex) {

        return crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se ha podido completar la operación.",
                "Ha ocurrido un error al almacenar el archivo."
        );
    }

    // Credenciales incorrectas
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> gestionarAutenticacionFallida(
            AuthenticationException ex) {

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Correo o contraseña incorrectos.");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(respuesta);
    }

    // Construir una respuesta de error consistente
    private ResponseEntity<?> crearRespuesta(
            HttpStatus estado,
            String mensaje,
            String detalle) {

        Map<String, String> errores = new LinkedHashMap<>();
        errores.put("general", detalle);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", mensaje);
        respuesta.put("errores", errores);

        return ResponseEntity.status(estado).body(respuesta);
    }
}
