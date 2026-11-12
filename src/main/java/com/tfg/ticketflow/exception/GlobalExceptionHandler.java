package com.tfg.ticketflow.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Gestiona los errores de validacion de los DTO
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

    // Gestiona los errores de argumentos no validos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> gestionarArgumentosInvalidos(IllegalArgumentException ex) {

        Map<String, String> errores = new LinkedHashMap<>();
        errores.put("general", ex.getMessage());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "No se ha podido completar la operacion.");
        respuesta.put("errores", errores);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }
}