package com.myspringproject.carwash.auth_service.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.myspringproject.carwash.auth_service.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class VerificationEmailEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailEventPublisher.class);
    private static final String ROUTING_KEY = "auth.user.registered";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchangeName;

    public VerificationEmailEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${carwash.events.exchange}") String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchangeName = exchangeName;
    }

    public void publish(User user, String verificationLink, long expiresInHours) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                ROUTING_KEY,
                Instant.now(),
                user.getEmail(),
                Map.of(
                        "userId", user.getId().toString(),
                        "role", user.getRole().name(),
                        "verificationLink", verificationLink,
                        "expiresInHours", expiresInHours));

        try {
            rabbitTemplate.convertAndSend(exchangeName, ROUTING_KEY, objectMapper.writeValueAsString(event));
            logger.info("Published verification email event for user {}", user.getId());
        } catch (AmqpException | JsonProcessingException e) {
            logger.warn("Unable to publish verification email event for user {}", user.getId(), e);
        }
    }
}
