package com.quini6.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quini6.analytics.client.Quini6ApiClient;
import com.quini6.analytics.domain.entity.Resultado;
import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.domain.repository.SorteoRepository;
import com.quini6.analytics.messaging.SorteoEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IngestaService {

    private static final Logger log = LoggerFactory.getLogger(IngestaService.class);
    private static final Pattern NUMEROS_PATTERN = Pattern.compile("\\d{1,2}");

    private final Quini6ApiClient apiClient;
    private final SorteoRepository sorteoRepository;
    private final ResultadoRepository resultadoRepository;
    private final SorteoEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Counter sorteosIngeridosCounter;

    public IngestaService(
            Quini6ApiClient apiClient,
            SorteoRepository sorteoRepository,
            ResultadoRepository resultadoRepository,
            SorteoEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.apiClient = apiClient;
        this.sorteoRepository = sorteoRepository;
        this.resultadoRepository = resultadoRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.sorteosIngeridosCounter = Counter.builder("sorteos.ingestados")
            .description("Total de sorteos ingested successfully")
            .register(meterRegistry);
    }

    @Transactional
    public IngestaResult ingerirSorteo(Integer numeroSorteo) {
        log.info("Ingestando sorteo {}...", numeroSorteo);

        if (sorteoRepository.existsByNumeroSorteo(numeroSorteo)) {
            log.info("Sorteo {} ya existe, ignorando", numeroSorteo);
            return IngestaResult.yaExistente(numeroSorteo);
        }

        Quini6ApiClient.ResultadoSorteoResponse response =
            apiClient.getSorteoByNumero(numeroSorteo);

        if (response == null || response.infoSorteo() == null || response.infoSorteo().isEmpty()) {
            log.error("No se pudo obtener datos del sorteo {}", numeroSorteo);
            return IngestaResult.error(numeroSorteo, "API returned no data");
        }

        Quini6ApiClient.InfoSorteo info = response.infoSorteo().getFirst();
        LocalDate fecha = parseFecha(info.fecha());

        Sorteo sorteo = new Sorteo(numeroSorteo, fecha);
        sorteo.setFuenteApi("Q6R");
        try {
            sorteo.setRawJson(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.warn("Could not serialize raw JSON for sorteo {}", numeroSorteo);
        }
        sorteo = sorteoRepository.save(sorteo);

        int modalidadesIngeridas = 0;
        if (response.resultados() != null) {
            for (Quini6ApiClient.ResultadoItem resultadoItem : response.resultados()) {
                Modalidad modalidad = mapModalidad(resultadoItem.titulo());
                if (modalidad == null) {
                    log.warn("Modalidad desconocida: {}", resultadoItem.titulo());
                    continue;
                }

                List<Short> numeros = parseNumeros(resultadoItem.numeros());
                if (numeros.size() != 6) {
                    log.warn("Sorteo {} modalidad {} no tiene 6 números válidos",
                        numeroSorteo, modalidad);
                    continue;
                }

                Resultado resultado = new Resultado(
                    sorteo, modalidad,
                    numeros.get(0), numeros.get(1), numeros.get(2),
                    numeros.get(3), numeros.get(4), numeros.get(5)
                );

                // Parse premios
                if (resultadoItem.premios() != null) {
                    parsePremios(resultado, resultadoItem.premios());
                }

                resultadoRepository.save(resultado);
                modalidadesIngeridas++;
            }
        }

        // Publish event for ES indexing and cache update
        eventPublisher.publicarSorteoIngerido(sorteo);

        log.info("Sorteo {} ingestado: {} modalidades", numeroSorteo, modalidadesIngeridas);
        sorteosIngeridosCounter.increment();
        return IngestaResult.exitoso(numeroSorteo, modalidadesIngeridas);
    }

    @Transactional
    public IngestaResult ingerirRango(LocalDate desde, LocalDate hasta) {
        log.info("Ingestando rango {} a {}...", desde, hasta);
        List<Quini6ApiClient.SorteoResponse> sorteos = apiClient.getSorteos();

        int ingested = 0;
        int skipped = 0;
        int errors = 0;

        for (Quini6ApiClient.SorteoResponse s : sorteos) {
            try {
                Integer num = Integer.parseInt(s.numero());
                LocalDate fechaSorteo = parseFecha(s.fecha());
                if (fechaSorteo.isBefore(desde) || fechaSorteo.isAfter(hasta)) {
                    continue;
                }
                IngestaResult result = ingerirSorteo(num);
                if (result.exitoso()) ingested++;
                else if (result.yaExistente()) skipped++;
                else errors++;
            } catch (Exception e) {
                log.error("Error procesando sorteo {}: {}", s.numero(), e.getMessage());
                errors++;
            }
        }

        log.info("Rango completado: {} ingeridos, {} existentes, {} errores", ingested, skipped, errors);
        return new IngestaResult(null, true, false, ingested + " sorteos ingeridos", null);
    }

    private LocalDate parseFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return LocalDate.now();
        // Format from API: "05/08/2026" or "05-08-2026"
        String normalized = fechaStr.replace("-", "/");
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            log.warn("No se pudo parsear fecha: {}", fechaStr);
            return LocalDate.now();
        }
    }

    private Modalidad mapModalidad(String titulo) {
        if (titulo == null) return null;
        String lower = titulo.toLowerCase().trim();
        if (lower.contains("tradicional") || lower.contains("primer sorteo")) {
            return Modalidad.TRADICIONAL;
        } else if (lower.contains("segunda")) {
            return Modalidad.SEGUNDA;
        } else if (lower.contains("revancha")) {
            return Modalidad.REVANCHA;
        } else if (lower.contains("siempre sale")) {
            return Modalidad.SIEMPRE_SALE;
        } else if (lower.contains("pozo extra")) {
            return Modalidad.POZO_EXTRA;
        }
        return null;
    }

    private List<Short> parseNumeros(String numerosStr) {
        if (numerosStr == null) return List.of();
        Matcher matcher = NUMEROS_PATTERN.matcher(numerosStr);
        List<Short> numeros = new ArrayList<>();
        while (matcher.find()) {
            numeros.add(Short.parseShort(matcher.group()));
        }
        return numeros;
    }

    private void parsePremios(Resultado resultado, List<Quini6ApiClient.PremioItem> premios) {
        for (Quini6ApiClient.PremioItem premio : premios) {
            if (premio.aciertos() == null) continue;
            String aciertos = premio.aciertos().trim();
            Integer ganadores = null;
            BigDecimal monto = null;

            try {
                if (premio.ganadores() != null && !premio.ganadores().equalsIgnoreCase("vacante")) {
                    ganadores = Integer.parseInt(premio.ganadores().replace(".", "").replace(",", ""));
                }
            } catch (NumberFormatException ignored) {}

            try {
                if (premio.premio() != null) {
                    String cleaned = premio.premio()
                        .replace("$", "")
                        .replace(".", "")
                        .replace(",", ".")
                        .trim();
                    monto = new BigDecimal(cleaned);
                }
            } catch (NumberFormatException ignored) {}

            switch (aciertos) {
                case "6" -> {
                    resultado.setGanadores6(ganadores);
                    resultado.setMonto6(monto);
                }
                case "5" -> {
                    resultado.setGanadores5(ganadores);
                    resultado.setMonto5(monto);
                }
                case "4" -> {
                    resultado.setGanadores4(ganadores);
                    resultado.setMonto4(monto);
                }
            }
        }
    }

    public record IngestaResult(
        Integer numeroSorteo,
        boolean exitoso,
        boolean yaExistente,
        String mensaje,
        Integer modalidadesIngeridas
    ) {
        static IngestaResult exitoso(Integer num, int modalidades) {
            return new IngestaResult(num, true, false, "Ingestado correctamente", modalidades);
        }
        static IngestaResult yaExistente(Integer num) {
            return new IngestaResult(num, false, true, "Ya existía en la base", null);
        }
        static IngestaResult error(Integer num, String msg) {
            return new IngestaResult(num, false, false, msg, null);
        }
    }
}
