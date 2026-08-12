package com.quini6.analytics.messaging;

import com.quini6.analytics.config.RabbitMQConfig;
import com.quini6.analytics.domain.entity.Sorteo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class SorteoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SorteoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public SorteoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarSorteoIngerido(Sorteo sorteo) {
        Map<String, Object> event = new HashMap<>();
        event.put("sorteoId", sorteo.getId().toString());
        event.put("numeroSorteo", sorteo.getNumeroSorteo());
        event.put("fecha", sorteo.getFecha().toString());
        event.put("timestamp", Instant.now().toString());

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_SORTEO_INGERIDO,
                event
            );
            log.info("Evento SorteoIngerido publicado para sorteo {}", sorteo.getNumeroSorteo());
        } catch (Exception e) {
            log.error("Error publicando evento para sorteo {}: {}",
                sorteo.getNumeroSorteo(), e.getMessage());
        }
    }
}
