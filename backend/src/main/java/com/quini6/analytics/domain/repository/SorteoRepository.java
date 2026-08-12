package com.quini6.analytics.domain.repository;

import com.quini6.analytics.domain.entity.Sorteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SorteoRepository extends JpaRepository<Sorteo, UUID> {

    Optional<Sorteo> findByNumeroSorteo(Integer numeroSorteo);

    boolean existsByNumeroSorteo(Integer numeroSorteo);

    List<Sorteo> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Sorteo> findAllByOrderByFechaDesc();

    @Query("SELECT s FROM Sorteo s WHERE s.fecha >= :desde ORDER BY s.fecha DESC")
    List<Sorteo> findRecentesDesde(@Param("desde") LocalDate desde);

    @Query("SELECT MAX(s.numeroSorteo) FROM Sorteo s")
    Optional<Integer> findMaxNumeroSorteo();
}
