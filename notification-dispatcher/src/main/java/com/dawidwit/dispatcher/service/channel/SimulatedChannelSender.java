package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.exception.SimulatedDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for the fake senders: logs the delivery, and throws for one configurable eventType so we can
 * exercise the retry/dead-letter path without a real provider.
 */
public abstract class SimulatedChannelSender implements NotificationChannelSender {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String failureEventType;

	protected SimulatedChannelSender(String failureEventType) {
		this.failureEventType = failureEventType;
	}

	@Override
	public void send(DeliveryRecord record) {
		if (!failureEventType.isBlank() && failureEventType.equals(record.getEventType())) {
			throw new SimulatedDeliveryException(supports(), record.getEventType());
		}
		log.atInfo()
				.setMessage("Simulated delivery")
				.addKeyValue("channel", supports())
				.addKeyValue("eventId", record.getEventId())
				.addKeyValue("userId", record.getUserId())
				.log();
	}
}
