import { randomUUID } from "crypto";
import os from "os";
import { MicroserviceName } from "./microservices.js";
import { LogEvent, LogLevel, LogContext, LogMeta } from "./logTypes.js";

export class LogFactory {
  private serviceName: MicroserviceName[keyof MicroserviceName];
  private environment: "production" | "staging" | "development";
  private version: string;
  private hostname: string;

  constructor(options: {
    serviceName: MicroserviceName[keyof MicroserviceName];
    environment?: "production" | "staging" | "development";
    version?: string;
    hostname?: string;
  }) {
    this.serviceName = options.serviceName;

    // Detect environment from options or NODE_ENV
    const nodeEnv = process.env.NODE_ENV?.toLowerCase();
    if (options.environment) {
      this.environment = options.environment;
    } else if (nodeEnv === "production" || nodeEnv === "prod") {
      this.environment = "production";
    } else if (nodeEnv === "staging") {
      this.environment = "staging";
    } else {
      this.environment = "development";
    }

    this.version = options.version || process.env.APP_VERSION || "1.0.0";
    this.hostname = options.hostname || os.hostname() || "unknown";
  }

  public createLog(
    level: LogLevel,
    message: string,
    context?: LogContext,
    payload?: Record<string, unknown>,
    codeContext?: string,
  ): LogEvent {
    const meta: LogMeta = {
      environment: this.environment,
      version: this.version,
      hostname: this.hostname,
    };
    if (codeContext) {
      meta.codeContext = codeContext;
    }

    return {
      eventId: randomUUID(),
      timestamp: new Date().toISOString(),
      serviceName: this.serviceName,
      level,
      message,
      context,
      payload,
      meta,
    };
  }

  public info(
    message: string,
    context?: LogContext,
    payload?: Record<string, unknown>,
    codeContext?: string,
  ): LogEvent {
    return this.createLog("info", message, context, payload, codeContext);
  }

  public warn(
    message: string,
    context?: LogContext,
    payload?: Record<string, unknown>,
    codeContext?: string,
  ): LogEvent {
    return this.createLog("warn", message, context, payload, codeContext);
  }

  public error(
    message: string,
    context?: LogContext,
    payload?: Record<string, unknown>,
    codeContext?: string,
  ): LogEvent {
    return this.createLog("error", message, context, payload, codeContext);
  }

  public debug(
    message: string,
    context?: LogContext,
    payload?: Record<string, unknown>,
    codeContext?: string,
  ): LogEvent {
    return this.createLog("debug", message, context, payload, codeContext);
  }
}
