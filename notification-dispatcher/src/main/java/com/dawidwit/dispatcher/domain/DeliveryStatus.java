package com.dawidwit.dispatcher.domain;

/**
 * Lifecycle state of a single {@link DeliveryRecord}. Persisted by name, so these constant names
 * are part of the database contract (see the {@code status} column of {@code delivery_record}).
 */
public enum DeliveryStatus {
	PENDING,
	SENT,
	FAILED,
	DEAD_LETTERED
}
