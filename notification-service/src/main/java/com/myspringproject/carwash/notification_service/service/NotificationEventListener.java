package com.myspringproject.carwash.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.myspringproject.carwash.notification_service.dto.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationMailService notificationMailService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationMailService notificationMailService, ObjectMapper objectMapper) {
        this.notificationMailService = notificationMailService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${carwash.events.notification-queue}")
    public void handleEvent(String payload) throws JsonProcessingException {
        NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
        logger.info("Received notification event {} type {}", event.eventId(), event.eventType());
        notificationMailService.send(event);
    }
}
