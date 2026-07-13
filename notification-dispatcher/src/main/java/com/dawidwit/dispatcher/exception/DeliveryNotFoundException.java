package com.dawidwit.dispatcher.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a delivery id does not exist; mapped to HTTP 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DeliveryNotFoundException extends RuntimeException {

	public DeliveryNotFoundException(Long id) {
		super("Delivery not found: " + id);
	}
}
