package com.dawidwit.dispatcher.dto;

import com.dawidwit.dispatcher.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

/**
 * The decision emitted by the orchestrator on the {@code notification.decisions} topic. Immutable
 * transport type — deserialized from JSON off Kafka, never persisted directly.
 *
 * <p>Channel names use the UPPERCASE wire contract shared with the TypeScript orchestrator
 * ({@code EMAIL | SMS | PUSH}). Bean Validation guards the fields we must have to process a delivery.
 */
public record NotificationDecisionEvent(
		@NotBlank String eventId,
		@NotBlank String userId,
		@NotBlank String eventType,
		String decision,
		@NotEmpty List<Channel> channels,
		Instant occurredAt) {}
