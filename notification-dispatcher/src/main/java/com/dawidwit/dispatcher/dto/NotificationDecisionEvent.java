package com.dawidwit.dispatcher.dto;

import com.dawidwit.dispatcher.domain.Channel;
import java.time.Instant;
import java.util.List;

/**
 * The decision emitted by the orchestrator on the {@code notification.decisions} topic. Immutable
 * transport type — deserialized from JSON off Kafka, never persisted directly.
 *
 * <p>Channel names use the UPPERCASE wire contract shared with the TypeScript orchestrator
 * ({@code EMAIL | SMS | PUSH}).
 */
public record NotificationDecisionEvent(
		String eventId,
		String userId,
		String eventType,
		String decision,
		List<Channel> channels,
		Instant occurredAt) {}
