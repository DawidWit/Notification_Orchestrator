package com.dawidwit.dispatcher.repository;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.domain.DeliveryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link DeliveryRecord}. Spring Data derives the implementations of these queries
 * at runtime from their method names.
 */
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, Long> {

	/** All deliveries produced for one source event (one per channel). */
	List<DeliveryRecord> findByEventId(String eventId);

	/** The single delivery for an (event, channel) pair — the idempotency lookup. */
	Optional<DeliveryRecord> findByEventIdAndChannel(String eventId, Channel channel);

	/** Deliveries for a user in a given state, paged for the REST API. */
	Page<DeliveryRecord> findByUserIdAndStatus(String userId, DeliveryStatus status, Pageable pageable);
}
