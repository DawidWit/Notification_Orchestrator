import { MicroserviceName } from "./microservices.js";

export type LogLevel = "info" | "warn" | "error" | "debug";

export type LogEnvironment = "production" | "staging" | "development";

export interface LogMeta {
  environment: LogEnvironment;
  version: string;
  hostname: string;
  codeContext?: string;
}

export interface LogContext {
  userId?: string;
  correlationId?: string;
  http?: {
    method: string;
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
  serviceName: MicroserviceName;
  level: LogLevel;
  message: string;
  context?: LogContext;
  payload?: Record<string, unknown>;
  meta: LogMeta;
}
