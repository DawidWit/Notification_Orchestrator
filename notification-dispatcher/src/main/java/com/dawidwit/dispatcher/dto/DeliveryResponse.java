package com.dawidwit.dispatcher.dto;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.domain.DeliveryStatus;
import java.time.Instant;

/** Read model for a delivery, returned by the REST API. The JPA entity is never exposed directly. */
public record DeliveryResponse(
		Long id,
		String eventId,
		String userId,
		String eventType,
		Channel channel,
		DeliveryStatus status,
		int attemptCount,
		String failureReason,
		Instant createdAt,
		Instant updatedAt) {

	public static DeliveryResponse from(DeliveryRecord record) {
		return new DeliveryResponse(
				record.getId(),
				record.getEventId(),
				record.getUserId(),
				record.getEventType(),
				record.getChannel(),
				record.getStatus(),
				record.getAttemptCount(),
				record.getFailureReason(),
				record.getCreatedAt(),
				record.getUpdatedAt());
	}
}
