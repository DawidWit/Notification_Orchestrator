import { getUserPreferences } from './preferenceService.js';
import type { EventPayload, NotificationDecision, DndWindow } from '../types/index.js';

const DAY_NAME_TO_INDEX: Record<string, number> = {
  sunday: 0,
  monday: 1,
  tuesday: 2,
  wednesday: 3,
  thursday: 4,
  friday: 5,
  saturday: 6,
};

const isDuringDnd = (timestamp: string, dndWindows: DndWindow[]): boolean => {
  const eventDate = new Date(timestamp);
  const eventDay = eventDate.getUTCDay();
  const eventHours = eventDate.getUTCHours();
  const eventMinutes = eventDate.getUTCMinutes();
  const eventTimeInMinutes = eventHours * 60 + eventMinutes;

  for (const dnd of dndWindows) {
    let dndDays = dnd.dayOfWeek;
    if (!Array.isArray(dndDays)) {
      dndDays = [dndDays];
    }

    const dndDayIndexes = dndDays
      .map((day) => DAY_NAME_TO_INDEX[day.toLowerCase()] ?? -1)
      .filter((day) => day !== -1);

    if (dndDayIndexes.includes(eventDay)) {
      if (dnd.isFullDay) {
        return true;
      }

      if (dnd.startTime && dnd.endTime) {
        const [startHour, startMinute] = dnd.startTime.split(':').map(Number);
        const [endHour, endMinute] = dnd.endTime.split(':').map(Number);

        const dndStartTimeInMinutes = startHour * 60 + startMinute;
        const dndEndTimeInMinutes = endHour * 60 + endMinute;

        if (dndStartTimeInMinutes < dndEndTimeInMinutes) {
          if (eventTimeInMinutes >= dndStartTimeInMinutes && eventTimeInMinutes <= dndEndTimeInMinutes) {
            return true;
          }
        } else {
          if (eventTimeInMinutes >= dndStartTimeInMinutes || eventTimeInMinutes <= dndEndTimeInMinutes) {
            return true;
          }
        }
      }
    }
  }

  return false;
};

export const evaluateNotificationDecision = async (event: EventPayload): Promise<NotificationDecision> => {
  const { userId, eventType, timestamp } = event;

  const userPreferences = await getUserPreferences(userId);

  if (!userPreferences) {
    return {
      decision: 'DO_NOT_NOTIFY',
      eventId: event.eventId,
      userId,
      reason: 'NO_PREFERENCES_FOUND',
    };
  }

  const { preferences, dndWindows = [] } = userPreferences;

  if (isDuringDnd(timestamp, dndWindows)) {
    return {
      decision: 'DO_NOT_NOTIFY',
      eventId: event.eventId,
      userId,
      reason: 'DND_ACTIVE',
    };
  }

  const eventTypePreference = preferences[eventType];

  if (!eventTypePreference || !eventTypePreference.enabled) {
    return {
      decision: 'DO_NOT_NOTIFY',
      eventId: event.eventId,
      userId,
      reason: 'PREFERENCES_DISABLED',
    };
  }

  if (!eventTypePreference.channels || eventTypePreference.channels.length === 0) {
    return {
      decision: 'DO_NOT_NOTIFY',
      eventId: event.eventId,
      userId,
      reason: 'NO_CHANNELS_CONFIGURED',
    };
  }

  return {
    decision: 'PROCESS_NOTIFICATION',
    eventId: event.eventId,
    userId,
    channels: eventTypePreference.channels,
  };
};
