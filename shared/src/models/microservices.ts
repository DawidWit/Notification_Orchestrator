export const MicroserviceName = {
  NOTIFICATION_ORCHESTRATOR: "notification-orchestrator",
  NOTIFICATION_DISPATCHER: "notification-dispatcher",
  LOGGER: "logger",
} as const;

export type MicroserviceName = typeof MicroserviceName[keyof typeof MicroserviceName];
