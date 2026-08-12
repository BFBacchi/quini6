package com.quini6.analytics.dto;

import com.quini6.analytics.domain.enums.Modalidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SorteoDTO(
    UUID id,
    Integer numeroSorteo,
    LocalDate fecha,
    BigDecimal pozoMonto,
    List<ResultadoDTO> resultados
) {}
