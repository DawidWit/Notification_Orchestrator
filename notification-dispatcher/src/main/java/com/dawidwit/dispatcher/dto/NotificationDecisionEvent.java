package com.dawidwit.dispatcher.dto;

import com.dawidwit.dispatcher.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

/** A decision consumed off the notification.decisions topic. Immutable; never persisted. */
public record NotificationDecisionEvent(
		@NotBlank String eventId,
		@NotBlank String userId,
		@NotBlank String eventType,
		String decision,
		@NotEmpty List<Channel> channels,
		Instant occurredAt) {}
