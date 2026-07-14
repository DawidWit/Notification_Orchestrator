# Notification Orchestrator

A small, event-driven notification platform built as two cooperating microservices: one **decides** whether and how a user should be notified about a product event, the other **delivers** that notification and tracks every attempt. The split mirrors how real notification systems separate *policy* (preferences, quiet hours) from *delivery* (channels, retries, auditing).

The focus of this project is **design and code quality** — clean separation of concerns, explicit contracts between services, idempotent processing, and resilient delivery — rather than breadth of features.

## Table of contents

- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Service: notification-orchestrator](#service-notification-orchestrator)
- [Service: notification-dispatcher](#service-notification-dispatcher)
- [Design & Clean Code](#design--clean-code)
- [Running locally](#running-locally)
- [Testing & documentation](#testing--documentation)
- [Deployment](#deployment)
- [Assumptions](#assumptions)

## Architecture

```
                    ┌──────────────────────────────┐
   product event    │   notification-orchestrator   │   DynamoDB
   ───────────────▶ │   (TypeScript · Express)      │◀────────────▶  user preferences
   POST /events     │                               │                + DND windows
                    │  evaluate preferences + DND   │
                    └───────────────┬───────────────┘
                                    │  decision
                                    │  { PROCESS_NOTIFICATION, channels:[EMAIL,SMS,…] }
                                    │  or { DO_NOT_NOTIFY, reason }
                                    ▼
                        Kafka topic  notification.decisions
                                    │
                                    ▼
                    ┌──────────────────────────────┐
                    │    notification-dispatcher     │   MS SQL Server
                    │    (Java 21 · Spring Boot)      │◀───────────▶  delivery_record
                    │                                │                (one row per channel)
                    │  one delivery per channel      │
                    │  send → retry → dead-letter    │
                    └───────────────┬───────────────┘
                                    │  GET /api/v1/deliveries
                                    ▼
                            delivery status API
```

**Flow.** A product event (`item_shipped`, `security_alert`, …) is posted to the orchestrator. It looks up the user's preferences and "Do Not Disturb" windows and returns a **decision**: either `PROCESS_NOTIFICATION` (with the resolved channels) or `DO_NOT_NOTIFY` (with a reason). Decisions are designed to travel to the dispatcher over the Kafka topic `notification.decisions`; the dispatcher turns each decision into **one delivery per channel**, attempts it, and records the outcome — retrying transient failures with backoff and dead-lettering the rest.

**Integration status.** Each service is complete on its own side of the contract: the orchestrator computes decisions in the exact wire shape the dispatcher validates (`eventId`, `userId`, `eventType`, UPPERCASE `channels`, `occurredAt`), and the dispatcher fully consumes, delivers, and tracks from Kafka. The Kafka **producer** that publishes the orchestrator's decisions onto the topic is the one remaining integration step; today the orchestrator returns the decision over HTTP.

## Tech stack

| | Orchestrator | Dispatcher |
|---|---|---|
| Language | TypeScript (Node.js) | Java 21 |
| Framework | Express | Spring Boot 4.1 |
| Build | npm | Gradle |
| Data store | DynamoDB | MS SQL Server (JPA/Hibernate + Flyway) |
| Messaging | — | Apache Kafka (consumer, retry, DLT) |
| Validation | Joi | Jakarta Bean Validation |
| Tests | Jest + Supertest | — (documented instead; see below) |
| Deploy | AWS Lambda (API Gateway) / Docker | Docker Compose (local) |

Cross-cutting: **REST APIs**, **Git**, and a **shared structured-logging contract** (`shared/`) emitted identically by both services.

## Repository layout

```
.
├── notification-orchestrator-service/   # TS decision engine (Express + DynamoDB)
│   ├── src/
│   │   ├── controllers/   # HTTP handlers (thin)
│   │   ├── services/      # decision logic + preference logic (pure)
│   │   ├── models/        # DynamoDB access
│   │   ├── middleware/    # Joi validation, error handling
│   │   ├── routes/        # endpoint → controller wiring
│   │   └── types/         # domain + wire-contract types
│   └── tests/             # Jest integration tests
├── notification-dispatcher/             # Java delivery engine (Spring Boot + Kafka + SQL)
│   └── src/main/java/com/dawidwit/dispatcher/
│       ├── consumer/      # Kafka listener (thin adapter)
│       ├── service/       # delivery logic + channel senders (strategy)
│       ├── domain/        # JPA entity + enums
│       ├── repository/    # Spring Data repository
│       ├── web/           # read-only REST API + error handling
│       └── config/        # Kafka / retry wiring
├── shared/                              # cross-service LogEvent schema + LogFactory
├── infra/                               # Terraform: API Gateway, Lambda, DynamoDB, RDS
├── docker-compose.yml                   # local orchestrator stack (app + DynamoDB Local)
└── deploy.sh                            # build + package Lambda + terraform plan
```

## Service: notification-orchestrator

The **decision engine**. Stateless request/response; all state lives in DynamoDB.

**Responsibilities**
- Store and merge per-user notification **preferences** (which event types are enabled, and on which channels).
- Enforce **Do Not Disturb** windows (per weekday, time ranges in UTC, or full-day).
- Evaluate an incoming event into a `PROCESS_NOTIFICATION` / `DO_NOT_NOTIFY` decision.

**Endpoints**

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/events` | Evaluate an event → `202` `PROCESS_NOTIFICATION` or `200` `DO_NOT_NOTIFY` |
| `GET` | `/preferences/:userId` | Fetch a user's preferences |
| `POST` | `/preferences/:userId` | Create / overwrite preferences |
| `PUT` | `/preferences/:userId` | Deep-merge preferences / replace DND windows |

A `PROCESS_NOTIFICATION` decision looks like:

```json
{
  "decision": "PROCESS_NOTIFICATION",
  "eventId": "evt_123",
  "userId": "user_42",
  "eventType": "item_shipped",
  "channels": ["EMAIL", "SMS"],
  "occurredAt": "2024-05-28T10:00:00Z"
}
```

A `DO_NOT_NOTIFY` decision carries a `reason`: `NO_PREFERENCES_FOUND`, `DND_ACTIVE`, `PREFERENCES_DISABLED`, or `NO_CHANNELS_CONFIGURED`.

**DynamoDB design.** Single table `NotificationPreferences`, partition key `userId` — the access pattern is always "read/write one user's preferences," so a single-key lookup is optimal and every user's settings live together.

## Service: notification-dispatcher

The **delivery engine**. Consumes decisions and owns the full lifecycle of each delivery.

**Responsibilities**
- Consume decisions from `notification.decisions`.
- Fan each decision out into **one delivery per channel** (email / SMS / push — simulated; no real provider is in scope).
- Persist every delivery and its state to MS SQL Server.
- **Retry** transient failures on non-blocking backoff topics (1s → 2s → 4s), then **dead-letter** after 4 attempts.
- Expose a read-only status API.

**Delivery lifecycle:** `PENDING → SENT`, or `PENDING → FAILED → … → DEAD_LETTERED`.

**Endpoints**

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/deliveries/{id}` | One delivery by id |
| `GET` | `/api/v1/deliveries?userId=&status=&page=&size=` | Filtered, paged list |
| `GET` | `/api/v1/deliveries/event/{eventId}` | All deliveries for one source event |
| `GET` | `/api/v1/deliveries/stats` | Counts grouped by status |
| `GET` | `/actuator/health` | Liveness/readiness (incl. DB) |

## Design & Clean Code

The project is deliberately structured around a handful of principles. Concrete examples:

**Separation of concerns — adapters stay thin, logic stays central.**
The Kafka listener ([`DecisionEventListener`](notification-dispatcher/src/main/java/com/dawidwit/dispatcher/consumer/DecisionEventListener.java)) only validates and delegates; every delivery decision lives in [`DeliveryService`](notification-dispatcher/src/main/java/com/dawidwit/dispatcher/service/DeliveryService.java). The web layer ([`DeliveryController`](notification-dispatcher/src/main/java/com/dawidwit/dispatcher/web/DeliveryController.java)) only maps to DTOs. On the TS side the same layering holds: `routes → controllers → services → models`, with pure decision logic (`evaluateNotificationDecision`) kept separate from persistence.

**Open/closed via the Strategy pattern.**
Channels are handled by a [`NotificationChannelSender`](notification-dispatcher/src/main/java/com/dawidwit/dispatcher/service/channel/NotificationChannelSender.java) interface and resolved through a registry — callers never `switch` on channel type. Adding a channel is one new class.

**Explicit boundaries — DTOs are never entities.**
The REST API returns `DeliveryResponse`, never the `DeliveryRecord` JPA entity; the Kafka `NotificationDecisionEvent` is an immutable transport record that is never persisted. Transport shape, storage shape, and API shape are kept independent.

**The entity owns its state.**
`DeliveryRecord` has no setters — state changes go through intent methods (`markSent()`, `markFailed()`, `markDeadLettered()`), so invalid transitions aren't expressible.

**Idempotency at two layers.**
Re-processing the same decision must never double-send or duplicate rows: the service skips already-terminal deliveries and reuses in-flight ones, and the database enforces a `UNIQUE (event_id, channel)` constraint as a backstop.

**The database owns the schema.**
Flyway migrations are the single source of truth; Hibernate runs in `validate` mode (checks the mapping, never mutates the schema).

**Resilience is explicit.**
Non-blocking retry topics with exponential backoff and a dead-letter topic; validation failures are excluded from retries (a malformed message can't be fixed by retrying) and dead-lettered immediately.

**One logging contract across languages.**
Both services emit the same structured `LogEvent` JSON (defined once in [`shared/`](shared/src/models/logTypes.ts)) — the TS services via `LogFactory`, the Java service via a Logback formatter — so logs are uniform system-wide.

**Dependency injection, everywhere, by constructor.**
Collaborators are `final` and set in the single constructor — no hidden state, trivially testable.

## Running locally

### Orchestrator (+ DynamoDB Local)

```bash
docker-compose up --build          # starts the app on :3000 and DynamoDB Local on :8000
```

Create the preferences table once (requires the AWS CLI with any dummy credentials):

```bash
aws dynamodb create-table \
  --table-name NotificationPreferences \
  --attribute-definitions AttributeName=userId,AttributeType=S \
  --key-schema AttributeName=userId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000
```

### Dispatcher (+ Kafka + MS SQL Server)

```bash
cd notification-dispatcher
docker compose up -d               # Kafka (KRaft) + SQL Server, and creates the 'dispatcher' DB
./gradlew bootRun                  # app starts; GET http://localhost:8080/actuator/health → UP
./scripts/send-sample-event.sh     # publish a sample decision (pass 'force_failure' to drive the DLT path)
```

## Testing & documentation

The orchestrator ships **Jest integration tests** (`tests/`) covering the decision and preference endpoints against DynamoDB Local:

```bash
npm test -w notification-orchestrator-service
```

The dispatcher favours **documentation and runtime validation** over unit tests: inbound events are validated with Bean Validation before processing, the delivery contract is enforced by the database, and behaviour is verified by running the service end-to-end (the sample script drives both the happy path and the retry/dead-letter path). This satisfies the "documentation via tests **or** technical docs" expectation by leaning on the latter for the delivery service.

## Deployment

The orchestrator is packaged as an **AWS Lambda** behind **API Gateway**, with **DynamoDB** and an **RDS** instance provisioned by Terraform in [`infra/`](infra/):

```bash
./deploy.sh          # builds TS, zips the Lambda, runs `terraform init` + `plan`
# then: cd infra && terraform apply tfplan
```

## Assumptions

- **Channels** are limited to `email`, `sms`, `push`. The canonical **wire format is UPPERCASE** (`EMAIL|SMS|PUSH`); the orchestrator stores them lowercase internally and maps at the boundary, and the dispatcher's enum matches the wire form exactly.
- **Time** is handled in **UTC** throughout — event timestamps, DND evaluation, and stored delivery timestamps.
- **No real providers.** Email/SMS/push sending is simulated; the interesting part is the orchestration, tracking, and resilience around it.
- **Event schema is stable** (`eventId`, `userId`, `eventType`, `timestamp`, `payload`).
