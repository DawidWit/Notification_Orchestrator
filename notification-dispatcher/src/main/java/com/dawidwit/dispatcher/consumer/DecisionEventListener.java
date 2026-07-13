package com.dawidwit.dispatcher.consumer;

import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import com.dawidwit.dispatcher.service.DeliveryService;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes notification decisions and hands each off to {@link DeliveryService}. Transient failures
 * retry on non-blocking retry topics ({@code @RetryableTopic}); once attempts are exhausted the
 * record lands on the dead-letter topic and {@link #onDecisionDlt} marks the permanent failure. The
 * listener stays thin — no delivery logic here.
 */
@Component
public class DecisionEventListener {

	private final DeliveryService deliveryService;

	public DecisionEventListener(DeliveryService deliveryService) {
		this.deliveryService = deliveryService;
	}

	@RetryableTopic(
			attempts = "${dispatcher.kafka.retry.attempts}",
			backOff =
					@BackOff(
							delayString = "${dispatcher.kafka.retry.initial-delay-ms}",
							multiplierString = "${dispatcher.kafka.retry.multiplier}",
							maxDelayString = "${dispatcher.kafka.retry.max-delay-ms}"))
	@KafkaListener(topics = "${dispatcher.kafka.decisions-topic}")
	public void onDecision(NotificationDecisionEvent event) {
		deliveryService.process(event);
	}

	@DltHandler
	public void onDecisionDlt(
			NotificationDecisionEvent event,
			@Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
		deliveryService.deadLetter(event, exceptionMessage);
	}
}
