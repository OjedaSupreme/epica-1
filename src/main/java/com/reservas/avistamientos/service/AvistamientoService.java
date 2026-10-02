package com.reservas.avistamientos.service;

import com.reservas.avistamientos.dto.AvistamientoRequest;
import com.reservas.avistamientos.dto.AvistamientoResponse;
import com.reservas.avistamientos.exception.RecursoNoEncontradoException;
import com.reservas.avistamientos.model.Avistamiento;
import com.reservas.avistamientos.repository.AvistamientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<AvistamientoResponse> listarTodos() {
        return repositorio.buscarConFiltros(null, null, null, null)
                .stream()
                .map(AvistamientoResponse::desde)
                .toList();
    }

    public List<AvistamientoResponse> buscar(String especie,
                                             String zona,
                                             LocalDateTime desde,
                                             LocalDateTime hasta) {
        List<AvistamientoResponse> resultado = repositorio
                .buscarConFiltros(normalizar(especie), normalizar(zona), desde, hasta)
                .stream()
                .map(AvistamientoResponse::desde)
                .toList();

        log.debug("Busqueda especie={} zona={} desde={} hasta={} -> {} resultados",
                especie, zona, desde, hasta, resultado.size());

        return resultado;
    }

    public AvistamientoResponse obtenerPorId(Long id) {
        return repositorio.findById(id)
                .map(AvistamientoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un avistamiento con id " + id));
    }

    public Map<String, Long> resumenPorEspecie() {
        Map<String, Long> resumen = new LinkedHashMap<>();
        for (Object[] fila : repositorio.contarPorEspecie()) {
            resumen.put((String) fila[0], (Long) fila[1]);
        }
        return resumen;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
