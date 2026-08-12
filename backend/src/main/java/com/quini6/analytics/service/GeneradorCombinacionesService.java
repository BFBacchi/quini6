package com.quini6.analytics.service;

import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.dto.CombinacionGenerada;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GeneradorCombinacionesService {

    private static final int TOTAL_NUMEROS = 46; // 0 a 45
    private static final int NUMEROS_POR_SORTEO = 6;

    /**
     * Genera una combinación aleatoria pura.
     * Cada combinación tiene exactamente la misma probabilidad: 1/9,366,819.
     */
    public CombinacionGenerada generarAleatorio() {
        List<Short> numeros = IntStream.rangeClosed(0, 45)
            .boxed()
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    Collections.shuffle(list);
                    return list;
                }
            ))
            .subList(0, NUMEROS_POR_SORTEO)
            .stream()
            .sorted()
            .map(Integer::shortValue)
            .collect(Collectors.toList());

        return new CombinacionGenerada(numeros, "Aleatorio puro",
            "Probabilidad de esta combinación: 1/9,366,819, igual a cualquier otra. " +
            "Generada al azar sin ningún criterio predictivo.");
    }

    /**
     * Genera una combinación ponderada por frecuencia histórica.
     * INCLUYE DISCLAIMER: esto NO es un método "mejor", solo una alternativa
     * de entretenimiento para comparar con el aleatorio puro.
     */
    public CombinacionGenerada generarPonderado(Map<Integer, Long> frecuencias) {
        if (frecuencias == null || frecuencias.isEmpty()) {
            return generarAleatorio();
        }

        long totalApariciones = frecuencias.values().stream().mapToLong(Long::longValue).sum();
        if (totalApariciones == 0) {
            return generarAleatorio();
        }

        // Weighted random selection without replacement
        Map<Integer, Double> pesos = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : frecuencias.entrySet()) {
            pesos.put(entry.getKey(), (double) entry.getValue() / totalApariciones);
        }

        List<Short> seleccionados = new ArrayList<>();
        Set<Integer> usados = new HashSet<>();
        Random random = new Random();

        while (seleccionados.size() < NUMEROS_POR_SORTEO) {
            double totalPeso = 0;
            for (Map.Entry<Integer, Double> e : pesos.entrySet()) {
                if (!usados.contains(e.getKey())) {
                    totalPeso += e.getValue();
                }
            }

            double r = random.nextDouble() * totalPeso;
            double acumulado = 0;
            for (Map.Entry<Integer, Double> e : pesos.entrySet()) {
                if (!usados.contains(e.getKey())) {
                    acumulado += e.getValue();
                    if (acumulado >= r) {
                        seleccionados.add(e.getKey().shortValue());
                        usados.add(e.getKey());
                        break;
                    }
                }
            }
        }

        Collections.sort(seleccionados);

        return new CombinacionGenerada(seleccionados, "Ponderado por frecuencia historica",
            "Probabilidad de esta combinacion: 1/9,366,819, igual a cualquier otra. " +
            "El metodo ponderado NO produce combinaciones mejores que el aleatorio puro. " +
            "Los sorteos son eventos independientes (i.i.d.).");
    }
}
