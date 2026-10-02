package com.reservas.avistamientos.service;

import com.reservas.avistamientos.repository.AvistamientoRepository;
import org.springframework.stereotype.Service;

@Service
public class AvistamientoService {

    private final AvistamientoRepository repositorio;

    public AvistamientoService(AvistamientoRepository repositorio) {
        this.repositorio = repositorio;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
