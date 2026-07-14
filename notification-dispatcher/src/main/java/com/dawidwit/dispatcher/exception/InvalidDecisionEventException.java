package com.dawidwit.dispatcher.exception;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

/** Thrown when a decision event fails validation. Excluded from retries, so it dead-letters straight away. */
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
