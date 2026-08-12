package com.quini6.analytics.dto;

import java.util.List;

public record CombinacionGenerada(
    List<Short> numeros,
    String metodo,
    String disclaimer
) {
    public CombinacionGenerada {
        disclaimer = "Probabilidad de esta combinación: 1/9,366,819, igual a cualquier otra. "
            + "Esta combinación es generada como ENTRETENIMIENTO y no tiene valor predictivo.";
    }
}
