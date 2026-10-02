package com.reservas.avistamientos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.avistamientos.dto.AvistamientoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AvistamientoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registraUnAvistamientoYDevuelve201() throws Exception {
        AvistamientoRequest peticion = new AvistamientoRequest(
                "Puma",
                "Puma concolor",
                "Sector Sur",
                4.6512,
                -74.0921,
                LocalDateTime.now().minusHours(3),
                "Rastro de huellas junto al abrevadero.",
                "Ana Torres"
        );

        mockMvc.perform(post("/api/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.especie").value("Puma"))
                .andExpect(jsonPath("$.fechaRegistro").exists());
    }

    @Test
    void rechazaUnAvistamientoSinEspecieCon400() throws Exception {
        String cuerpoInvalido = """
                {
                  "zona": "Sector Norte",
                  "latitud": 4.7,
                  "longitud": -74.0,
                  "fechaAvistamiento": "2026-09-01T10:00:00",
                  "registradoPor": "Ana Torres"
                }
                """;

        mockMvc.perform(post("/api/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.detalles.especie").exists());
    }

    @Test
    void rechazaFechaFuturaCon400() throws Exception {
        AvistamientoRequest peticion = new AvistamientoRequest(
                "Tucan",
                null,
                "Sector Este",
                4.70,
                -74.05,
                LocalDateTime.now().plusDays(2),
                null,
                "Ana Torres"
        );

        mockMvc.perform(post("/api/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.fechaAvistamiento").exists());
    }
}
