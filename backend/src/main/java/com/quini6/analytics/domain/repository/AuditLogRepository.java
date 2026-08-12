package com.quini6.analytics.domain.repository;

import com.quini6.analytics.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntidad(String entidad, Pageable pageable);

    List<AuditLog> findByAccionAndTimestampBetween(
        String accion, Instant desde, Instant hasta
    );

    Page<AuditLog> findByActor(String actor, Pageable pageable);
}
