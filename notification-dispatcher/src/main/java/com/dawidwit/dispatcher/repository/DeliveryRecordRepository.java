package com.dawidwit.dispatcher.repository;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.domain.DeliveryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for delivery records. Simple lookups come from the method names; the rest are JPQL. */
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, Long> {

	/** All deliveries produced for one source event (one per channel). */
	List<DeliveryRecord> findByEventId(String eventId);

	/** The single delivery for an (event, channel) pair — the idempotency lookup. */
	Optional<DeliveryRecord> findByEventIdAndChannel(String eventId, Channel channel);

	/** Paged list with optional {@code userId} / {@code status} filters (null = ignore that filter). */
	@Query(
			"""
			SELECT d FROM DeliveryRecord d
			WHERE (:userId IS NULL OR d.userId = :userId)
			  AND (:status IS NULL OR d.status = :status)
			""")
	Page<DeliveryRecord> search(
			@Param("userId") String userId, @Param("status") DeliveryStatus status, Pageable pageable);

	/** Delivery counts grouped by status, for the stats endpoint. */
	@Query("SELECT d.status AS status, COUNT(d) AS count FROM DeliveryRecord d GROUP BY d.status")
	List<StatusCount> countByStatus();

	/** Projection for {@link #countByStatus()}. */
	interface StatusCount {
		DeliveryStatus getStatus();

		long getCount();
	}
}
