package com.quini6.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "quini6.events";
    public static final String QUEUE_SORTeos_INGERIDOS = "q6.sorteos.ingeridos";
    public static final String QUEUE_ELASTICSEARCH_INDEXAR = "q6.elasticsearch.indexar";
    public static final String QUEUE_CACHE_ACTUALIZAR = "q6.cache.actualizar";
    public static final String ROUTING_KEY_SORTEO_INGERIDO = "sorteo.ingerido";

    @Bean
    public TopicExchange quini6Exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue sorteosIngeridosQueue() {
        return QueueBuilder.durable(QUEUE_SORTeos_INGERIDOS).build();
    }

    @Bean
    public Queue elasticsearchIndexarQueue() {
        return QueueBuilder.durable(QUEUE_ELASTICSEARCH_INDEXAR).build();
    }

    @Bean
    public Queue cacheActualizarQueue() {
        return QueueBuilder.durable(QUEUE_CACHE_ACTUALIZAR).build();
    }

    @Bean
    public Binding sorteosBinding(Queue sorteosIngeridosQueue, TopicExchange quini6Exchange) {
        return BindingBuilder.bind(sorteosIngeridosQueue)
            .to(quini6Exchange)
            .with(ROUTING_KEY_SORTEO_INGERIDO);
    }

    @Bean
    public Binding elasticsearchBinding(Queue elasticsearchIndexarQueue, TopicExchange quini6Exchange) {
        return BindingBuilder.bind(elasticsearchIndexarQueue)
            .to(quini6Exchange)
            .with(ROUTING_KEY_SORTEO_INGERIDO);
    }

    @Bean
    public Binding cacheBinding(Queue cacheActualizarQueue, TopicExchange quini6Exchange) {
        return BindingBuilder.bind(cacheActualizarQueue)
            .to(quini6Exchange)
            .with(ROUTING_KEY_SORTEO_INGERIDO);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
