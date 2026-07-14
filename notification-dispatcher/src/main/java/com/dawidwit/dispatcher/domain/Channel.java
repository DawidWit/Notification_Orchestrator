package com.dawidwit.dispatcher.domain;

/** A channel a notification can go out on. Persisted by name, so these names are a DB contract. */
public enum Channel {
	EMAIL,
	SMS,
	PUSH
}
