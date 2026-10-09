package com.quini6.analytics.elasticsearch;

import com.quini6.analytics.domain.entity.Resultado;
import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.domain.repository.SorteoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SorteoIndexService {

    private static final Logger log = LoggerFactory.getLogger(SorteoIndexService.class);
    public static final String INDEX_NAME = "quini6-sorteos";

    private final ElasticsearchOperations elasticsearchOperations;
    private final SorteoRepository sorteoRepository;
    private final ResultadoRepository resultadoRepository;

    public SorteoIndexService(
            ElasticsearchOperations elasticsearchOperations,
            SorteoRepository sorteoRepository,
            ResultadoRepository resultadoRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.sorteoRepository = sorteoRepository;
        this.resultadoRepository = resultadoRepository;
    }

    public void crearIndiceSiNoExiste() {
        IndexOperations ops = elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME));
        if (!ops.exists()) {
            Map<String, Object> mapping = Map.of(
                "properties", Map.of(
                    "sorteoId", Map.of("type", "keyword"),
                    "numeroSorteo", Map.of("type", "integer"),
                    "fecha", Map.of("type", "date"),
                    "modalidad", Map.of("type", "keyword"),
                    "numero", Map.of("type", "integer"),
                    "posicion", Map.of("type", "integer")
                )
            );
            ops.create(mapping);
            log.info("Índice {} creado en Elasticsearch", INDEX_NAME);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void crearIndiceAlArrancar() {
        try {
            crearIndiceSiNoExiste();
        } catch (Exception e) {
            log.warn("No se pudo crear índice {} al arrancar: {}", INDEX_NAME, e.getMessage());
        }
    }

    public void indexarSorteo(String sorteoId) {
        Optional<Sorteo> sorteoOpt = sorteoRepository.findById(UUID.fromString(sorteoId));
        if (sorteoOpt.isEmpty()) {
            log.warn("Sorteo {} no encontrado para indexar", sorteoId);
            return;
        }

        Sorteo sorteo = sorteoOpt.get();
        List<Resultado> resultados = resultadoRepository.findBySorteoId(sorteo.getId());

        List<IndexQuery> queries = new ArrayList<>();
        for (Resultado resultado : resultados) {
            Short[] numeros = resultado.getNumerosAsArray();
            for (int i = 0; i < numeros.length; i++) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("sorteoId", sorteo.getId().toString());
                doc.put("numeroSorteo", sorteo.getNumeroSorteo());
                doc.put("fecha", sorteo.getFecha().toString());
                doc.put("modalidad", resultado.getModalidad().name());
                doc.put("numero", numeros[i].intValue());
                doc.put("posicion", i + 1);

                IndexQuery query = new IndexQueryBuilder()
                    .withObject(doc)
                    .build();
                queries.add(query);
            }
        }

        if (!queries.isEmpty()) {
            elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of(INDEX_NAME));
            log.info("Indexados {} documentos para sorteo {}",
                queries.size(), sorteo.getNumeroSorteo());
        }
    }
}
