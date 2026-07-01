export type Channel = 'email' | 'sms' | 'push';

export type DayOfWeek =
  | 'Monday'
  | 'Tuesday'
  | 'Wednesday'
  | 'Thursday'
  | 'Friday'
  | 'Saturday'
  | 'Sunday';

export interface DndWindow {
  dayOfWeek: DayOfWeek | DayOfWeek[];
  startTime?: string;
  endTime?: string;
  isFullDay: boolean;
}

export interface EventTypePreference {
  enabled: boolean;
  channels: Channel[];
}

export interface UserPreferences {
  userId: string;
  preferences: Record<string, EventTypePreference>;
  dndWindows: DndWindow[];
}
