package com.dawidwit.dispatcher.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;
import com.dawidwit.dispatcher.domain.DeliveryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the Spring context against a real MS SQL Server (Testcontainers), lets Flyway build the
 * schema, then verifies a {@link DeliveryRecord} round-trips and that the {@code (event_id, channel)}
 * idempotency constraint is enforced by the database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(DeliveryRecordPersistenceIT.SqlServerContainerConfig.class)
class DeliveryRecordPersistenceIT {

	@TestConfiguration(proxyBeanMethods = false)
	static class SqlServerContainerConfig {

		@Bean
		@ServiceConnection
		MSSQLServerContainer sqlServerContainer() {
			return new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
					.acceptLicense();
		}
	}

	private final DeliveryRecordRepository repository;

	DeliveryRecordPersistenceIT(@Autowired DeliveryRecordRepository repository) {
		this.repository = repository;
	}

	@Test
	void persistsAndReloadsDeliveryRecordInPendingState() {
		DeliveryRecord saved =
				repository.save(new DeliveryRecord("evt-1", "user-1", "security_alert", Channel.EMAIL));
		assertThat(saved.getId()).isNotNull();

		DeliveryRecord reloaded =
				repository.findByEventIdAndChannel("evt-1", Channel.EMAIL).orElseThrow();
		assertThat(reloaded.getUserId()).isEqualTo("user-1");
		assertThat(reloaded.getEventType()).isEqualTo("security_alert");
		assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PENDING);
		assertThat(reloaded.getAttemptCount()).isZero();
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateEventChannelToGuardIdempotency() {
		repository.saveAndFlush(new DeliveryRecord("evt-2", "user-1", "security_alert", Channel.SMS));

		assertThatThrownBy(
						() ->
								repository.saveAndFlush(
										new DeliveryRecord("evt-2", "user-1", "security_alert", Channel.SMS)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
