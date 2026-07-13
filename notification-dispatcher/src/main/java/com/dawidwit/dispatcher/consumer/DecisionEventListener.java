package com.dawidwit.dispatcher.consumer;

import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import com.dawidwit.dispatcher.service.DeliveryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes notification decisions from Kafka and hands each off to {@link DeliveryService}.
 *
 * <p>Intentionally thin: it only deserializes (done by the configured JSON deserializer) and
 * delegates. No delivery logic lives here — that belongs in the service.
 */
@Component
public class DecisionEventListener {

	private final DeliveryService deliveryService;

	public DecisionEventListener(DeliveryService deliveryService) {
		this.deliveryService = deliveryService;
	}

	@KafkaListener(topics = "${dispatcher.kafka.decisions-topic}")
	public void onDecision(NotificationDecisionEvent event) {
		deliveryService.process(event);
	}
}
