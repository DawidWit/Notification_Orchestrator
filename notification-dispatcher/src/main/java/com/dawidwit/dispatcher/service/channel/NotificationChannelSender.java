package com.dawidwit.dispatcher.service.channel;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.domain.DeliveryRecord;

/** Delivers a notification over one channel; each implementation says which via supports(). */
public interface NotificationChannelSender {

	/** The channel this sender handles. */
	Channel supports();

	/** Delivers the notification. Throws on failure so retries can kick in — don't swallow errors. */
	void send(DeliveryRecord record);
}
