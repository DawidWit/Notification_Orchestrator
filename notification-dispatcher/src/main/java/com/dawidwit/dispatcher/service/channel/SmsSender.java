package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Simulated SMS delivery. A real SMS-gateway integration is out of scope. */
@Component
public class SmsSender extends SimulatedChannelSender {

	public SmsSender(@Value("${dispatcher.simulation.failure-event-type:}") String failureEventType) {
		super(failureEventType);
	}

	@Override
	public Channel supports() {
		return Channel.SMS;
	}
}
