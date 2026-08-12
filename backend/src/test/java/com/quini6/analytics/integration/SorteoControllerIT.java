package com.quini6.analytics.integration;

import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.repository.SorteoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SorteoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SorteoRepository sorteoRepository;

    @Test
    void listar_vacio_retornaArrayVacio() throws Exception {
        mockMvc.perform(get("/api/sorteos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listar_conDatos_retornaSorteos() throws Exception {
        Sorteo sorteo = new Sorteo(3396, LocalDate.of(2026, 8, 5));
        sorteo.setFuenteApi("TEST");
        sorteoRepository.save(sorteo);

        mockMvc.perform(get("/api/sorteos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].numeroSorteo").value(3396))
            .andExpect(jsonPath("$[0].fecha").value("2026-08-05"));
    }

    @Test
    void porNumero_existente_retornaSorteo() throws Exception {
        Sorteo sorteo = new Sorteo(3396, LocalDate.of(2026, 8, 5));
        sorteo.setFuenteApi("TEST");
        sorteoRepository.save(sorteo);

        mockMvc.perform(get("/api/sorteos/3396"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numeroSorteo").value(3396));
    }

    @Test
    void porNumero_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/sorteos/9999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void porRango_retornaSorteosEnRango() throws Exception {
        Sorteo s1 = new Sorteo(3390, LocalDate.of(2026, 7, 1));
        s1.setFuenteApi("TEST");
        Sorteo s2 = new Sorteo(3391, LocalDate.of(2026, 7, 5));
        s2.setFuenteApi("TEST");
        Sorteo s3 = new Sorteo(3392, LocalDate.of(2026, 8, 1));
        s3.setFuenteApi("TEST");
        sorteoRepository.saveAll(List.of(s1, s2, s3));

        mockMvc.perform(get("/api/sorteos/rango")
                .param("desde", "2026-07-01")
                .param("hasta", "2026-07-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
