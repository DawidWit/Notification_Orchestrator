package com.dawidwit.dispatcher.exception;

import com.dawidwit.dispatcher.domain.Channel;

/** Raised when a {@link Channel} has no registered sender — a routing/configuration error. */
public class UnknownChannelException extends RuntimeException {

	public UnknownChannelException(Channel channel) {
		super("No sender registered for channel " + channel);
	}
}
