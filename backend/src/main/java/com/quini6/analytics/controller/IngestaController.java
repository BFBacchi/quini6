package com.quini6.analytics.controller;

import com.quini6.analytics.elasticsearch.SorteoIndexService;
import com.quini6.analytics.service.IngestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.quini6.analytics.domain.repository.SorteoRepository;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ingesta")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Ingesta", description = "Endpoints para ingesta manual de datos (solo ADMIN)")
public class IngestaController {

    private final IngestaService ingestaService;
    private final SorteoRepository sorteoRepository;
    private final SorteoIndexService sorteoIndexService;

    public IngestaController(
            IngestaService ingestaService,
            SorteoRepository sorteoRepository,
            SorteoIndexService sorteoIndexService) {
        this.ingestaService = ingestaService;
        this.sorteoRepository = sorteoRepository;
        this.sorteoIndexService = sorteoIndexService;
    }

    @PostMapping("/sorteo/{numeroSorteo}")
    @Operation(summary = "Ingerir sorteo manual por número")
    public ResponseEntity<IngestaService.IngestaResult> ingerirSorteo(
            @PathVariable Integer numeroSorteo) {
        IngestaService.IngestaResult result = ingestaService.ingerirSorteo(numeroSorteo);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rango")
    @Operation(summary = "Ingerir rango de fechas")
    public ResponseEntity<IngestaService.IngestaResult> ingerirRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        IngestaService.IngestaResult result = ingestaService.ingerirRango(desde, hasta);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reindex")
    @Operation(summary = "Reindexar todos los sorteos en Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindex() {
        sorteoIndexService.crearIndiceSiNoExiste();
        int indexed = 0;
        for (var sorteo : sorteoRepository.findAll()) {
            sorteoIndexService.indexarSorteo(sorteo.getId().toString());
            indexed++;
        }
        return ResponseEntity.ok(Map.of("reindexados", indexed));
    }
}
