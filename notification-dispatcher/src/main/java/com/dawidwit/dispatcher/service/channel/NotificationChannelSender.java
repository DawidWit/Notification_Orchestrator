package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;

/**
 * Strategy for delivering a notification over one channel. Each implementation declares the channel
 * it handles via {@link #supports()}, so callers never branch on channel type.
 */
public interface NotificationChannelSender {

	/** The channel this sender handles. */
	Channel supports();

	/**
	 * Delivers the notification. Throws on failure so the caller (and Kafka's retry) can react — never
	 * swallow the error here.
	 */
	void send(DeliveryRecord record);
}
