package com.quini6.analytics.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quini6.analytics.client.Quini6ApiClient;
import com.quini6.analytics.domain.entity.Sorteo;
import com.quini6.analytics.domain.enums.Modalidad;
import com.quini6.analytics.domain.repository.ResultadoRepository;
import com.quini6.analytics.domain.repository.SorteoRepository;
import com.quini6.analytics.messaging.SorteoEventPublisher;
import com.quini6.analytics.service.IngestaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestaServiceTest {

    @Mock
    private Quini6ApiClient apiClient;
    @Mock
    private SorteoRepository sorteoRepository;
    @Mock
    private ResultadoRepository resultadoRepository;
    @Mock
    private SorteoEventPublisher eventPublisher;

    private IngestaService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        io.micrometer.core.instrument.simple.SimpleMeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        service = new IngestaService(apiClient, sorteoRepository, resultadoRepository, eventPublisher, objectMapper, meterRegistry);
    }

    @Test
    void ingerirSorteo_yaExistente_retornaSinProcesar() {
        when(sorteoRepository.existsByNumeroSorteo(3396)).thenReturn(true);

        IngestaService.IngestaResult result = service.ingerirSorteo(3396);

        assertThat(result.yaExistente()).isTrue();
        assertThat(result.exitoso()).isFalse();
        verifyNoInteractions(apiClient);
    }

    @Test
    void ingerirSorteo_apiRetornaNull_retornaError() {
        when(sorteoRepository.existsByNumeroSorteo(3396)).thenReturn(false);
        when(apiClient.getSorteoByNumero(3396)).thenReturn(null);

        IngestaService.IngestaResult result = service.ingerirSorteo(3396);

        assertThat(result.exitoso()).isFalse();
        assertThat(result.mensaje()).contains("no data");
    }

    @Test
    void ingerirSorteo_exitoso_guardaSorteoYResultados() {
        when(sorteoRepository.existsByNumeroSorteo(3396)).thenReturn(false);

        Quini6ApiClient.InfoSorteo info = new Quini6ApiClient.InfoSorteo(
            "3396", "Sorteo 3396", "05/08/2026", "http://link"
        );
        Quini6ApiClient.ResultadoItem resultadoItem = new Quini6ApiClient.ResultadoItem(
            "Tradicional", "03 04 14 17 19 35",
            List.of(
                new Quini6ApiClient.PremioItem("6", "vacante", "$0"),
                new Quini6ApiClient.PremioItem("5", "10", "$5.369.821,20"),
                new Quini6ApiClient.PremioItem("4", "467", "$34.495,64")
            )
        );
        Quini6ApiClient.ResultadoSorteoResponse response =
            new Quini6ApiClient.ResultadoSorteoResponse(List.of(info), List.of(resultadoItem));

        when(apiClient.getSorteoByNumero(3396)).thenReturn(response);
        when(sorteoRepository.save(any(Sorteo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resultadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IngestaService.IngestaResult result = service.ingerirSorteo(3396);

        assertThat(result.exitoso()).isTrue();
        assertThat(result.modalidadesIngeridas()).isEqualTo(1);

        ArgumentCaptor<Sorteo> sorteoCaptor = ArgumentCaptor.forClass(Sorteo.class);
        verify(sorteoRepository).save(sorteoCaptor.capture());
        Sorteo savedSorteo = sorteoCaptor.getValue();
        assertThat(savedSorteo.getNumeroSorteo()).isEqualTo(3396);
        assertThat(savedSorteo.getFecha()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(savedSorteo.getFuenteApi()).isEqualTo("Q6R");

        verify(eventPublisher).publicarSorteoIngerido(any(Sorteo.class));
    }

    @Test
    void ingerirSorteo_modalidadDesconocida_seOmita() {
        when(sorteoRepository.existsByNumeroSorteo(3396)).thenReturn(false);

        Quini6ApiClient.InfoSorteo info = new Quini6ApiClient.InfoSorteo(
            "3396", "Sorteo 3396", "05/08/2026", "http://link"
        );
        Quini6ApiClient.ResultadoItem resultadoItem = new Quini6ApiClient.ResultadoItem(
            "Modalidad Inexistente", "03 04 14 17 19 35", List.of()
        );
        Quini6ApiClient.ResultadoSorteoResponse response =
            new Quini6ApiClient.ResultadoSorteoResponse(List.of(info), List.of(resultadoItem));

        when(apiClient.getSorteoByNumero(3396)).thenReturn(response);
        when(sorteoRepository.save(any(Sorteo.class))).thenAnswer(inv -> inv.getArgument(0));

        IngestaService.IngestaResult result = service.ingerirSorteo(3396);

        assertThat(result.exitoso()).isTrue();
        assertThat(result.modalidadesIngeridas()).isZero();
        verify(resultadoRepository, never()).save(any());
    }

    @Test
    void ingerirSorteo_numerosInvalidos_seOmita() {
        when(sorteoRepository.existsByNumeroSorteo(3396)).thenReturn(false);

        Quini6ApiClient.InfoSorteo info = new Quini6ApiClient.InfoSorteo(
            "3396", "Sorteo 3396", "05/08/2026", "http://link"
        );
        // Solo 4 números en vez de 6
        Quini6ApiClient.ResultadoItem resultadoItem = new Quini6ApiClient.ResultadoItem(
            "Tradicional", "03 04 14 17", List.of()
        );
        Quini6ApiClient.ResultadoSorteoResponse response =
            new Quini6ApiClient.ResultadoSorteoResponse(List.of(info), List.of(resultadoItem));

        when(apiClient.getSorteoByNumero(3396)).thenReturn(response);
        when(sorteoRepository.save(any(Sorteo.class))).thenAnswer(inv -> inv.getArgument(0));

        IngestaService.IngestaResult result = service.ingerirSorteo(3396);

        assertThat(result.modalidadesIngeridas()).isZero();
    }
}
