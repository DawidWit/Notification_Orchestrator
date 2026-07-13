package com.dawidwit.dispatcher.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

/** Verifies the formatter emits the shared cross-service LogEvent JSON schema. */
class SharedLogEventStructuredLogFormatterTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	@SuppressWarnings("unchecked")
	void formatsLogLineInSharedSchema() {
		MockEnvironment env =
				new MockEnvironment()
						.withProperty("dispatcher.logging.service-name", "notification-dispatcher")
						.withProperty("dispatcher.logging.environment", "development")
						.withProperty("dispatcher.logging.version", "9.9.9");
		SharedLogEventStructuredLogFormatter formatter = new SharedLogEventStructuredLogFormatter(env);

		LoggerContext loggerContext = new LoggerContext();
		LoggingEvent event =
				new LoggingEvent(
						"fqcn",
						loggerContext.getLogger("com.dawidwit.dispatcher.Demo"),
						Level.INFO,
						"decision received",
						null,
						null);
		event.setKeyValuePairs(List.of(new KeyValuePair("eventId", "evt-1")));
		event.setMDCPropertyMap(Map.of("userId", "user-9"));

		String line = formatter.format(event);

		assertThat(line).endsWith("\n");
		Map<String, Object> parsed = jsonMapper.readValue(line, Map.class);
		assertThat(parsed)
				.containsEntry("serviceName", "notification-dispatcher")
				.containsEntry("level", "info")
				.containsEntry("message", "decision received")
				.containsKeys("eventId", "timestamp");
		assertThat((Map<String, Object>) parsed.get("payload")).containsEntry("eventId", "evt-1");
		assertThat((Map<String, Object>) parsed.get("context")).containsEntry("userId", "user-9");

		Map<String, Object> meta = (Map<String, Object>) parsed.get("meta");
		assertThat(meta)
				.containsEntry("environment", "development")
				.containsEntry("version", "9.9.9")
				.containsEntry("codeContext", "com.dawidwit.dispatcher.Demo");
		assertThat(meta.get("hostname")).isNotNull();
	}
}
