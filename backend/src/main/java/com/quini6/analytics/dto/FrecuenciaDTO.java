package com.quini6.analytics.dto;

import java.util.List;
import java.util.Map;

public record FrecuenciaDTO(
    int totalSorteos,
    Map<Integer, FrecuenciaNumero> frecuencias,
    String disclaimer
) {
    public record FrecuenciaNumero(
        int numero,
        long absoluta,
        double relativa,
        long atraso
    ) {}
}
