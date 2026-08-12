package com.quini6.analytics.messaging;

import com.quini6.analytics.config.RabbitMQConfig;
import com.quini6.analytics.elasticsearch.SorteoIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SorteoIngestionListener {

    private static final Logger log = LoggerFactory.getLogger(SorteoIngestionListener.class);

    private final SorteoIndexService indexService;

    public SorteoIngestionListener(SorteoIndexService indexService) {
        this.indexService = indexService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ELASTICSEARCH_INDEXAR)
    public void handleSorteoIngerido(
            Map<String, Object> event,
            Message message,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            String sorteoId = (String) event.get("sorteoId");
            Integer numeroSorteo = (Integer) event.get("numeroSorteo");

            log.info("Indexando sorteo {} en Elasticsearch...", numeroSorteo);
            indexService.indexarSorteo(sorteoId);

        } catch (Exception e) {
            log.error("Error indexando sorteo: {}", e.getMessage());
        }
    }
}
