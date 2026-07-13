package com.dawidwit.dispatcher.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders every console log line as the shared cross-service {@code LogEvent} JSON (mirroring the
 * TypeScript {@code shared} LogFactory), so all services in the system emit one consistent structured
 * format.
 *
 * <p>Registered via {@code logging.structured.format.console}. It is instantiated by the logging
 * system rather than the Spring context, so the static meta values are read from the {@link
 * Environment} at construction. Application code keeps using plain SLF4J; MDC entries become
 * {@code context} and key-value pairs become {@code payload}.
 */
public class SharedLogEventStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final String serviceName;
	private final String environment;
	private final String version;
	private final String hostname;

	public SharedLogEventStructuredLogFormatter(Environment env) {
		this.serviceName = env.getProperty("dispatcher.logging.service-name", "notification-dispatcher");
		this.environment = env.getProperty("dispatcher.logging.environment", "development");
		this.version = env.getProperty("dispatcher.logging.version", "1.0.0");
		this.hostname = resolveHostname();
	}

	@Override
	public String format(ILoggingEvent event) {
		Map<String, Object> logEvent = new LinkedHashMap<>();
		logEvent.put("eventId", UUID.randomUUID().toString());
		logEvent.put("timestamp", event.getInstant().toString());
		logEvent.put("serviceName", serviceName);
		logEvent.put("level", event.getLevel().toString().toLowerCase(Locale.ROOT));
		logEvent.put("message", event.getFormattedMessage());

		Map<String, Object> context = buildContext(event);
		if (!context.isEmpty()) {
			logEvent.put("context", context);
		}
		Map<String, Object> payload = buildPayload(event);
		if (!payload.isEmpty()) {
			logEvent.put("payload", payload);
		}
		logEvent.put("meta", buildMeta(event));

		return jsonMapper.writeValueAsString(logEvent) + "\n";
	}

	private Map<String, Object> buildContext(ILoggingEvent event) {
		Map<String, Object> context = new LinkedHashMap<>();
		Map<String, String> mdc = event.getMDCPropertyMap();
		putIfPresent(context, "userId", mdc.get("userId"));
		putIfPresent(context, "correlationId", mdc.get("correlationId"));

		IThrowableProxy throwable = event.getThrowableProxy();
		if (throwable != null) {
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("message", throwable.getMessage());
			error.put("stack", ThrowableProxyUtil.asString(throwable));
			error.put("code", throwable.getClassName());
			context.put("error", error);
		}
		return context;
	}

	private Map<String, Object> buildPayload(ILoggingEvent event) {
		List<KeyValuePair> pairs = event.getKeyValuePairs();
		if (pairs == null || pairs.isEmpty()) {
			return Map.of();
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		for (KeyValuePair pair : pairs) {
			payload.put(pair.key, pair.value);
		}
		return payload;
	}

	private Map<String, Object> buildMeta(ILoggingEvent event) {
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("environment", environment);
		meta.put("version", version);
		meta.put("hostname", hostname);
		meta.put("codeContext", event.getLoggerName());
		return meta;
	}

	private static void putIfPresent(Map<String, Object> map, String key, String value) {
		if (value != null && !value.isBlank()) {
			map.put(key, value);
		}
	}

	private static String resolveHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			return "unknown";
		}
	}
}
