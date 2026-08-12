package com.quini6.analytics.controller;

import com.quini6.analytics.domain.repository.SorteoRepository;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.domain.repository.UsuarioRepository;
import com.quini6.analytics.domain.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/monitoring")
@Tag(name = "Monitoring", description = "Estado de servicios, bases de datos y tablas")
public class MonitoringController {

    private final SorteoRepository sorteoRepository;
    private final ResultadoRepository resultadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager em;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    public MonitoringController(SorteoRepository sorteoRepository,
                                 ResultadoRepository resultadoRepository,
                                 UsuarioRepository usuarioRepository,
                                 AuditLogRepository auditLogRepository) {
        this.sorteoRepository = sorteoRepository;
        this.resultadoRepository = resultadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ─── Stats ────────────────────────────────────────────

    @GetMapping("/database")
    @Operation(summary = "Estadísticas de PostgreSQL")
    public ResponseEntity<Map<String, Object>> databaseStats() {
        long sorteos = sorteoRepository.count();
        long resultados = resultadoRepository.count();
        long usuarios = usuarioRepository.count();
        long auditLogs = auditLogRepository.count();
        Integer maxSorteo = sorteoRepository.findMaxNumeroSorteo().orElse(0);

        List<Map<String, Object>> tableStats = List.of(
            tableInfo("sorteos", sorteos, "UUID PK, numero_sorteo INT UNIQUE, fecha DATE, pozo_monto NUMERIC, fuente_api VARCHAR, raw_json TEXT"),
            tableInfo("resultados", resultados, "UUID PK, sorteo_id FK→sorteos, modalidad VARCHAR, numero_1..6 SMALLINT, premio_*_*"),
            tableInfo("usuarios", usuarios, "UUID PK, username VARCHAR UNIQUE, password_hash VARCHAR, role VARCHAR, enabled BOOLEAN"),
            tableInfo("audit_log", auditLogs, "BIGSERIAL PK, entidad VARCHAR, accion VARCHAR, actor VARCHAR, timestamp TIMESTAMPTZ"),
            tableInfo("numeros_sorteados", countNumeros(), "BIGSERIAL PK, resultado_id FK, sorteo_id FK, modalidad VARCHAR, numero SMALLINT, posicion SMALLINT")
        );

        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "sorteos", sorteos,
            "resultados", resultados,
            "usuarios", usuarios,
            "auditLogs", auditLogs,
            "ultimoSorteo", maxSorteo,
            "tables", tableStats
        ));
    }

    private long countNumeros() {
        try {
            return (Long) em.createNativeQuery("SELECT COUNT(*) FROM numeros_sorteados").getSingleResult();
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> tableInfo(String name, long count, String schema) {
        return Map.of("name", name, "rows", count, "schema", schema);
    }

    // ─── Table data ───────────────────────────────────────

    @GetMapping("/database/{table}")
    @Operation(summary = "Datos completos de una tabla")
    public ResponseEntity<?> tableData(
            @PathVariable String table,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        return switch (table) {
            case "sorteos" -> ResponseEntity.ok(sorteosData(page, size));
            case "resultados" -> ResponseEntity.ok(resultadosData(page, size));
            case "usuarios" -> ResponseEntity.ok(usuariosData(page, size));
            case "audit_log" -> ResponseEntity.ok(auditLogData(page, size));
            case "numeros_sorteados" -> ResponseEntity.ok(numerosData(page, size));
            default -> ResponseEntity.badRequest().body(Map.of("error", "Tabla no válida: " + table));
        };
    }

    @GetMapping("/database/schema")
    @Operation(summary = "Schema completo de la BD (columnas, tipos, constraints)")
    public ResponseEntity<?> schemaInfo() {
        List<Object[]> columns = em.createNativeQuery("""
            SELECT table_name, column_name, data_type, is_nullable, column_default,
                   character_maximum_length, numeric_precision
            FROM information_schema.columns
            WHERE table_schema = 'public'
            ORDER BY table_name, ordinal_position
            """).getResultList();

        List<Object[]> constraints = em.createNativeQuery("""
            SELECT tc.table_name, tc.constraint_name, tc.constraint_type,
                   kcu.column_name, ccu.table_name AS foreign_table, ccu.column_name AS foreign_column
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
            LEFT JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
            WHERE tc.table_schema = 'public'
            ORDER BY tc.table_name, tc.constraint_name
            """).getResultList();

        List<Object[]> indexes = em.createNativeQuery("""
            SELECT schemaname, tablename, indexname, indexdef
            FROM pg_indexes
            WHERE schemaname = 'public'
            ORDER BY tablename, indexname
            """).getResultList();

        List<Map<String, Object>> tables = new ArrayList<>();
        String currentTable = "";
        List<Map<String, Object>> currentColumns = null;

        for (Object[] row : columns) {
            String tableName = (String) row[0];
            if (!tableName.equals(currentTable)) {
                if (currentColumns != null) {
                    tables.add(Map.of("table", currentTable, "columns", currentColumns));
                }
                currentTable = tableName;
                currentColumns = new ArrayList<>();
            }
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("name", row[1]);
            col.put("type", row[2]);
            col.put("nullable", "YES".equals(row[3]));
            col.put("default", row[4]);
            col.put("maxLength", row[5]);
            currentColumns.add(col);
        }
        if (currentColumns != null) {
            tables.add(Map.of("table", currentTable, "columns", currentColumns));
        }

        List<Map<String, Object>> constraintList = new ArrayList<>();
        for (Object[] row : constraints) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("table", row[0]);
            c.put("name", row[1]);
            c.put("type", row[2]);
            c.put("column", row[3]);
            if (row[4] != null) c.put("foreignTable", row[4]);
            if (row[5] != null) c.put("foreignColumn", row[5]);
            constraintList.add(c);
        }

        List<Map<String, Object>> indexList = new ArrayList<>();
        for (Object[] row : indexes) {
            Map<String, Object> idx = new LinkedHashMap<>();
            idx.put("table", row[1]);
            idx.put("name", row[2]);
            idx.put("definition", row[3]);
            indexList.add(idx);
        }

        return ResponseEntity.ok(Map.of(
            "tables", tables,
            "constraints", constraintList,
            "indexes", indexList
        ));
    }

    private Map<String, Object> sorteosData(int page, int size) {
        long total = sorteoRepository.count();
        List<?> data = em.createNativeQuery(
            "SELECT id, numero_sorteo, fecha, pozo_monto, fuente_api, created_at, updated_at FROM sorteos ORDER BY numero_sorteo DESC")
            .setFirstResult(page * size).setMaxResults(size).getResultList();
        return Map.of("table", "sorteos", "total", total, "page", page, "size", size, "columns",
            List.of("id", "numero_sorteo", "fecha", "pozo_monto", "fuente_api", "created_at", "updated_at"),
            "data", data);
    }

    private Map<String, Object> resultadosData(int page, int size) {
        long total = resultadoRepository.count();
        List<?> data = em.createNativeQuery(
            "SELECT r.id, s.numero_sorteo, r.modalidad, r.numero_1, r.numero_2, r.numero_3, r.numero_4, r.numero_5, r.numero_6, " +
            "r.premio_6_aciertos_ganadores, r.premio_6_aciertos_monto, r.premio_5_aciertos_ganadores, r.premio_5_aciertos_monto, " +
            "r.premio_4_aciertos_ganadores, r.premio_4_aciertos_monto, r.created_at " +
            "FROM resultados r JOIN sorteos s ON s.id = r.sorteo_id ORDER BY s.numero_sorteo DESC, r.modalidad")
            .setFirstResult(page * size).setMaxResults(size).getResultList();
        return Map.of("table", "resultados", "total", total, "page", page, "size", size, "columns",
            List.of("id", "numero_sorteo", "modalidad", "n1", "n2", "n3", "n4", "n5", "n6",
                     "ganadores_6", "monto_6", "ganadores_5", "monto_5", "ganadores_4", "monto_4", "created_at"),
            "data", data);
    }

    private Map<String, Object> usuariosData(int page, int size) {
        long total = usuarioRepository.count();
        List<?> data = em.createNativeQuery(
            "SELECT id, username, role, enabled, created_at, updated_at FROM usuarios ORDER BY created_at")
            .setFirstResult(page * size).setMaxResults(size).getResultList();
        return Map.of("table", "usuarios", "total", total, "page", page, "size", size, "columns",
            List.of("id", "username", "role", "enabled", "created_at", "updated_at"),
            "data", data);
    }

    private Map<String, Object> auditLogData(int page, int size) {
        long total = auditLogRepository.count();
        List<?> data = em.createNativeQuery(
            "SELECT id, entidad, accion, actor, timestamp, entidad_id FROM audit_log ORDER BY timestamp DESC")
            .setFirstResult(page * size).setMaxResults(size).getResultList();
        return Map.of("table", "audit_log", "total", total, "page", page, "size", size, "columns",
            List.of("id", "entidad", "accion", "actor", "timestamp", "entidad_id"),
            "data", data);
    }

    private Map<String, Object> numerosData(int page, int size) {
        long total = countNumeros();
        List<?> data = em.createNativeQuery(
            "SELECT id, numero_sorteo, modalidad, numero, posicion, fecha FROM numeros_sorteados ORDER BY numero_sorteo DESC, posicion")
            .setFirstResult(page * size).setMaxResults(size).getResultList();
        return Map.of("table", "numeros_sorteados", "total", total, "page", page, "size", size, "columns",
            List.of("id", "numero_sorteo", "modalidad", "numero", "posicion", "fecha"),
            "data", data);
    }

    // ─── Elasticsearch ────────────────────────────────────

    @GetMapping("/elasticsearch")
    @Operation(summary = "Estado de Elasticsearch")
    public ResponseEntity<Map<String, Object>> elasticsearchStats() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

            HttpRequest healthReq = HttpRequest.newBuilder()
                .uri(URI.create(elasticsearchUri + "/_cluster/health"))
                .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> healthResp = client.send(healthReq, HttpResponse.BodyHandlers.ofString());

            HttpRequest indicesReq = HttpRequest.newBuilder()
                .uri(URI.create(elasticsearchUri + "/_cat/indices?format=json"))
                .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> indicesResp = client.send(indicesReq, HttpResponse.BodyHandlers.ofString());

            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "cluster", parseSimple(healthResp.body(), "cluster_name", "status", "number_of_nodes", "active_shards"),
                "indices", indicesResp.body()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    // ─── RabbitMQ ─────────────────────────────────────────

    @GetMapping("/rabbitmq")
    @Operation(summary = "Estado de RabbitMQ")
    public ResponseEntity<Map<String, Object>> rabbitmqStats() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            String auth = "Basic " + Base64.getEncoder().encodeToString("quini6:quini6_dev_2026".getBytes());

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://" + rabbitmqHost + ":15672/api/overview"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", auth).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            HttpRequest queuesReq = HttpRequest.newBuilder()
                .uri(URI.create("http://" + rabbitmqHost + ":15672/api/queues?columns=name,state,messages"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", auth).GET().build();
            HttpResponse<String> queuesResp = client.send(queuesReq, HttpResponse.BodyHandlers.ofString());

            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "overview", parseSimple(resp.body(), "management_version", "object_totals"),
                "queues", queuesResp.body()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    // ─── Health ───────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check completo")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean pgUp = checkPg();
        boolean esUp = checkHttp(elasticsearchUri + "/_cluster/health");
        boolean rmqUp = checkHttp("http://" + rabbitmqHost + ":15672/api/overview");
        String overall = pgUp && esUp && rmqUp ? "UP" : "DEGRADED";

        return ResponseEntity.ok(Map.of(
            "status", overall,
            "postgres", pgUp ? "UP" : "DOWN",
            "elasticsearch", esUp ? "UP" : "DOWN",
            "rabbitmq", rmqUp ? "UP" : "DOWN"
        ));
    }

    private boolean checkPg() {
        try { return sorteoRepository.count() >= 0; }
        catch (Exception e) { return false; }
    }

    private boolean checkHttp(String url) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() < 400;
        } catch (Exception e) { return false; }
    }

    private Map<String, Object> parseSimple(String json, String... keys) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : keys) {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx >= 0) {
                int colon = json.indexOf(':', idx);
                int start = json.indexOf('"', colon + 1);
                if (start >= 0 && start < colon + 20) {
                    int end = json.indexOf('"', start + 1);
                    result.put(key, json.substring(start + 1, end));
                } else {
                    int end = json.indexOf(',', start + 1);
                    if (end < 0) end = json.indexOf('}', start + 1);
                    result.put(key, json.substring(start + 1, end).trim());
                }
            }
        }
        return result;
    }
}
