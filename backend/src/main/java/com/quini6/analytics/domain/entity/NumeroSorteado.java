package com.quini6.analytics.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "numeros_sorteados")
public class NumeroSorteado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resultado_id", nullable = false)
    private java.util.UUID resultadoId;

    @Column(name = "sorteo_id", nullable = false)
    private java.util.UUID sorteoId;

    @Column(nullable = false)
    private String modalidad;

    @Column(nullable = false)
    private Short numero;

    @Column(nullable = false)
    private Short posicion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_sorteo", nullable = false)
    private Integer numeroSorteo;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public java.util.UUID getResultadoId() { return resultadoId; }
    public java.util.UUID getSorteoId() { return sorteoId; }
    public String getModalidad() { return modalidad; }
    public Short getNumero() { return numero; }
    public Short getPosicion() { return posicion; }
    public LocalDate getFecha() { return fecha; }
    public Integer getNumeroSorteo() { return numeroSorteo; }
    public Instant getCreatedAt() { return createdAt; }
}
