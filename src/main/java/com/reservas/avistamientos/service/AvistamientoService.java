package com.reservas.avistamientos.service;

import com.reservas.avistamientos.dto.AvistamientoRequest;
import com.reservas.avistamientos.dto.AvistamientoResponse;
import com.reservas.avistamientos.model.Avistamiento;
import com.reservas.avistamientos.repository.AvistamientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AvistamientoService {

    private static final Logger log = LoggerFactory.getLogger(AvistamientoService.class);

    private final AvistamientoRepository repositorio;

    public AvistamientoService(AvistamientoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public AvistamientoResponse registrar(AvistamientoRequest peticion) {
        Avistamiento entidad = new Avistamiento(
                normalizar(peticion.especie()),
                normalizar(peticion.nombreCientifico()),
                normalizar(peticion.zona()),
                peticion.latitud(),
                peticion.longitud(),
                peticion.fechaAvistamiento(),
                normalizar(peticion.observaciones()),
                normalizar(peticion.registradoPor())
        );

        Avistamiento guardado = repositorio.save(entidad);

        log.info("Avistamiento registrado id={} especie={} zona={}",
                guardado.getId(), guardado.getEspecie(), guardado.getZona());

        return AvistamientoResponse.desde(guardado);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
