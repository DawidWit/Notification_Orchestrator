export const MicroserviceName = {
  NOTIFICATION_ORCHESTRATOR: "notification-orchestrator",
  LOGGER: "logger",
} as const;

export type MicroserviceName = typeof MicroserviceName[keyof typeof MicroserviceName];
