package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Simulated push delivery. A real push-provider integration is out of scope. */
@Component
public class PushSender extends SimulatedChannelSender {

	public PushSender(@Value("${dispatcher.simulation.failure-event-type:}") String failureEventType) {
		super(failureEventType);
	}

	@Override
	public Channel supports() {
		return Channel.PUSH;
	}
}
