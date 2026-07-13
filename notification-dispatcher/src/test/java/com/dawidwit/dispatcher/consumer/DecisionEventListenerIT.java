package com.dawidwit.dispatcher.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.dawidwit.dispatcher.TestcontainersConfiguration;
import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import com.dawidwit.dispatcher.service.DeliveryService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Publishes a decision event to a real Kafka broker (Testcontainers) and asserts the {@link
 * DecisionEventListener} consumes it, deserializes the JSON, and hands the event to {@link
 * DeliveryService}. {@code DeliveryService} is mocked so this stays focused on the consume/handoff
 * path (its real logic is Phase 3).
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
			"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
			"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
		})
@Import(TestcontainersConfiguration.class)
class DecisionEventListenerIT {

	@MockitoBean private DeliveryService deliveryService;

	@Autowired private KafkaTemplate<String, String> kafkaTemplate;

	@Value("${dispatcher.kafka.decisions-topic}")
	private String decisionsTopic;

	@Test
	void consumesDecisionAndHandsOffToDeliveryService() {
		String json =
				"""
				{
				  "eventId": "evt-42",
				  "userId": "user-123",
				  "eventType": "security_alert",
				  "decision": "PROCESS_NOTIFICATION",
				  "channels": ["EMAIL", "SMS"],
				  "occurredAt": "2026-07-13T10:00:00Z"
				}
				""";

		kafkaTemplate.send(decisionsTopic, "evt-42", json);

		ArgumentCaptor<NotificationDecisionEvent> captor =
				ArgumentCaptor.forClass(NotificationDecisionEvent.class);
		verify(deliveryService, timeout(20_000)).process(captor.capture());

		NotificationDecisionEvent received = captor.getValue();
		assertThat(received.eventId()).isEqualTo("evt-42");
		assertThat(received.userId()).isEqualTo("user-123");
		assertThat(received.eventType()).isEqualTo("security_alert");
		assertThat(received.decision()).isEqualTo("PROCESS_NOTIFICATION");
		assertThat(received.channels()).containsExactly(Channel.EMAIL, Channel.SMS);
		assertThat(received.occurredAt()).isEqualTo(Instant.parse("2026-07-13T10:00:00Z"));
	}
}
