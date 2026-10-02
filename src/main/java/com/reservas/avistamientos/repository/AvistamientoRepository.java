package com.reservas.avistamientos.repository;

import com.reservas.avistamientos.model.Avistamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvistamientoRepository extends JpaRepository<Avistamiento, Long> {
}
