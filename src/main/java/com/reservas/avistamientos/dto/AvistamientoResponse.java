package com.reservas.avistamientos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.reservas.avistamientos.model.Avistamiento;

import java.time.LocalDateTime;

public record AvistamientoResponse(

        Long id,
        String especie,
        String nombreCientifico,
        String zona,
        Double latitud,
        Double longitud,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fechaAvistamiento,

        String observaciones,
        String registradoPor,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fechaRegistro
) {

    public static AvistamientoResponse desde(Avistamiento entidad) {
        return new AvistamientoResponse(
                entidad.getId(),
                entidad.getEspecie(),
                entidad.getNombreCientifico(),
                entidad.getZona(),
                entidad.getLatitud(),
                entidad.getLongitud(),
                entidad.getFechaAvistamiento(),
                entidad.getObservaciones(),
                entidad.getRegistradoPor(),
                entidad.getFechaRegistro()
        );
    }
}
