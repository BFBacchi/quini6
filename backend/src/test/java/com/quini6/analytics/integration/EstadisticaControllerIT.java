package com.quini6.analytics.integration;

import com.quini6.analytics.domain.entity.Resultado;
import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.domain.repository.SorteoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class EstadisticaControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SorteoRepository sorteoRepository;

    @Autowired
    private ResultadoRepository resultadoRepository;

    @BeforeEach
    void setUp() {
        resultadoRepository.deleteAll();
        sorteoRepository.deleteAll();
    }

    @Test
    void frecuencia_sinDatos_retornaVacio() throws Exception {
        mockMvc.perform(get("/api/estadisticas/frecuencia")
                .param("modalidad", "TRADICIONAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSorteos").value(0));
    }

    @Test
    void frecuencia_conDatos_retornaFrecuencias() throws Exception {
        Sorteo sorteo = new Sorteo(1, LocalDate.of(2026, 1, 1));
        sorteo.setFuenteApi("TEST");
        sorteo = sorteoRepository.save(sorteo);

        Resultado r = new Resultado(sorteo, Modalidad.TRADICIONAL,
            (short)0, (short)1, (short)2, (short)3, (short)4, (short)5);
        resultadoRepository.save(r);

        mockMvc.perform(get("/api/estadisticas/frecuencia")
                .param("modalidad", "TRADICIONAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSorteos").value(1))
            .andExpect(jsonPath("$.frecuencias.0.absoluta").value(1))
            .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void frecuencia_conRango_retornaFiltrado() throws Exception {
        Sorteo s1 = new Sorteo(1, LocalDate.of(2026, 1, 1));
        s1.setFuenteApi("TEST");
        s1 = sorteoRepository.save(s1);

        Resultado r = new Resultado(s1, Modalidad.TRADICIONAL,
            (short)0, (short)1, (short)2, (short)3, (short)4, (short)5);
        resultadoRepository.save(r);

        mockMvc.perform(get("/api/estadisticas/frecuencia")
                .param("modalidad", "TRADICIONAL")
                .param("desde", "2026-01-01")
                .param("hasta", "2026-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSorteos").value(1));
    }

    @Test
    void chiCuadrado_sinDatos_retornaMensaje() throws Exception {
        mockMvc.perform(get("/api/estadisticas/chi-cuadrado")
                .param("modalidad", "TRADICIONAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.interpretacion").isNotEmpty());
    }

    @Test
    void chiCuadrado_conDatos_retornaResultado() throws Exception {
        Sorteo sorteo = new Sorteo(1, LocalDate.of(2026, 1, 1));
        sorteo.setFuenteApi("TEST");
        sorteo = sorteoRepository.save(sorteo);

        Resultado r = new Resultado(sorteo, Modalidad.TRADICIONAL,
            (short)0, (short)1, (short)2, (short)3, (short)4, (short)5);
        resultadoRepository.save(r);

        mockMvc.perform(get("/api/estadisticas/chi-cuadrado")
                .param("modalidad", "TRADICIONAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSorteos").value(1))
            .andExpect(jsonPath("$.gradosLibertad").value(45))
            .andExpect(jsonPath("$.chiCuadrado").isNumber())
            .andExpect(jsonPath("$.pValue").isNumber())
            .andExpect(jsonPath("$.interpretacion").isNotEmpty());
    }
}
