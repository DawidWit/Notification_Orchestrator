package com.dawidwit.dispatcher.service;

import com.dawidwit.dispatcher.domain.Channel;
import com.dawidwit.dispatcher.exception.UnknownChannelException;
import com.dawidwit.dispatcher.service.channel.NotificationChannelSender;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Looks up the sender for a channel, so callers never switch on channel type. */
@Component
public class ChannelSenderRegistry {

	private final Map<Channel, NotificationChannelSender> sendersByChannel;

	public ChannelSenderRegistry(List<NotificationChannelSender> senders) {
		this.sendersByChannel =
				senders.stream()
						.collect(Collectors.toMap(NotificationChannelSender::supports, Function.identity()));
	}

	public NotificationChannelSender senderFor(Channel channel) {
		NotificationChannelSender sender = sendersByChannel.get(channel);
		if (sender == null) {
			throw new UnknownChannelException(channel);
		}
		return sender;
	}
}
