package com.dawidwit.dispatcher.service;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import com.dawidwit.dispatcher.repository.DeliveryRecordRepository;
import com.dawidwit.dispatcher.service.channel.NotificationChannelSender;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Turns a consumed {@link NotificationDecisionEvent} into one delivery per channel. It enforces
 * idempotency on {@code (eventId, channel)} — terminal deliveries are skipped, non-terminal ones are
 * reused across retries so no duplicate rows appear — records each attempt, and invokes the sender.
 * All delivery decisions live here; the Kafka listener stays a thin adapter.
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
		RuntimeException firstFailure = null;
		for (Channel channel : event.channels()) {
			try {
				deliver(event, channel);
			} catch (RuntimeException ex) {
				// Keep delivering the other channels; propagate afterwards so Kafka retries the failed ones.
				if (firstFailure == null) {
					firstFailure = ex;
				}
			}
		}
		if (firstFailure != null) {
			throw firstFailure;
		}
	}

	private void deliver(NotificationDecisionEvent event, Channel channel) {
		Optional<DeliveryRecord> existing = repository.findByEventIdAndChannel(event.eventId(), channel);
		if (existing.isPresent() && existing.get().isTerminal()) {
			return; // already SENT or DEAD_LETTERED — idempotent skip
		}

		DeliveryRecord record =
				existing.orElseGet(
						() ->
								new DeliveryRecord(
										event.eventId(), event.userId(), event.eventType(), channel));
		NotificationChannelSender sender = senderRegistry.senderFor(channel);
		try {
			sender.send(record);
			record.markSent();
			repository.save(record);
			log.atInfo()
					.setMessage("Delivery sent")
					.addKeyValue("eventId", event.eventId())
					.addKeyValue("channel", channel)
					.log();
		} catch (RuntimeException ex) {
			// Record the failed attempt, then re-throw so Kafka's non-blocking retry / DLT acts (§4.4).
			record.markFailed(ex.getMessage());
			repository.save(record);
			throw ex;
		}
	}

	/** Marks every not-yet-terminal delivery for an event as dead-lettered; invoked from the DLT. */
	public void deadLetter(NotificationDecisionEvent event, String reason) {
		for (DeliveryRecord record : repository.findByEventId(event.eventId())) {
			if (!record.isTerminal()) {
				record.markDeadLettered(reason);
				repository.save(record);
				log.atWarn()
						.setMessage("Delivery dead-lettered")
						.addKeyValue("eventId", event.eventId())
						.addKeyValue("channel", record.getChannel())
						.addKeyValue("reason", reason)
						.log();
			}
		}
	}
}
