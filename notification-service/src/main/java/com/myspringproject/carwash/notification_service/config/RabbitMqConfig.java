package com.myspringproject.carwash.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public TopicExchange carwashEventsExchange(@Value("${carwash.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationEmailQueue(@Value("${carwash.events.notification-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding authUserRegisteredBinding(Queue notificationEmailQueue, TopicExchange carwashEventsExchange) {
        return BindingBuilder.bind(notificationEmailQueue)
                .to(carwashEventsExchange)
                .with("auth.user.registered");
    }

    @Bean
    public Binding paymentSuccessBinding(Queue notificationEmailQueue, TopicExchange carwashEventsExchange) {
        return BindingBuilder.bind(notificationEmailQueue)
                .to(carwashEventsExchange)
                .with("payment.success");
    }

    @Bean
    public Binding paymentFailedBinding(Queue notificationEmailQueue, TopicExchange carwashEventsExchange) {
        return BindingBuilder.bind(notificationEmailQueue)
                .to(carwashEventsExchange)
                .with("payment.failed");
    }

    @Bean
    public Binding bookingConfirmedBinding(Queue notificationEmailQueue, TopicExchange carwashEventsExchange) {
        return BindingBuilder.bind(notificationEmailQueue)
                .to(carwashEventsExchange)
                .with("booking.confirmed");
    }

    @Bean
    public Binding bookingCompletedBinding(Queue notificationEmailQueue, TopicExchange carwashEventsExchange) {
        return BindingBuilder.bind(notificationEmailQueue)
                .to(carwashEventsExchange)
                .with("booking.completed");
    }
}
