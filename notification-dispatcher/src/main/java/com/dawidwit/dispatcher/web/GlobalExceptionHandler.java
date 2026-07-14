package com.dawidwit.dispatcher.web;

import com.dawidwit.dispatcher.exception.DeliveryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps REST-layer exceptions to RFC-7807 ProblemDetail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(DeliveryNotFoundException.class)
	ProblemDetail handleNotFound(DeliveryNotFoundException ex) {
		log.debug("Delivery not found: {}", ex.getMessage());
		return problem(HttpStatus.NOT_FOUND, "Delivery not found", ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String detail = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
		log.debug("Bad request: {}", detail);
		return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter", detail);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error handling request", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Unexpected error");
	}

	private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
