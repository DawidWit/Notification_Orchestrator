package com.dawidwit.dispatcher.service;

import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Turns a consumed {@link NotificationDecisionEvent} into deliveries.
 *
 * <p>The delivery logic — idempotency, one record per channel, invoking senders — arrives in Phase 3.
 * For now this is the seam the Kafka listener hands off to, keeping the listener free of any logic.
 */
@Service
public class DeliveryService {

	private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

	public void process(NotificationDecisionEvent event) {
		log.atInfo()
				.setMessage("Received notification decision")
				.addKeyValue("eventId", event.eventId())
				.addKeyValue("channels", event.channels())
				.log();
	}
}
