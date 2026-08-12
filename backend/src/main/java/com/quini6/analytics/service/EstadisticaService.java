package com.quini6.analytics.service;

import com.quini6.analytics.domain.entity.Resultado;
import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.dto.FrecuenciaDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

@Service
public class EstadisticaService {

    private static final int TOTAL_NUMEROS = 46; // 0 a 45
    private static final double PROBABILIDAD_ESPERADA = 1.0 / TOTAL_NUMEROS;

    private final ResultadoRepository resultadoRepository;

    public EstadisticaService(ResultadoRepository resultadoRepository) {
        this.resultadoRepository = resultadoRepository;
    }

    public FrecuenciaDTO calcularFrecuencia(Modalidad modalidad, LocalDate desde, LocalDate hasta) {
        List<Resultado> resultados;
        if (desde != null && hasta != null) {
            resultados = resultadoRepository.findByRangoFechasYModalidad(desde, hasta, modalidad);
        } else {
            resultados = resultadoRepository.findHistorialByModalidad(modalidad);
        }

        int totalSorteos = resultados.size();
        if (totalSorteos == 0) {
            return new FrecuenciaDTO(0, Map.of(),
                "No hay datos disponibles para esta consulta.");
        }

        long[] conteo = new long[TOTAL_NUMEROS];
        LocalDate[] ultimaVez = new LocalDate[TOTAL_NUMEROS];
        LocalDate fechaMasReciente = LocalDate.MIN;

        for (Resultado r : resultados) {
            Short[] numeros = r.getNumerosAsArray();
            LocalDate fechaSorteo = r.getSorteo().getFecha();
            if (fechaSorteo.isAfter(fechaMasReciente)) {
                fechaMasReciente = fechaSorteo;
            }
            for (Short num : numeros) {
                int idx = num.intValue();
                conteo[idx]++;
                if (ultimaVez[idx] == null || fechaSorteo.isAfter(ultimaVez[idx])) {
                    ultimaVez[idx] = fechaSorteo;
                }
            }
        }

        Map<Integer, FrecuenciaDTO.FrecuenciaNumero> frecuencias = new LinkedHashMap<>();
        for (int i = 0; i < TOTAL_NUMEROS; i++) {
            double relativa = (double) conteo[i] / (totalSorteos * 6);
            long atraso = ultimaVez[i] != null
                ? java.time.temporal.ChronoUnit.DAYS.between(ultimaVez[i], fechaMasReciente)
                : totalSorteos;
            frecuencias.put(i, new FrecuenciaDTO.FrecuenciaNumero(
                i, conteo[i], relativa, atraso
            ));
        }

        return new FrecuenciaDTO(totalSorteos, frecuencias,
            "Análisis descriptivo de frecuencias. Los sorteos son eventos independientes.");
    }

    public ChiCuadradoResult calcularChiCuadrado(Modalidad modalidad) {
        List<Resultado> resultados = resultadoRepository.findHistorialByModalidad(modalidad);
        int n = resultados.size();

        if (n == 0) {
            return new ChiCuadradoResult(0, 0, 0, 0, "Sin datos para analizar");
        }

        long[] observado = new long[TOTAL_NUMEROS];
        for (Resultado r : resultados) {
            for (Short num : r.getNumerosAsArray()) {
                observado[num.intValue()]++;
            }
        }

        long totalObservaciones = n * 6L;
        double esperado = (double) totalObservaciones / TOTAL_NUMEROS;

        double chiCuadrado = 0;
        for (int i = 0; i < TOTAL_NUMEROS; i++) {
            double diff = observado[i] - esperado;
            chiCuadrado += (diff * diff) / esperado;
        }

        int gradosLibertad = TOTAL_NUMEROS - 1;
        // p-value approximation for chi-squared distribution
        double pValue = approximatePValue(chiCuadrado, gradosLibertad);

        String interpretacion;
        if (pValue > 0.05) {
            interpretacion = String.format(
                "No se rechaza la hipótesis de uniformidad (p=%.4f > 0.05). " +
                "La distribución de números es consistente con lo esperado en un juego aleatorio justo.",
                pValue);
        } else {
            interpretacion = String.format(
                "Se detecta una desviación de la uniformidad (p=%.4f ≤ 0.05). " +
                "Sin embargo, esto NO predice resultados futuros. Los sorteos son eventos independientes.",
                pValue);
        }

        return new ChiCuadradoResult(chiCuadrado, gradosLibertad, pValue, n, interpretacion);
    }

    private double approximatePValue(double chi2, int df) {
        // Rough approximation using incomplete gamma function
        // For production, use Apache Commons Math or similar
        double x = chi2 / 2.0;
        double a = df / 2.0;
        // Simple approximation: for large df, chi2 ~ normal
        double z = Math.pow(chi2 / df, 1.0 / 3.0) - (1.0 - 2.0 / (9.0 * df));
        z = z / Math.sqrt(2.0 / (9.0 * df));
        return 1.0 - normalCDF(z);
    }

    private double normalCDF(double x) {
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(x));
        double d = 0.3989422804014327;
        double p = d * Math.exp(-x * x / 2.0)
            * (t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.8212560 + t * 1.3302744)))));
        return x > 0 ? 1.0 - p : p;
    }

    public record ChiCuadradoResult(
        double chiCuadrado,
        int gradosLibertad,
        double pValue,
        int totalSorteos,
        String interpretacion
    ) {}
}
