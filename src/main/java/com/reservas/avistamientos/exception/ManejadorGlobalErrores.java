package com.reservas.avistamientos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorGlobalErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.put(error.getField(), error.getDefaultMessage());
        }

        RespuestaError cuerpo = new RespuestaError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Datos invalidos",
                "La peticion contiene campos que no cumplen las reglas de validacion",
                detalles
        );
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(RespuestaError.de(HttpStatus.NOT_FOUND.value(), "No encontrado", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parametro " + ex.getName() + " tiene un formato invalido";
        return ResponseEntity
                .badRequest()
                .body(RespuestaError.de(HttpStatus.BAD_REQUEST.value(), "Parametro invalido", mensaje));
    }
}
