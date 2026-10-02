package com.reservas.avistamientos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "avistamientos")
public class Avistamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String especie;

    @Column(name = "nombre_cientifico", length = 150)
    private String nombreCientifico;

    @Column(nullable = false, length = 120)
    private String zona;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(name = "fecha_avistamiento", nullable = false)
    private LocalDateTime fechaAvistamiento;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "registrado_por", nullable = false, length = 120)
    private String registradoPor;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public Avistamiento() {
    }

    public Avistamiento(String especie,
                        String nombreCientifico,
                        String zona,
                        Double latitud,
                        Double longitud,
                        LocalDateTime fechaAvistamiento,
                        String observaciones,
                        String registradoPor) {
        this.especie = especie;
        this.nombreCientifico = nombreCientifico;
        this.zona = zona;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fechaAvistamiento = fechaAvistamiento;
        this.observaciones = observaciones;
        this.registradoPor = registradoPor;
    }

    @PrePersist
    void alPersistir() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getNombreCientifico() {
        return nombreCientifico;
    }

    public void setNombreCientifico(String nombreCientifico) {
        this.nombreCientifico = nombreCientifico;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public LocalDateTime getFechaAvistamiento() {
        return fechaAvistamiento;
    }

    public void setFechaAvistamiento(LocalDateTime fechaAvistamiento) {
        this.fechaAvistamiento = fechaAvistamiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return "Avistamiento{id=%d, especie='%s', zona='%s', fechaAvistamiento=%s}"
                .formatted(id, especie, zona, fechaAvistamiento);
    }
}
