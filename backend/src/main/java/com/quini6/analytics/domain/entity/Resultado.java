package com.quini6.analytics.domain.entity;

import com.quini6.analytics.domain.enums.Modalidad;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "resultados",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sorteo_id", "modalidad"})
)
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sorteo_id", nullable = false)
    private Sorteo sorteo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidad modalidad;

    @Column(name = "numero_1", nullable = false)
    private Short numero1;

    @Column(name = "numero_2", nullable = false)
    private Short numero2;

    @Column(name = "numero_3", nullable = false)
    private Short numero3;

    @Column(name = "numero_4", nullable = false)
    private Short numero4;

    @Column(name = "numero_5", nullable = false)
    private Short numero5;

    @Column(name = "numero_6", nullable = false)
    private Short numero6;

    @Column(name = "premio_6_aciertos_ganadores")
    private Integer ganadores6;

    @Column(name = "premio_6_aciertos_monto")
    private BigDecimal monto6;

    @Column(name = "premio_5_aciertos_ganadores")
    private Integer ganadores5;

    @Column(name = "premio_5_aciertos_monto")
    private BigDecimal monto5;

    @Column(name = "premio_4_aciertos_ganadores")
    private Integer ganadores4;

    @Column(name = "premio_4_aciertos_monto")
    private BigDecimal monto4;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Resultado() {}

    public Resultado(Sorteo sorteo, Modalidad modalidad,
                     Short n1, Short n2, Short n3, Short n4, Short n5, Short n6) {
        this.sorteo = sorteo;
        this.modalidad = modalidad;
        this.numero1 = n1;
        this.numero2 = n2;
        this.numero3 = n3;
        this.numero4 = n4;
        this.numero5 = n5;
        this.numero6 = n6;
    }

    public Short[] getNumerosAsArray() {
        return new Short[]{numero1, numero2, numero3, numero4, numero5, numero6};
    }

    public UUID getId() { return id; }
    public Sorteo getSorteo() { return sorteo; }
    public Modalidad getModalidad() { return modalidad; }
    public Short getNumero1() { return numero1; }
    public Short getNumero2() { return numero2; }
    public Short getNumero3() { return numero3; }
    public Short getNumero4() { return numero4; }
    public Short getNumero5() { return numero5; }
    public Short getNumero6() { return numero6; }
    public Integer getGanadores6() { return ganadores6; }
    public BigDecimal getMonto6() { return monto6; }
    public Integer getGanadores5() { return ganadores5; }
    public BigDecimal getMonto5() { return monto5; }
    public Integer getGanadores4() { return ganadores4; }
    public BigDecimal getMonto4() { return monto4; }
    public Instant getCreatedAt() { return createdAt; }

    public void setSorteo(Sorteo sorteo) { this.sorteo = sorteo; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
    public void setNumero1(Short n) { this.numero1 = n; }
    public void setNumero2(Short n) { this.numero2 = n; }
    public void setNumero3(Short n) { this.numero3 = n; }
    public void setNumero4(Short n) { this.numero4 = n; }
    public void setNumero5(Short n) { this.numero5 = n; }
    public void setNumero6(Short n) { this.numero6 = n; }
    public void setGanadores6(Integer g) { this.ganadores6 = g; }
    public void setMonto6(BigDecimal m) { this.monto6 = m; }
    public void setGanadores5(Integer g) { this.ganadores5 = g; }
    public void setMonto5(BigDecimal m) { this.monto5 = m; }
    public void setGanadores4(Integer g) { this.ganadores4 = g; }
    public void setMonto4(BigDecimal m) { this.monto4 = m; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resultado other)) return false;
        return sorteo != null && modalidad != null
            && Objects.equals(sorteo.getId(), other.sorteo.getId())
            && modalidad == other.modalidad;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            sorteo != null ? sorteo.getId() : null,
            modalidad
        );
    }
}
