package com.myspringproject.carwash.payment_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange carwashEventsExchange(@Value("${carwash.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }
}
