package com.reservas.avistamientos.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record RespuestaError(
        LocalDateTime momento,
        int estado,
        String error,
        String mensaje,
        Map<String, String> detalles
) {

    public static RespuestaError de(int estado, String error, String mensaje) {
        return new RespuestaError(LocalDateTime.now(), estado, error, mensaje, null);
    }
}
