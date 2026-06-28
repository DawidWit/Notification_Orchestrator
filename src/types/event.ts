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
      channels: Channel[];
    };
