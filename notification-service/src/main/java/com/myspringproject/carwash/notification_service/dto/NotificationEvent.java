package com.myspringproject.carwash.notification_service.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String recipientEmail,
        Map<String, Object> data) {
}
