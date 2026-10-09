package com.quini6.analytics.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entidad;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "entidad_id")
    private java.util.UUID entidadId;

    @Column(name = "detalle_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String detalleJson;

    @Column(name = "diff_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String diffJson;

    @PrePersist
    void prePersist() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public AuditLog() {}

    public AuditLog(String entidad, String accion, String actor) {
        this.entidad = entidad;
        this.accion = accion;
        this.actor = actor;
    }

    public static AuditLog ingesta(String actor, String entidad, java.util.UUID entidadId, String detalle) {
        AuditLog log = new AuditLog(entidad, "INGESTA", actor);
        log.entidadId = entidadId;
        log.detalleJson = detalle;
        return log;
    }

    public Long getId() { return id; }
    public String getEntidad() { return entidad; }
    public String getAccion() { return accion; }
    public String getActor() { return actor; }
    public Instant getTimestamp() { return timestamp; }
    public java.util.UUID getEntidadId() { return entidadId; }
    public String getDetalleJson() { return detalleJson; }
    public String getDiffJson() { return diffJson; }

    public void setDetalleJson(String json) { this.detalleJson = json; }
    public void setDiffJson(String json) { this.diffJson = json; }
}
