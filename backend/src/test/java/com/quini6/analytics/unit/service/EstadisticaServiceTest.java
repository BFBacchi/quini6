package com.quini6.analytics.unit.service;

import com.quini6.analytics.domain.entity.Resultado;
import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.dto.FrecuenciaDTO;
import com.quini6.analytics.service.EstadisticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticaServiceTest {

    @Mock
    private ResultadoRepository resultadoRepository;

    private EstadisticaService service;

    @BeforeEach
    void setUp() {
        service = new EstadisticaService(resultadoRepository);
    }

    @Test
    void calcularFrecuencia_sinDatos_retornaVacio() {
        when(resultadoRepository.findHistorialByModalidad(Modalidad.TRADICIONAL))
            .thenReturn(List.of());

        FrecuenciaDTO result = service.calcularFrecuencia(Modalidad.TRADICIONAL, null, null);

        assertThat(result.totalSorteos()).isZero();
        assertThat(result.frecuencias()).isEmpty();
    }

    @Test
    void calcularFrecuencia_conDatos_retornaFrecuenciasCorrectas() {
        Sorteo sorteo1 = crearSorteo(1, LocalDate.of(2026, 1, 5));
        Sorteo sorteo2 = crearSorteo(2, LocalDate.of(2026, 1, 8));

        // Sorteo 1: números 0,1,2,3,4,5
        Resultado r1 = crearResultado(sorteo1, Modalidad.TRADICIONAL,
            (short)0, (short)1, (short)2, (short)3, (short)4, (short)5);
        // Sorteo 2: números 0,1,2,3,4,6
        Resultado r2 = crearResultado(sorteo2, Modalidad.TRADICIONAL,
            (short)0, (short)1, (short)2, (short)3, (short)4, (short)6);

        when(resultadoRepository.findHistorialByModalidad(Modalidad.TRADICIONAL))
            .thenReturn(List.of(r1, r2));

        FrecuenciaDTO result = service.calcularFrecuencia(Modalidad.TRADICIONAL, null, null);

        assertThat(result.totalSorteos()).isEqualTo(2);

        // Número 0 apareció 2 veces en 12 extracciones → relativa = 2/12
        FrecuenciaDTO.FrecuenciaNumero f0 = result.frecuencias().get(0);
        assertThat(f0.absoluta()).isEqualTo(2);
        assertThat(f0.relativa()).isCloseTo(2.0 / 12.0, org.assertj.core.data.Offset.offset(0.001));

        // Número 5 apareció 1 vez
        FrecuenciaDTO.FrecuenciaNumero f5 = result.frecuencias().get(5);
        assertThat(f5.absoluta()).isEqualTo(1);

        // Número 6 apareció 1 vez
        FrecuenciaDTO.FrecuenciaNumero f6 = result.frecuencias().get(6);
        assertThat(f6.absoluta()).isEqualTo(1);

        // Número 10 no apareció → absoluta = 0
        FrecuenciaDTO.FrecuenciaNumero f10 = result.frecuencias().get(10);
        assertThat(f10.absoluta()).isZero();
    }

    @Test
    void calcularFrecuencia_conRangoUsaQueryEspecifica() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        when(resultadoRepository.findByRangoFechasYModalidad(desde, hasta, Modalidad.TRADICIONAL))
            .thenReturn(List.of());

        FrecuenciaDTO result = service.calcularFrecuencia(Modalidad.TRADICIONAL, desde, hasta);

        assertThat(result.totalSorteos()).isZero();
    }

    @Test
    void calcularFrecuencia_atrasoCalculadoCorrectamente() {
        Sorteo sorteo1 = crearSorteo(1, LocalDate.of(2026, 1, 1));
        Sorteo sorteo2 = crearSorteo(2, LocalDate.of(2026, 1, 15));

        // Sorteo 1: número 0 sale
        Resultado r1 = crearResultado(sorteo1, Modalidad.TRADICIONAL,
            (short)0, (short)10, (short)20, (short)30, (short)40, (short)45);
        // Sorteo 2: número 0 NO sale
        Resultado r2 = crearResultado(sorteo2, Modalidad.TRADICIONAL,
            (short)1, (short)11, (short)21, (short)31, (short)41, (short)42);

        when(resultadoRepository.findHistorialByModalidad(Modalidad.TRADICIONAL))
            .thenReturn(List.of(r1, r2));

        FrecuenciaDTO result = service.calcularFrecuencia(Modalidad.TRADICIONAL, null, null);

        // Número 0: última vez 1/1, fecha más reciente 1/15 → atraso = 14 días
        assertThat(result.frecuencias().get(0).atraso()).isEqualTo(14);

        // Número 1: última vez 1/15 → atraso = 0
        assertThat(result.frecuencias().get(1).atraso()).isZero();
    }

    @Test
    void calcularChiCuadrado_distribucionUniforme_noRechaza() {
        // Crear sorteo perfectamente distribuido
        Sorteo sorteo = crearSorteo(1, LocalDate.of(2026, 1, 1));
        // Todos los números 0-5 aparecen exactamente 1 vez en 6 extracciones
        Resultado r = crearResultado(sorteo, Modalidad.TRADICIONAL,
            (short)0, (short)1, (short)2, (short)3, (short)4, (short)5);

        when(resultadoRepository.findHistorialByModalidad(Modalidad.TRADICIONAL))
            .thenReturn(List.of(r));

        var result = service.calcularChiCuadrado(Modalidad.TRADICIONAL);

        assertThat(result.totalSorteos()).isEqualTo(1);
        assertThat(result.gradosLibertad()).isEqualTo(45);
        assertThat(result.chiCuadrado()).isGreaterThanOrEqualTo(0);
        assertThat(result.interpretacion()).isNotEmpty();
    }

    @Test
    void calcularChiCuadrado_sinDatos_retornaCero() {
        when(resultadoRepository.findHistorialByModalidad(Modalidad.TRADICIONAL))
            .thenReturn(List.of());

        var result = service.calcularChiCuadrado(Modalidad.TRADICIONAL);

        assertThat(result.totalSorteos()).isZero();
        assertThat(result.interpretacion()).contains("Sin datos");
    }

    // ─── Helpers ─────────────────────────────────────────

    private Sorteo crearSorteo(int numero, LocalDate fecha) {
        Sorteo s = new Sorteo(numero, fecha);
        s.setFuenteApi("TEST");
        return s;
    }

    private Resultado crearResultado(Sorteo sorteo, Modalidad modalidad,
                                      Short n1, Short n2, Short n3,
                                      Short n4, Short n5, Short n6) {
        return new Resultado(sorteo, modalidad, n1, n2, n3, n4, n5, n6);
    }
}
