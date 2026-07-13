package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulated email delivery — logs the attempt. A real SMTP integration is out of scope. */
@Component
public class EmailSender implements NotificationChannelSender {

	private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

	@Override
	public Channel supports() {
		return Channel.EMAIL;
	}

	@Override
	public void send(DeliveryRecord record) {
		log.atInfo()
				.setMessage("Simulated email delivery")
				.addKeyValue("eventId", record.getEventId())
				.addKeyValue("userId", record.getUserId())
				.log();
	}
}
