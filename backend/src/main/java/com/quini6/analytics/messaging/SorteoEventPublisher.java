package com.quini6.analytics.messaging;

import com.quini6.analytics.config.RabbitMQConfig;
import com.quini6.analytics.domain.entity.Sorteo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        // Deferir el publish hasta AFTER COMMIT: el listener de RabbitMQ lee la DB
        // y si el evento se publica antes del commit, el sorteo aún no es visible.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event, sorteo.getNumeroSorteo());
                }
            });
        } else {
            publish(event, sorteo.getNumeroSorteo());
        }
    }

    private void publish(Map<String, Object> event, Integer numeroSorteo) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_SORTEO_INGERIDO,
                event
            );
            log.info("Evento SorteoIngerido publicado para sorteo {}", numeroSorteo);
        } catch (Exception e) {
            log.error("Error publicando evento para sorteo {}: {}",
                numeroSorteo, e.getMessage());
        }
    }
}
