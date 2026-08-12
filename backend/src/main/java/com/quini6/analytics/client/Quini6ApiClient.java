package com.quini6.analytics.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Client that fetches Quini 6 data via direct web scraping (Jsoup).
 * Previously depended on external node_quini6 API; now self-contained.
 *
 * DISCLAIMER: Los sorteos del Quini 6 son eventos independientes (i.i.d.).
 * Este servicio obtiene datos históricos para análisis descriptivo.
 */
@Component
public class Quini6ApiClient {

    private static final Logger log = LoggerFactory.getLogger(Quini6ApiClient.class);

    private final WebScrapingService scrapingService;

    public Quini6ApiClient(WebScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    public List<SorteoResponse> getSorteos() {
        log.info("Fetching sorteos via web scraping...");
        List<WebScrapingService.ScrapedSorteo> sorteos = scrapingService.obtenerListaSorteos();
        return sorteos.stream()
            .map(s -> new SorteoResponse(s.numero(), s.titulo(), s.fecha(), s.link()))
            .collect(Collectors.toList());
    }

    public ResultadoSorteoResponse getSorteoByNumero(Integer numeroSorteo) {
        log.info("Fetching sorteo {} via web scraping...", numeroSorteo);
        WebScrapingService.ScrapedResultadoSorteo scraped =
            scrapingService.obtenerResultadoSorteo(numeroSorteo);

        List<InfoSorteo> info = scraped.infoSorteo().stream()
            .map(i -> new InfoSorteo(i.numero(), i.titulo(), i.fecha(), i.link()))
            .collect(Collectors.toList());

        List<ResultadoItem> resultados = scraped.resultados().stream()
            .map(r -> new ResultadoItem(
                r.titulo(),
                r.numeros(),
                r.premios().stream()
                    .map(p -> new PremioItem(p.aciertos(), p.ganadores(), p.premio()))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());

        return new ResultadoSorteoResponse(info, resultados);
    }

    public TodosLosNumerosResponse getTodosLosNumeros() {
        log.info("Fetching todoslosnumeros via web scraping...");
        List<SorteoResponse> sorteos = getSorteos();

        List<NumeroItem> tradicional = new java.util.ArrayList<>();
        List<NumeroItem> segunda = new java.util.ArrayList<>();
        List<NumeroItem> revancha = new java.util.ArrayList<>();
        List<NumeroItem> siempreSale = new java.util.ArrayList<>();

        for (SorteoResponse s : sorteos) {
            try {
                ResultadoSorteoResponse resultado = getSorteoByNumero(Integer.parseInt(s.numero()));
                if (resultado.resultados() == null) continue;

                for (ResultadoItem r : resultado.resultados()) {
                    NumeroItem item = new NumeroItem(s.numero(), s.fecha(), r.numeros());
                    String titulo = r.titulo().toLowerCase();
                    if (titulo.contains("tradicional")) tradicional.add(item);
                    else if (titulo.contains("segunda")) segunda.add(item);
                    else if (titulo.contains("revancha")) revancha.add(item);
                    else if (titulo.contains("siempre sale")) siempreSale.add(item);
                }
            } catch (Exception e) {
                log.warn("Error fetching sorteo {}: {}", s.numero(), e.getMessage());
            }
        }

        List<TipoSorteoItem> tipos = List.of(
            new TipoSorteoItem("SORTEO TRADICIONAL", tradicional),
            new TipoSorteoItem("LA SEGUNDA DEL QUINI", segunda),
            new TipoSorteoItem("SORTEO REVANCHA", revancha),
            new TipoSorteoItem("SIEMPRE SALE", siempreSale)
        );

        return new TodosLosNumerosResponse(tipos);
    }

    // --- Response records ---

    public record SorteoResponseList(
        @JsonProperty("data") List<SorteoResponse> sorteos
    ) {}

    public record SorteoResponse(
        String numero,
        String titulo,
        String fecha,
        String link
    ) {}

    public record ResultadoSorteoResponse(
        @JsonProperty("infoSorteo") List<InfoSorteo> infoSorteo,
        List<ResultadoItem> resultados
    ) {}

    public record InfoSorteo(
        String numero,
        String titulo,
        String fecha,
        String link
    ) {}

    public record ResultadoItem(
        String titulo,
        String numeros,
        List<PremioItem> premios
    ) {}

    public record PremioItem(
        String aciertos,
        String ganadores,
        String premio
    ) {}

    public record TodosLosNumerosResponse(
        @JsonProperty("tiposorteo") List<TipoSorteoItem> tiposorteo
    ) {}

    public record TipoSorteoItem(
        String titulo,
        List<NumeroItem> numeros
    ) {}

    public record NumeroItem(
        String numero,
        String fecha,
        String numeros
    ) {}
}
