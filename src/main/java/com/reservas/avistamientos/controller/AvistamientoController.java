package com.reservas.avistamientos.controller;

import com.reservas.avistamientos.service.AvistamientoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/avistamientos")
public class AvistamientoController {

    private final AvistamientoService servicio;

    public AvistamientoController(AvistamientoService servicio) {
        this.servicio = servicio;
    }
}
