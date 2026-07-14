package com.dawidwit.dispatcher.domain;

/** Lifecycle state of a delivery. Persisted by name, so these names are a DB contract. */
public enum DeliveryStatus {
	PENDING,
	SENT,
	FAILED,
	DEAD_LETTERED
}
