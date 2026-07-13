package com.dawidwit.dispatcher.config;

import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kafka consumer wiring.
 *
 * <p>The base consumer config (group id, offset reset) comes from {@code application.yml}, and the
 * broker address from {@link KafkaConnectionDetails} (Testcontainers {@code @ServiceConnection} in
 * tests, {@code SPRING_KAFKA_BOOTSTRAP_SERVERS} in production). We build the value deserializer
 * explicitly with Spring's Jackson 3 {@link JsonMapper} — which has java.time support and can parse
 * the event's {@code Instant} — because the deserializer Spring Kafka would create from a property
 * uses a bare mapper that cannot.
 */
@Configuration
public class KafkaConsumerConfig {

	@Bean
	ConsumerFactory<String, NotificationDecisionEvent> consumerFactory(
			KafkaProperties properties, KafkaConnectionDetails connectionDetails, JsonMapper jsonMapper) {
		Map<String, Object> config = properties.buildConsumerProperties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
		JacksonJsonDeserializer<NotificationDecisionEvent> valueDeserializer =
				new JacksonJsonDeserializer<>(NotificationDecisionEvent.class, jsonMapper);
		valueDeserializer.ignoreTypeHeaders(); // TS producer sends no Java type headers
		return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, NotificationDecisionEvent> kafkaListenerContainerFactory(
			ConsumerFactory<String, NotificationDecisionEvent> consumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, NotificationDecisionEvent> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		return factory;
	}
}
