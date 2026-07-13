package com.dawidwit.dispatcher.domain;

/**
 * Delivery channel a notification can be sent through. Persisted by name, so these constant names
 * are part of the database contract (see the {@code channel} column of {@code delivery_record}).
 */
public enum Channel {
	EMAIL,
	SMS,
	PUSH
}
