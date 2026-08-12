package com.quini6.analytics.domain.repository;

import com.quini6.analytics.domain.entity.Resultado;
import com.quini6.analytics.domain.enums.Modalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultadoRepository extends JpaRepository<Resultado, UUID> {

    List<Resultado> findBySorteoId(UUID sorteoId);

    List<Resultado> findByModalidad(Modalidad modalidad);

    Optional<Resultado> findBySorteoIdAndModalidad(UUID sorteoId, Modalidad modalidad);

    @Query("SELECT r FROM Resultado r JOIN FETCH r.sorteo WHERE r.modalidad = :modalidad")
    List<Resultado> findHistorialByModalidad(@Param("modalidad") Modalidad modalidad);

    @Query("SELECT r FROM Resultado r JOIN FETCH r.sorteo WHERE r.sorteo.fecha BETWEEN :desde AND :hasta AND r.modalidad = :modalidad")
    List<Resultado> findByRangoFechasYModalidad(
        @Param("desde") java.time.LocalDate desde,
        @Param("hasta") java.time.LocalDate hasta,
        @Param("modalidad") Modalidad modalidad
    );
}
