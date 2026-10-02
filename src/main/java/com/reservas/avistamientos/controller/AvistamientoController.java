package com.reservas.avistamientos.controller;

import com.reservas.avistamientos.dto.AvistamientoRequest;
import com.reservas.avistamientos.dto.AvistamientoResponse;
import com.reservas.avistamientos.service.AvistamientoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @GetMapping
    public ResponseEntity<List<AvistamientoResponse>> listar(
            @RequestParam(required = false) String especie,
            @RequestParam(required = false) String zona,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        boolean sinFiltros = especie == null && zona == null && desde == null && hasta == null;

        List<AvistamientoResponse> resultado = sinFiltros
                ? servicio.listarTodos()
                : servicio.buscar(especie, zona, desde, hasta);

        return resultado.isEmpty()
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                : ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvistamientoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.obtenerPorId(id));
    }

    @GetMapping("/resumen/especies")
    public ResponseEntity<Map<String, Long>> resumenPorEspecie() {
        return ResponseEntity.ok(servicio.resumenPorEspecie());
    }
}
