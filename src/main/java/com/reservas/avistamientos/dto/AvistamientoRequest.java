package com.reservas.avistamientos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AvistamientoRequest(

        @NotBlank(message = "La especie es obligatoria")
        @Size(max = 120, message = "La especie no puede exceder 120 caracteres")
        String especie,

        @Size(max = 150, message = "El nombre cientifico no puede exceder 150 caracteres")
        String nombreCientifico,

        @NotBlank(message = "La zona es obligatoria")
        @Size(max = 120, message = "La zona no puede exceder 120 caracteres")
        String zona,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud minima es -90")
        @DecimalMax(value = "90.0", message = "La latitud maxima es 90")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud minima es -180")
        @DecimalMax(value = "180.0", message = "La longitud maxima es 180")
        Double longitud,

        @NotNull(message = "La fecha del avistamiento es obligatoria")
        @PastOrPresent(message = "La fecha del avistamiento no puede ser futura")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fechaAvistamiento,

        @Size(max = 2000, message = "Las observaciones no pueden exceder 2000 caracteres")
        String observaciones,

        @NotBlank(message = "Debe indicar quien realiza el registro")
        @Size(max = 120, message = "El nombre del responsable no puede exceder 120 caracteres")
        String registradoPor
) {
}
