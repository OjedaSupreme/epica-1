package com.reservas.avistamientos.repository;

import com.reservas.avistamientos.model.Avistamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvistamientoRepository extends JpaRepository<Avistamiento, Long> {

    @Query("""
            select a from Avistamiento a
            where (:especie is null or lower(a.especie) like lower(concat('%', :especie, '%')))
              and (:zona    is null or lower(a.zona)    like lower(concat('%', :zona,    '%')))
              and (:desde   is null or a.fechaAvistamiento >= :desde)
              and (:hasta   is null or a.fechaAvistamiento <= :hasta)
            order by a.fechaAvistamiento desc
            """)
    List<Avistamiento> buscarConFiltros(@Param("especie") String especie,
                                        @Param("zona") String zona,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    @Query("""
            select a.especie, count(a)
            from Avistamiento a
            group by a.especie
            order by count(a) desc
            """)
    List<Object[]> contarPorEspecie();
}
