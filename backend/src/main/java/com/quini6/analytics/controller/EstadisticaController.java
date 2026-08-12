package com.quini6.analytics.controller;

import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.dto.FrecuenciaDTO;
import com.quini6.analytics.service.EstadisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = "Estadísticas", description = "Análisis estadístico descriptivo de datos históricos")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping("/frecuencia")
    @Operation(
        summary = "Frecuencia de números",
        description = "Calcula la frecuencia absoluta y relativa de cada número (0-45) en un período dado"
    )
    public ResponseEntity<FrecuenciaDTO> frecuencia(
            @RequestParam Modalidad modalidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(estadisticaService.calcularFrecuencia(modalidad, desde, hasta));
    }

    @GetMapping("/chi-cuadrado")
    @Operation(
        summary = "Test chi-cuadrado de uniformidad",
        description = "Evalúa si la distribución de números es consistente con una distribución uniforme"
    )
    public ResponseEntity<EstadisticaService.ChiCuadradoResult> chiCuadrado(
            @RequestParam Modalidad modalidad) {
        return ResponseEntity.ok(estadisticaService.calcularChiCuadrado(modalidad));
    }
}
