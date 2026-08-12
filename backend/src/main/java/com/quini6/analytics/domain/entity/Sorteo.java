package com.quini6.analytics.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sorteos")
public class Sorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_sorteo", unique = true, nullable = false)
    private Integer numeroSorteo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "pozo_monto")
    private BigDecimal pozoMonto;

    @Column(name = "fuente_api", nullable = false)
    private String fuenteApi;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @OneToMany(mappedBy = "sorteo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Resultado> resultados = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Sorteo() {}

    public Sorteo(Integer numeroSorteo, LocalDate fecha) {
        this.numeroSorteo = numeroSorteo;
        this.fecha = fecha;
    }

    public UUID getId() { return id; }
    public Integer getNumeroSorteo() { return numeroSorteo; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getPozoMonto() { return pozoMonto; }
    public String getFuenteApi() { return fuenteApi; }
    public String getRawJson() { return rawJson; }
    public List<Resultado> getResultados() { return resultados; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setNumeroSorteo(Integer numeroSorteo) { this.numeroSorteo = numeroSorteo; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public void setPozoMonto(BigDecimal pozoMonto) { this.pozoMonto = pozoMonto; }
    public void setFuenteApi(String fuenteApi) { this.fuenteApi = fuenteApi; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public void setResultados(List<Resultado> resultados) { this.resultados = resultados; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sorteo other)) return false;
        return numeroSorteo != null && Objects.equals(numeroSorteo, other.numeroSorteo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroSorteo);
    }

    @Override
    public String toString() {
        return "Sorteo{n°" + numeroSorteo + ", fecha=" + fecha + "}";
    }
}
