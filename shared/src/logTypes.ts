import { MicroserviceName } from "./microservices.js";

export type LogLevel = "info" | "warn" | "error" | "debug";

// Global infrastructure tags added by Winston automatically
export interface LogMeta {
  environment: "production" | "staging" | "development";
  version: string;
  hostname: string;
  codeContext?: string;
}

export interface LogContext {
  userId?: string;
  correlationId?: string;
  http?: {
    method: "GET" | "POST" | "PUT" | "DELETE";
    path: string;
    status: number;
    responseTimeMs?: number;
  };
  error?: {
    message: string;
    stack?: string;
    code?: string;
  };
}

export interface LogEvent {
  eventId: string;
  timestamp: string; // ISO-8601 Timestamp
  serviceName: MicroserviceName[keyof MicroserviceName];
  level: LogLevel;
  message: string;
  context?: LogContext;
  payload?: Record<string, unknown>;
  meta: LogMeta;
}
