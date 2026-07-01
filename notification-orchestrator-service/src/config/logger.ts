import { LogFactory, MicroserviceName } from "shared";

export const logger = new LogFactory({
  serviceName: MicroserviceName.NOTIFICATION_ORCHESTRATOR,
});
