package com.quini6.analytics.dto;

import com.quini6.analytics.domain.enums.Modalidad;
import java.math.BigDecimal;
import java.util.List;

public record ResultadoDTO(
    Modalidad modalidad,
    List<Short> numeros,
    Integer ganadores6,
    BigDecimal monto6,
    Integer ganadores5,
    BigDecimal monto5,
    Integer ganadores4,
    BigDecimal monto4
) {}
