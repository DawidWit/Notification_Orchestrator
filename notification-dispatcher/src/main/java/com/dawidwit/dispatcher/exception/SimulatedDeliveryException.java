package com.dawidwit.dispatcher.exception;

import com.dawidwit.dispatcher.domain.Channel;

/** Simulated failure from a channel sender, used to exercise the retry/dead-letter path. */
public class SimulatedDeliveryException extends RuntimeException {

	public SimulatedDeliveryException(Channel channel, String eventType) {
		super("Simulated delivery failure on channel " + channel + " for eventType '" + eventType + "'");
	}
}
