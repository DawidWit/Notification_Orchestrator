package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Simulated email delivery. A real SMTP integration is out of scope. */
@Component
public class EmailSender extends SimulatedChannelSender {

	public EmailSender(@Value("${dispatcher.simulation.failure-event-type:}") String failureEventType) {
		super(failureEventType);
	}

	@Override
	public Channel supports() {
		return Channel.EMAIL;
	}
}
