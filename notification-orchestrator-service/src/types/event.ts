import type { Channel } from './preference.js';

export interface EventPayload {
  eventId: string;
  userId: string;
  eventType: string;
  timestamp: string;
  payload?: Record<string, unknown>;
}

export type DecisionReason =
  | 'NO_PREFERENCES_FOUND'
  | 'DND_ACTIVE'
  | 'PREFERENCES_DISABLED'
  | 'NO_CHANNELS_CONFIGURED';

// Channels travel UPPERCASE on the wire — the contract the Java dispatcher validates (NotificationDecisionEvent).
export type WireChannel = 'EMAIL' | 'SMS' | 'PUSH';

const CHANNEL_TO_WIRE: Record<Channel, WireChannel> = {
  email: 'EMAIL',
  sms: 'SMS',
  push: 'PUSH',
};

export const toWireChannel = (channel: Channel): WireChannel => CHANNEL_TO_WIRE[channel];

export type NotificationDecision =
  | {
      decision: 'DO_NOT_NOTIFY';
      eventId: string;
      userId: string;
      reason: DecisionReason;
    }
  | {
      decision: 'PROCESS_NOTIFICATION';
      eventId: string;
      userId: string;
      eventType: string;
      channels: WireChannel[];
      occurredAt: string;
    };
