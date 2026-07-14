package com.dawidwit.dispatcher.consumer;

import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import com.dawidwit.dispatcher.exception.InvalidDecisionEventException;
import com.dawidwit.dispatcher.service.DeliveryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Validates each decision and hands it to DeliveryService. Transient failures retry on backoff
 * topics then dead-letter; validation failures skip retries and dead-letter straight away.
 */
@Component
public class DecisionEventListener {

	private final DeliveryService deliveryService;
	private final Validator validator;

	public DecisionEventListener(DeliveryService deliveryService, Validator validator) {
		this.deliveryService = deliveryService;
		this.validator = validator;
	}

	@RetryableTopic(
			attempts = "${dispatcher.kafka.retry.attempts}",
			backOff =
					@BackOff(
							delayString = "${dispatcher.kafka.retry.initial-delay-ms}",
							multiplierString = "${dispatcher.kafka.retry.multiplier}",
							maxDelayString = "${dispatcher.kafka.retry.max-delay-ms}"),
			exclude = InvalidDecisionEventException.class,
			traversingCauses = "true")
	@KafkaListener(topics = "${dispatcher.kafka.decisions-topic}")
	public void onDecision(NotificationDecisionEvent event) {
		validate(event);
		deliveryService.process(event);
	}

	@DltHandler
	public void onDecisionDlt(
			NotificationDecisionEvent event,
			@Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
		deliveryService.deadLetter(event, exceptionMessage);
	}

	private void validate(NotificationDecisionEvent event) {
		Set<ConstraintViolation<NotificationDecisionEvent>> violations = validator.validate(event);
		if (!violations.isEmpty()) {
			throw new InvalidDecisionEventException(violations);
		}
	}
}
