package com.dawidwit.dispatcher.exception;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thrown when an inbound decision event fails Bean Validation. It is excluded from Kafka retries
 * (retrying a malformed message never helps), so such events go straight to the dead-letter topic.
 */
public class InvalidDecisionEventException extends RuntimeException {

	public InvalidDecisionEventException(Set<? extends ConstraintViolation<?>> violations) {
		super("Invalid decision event: " + summarize(violations));
	}

	private static String summarize(Set<? extends ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
				.collect(Collectors.joining("; "));
	}
}
