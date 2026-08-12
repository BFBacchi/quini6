package com.quini6.analytics.audit;

import com.quini6.analytics.domain.entity.AuditLog;
import com.quini6.analytics.domain.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(String entidad, String accion, String actor, String detalle) {
        AuditLog log = AuditLog.ingesta(actor, entidad, null, detalle);
        auditLogRepository.save(log);
    }

    public void registrar(String entidad, String accion, String actor,
                          java.util.UUID entidadId, String detalle) {
        AuditLog log = AuditLog.ingesta(actor, entidad, entidadId, detalle);
        auditLogRepository.save(log);
    }
}
