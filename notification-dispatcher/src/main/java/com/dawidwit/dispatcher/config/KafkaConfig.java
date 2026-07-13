package com.dawidwit.dispatcher.config;

import com.dawidwit.dispatcher.dto.NotificationDecisionEvent;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.retrytopic.RetryTopicSchedulerWrapper;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kafka wiring. Consumer and producer both use Spring's Jackson 3 {@link JsonMapper} (java.time-aware)
 * for JSON, and the broker address comes from {@link KafkaConnectionDetails}. The producer exists so
 * {@code @RetryableTopic} (enabled here) can forward failed records to the retry and dead-letter
 * topics; {@code @EnableKafkaRetryTopic} bootstraps that non-blocking retry infrastructure.
 */
@Configuration
@EnableKafkaRetryTopic
public class KafkaConfig {

	@Bean
	ConsumerFactory<String, NotificationDecisionEvent> consumerFactory(
			KafkaProperties properties, KafkaConnectionDetails connectionDetails, JsonMapper jsonMapper) {
		Map<String, Object> config = properties.buildConsumerProperties();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
		JacksonJsonDeserializer<NotificationDecisionEvent> valueDeserializer =
				new JacksonJsonDeserializer<>(NotificationDecisionEvent.class, jsonMapper);
		valueDeserializer.ignoreTypeHeaders();
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

	@Bean
	ProducerFactory<String, Object> producerFactory(
			KafkaProperties properties, KafkaConnectionDetails connectionDetails, JsonMapper jsonMapper) {
		Map<String, Object> config = properties.buildProducerProperties();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
		return new DefaultKafkaProducerFactory<>(
				config, new StringSerializer(), new JacksonJsonSerializer<Object>(jsonMapper));
	}

	@Bean
	KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	/**
	 * Scheduler the non-blocking retry infrastructure uses to honour backoff delays. Spring Boot only
	 * auto-creates a {@code TaskScheduler} when scheduling is enabled, so {@code @RetryableTopic} needs
	 * one provided explicitly.
	 */
	@Bean
	RetryTopicSchedulerWrapper retryTopicSchedulerWrapper() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("kafka-retry-");
		scheduler.initialize();
		return new RetryTopicSchedulerWrapper(scheduler);
	}
}
