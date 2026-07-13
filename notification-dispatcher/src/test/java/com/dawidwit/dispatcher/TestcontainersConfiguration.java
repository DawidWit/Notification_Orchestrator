package com.dawidwit.dispatcher;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared integration-test infrastructure: real Kafka and MS SQL Server in throwaway Docker
 * containers, auto-wired into the app via {@code @ServiceConnection}. Imported by integration tests
 * that need the full context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.3.1"));
	}

	@Bean
	@ServiceConnection
	MSSQLServerContainer sqlServerContainer() {
		// acceptLicense() is mandatory or the SQL Server image refuses to start.
		return new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
				.acceptLicense();
	}
}
