package com.quini6.analytics.controller;

import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.dto.CombinacionGenerada;
import com.quini6.analytics.dto.FrecuenciaDTO;
import com.quini6.analytics.service.EstadisticaService;
import com.quini6.analytics.service.GeneradorCombinacionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/generador")
@Tag(
    name = "Generador de Combinaciones",
    description = "Herramienta de ENTRETENIMIENTO. NO predice resultados. " +
                  "Cada combinación tiene exactamente la misma probabilidad: 1/9,366,819."
)
public class GeneradorController {

    private final GeneradorCombinacionesService generadorService;
    private final EstadisticaService estadisticaService;

    public GeneradorController(
            GeneradorCombinacionesService generadorService,
            EstadisticaService estadisticaService) {
        this.generadorService = generadorService;
        this.estadisticaService = estadisticaService;
    }

    @GetMapping("/aleatorio")
    @Operation(
        summary = "Generar combinación aleatoria",
        description = "Genera 6 números del 0 al 45 al azar. Cada combinación tiene probabilidad 1/9,366,819."
    )
    public ResponseEntity<CombinacionGenerada> aleatorio() {
        return ResponseEntity.ok(generadorService.generarAleatorio());
    }

    @GetMapping("/ponderado")
    @Operation(
        summary = "Generar combinación ponderada por frecuencia",
        description = """
            Genera 6 números ponderados por frecuencia histórica.
            **IMPORTANTE:** Este método NO es "mejor" que el aleatorio puro.
            Los sorteos son eventos independientes (i.i.d.).
            La frecuencia pasada no influye en resultados futuros.
            """)
    public ResponseEntity<CombinacionGenerada> ponderado(
            @RequestParam Modalidad modalidad) {
        FrecuenciaDTO frecuencia = estadisticaService.calcularFrecuencia(modalidad, null, null);
        Map<Integer, Long> freqMap = frecuencia.frecuencias().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (long) e.getValue().absoluta()
            ));
        return ResponseEntity.ok(generadorService.generarPonderado(freqMap));
    }
}
