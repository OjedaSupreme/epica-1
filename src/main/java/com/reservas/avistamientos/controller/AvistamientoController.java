package com.reservas.avistamientos.controller;

import com.reservas.avistamientos.dto.AvistamientoRequest;
import com.reservas.avistamientos.dto.AvistamientoResponse;
import com.reservas.avistamientos.service.AvistamientoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/avistamientos")
public class AvistamientoController {

    private final AvistamientoService servicio;

    public AvistamientoController(AvistamientoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<AvistamientoResponse> registrar(@Valid @RequestBody AvistamientoRequest peticion,
                                                          UriComponentsBuilder uriBuilder) {
        AvistamientoResponse creado = servicio.registrar(peticion);

        URI ubicacion = uriBuilder
                .path("/api/avistamientos/{id}")
                .buildAndExpand(creado.id())
                .toUri();

        return ResponseEntity.created(ubicacion).body(creado);
    }
}
