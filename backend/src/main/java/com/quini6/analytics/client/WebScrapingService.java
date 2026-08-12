package com.quini6.analytics.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Web scraping service that replicates the logic from kabeza/node_quini6.
 * Scrapes quini-6-resultados.com.ar to obtain lottery draw results.
 *
 * DISCLAIMER: Los sorteos del Quini 6 son eventos independientes (i.i.d.).
 * Este servicio obtiene datos históricos para análisis descriptivo.
 * Ningún resultado pasado predice resultados futuros.
 */
@Service
public class WebScrapingService {

    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);

    private static final String BASE_URL = "https://www.quini-6-resultados.com.ar";
    private static final String SORTEOS_URL = BASE_URL + "/quini6/sorteos-anteriores.aspx";

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    /**
     * Obtiene la lista de todos los sorteos disponibles.
     */
    public List<ScrapedSorteo> obtenerListaSorteos() {
        try {
            log.info("Scraping lista de sorteos desde {}", SORTEOS_URL);
            Document doc = Jsoup.connect(SORTEOS_URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

            List<ScrapedSorteo> sorteos = new ArrayList<>();
            Elements links = doc.select("div.col-md-3 p a");

            for (Element el : links) {
                String text = el.text();
                String[] parts = text.split("del ");
                if (parts.length < 2) continue;

                String numero = parts[0].replace("Sorteo ", "").trim();
                String fecha = parts[1].replace("-", "/").trim();
                String link = el.attr("abs:href");

                sorteos.add(new ScrapedSorteo(numero, text.split("del ")[0].trim(), fecha, link));
            }

            sorteos.sort(Comparator.comparingInt(s -> -Integer.parseInt(s.numero())));
            log.info("Encontrados {} sorteos", sorteos.size());
            return sorteos;
        } catch (IOException e) {
            log.error("Error scraping lista de sorteos: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene los resultados detallados de un sorteo específico.
     */
    public ScrapedResultadoSorteo obtenerResultadoSorteo(int numeroSorteo) {
        List<ScrapedSorteo> lista = obtenerListaSorteos();
        ScrapedSorteo sorteoInfo = lista.stream()
            .filter(s -> s.numero().equals(String.valueOf(numeroSorteo)))
            .findFirst()
            .orElse(null);

        if (sorteoInfo == null) {
            log.warn("Sorteo {} no encontrado en la lista", numeroSorteo);
            return new ScrapedResultadoSorteo(List.of(), List.of());
        }

        try {
            log.info("Scraping resultado del sorteo {} desde {}", numeroSorteo, sorteoInfo.link());
            Document doc = Jsoup.connect(sorteoInfo.link())
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

            List<ScrapedResultado> resultados = new ArrayList<>();

            // 1. SORTEO TRADICIONAL
            resultados.add(scrapeModalidad(doc,
                "SORTEO TRADICIONAL",
                "SORTEO TRADICIONAL",
                "SORTEO TRADICIONAL"));

            // 2. LA SEGUNDA DEL QUINI
            resultados.add(scrapeModalidad(doc,
                "LA SEGUNDA DEL QUINI",
                "LA SEGUNDA DEL QUINI 6",
                "LA SEGUNDA DEL QUINI"));

            // 3. SORTEO REVANCHA
            resultados.add(scrapeModalidad(doc,
                "SORTEO REVANCHA",
                "LA SEGUNDA DEL QUINI 6 REVANCHA",
                "SORTEO REVANCHA"));

            // 4. SIEMPRE SALE
            resultados.add(scrapeModalidad(doc,
                "QUE SIEMPRE SALE",
                "EL QUINI QUE SIEMPRE SALE",
                "SIEMPRE SALE"));

            // 5. POZO EXTRA (solo premios)
            ScrapedResultado pozoExtra = scrapePozoExtra(doc);
            resultados.add(pozoExtra);

            return new ScrapedResultadoSorteo(
                List.of(new ScrapedInfoSorteo(
                    String.valueOf(numeroSorteo),
                    sorteoInfo.titulo(),
                    sorteoInfo.fecha(),
                    sorteoInfo.link()
                )),
                resultados
            );
        } catch (IOException e) {
            log.error("Error scraping sorteo {}: {}", numeroSorteo, e.getMessage());
            return new ScrapedResultadoSorteo(List.of(), List.of());
        }
    }

    private ScrapedResultado scrapeModalidad(
            Document doc,
            String h3Contains,
            String premioContains,
            String titulo) {

        String numeros = "";

        // Extract numbers: find h3 containing text, then get next sibling's text
        Elements h3s = doc.select("h3");
        for (Element h3 : h3s) {
            if (h3.text().contains(h3Contains)) {
                Element next = h3.nextElementSibling();
                if (next != null) {
                    numeros = next.text().trim()
                        .replace("-", ",")
                        .replace("\\s+", "");
                }
                break;
            }
        }

        // Extract prizes
        List<ScrapedPremio> premios = new ArrayList<>();
        Elements rows = doc.select("tr.verde");
        for (Element row : rows) {
            if (row.text().contains(premioContains)) {
                Element current = row.nextElementSibling();
                while (current != null && !current.hasClass("verde")) {
                    if (current.tagName().equals("tr")) {
                        Elements tds = current.select("td");
                        if (tds.size() >= 3) {
                            premios.add(new ScrapedPremio(
                                tds.get(0).text().trim(),
                                tds.get(1).text().trim(),
                                tds.get(2).text().trim()
                            ));
                        }
                    }
                    current = current.nextElementSibling();
                }
                break;
            }
        }

        return new ScrapedResultado(titulo, numeros, premios);
    }

    private ScrapedResultado scrapePozoExtra(Document doc) {
        List<ScrapedPremio> premios = new ArrayList<>();
        Elements rows = doc.select("tr.verde");
        for (Element row : rows) {
            if (row.text().contains("QUINI 6 POZO EXTRA")) {
                Element current = row.nextElementSibling();
                while (current != null && !current.hasClass("verde")) {
                    if (current.tagName().equals("tr")) {
                        Elements tds = current.select("td");
                        if (tds.size() >= 3) {
                            premios.add(new ScrapedPremio(
                                tds.get(0).text().trim(),
                                tds.get(1).text().trim(),
                                tds.get(2).text().trim()
                            ));
                        }
                    }
                    current = current.nextElementSibling();
                }
                break;
            }
        }

        return new ScrapedResultado(
            "POZO EXTRA",
            "Se reparte entre los que tengan seis aciertos contando los tres primeros sorteos.",
            premios
        );
    }

    // --- Records ---

    public record ScrapedSorteo(
        String numero,
        String titulo,
        String fecha,
        String link
    ) {}

    public record ScrapedResultadoSorteo(
        List<ScrapedInfoSorteo> infoSorteo,
        List<ScrapedResultado> resultados
    ) {}

    public record ScrapedInfoSorteo(
        String numero,
        String titulo,
        String fecha,
        String link
    ) {}

    public record ScrapedResultado(
        String titulo,
        String numeros,
        List<ScrapedPremio> premios
    ) {}

    public record ScrapedPremio(
        String aciertos,
        String ganadores,
        String premio
    ) {}
}
