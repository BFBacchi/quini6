package com.quini6.analytics.controller;

import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.repository.SorteoRepository;
import com.quini6.analytics.dto.SorteoDTO;
import com.quini6.analytics.dto.ResultadoDTO;
import com.quini6.analytics.domain.entity.Resultado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sorteos")
@Tag(name = "Sorteos", description = "Consulta de sorteos históricos")
@Transactional(readOnly = true)
public class SorteoController {

    private final SorteoRepository sorteoRepository;

    public SorteoController(SorteoRepository sorteoRepository) {
        this.sorteoRepository = sorteoRepository;
    }

    @GetMapping
    @Operation(summary = "Listar sorteos", description = "Obtiene todos los sorteos ordenados por fecha descendente")
    public ResponseEntity<List<SorteoDTO>> listar() {
        List<SorteoDTO> sorteos = sorteoRepository.findAllByOrderByFechaDesc().stream()
            .map(this::toDTO)
            .toList();
        return ResponseEntity.ok(sorteos);
    }

    @GetMapping("/{numeroSorteo}")
    @Operation(summary = "Obtener sorteo por número")
    public ResponseEntity<SorteoDTO> porNumero(
            @PathVariable Integer numeroSorteo) {
        return sorteoRepository.findByNumeroSorteo(numeroSorteo)
            .map(s -> ResponseEntity.ok(toDTO(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rango")
    @Operation(summary = "Buscar sorteos por rango de fechas")
    public ResponseEntity<List<SorteoDTO>> porRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<SorteoDTO> sorteos = sorteoRepository.findByFechaBetween(desde, hasta).stream()
            .map(this::toDTO)
            .toList();
        return ResponseEntity.ok(sorteos);
    }

    private SorteoDTO toDTO(Sorteo sorteo) {
        List<ResultadoDTO> resultados = sorteo.getResultados().stream()
            .map(this::toResultadoDTO)
            .toList();
        return new SorteoDTO(
            sorteo.getId(),
            sorteo.getNumeroSorteo(),
            sorteo.getFecha(),
            sorteo.getPozoMonto(),
            resultados
        );
    }

    private ResultadoDTO toResultadoDTO(Resultado r) {
        return new ResultadoDTO(
            r.getModalidad(),
            Arrays.asList(r.getNumerosAsArray()),
            r.getGanadores6(), r.getMonto6(),
            r.getGanadores5(), r.getMonto5(),
            r.getGanadores4(), r.getMonto4()
        );
    }
}
