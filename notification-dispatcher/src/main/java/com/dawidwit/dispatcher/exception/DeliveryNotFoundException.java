package com.dawidwit.dispatcher.exception;

/** Thrown when a delivery id does not exist; mapped to HTTP 404 by the GlobalExceptionHandler. */
public class DeliveryNotFoundException extends RuntimeException {

	public DeliveryNotFoundException(Long id) {
		super("Delivery not found: " + id);
	}
}
