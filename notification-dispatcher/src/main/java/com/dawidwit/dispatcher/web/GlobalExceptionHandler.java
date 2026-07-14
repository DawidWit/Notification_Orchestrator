package com.dawidwit.dispatcher.web;

import com.dawidwit.dispatcher.exception.DeliveryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns exceptions thrown by the REST layer into consistent RFC-7807 {@link ProblemDetail} responses
 * (served as {@code application/problem+json}). Client errors are logged quietly; unexpected ones at
 * error level.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(DeliveryNotFoundException.class)
	ProblemDetail handleNotFound(DeliveryNotFoundException ex) {
		log.debug("Delivery not found: {}", ex.getMessage());
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Delivery not found");
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String detail = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
		log.debug("Bad request: {}", detail);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid request parameter");
		return problem;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error handling request", ex);
		ProblemDetail problem =
				ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
		problem.setTitle("Internal server error");
		return problem;
	}
}
