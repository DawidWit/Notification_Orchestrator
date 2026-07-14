package com.dawidwit.dispatcher.web;

import com.dawidwit.dispatcher.domain.DeliveryStatus;
import com.dawidwit.dispatcher.dto.DeliveryResponse;
import com.dawidwit.dispatcher.exception.DeliveryNotFoundException;
import com.dawidwit.dispatcher.repository.DeliveryRecordRepository;
import com.dawidwit.dispatcher.repository.DeliveryRecordRepository.StatusCount;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only REST API over delivery records. Always returns DeliveryResponse DTOs, never entities. */
@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

	private final DeliveryRecordRepository repository;

	public DeliveryController(DeliveryRecordRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/{id}")
	public DeliveryResponse getById(@PathVariable Long id) {
		return repository
				.findById(id)
				.map(DeliveryResponse::from)
				.orElseThrow(() -> new DeliveryNotFoundException(id));
	}

	@GetMapping
	public PagedModel<DeliveryResponse> list(
			@RequestParam(required = false) String userId,
			@RequestParam(required = false) DeliveryStatus status,
			Pageable pageable) {
		return new PagedModel<>(repository.search(userId, status, pageable).map(DeliveryResponse::from));
	}

	@GetMapping("/event/{eventId}")
	public List<DeliveryResponse> getByEvent(@PathVariable String eventId) {
		return repository.findByEventId(eventId).stream().map(DeliveryResponse::from).toList();
	}

	@GetMapping("/stats")
	public Map<DeliveryStatus, Long> stats() {
		return repository.countByStatus().stream()
				.collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));
	}
}
