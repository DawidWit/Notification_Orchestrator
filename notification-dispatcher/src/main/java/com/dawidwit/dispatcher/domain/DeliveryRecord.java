package com.dawidwit.dispatcher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One delivery of a source event to a single channel — the unit that gets sent and retried.
 *
 * <p>The pair {@code (eventId, channel)} is unique (enforced by the database) and acts as the
 * idempotency guard: re-consuming the same source event must never create duplicate rows.
 */
@Entity
@Table(
		name = "delivery_record",
		uniqueConstraints =
				@UniqueConstraint(
						name = "uq_delivery_record_event_channel",
						columnNames = {"event_id", "channel"}))
public class DeliveryRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false)
	private String eventId;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false)
	private Channel channel;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private DeliveryStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "failure_reason")
	private String failureReason;

	// Hibernate maps Instant to SQL Server 'datetimeoffset' by default; pin it to DATETIME2 to match
	// the migration. Values stay UTC via spring.jpa.properties.hibernate.jdbc.time_zone=UTC.
	@JdbcTypeCode(SqlTypes.TIMESTAMP)
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@JdbcTypeCode(SqlTypes.TIMESTAMP)
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Required by JPA; not for application use. */
	protected DeliveryRecord() {}

	/** Creates a fresh record in {@link DeliveryStatus#PENDING} with a zero attempt count. */
	public DeliveryRecord(String eventId, String userId, String eventType, Channel channel) {
		this.eventId = eventId;
		this.userId = userId;
		this.eventType = eventType;
		this.channel = channel;
		this.status = DeliveryStatus.PENDING;
		this.attemptCount = 0;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	/** Marks this delivery as successfully sent, counting the attempt. */
	public void markSent() {
		this.attemptCount++;
		this.status = DeliveryStatus.SENT;
	}

	public Long getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public String getUserId() {
		return userId;
	}

	public String getEventType() {
		return eventType;
	}

	public Channel getChannel() {
		return channel;
	}

	public DeliveryStatus getStatus() {
		return status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
