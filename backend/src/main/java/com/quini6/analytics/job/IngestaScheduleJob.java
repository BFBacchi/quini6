package com.quini6.analytics.job;

import com.quini6.analytics.domain.repository.SorteoRepository;
import com.quini6.analytics.service.IngestaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class IngestaScheduleJob {

    private static final Logger log = LoggerFactory.getLogger(IngestaScheduleJob.class);
    private static final ZoneId ART = ZoneId.of("America/Argentina/Buenos_Aires");

    private final IngestaService ingestaService;
    private final SorteoRepository sorteoRepository;

    public IngestaScheduleJob(IngestaService ingestaService, SorteoRepository sorteoRepository) {
        this.ingestaService = ingestaService;
        this.sorteoRepository = sorteoRepository;
    }

    /**
     * Corre cada miércoles y domingo a las 22:30 ART (después del sorteo ~21:30)
     * Spring cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 30 22 * * WED,SUN", zone = "America/Argentina/Buenos_Aires")
    public void ingerirUltimoSorteo() {
        log.info("Job schedule: verificando si hay sorteo nuevo...");

        LocalDate hoy = LocalDate.now(ART);
        DayOfWeek diaSemana = hoy.getDayOfWeek();

        if (diaSemana != DayOfWeek.WEDNESDAY && diaSemana != DayOfWeek.SUNDAY) {
            log.info("Hoy no es día de sorteo ({}), saltando", diaSemana);
            return;
        }

        try {
            sorteoRepository.findMaxNumeroSorteo()
                .ifPresentOrElse(
                    maxNum -> {
                        int siguiente = maxNum + 1;
                        log.info("Intentando ingerir sorteo n°{}", siguiente);
                        ingestaService.ingerirSorteo(siguiente);
                    },
                    () -> {
                        log.info("No hay sorteos en la DB, iniciando backfill...");
                        ingestaService.ingerirRango(
                            LocalDate.of(2024, 1, 1),
                            hoy
                        );
                    }
                );
        } catch (Exception e) {
            log.error("Error en job de ingesta: {}", e.getMessage(), e);
        }
    }
}
