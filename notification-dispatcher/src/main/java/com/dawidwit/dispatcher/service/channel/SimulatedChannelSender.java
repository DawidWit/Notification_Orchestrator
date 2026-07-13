package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.exception.SimulatedDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for the simulated channel senders: it logs the (fake) delivery and, as a demo/verification
 * hook, throws for a configurable event type so the retry/dead-letter path can be exercised without a
 * real provider. Subclasses only declare which channel they handle via {@link #supports()}.
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
