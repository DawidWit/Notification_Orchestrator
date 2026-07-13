package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulated SMS delivery — logs the attempt. A real SMS-gateway integration is out of scope. */
@Component
public class SmsSender implements NotificationChannelSender {

	private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

	@Override
	public Channel supports() {
		return Channel.SMS;
	}

	@Override
	public void send(DeliveryRecord record) {
		log.atInfo()
				.setMessage("Simulated SMS delivery")
				.addKeyValue("eventId", record.getEventId())
				.addKeyValue("userId", record.getUserId())
				.log();
	}
}
