package com.dawidwit.dispatcher.service;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import com.dawidwit.dispatcher.repository.DeliveryRecordRepository;
import com.dawidwit.dispatcher.service.channel.NotificationChannelSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Turns a consumed {@link NotificationDecisionEvent} into one delivery per channel: it enforces
 * idempotency on {@code (eventId, channel)}, records the attempt, and invokes the channel sender.
 * All delivery decisions live here — the Kafka listener stays a thin adapter.
 */
@Service
public class DeliveryService {

	private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

	private final DeliveryRecordRepository repository;
	private final ChannelSenderRegistry senderRegistry;

	public DeliveryService(DeliveryRecordRepository repository, ChannelSenderRegistry senderRegistry) {
		this.repository = repository;
		this.senderRegistry = senderRegistry;
	}

	public void process(NotificationDecisionEvent event) {
		log.atInfo()
				.setMessage("Processing notification decision")
				.addKeyValue("eventId", event.eventId())
				.addKeyValue("channels", event.channels())
				.log();
		for (Channel channel : event.channels()) {
			deliver(event, channel);
		}
	}

	private void deliver(NotificationDecisionEvent event, Channel channel) {
		if (repository.findByEventIdAndChannel(event.eventId(), channel).isPresent()) {
			log.atInfo()
					.setMessage("Skipping already-processed delivery")
					.addKeyValue("eventId", event.eventId())
					.addKeyValue("channel", channel)
					.log();
			return;
		}

		NotificationChannelSender sender = senderRegistry.senderFor(channel);
		DeliveryRecord record =
				repository.save(
						new DeliveryRecord(event.eventId(), event.userId(), event.eventType(), channel));

		// Throw, don't catch: a failed send must propagate so Kafka's retry/DLT can act on it (§4.4).
		sender.send(record);

		record.markSent();
		repository.save(record);

		log.atInfo()
				.setMessage("Delivery sent")
				.addKeyValue("eventId", event.eventId())
				.addKeyValue("channel", channel)
				.log();
	}
}
