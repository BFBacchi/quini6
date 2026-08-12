package com.quini6.analytics.unit.service;

import com.quini6.analytics.dto.CombinacionGenerada;
import com.quini6.analytics.service.GeneradorCombinacionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorCombinacionesServiceTest {

    private GeneradorCombinacionesService service;

    @BeforeEach
    void setUp() {
        service = new GeneradorCombinacionesService();
    }

    @Test
    void generarAleatorio_retorna6Numeros() {
        CombinacionGenerada result = service.generarAleatorio();

        assertThat(result.numeros()).hasSize(6);
    }

    @Test
    void generarAleatorio_numerosEnRango() {
        CombinacionGenerada result = service.generarAleatorio();

        for (Short num : result.numeros()) {
            assertThat(num).isBetween((short) 0, (short) 45);
        }
    }

    @Test
    void generarAleatorio_numerosOrdenados() {
        CombinacionGenerada result = service.generarAleatorio();

        for (int i = 1; i < result.numeros().size(); i++) {
            assertThat(result.numeros().get(i))
                .isGreaterThanOrEqualTo(result.numeros().get(i - 1));
        }
    }

    @Test
    void generarAleatorio_numerosUnicos() {
        CombinacionGenerada result = service.generarAleatorio();

        assertThat(result.numeros().stream().distinct().count()).isEqualTo(6);
    }

    @Test
    void generarAleatorio_incluyeDisclaimer() {
        CombinacionGenerada result = service.generarAleatorio();

        assertThat(result.disclaimer()).contains("1/9,366,819");
        assertThat(result.disclaimer()).contains("ENTRETENIMIENTO");
        assertThat(result.disclaimer()).contains("independientes");
    }

    @Test
    void generarAleatorio_metodoEsAleatorio() {
        CombinacionGenerada result = service.generarAleatorio();

        assertThat(result.metodo()).isEqualTo("Aleatorio puro");
    }

    @Test
    void generarPonderado_frecuenciasVacias_fallbackAAleatorio() {
        CombinacionGenerada result = service.generarPonderado(Map.of());

        assertThat(result.numeros()).hasSize(6);
        assertThat(result.metodo()).isEqualTo("Aleatorio puro");
    }

    @Test
    void generarPonderado_frecuenciasNulas_fallbackAAleatorio() {
        CombinacionGenerada result = service.generarPonderado(null);

        assertThat(result.numeros()).hasSize(6);
    }

    @Test
    void generarPonderado_conFrecuencias_retorna6Numeros() {
        Map<Integer, Long> freq = new HashMap<>();
        for (int i = 0; i <= 45; i++) {
            freq.put(i, (long) (i + 1));
        }

        CombinacionGenerada result = service.generarPonderado(freq);

        assertThat(result.numeros()).hasSize(6);
        assertThat(result.metodo()).contains("Ponderado");
    }

    @Test
    void generarPonderado_numerosEnRango() {
        Map<Integer, Long> freq = new HashMap<>();
        for (int i = 0; i <= 45; i++) {
            freq.put(i, 10L);
        }

        CombinacionGenerada result = service.generarPonderado(freq);

        for (Short num : result.numeros()) {
            assertThat(num).isBetween((short) 0, (short) 45);
        }
    }

    @Test
    void generarPonderado_numerosUnicos() {
        Map<Integer, Long> freq = new HashMap<>();
        for (int i = 0; i <= 45; i++) {
            freq.put(i, 1L);
        }

        CombinacionGenerada result = service.generarPonderado(freq);

        assertThat(result.numeros().stream().distinct().count()).isEqualTo(6);
    }

    @Test
    void generarPonderado_incluyeDisclaimer() {
        Map<Integer, Long> freq = new HashMap<>();
        for (int i = 0; i <= 45; i++) {
            freq.put(i, 1L);
        }

        CombinacionGenerada result = service.generarPonderado(freq);

        assertThat(result.disclaimer()).contains("1/9,366,819");
        assertThat(result.disclaimer()).contains("NO produce combinaciones");
        assertThat(result.disclaimer()).contains("independientes");
    }
}
