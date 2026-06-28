# TypeScript Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the Notification Orchestrator from JavaScript (ESM) to TypeScript while preserving all existing behavior, tests, and deployment targets.

**Architecture:** Rename `.js` to `.ts`, add a `tsconfig.json` that compiles to `dist/`, define domain interfaces in a `src/types/` directory, and update all build/deploy/test tooling to work with the compiled output. The source tree structure (`src/config`, `src/controllers`, `src/middleware`, `src/models`, `src/routes`, `src/services`) stays identical.

**Tech Stack:** TypeScript 5.x, ts-jest (ESM preset), `@types/express`, `@types/supertest`, existing AWS SDK v3 (ships its own types), Joi (ships its own types), deepmerge (ships its own types).

## Global Constraints

- Node.js 22.x runtime (matches `lambda.tf` `nodejs22.x`)
- ESM module system (`"type": "module"` in package.json)
- Express 5
- Compiled output goes to `dist/` (never committed)
- No library swaps (Joi stays Joi, deepmerge stays deepmerge) -- this is a port, not a rewrite
- All existing integration tests must pass against the compiled output

## Proposed File Layout (after migration)

```
.
├── src/
│   ├── types/
│   │   ├── event.ts            # EventPayload, NotificationDecision, DecisionReason
│   │   ├── preference.ts       # UserPreferences, EventTypePreference, DndWindow, Channel
│   │   └── index.ts            # Re-exports all types
│   ├── config/
│   │   └── dynamodb.ts         # (renamed from .js)
│   ├── models/
│   │   └── preferenceModel.ts  # (renamed from .js)
│   ├── middleware/
│   │   └── validatePayload.ts  # (renamed from .js)
│   ├── services/
│   │   ├── notificationService.ts
│   │   └── preferenceService.ts
│   ├── controllers/
│   │   ├── eventController.ts
│   │   └── preferenceController.ts
│   ├── routes/
│   │   ├── eventRoutes.ts
│   │   └── preferenceRoutes.ts
│   ├── app.ts
│   └── lambda.ts
├── tests/
│   └── integration/
│       └── api.test.ts         # (renamed from .js)
├── dist/                       # Compiled output (gitignored)
├── tsconfig.json
├── jest.config.ts              # (renamed from .mjs, uses ts-jest)
├── Dockerfile                  # (updated for build step)
├── deploy.sh                   # (updated to zip dist/ instead of src/)
├── docker-compose.yml          # (updated command)
└── package.json                # (updated scripts, deps)
```

**What changes vs. current layout:**
- New `src/types/` directory (3 files) for domain interfaces
- Every `.js` in `src/` and `tests/` renamed to `.ts`
- `jest.config.mjs` becomes `jest.config.ts`
- New `tsconfig.json` at root
- New `dist/` directory (gitignored)

---

### Task 1: TypeScript tooling and configuration

**Files:**
- Create: `tsconfig.json`
- Create: `jest.config.ts`
- Modify: `package.json`
- Modify: `.gitignore`
- Delete: `jest.config.mjs`

**Interfaces:**
- Consumes: nothing
- Produces: working `npm run build` command, working `npm test` command (initially no `.ts` files yet -- just validates config)

- [ ] **Step 1: Install TypeScript and type dependencies**

```bash
npm install --save-dev typescript @types/express @types/node ts-jest @types/jest @types/supertest
```

- [ ] **Step 2: Create `tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "outDir": "dist",
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "resolveJsonModule": true
  },
  "include": ["src/**/*.ts"],
  "exclude": ["node_modules", "dist", "tests"]
}
```

- [ ] **Step 3: Create `jest.config.ts`**

```ts
import type { Config } from 'jest';

const config: Config = {
  testEnvironment: 'node',
  verbose: true,
  extensionsToTreatAsEsm: ['.ts'],
  transform: {
    '^.+\\.ts$': [
      'ts-jest',
      {
        useESM: true,
        tsconfig: 'tsconfig.json',
      },
    ],
  },
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1',
  },
};

export default config;
```

- [ ] **Step 4: Delete old `jest.config.mjs`**

```bash
rm jest.config.mjs
```

- [ ] **Step 5: Update `package.json` scripts and main**

Change these fields:

```json
{
  "main": "dist/app.js",
  "scripts": {
    "build": "tsc",
    "start": "node dist/app.js",
    "dev": "tsc --watch & nodemon dist/app.js",
    "test": "node --experimental-vm-modules node_modules/jest/bin/jest.js",
    "test:unit": "node --experimental-vm-modules node_modules/jest/bin/jest.js tests/unit",
    "test:integration": "node --experimental-vm-modules node_modules/jest/bin/jest.js tests/integration"
  }
}
```

- [ ] **Step 6: Add `dist/` to `.gitignore`**

Append to `.gitignore`:
```
dist/
```

- [ ] **Step 7: Verify config compiles with no source files**

```bash
npx tsc --noEmit
```

Expected: succeeds with no errors (no `.ts` files to compile yet).

- [ ] **Step 8: Commit**

```bash
git add tsconfig.json jest.config.ts package.json package-lock.json .gitignore
git rm jest.config.mjs
git commit -m "chore: add TypeScript and ts-jest configuration"
```

---

### Task 2: Domain type definitions

**Files:**
- Create: `src/types/event.ts`
- Create: `src/types/preference.ts`
- Create: `src/types/index.ts`

**Interfaces:**
- Consumes: nothing
- Produces: all domain types used across the codebase:
  - `Channel` (type alias: `'email' | 'sms' | 'push'`)
  - `EventPayload` (interface: `eventId`, `userId`, `eventType`, `timestamp`, `payload?`)
  - `DecisionReason` (type alias: `'NO_PREFERENCES_FOUND' | 'DND_ACTIVE' | 'PREFERENCES_DISABLED' | 'NO_CHANNELS_CONFIGURED'`)
  - `NotificationDecision` (discriminated union on `decision` field)
  - `DayOfWeek` (type alias for the seven day strings)
  - `DndWindow` (interface: `dayOfWeek`, `startTime?`, `endTime?`, `isFullDay`)
  - `EventTypePreference` (interface: `enabled`, `channels`)
  - `UserPreferences` (interface: `userId`, `preferences`, `dndWindows`)

- [ ] **Step 1: Create `src/types/event.ts`**

```ts
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
```

- [ ] **Step 2: Create `src/types/preference.ts`**

```ts
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
```

- [ ] **Step 3: Create `src/types/index.ts`**

```ts
export type {
  EventPayload,
  DecisionReason,
  NotificationDecision,
} from './event.js';

export type {
  Channel,
  DayOfWeek,
  DndWindow,
  EventTypePreference,
  UserPreferences,
} from './preference.js';
```

- [ ] **Step 4: Verify types compile**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add src/types/
git commit -m "chore: add domain type definitions for TypeScript migration"
```

---

### Task 3: Port config and model layer

**Files:**
- Rename: `src/config/dynamodb.js` -> `src/config/dynamodb.ts`
- Rename: `src/models/preferenceModel.js` -> `src/models/preferenceModel.ts`

**Interfaces:**
- Consumes: `UserPreferences` from `src/types/index.js`
- Produces:
  - `ddbClient: DynamoDBClient` (named export)
  - `ddbDocClient: DynamoDBDocumentClient` (named export)
  - `getPreferences(userId: string): Promise<UserPreferences | undefined>`
  - `setPreferences(userId: string, preferences: Record<string, EventTypePreference>, dndWindows: DndWindow[]): Promise<UserPreferences>`

- [ ] **Step 1: Rename and port `src/config/dynamodb.ts`**

```bash
git mv src/config/dynamodb.js src/config/dynamodb.ts
```

Update the file contents:

```ts
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';
import dotenv from 'dotenv';

dotenv.config();

const isLocal = process.env.DYNAMODB_ENDPOINT != null;

const clientConfig: ConstructorParameters<typeof DynamoDBClient>[0] = {
  region: process.env.AWS_REGION_CUSTOM || process.env.AWS_REGION || 'us-east-1',
};

if (isLocal) {
  clientConfig.endpoint = process.env.DYNAMODB_ENDPOINT;
  clientConfig.credentials = {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID || 'dummy',
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY || 'dummy',
  };
}

const ddbClient = new DynamoDBClient(clientConfig);
const ddbDocClient = DynamoDBDocumentClient.from(ddbClient);

export { ddbClient, ddbDocClient };
```

- [ ] **Step 2: Rename and port `src/models/preferenceModel.ts`**

```bash
git mv src/models/preferenceModel.js src/models/preferenceModel.ts
```

Update the file contents:

```ts
import { ddbDocClient } from '../config/dynamodb.js';
import { GetCommand, PutCommand } from '@aws-sdk/lib-dynamodb';
import type { UserPreferences, EventTypePreference, DndWindow } from '../types/index.js';

const TABLE_NAME = 'NotificationPreferences';

export const getPreferences = async (userId: string): Promise<UserPreferences | undefined> => {
  const params = {
    TableName: TABLE_NAME,
    Key: { userId },
  };
  const { Item } = await ddbDocClient.send(new GetCommand(params));
  return Item as UserPreferences | undefined;
};

export const setPreferences = async (
  userId: string,
  preferences: Record<string, EventTypePreference>,
  dndWindows: DndWindow[],
): Promise<UserPreferences> => {
  const itemToPut: UserPreferences = {
    userId,
    preferences: preferences || {},
    dndWindows: dndWindows || [],
  };
  const params = {
    TableName: TABLE_NAME,
    Item: itemToPut,
  };
  await ddbDocClient.send(new PutCommand(params));
  return itemToPut;
};
```

- [ ] **Step 3: Verify compilation**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add src/config/dynamodb.ts src/models/preferenceModel.ts
git commit -m "chore: port config and model layer to TypeScript"
```

---

### Task 4: Port service layer

**Files:**
- Rename: `src/services/preferenceService.js` -> `src/services/preferenceService.ts`
- Rename: `src/services/notificationService.js` -> `src/services/notificationService.ts`

**Interfaces:**
- Consumes:
  - `getPreferences`, `setPreferences` from `src/models/preferenceModel.js`
  - All types from `src/types/index.js`
- Produces:
  - `getUserPreferences(userId: string): Promise<UserPreferences | undefined>`
  - `upsertUserPreferences(userId: string, preferences: Record<string, EventTypePreference>, dndWindows: DndWindow[]): Promise<UserPreferences>`
  - `updateSpecificUserPreferences(userId: string, payload: Partial<Pick<UserPreferences, 'preferences' | 'dndWindows'>>): Promise<UserPreferences | null>`
  - `evaluateNotificationDecision(event: EventPayload): Promise<NotificationDecision>`

- [ ] **Step 1: Rename and port `src/services/preferenceService.ts`**

```bash
git mv src/services/preferenceService.js src/services/preferenceService.ts
```

Update the file contents:

```ts
import { getPreferences, setPreferences } from '../models/preferenceModel.js';
import deepmerge from 'deepmerge';
import type { UserPreferences, EventTypePreference, DndWindow } from '../types/index.js';

const overwriteMerge = (_destinationArray: unknown[], sourceArray: unknown[]): unknown[] => sourceArray;

export const getUserPreferences = async (userId: string): Promise<UserPreferences | undefined> => {
  return await getPreferences(userId);
};

export const upsertUserPreferences = async (
  userId: string,
  preferences: Record<string, EventTypePreference>,
  dndWindows: DndWindow[],
): Promise<UserPreferences> => {
  return await setPreferences(userId, preferences, dndWindows);
};

export const updateSpecificUserPreferences = async (
  userId: string,
  payload: Partial<Pick<UserPreferences, 'preferences' | 'dndWindows'>>,
): Promise<UserPreferences | null> => {
  const existingItem = await getPreferences(userId);

  if (!existingItem) {
    return null;
  }

  const currentPreferences = existingItem.preferences || {};
  const currentDndWindows = existingItem.dndWindows || [];

  const mergedPreferences = payload.preferences
    ? deepmerge(currentPreferences, payload.preferences, { arrayMerge: overwriteMerge })
    : currentPreferences;

  const mergedDndWindows = payload.dndWindows !== undefined ? payload.dndWindows : currentDndWindows;

  const fullUpdatedItem: UserPreferences = {
    ...existingItem,
    preferences: mergedPreferences,
    dndWindows: mergedDndWindows,
  };

  await setPreferences(fullUpdatedItem.userId, fullUpdatedItem.preferences, fullUpdatedItem.dndWindows);
  return fullUpdatedItem;
};
```

- [ ] **Step 2: Rename and port `src/services/notificationService.ts`**

```bash
git mv src/services/notificationService.js src/services/notificationService.ts
```

Update the file contents:

```ts
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
```

- [ ] **Step 3: Verify compilation**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add src/services/preferenceService.ts src/services/notificationService.ts
git commit -m "chore: port service layer to TypeScript"
```

---

### Task 5: Port middleware, controllers, and routes

**Files:**
- Rename: `src/middleware/validatePayload.js` -> `src/middleware/validatePayload.ts`
- Rename: `src/controllers/eventController.js` -> `src/controllers/eventController.ts`
- Rename: `src/controllers/preferenceController.js` -> `src/controllers/preferenceController.ts`
- Rename: `src/routes/eventRoutes.js` -> `src/routes/eventRoutes.ts`
- Rename: `src/routes/preferenceRoutes.js` -> `src/routes/preferenceRoutes.ts`

**Interfaces:**
- Consumes: service functions, all types from `src/types/index.js`
- Produces: Express `Router` instances, Joi schema exports, `validatePayload` middleware

- [ ] **Step 1: Rename and port `src/middleware/validatePayload.ts`**

```bash
git mv src/middleware/validatePayload.js src/middleware/validatePayload.ts
```

Update the file contents:

```ts
import Joi from 'joi';
import type { Request, Response, NextFunction } from 'express';

const eventSchema = Joi.object({
  eventId: Joi.string().required(),
  userId: Joi.string().required(),
  eventType: Joi.string().required(),
  timestamp: Joi.string().isoDate().required(),
  payload: Joi.object().optional(),
});

const preferenceSchema = Joi.object({
  preferences: Joi.object()
    .pattern(
      Joi.string(),
      Joi.object({
        enabled: Joi.boolean().required(),
        channels: Joi.array().items(Joi.string().valid('email', 'sms', 'push')).required(),
      }),
    )
    .optional(),
  dndWindows: Joi.array()
    .items(
      Joi.object({
        dayOfWeek: Joi.alternatives()
          .try(
            Joi.string().valid('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'),
            Joi.array().items(
              Joi.string().valid('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'),
            ),
          )
          .required(),
        startTime: Joi.string()
          .pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)
          .optional(),
        endTime: Joi.string()
          .pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)
          .optional(),
        isFullDay: Joi.boolean().default(false),
      })
        .xor('startTime', 'isFullDay')
        .xor('endTime', 'isFullDay'),
    )
    .optional(),
});

const preferenceUpdateSchema = Joi.object({
  preferences: Joi.object()
    .pattern(
      Joi.string(),
      Joi.object({
        enabled: Joi.boolean().required(),
        channels: Joi.array().items(Joi.string().valid('email', 'sms', 'push')).required(),
      }),
    )
    .optional(),
  dndWindows: Joi.array()
    .items(
      Joi.object({
        dayOfWeek: Joi.alternatives()
          .try(
            Joi.string().valid('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'),
            Joi.array().items(
              Joi.string().valid('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'),
            ),
          )
          .required(),
        startTime: Joi.string()
          .pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)
          .optional(),
        endTime: Joi.string()
          .pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)
          .optional(),
        isFullDay: Joi.boolean().default(false),
      })
        .xor('startTime', 'isFullDay')
        .xor('endTime', 'isFullDay'),
    )
    .optional(),
}).min(1);

export const validatePayload =
  (schema: Joi.ObjectSchema) =>
  (req: Request, res: Response, next: NextFunction): void => {
    const dataToValidate = req.body === undefined || req.body === null ? {} : req.body;

    const { error } = schema.validate(dataToValidate);
    if (error) {
      res.status(400).json({ message: error.details[0].message });
      return;
    }
    next();
  };

export { eventSchema, preferenceSchema, preferenceUpdateSchema };
```

- [ ] **Step 2: Rename and port `src/controllers/eventController.ts`**

```bash
git mv src/controllers/eventController.js src/controllers/eventController.ts
```

Update the file contents:

```ts
import type { Request, Response } from 'express';
import { evaluateNotificationDecision } from '../services/notificationService.js';
import type { EventPayload } from '../types/index.js';

export const ingestEvent = async (req: Request, res: Response): Promise<void> => {
  try {
    const decision = await evaluateNotificationDecision(req.body as EventPayload);

    if (decision.decision === 'PROCESS_NOTIFICATION') {
      res.status(202).json(decision);
    } else {
      res.status(200).json(decision);
    }
  } catch (error) {
    console.error('Error ingesting event:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
```

- [ ] **Step 3: Rename and port `src/controllers/preferenceController.ts`**

```bash
git mv src/controllers/preferenceController.js src/controllers/preferenceController.ts
```

Update the file contents:

```ts
import type { Request, Response } from 'express';
import {
  getUserPreferences,
  upsertUserPreferences,
  updateSpecificUserPreferences,
} from '../services/preferenceService.js';

export const getPreferencesForUser = async (req: Request, res: Response): Promise<void> => {
  try {
    const { userId } = req.params;
    const preferences = await getUserPreferences(userId);

    if (!preferences) {
      res.status(404).json({ message: 'User preferences not found' });
      return;
    }
    res.status(200).json(preferences);
  } catch (error) {
    console.error('Error getting preferences:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};

export const setPreferencesForUser = async (req: Request, res: Response): Promise<void> => {
  try {
    const { userId } = req.params;
    const { preferences, dndWindows } = req.body;
    const newPreferences = await upsertUserPreferences(userId, preferences, dndWindows);
    res.status(201).json(newPreferences);
  } catch (error) {
    console.error('Error setting preferences:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};

export const updatePreferencesForUser = async (req: Request, res: Response): Promise<void> => {
  try {
    const { userId } = req.params;
    const updated = await updateSpecificUserPreferences(userId, req.body);

    if (!updated) {
      res.status(404).json({ message: 'User preferences not found or no changes applied' });
      return;
    }
    res.status(200).json(updated);
  } catch (error) {
    console.error('Error updating preferences:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
};
```

- [ ] **Step 4: Rename and port `src/routes/eventRoutes.ts`**

```bash
git mv src/routes/eventRoutes.js src/routes/eventRoutes.ts
```

Update the file contents:

```ts
import { Router } from 'express';
import { ingestEvent } from '../controllers/eventController.js';
import { validatePayload, eventSchema } from '../middleware/validatePayload.js';

const router = Router();

router.post('/', validatePayload(eventSchema), ingestEvent);

export default router;
```

- [ ] **Step 5: Rename and port `src/routes/preferenceRoutes.ts`**

```bash
git mv src/routes/preferenceRoutes.js src/routes/preferenceRoutes.ts
```

Update the file contents:

```ts
import { Router } from 'express';
import {
  getPreferencesForUser,
  setPreferencesForUser,
  updatePreferencesForUser,
} from '../controllers/preferenceController.js';
import { validatePayload, preferenceSchema, preferenceUpdateSchema } from '../middleware/validatePayload.js';

const router = Router();

router.get('/:userId', getPreferencesForUser);
router.post('/:userId', validatePayload(preferenceSchema), setPreferencesForUser);
router.put('/:userId', validatePayload(preferenceUpdateSchema), updatePreferencesForUser);

export default router;
```

- [ ] **Step 6: Verify compilation**

```bash
npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 7: Commit**

```bash
git add src/middleware/ src/controllers/ src/routes/
git commit -m "chore: port middleware, controllers, and routes to TypeScript"
```

---

### Task 6: Port app entrypoints and tests

**Files:**
- Rename: `src/app.js` -> `src/app.ts`
- Rename: `src/lambda.js` -> `src/lambda.ts`
- Rename: `tests/integration/api.test.js` -> `tests/integration/api.test.ts`

**Interfaces:**
- Consumes: all prior tasks
- Produces: working `npm run build` + `npm test`

- [ ] **Step 1: Rename and port `src/app.ts`**

```bash
git mv src/app.js src/app.ts
```

Update the file contents:

```ts
import express from 'express';
import dotenv from 'dotenv';
import eventRoutes from './routes/eventRoutes.js';
import preferenceRoutes from './routes/preferenceRoutes.js';

dotenv.config();

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get('/', (_req, res) => {
  res.send('Notification Orchestrator is running!');
});

app.use('/events', eventRoutes);
app.use('/preferences', preferenceRoutes);

const PORT = process.env.PORT || 3000;

if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, () => {
    console.log(`Notification orchestrator microservice running on port ${PORT}`);
  });
}

export default app;
```

- [ ] **Step 2: Rename and port `src/lambda.ts`**

```bash
git mv src/lambda.js src/lambda.ts
```

Update the file contents:

```ts
import serverless from 'serverless-http';
import app from './app.js';

export const handler = serverless(app);
```

- [ ] **Step 3: Rename and port `tests/integration/api.test.ts`**

```bash
git mv tests/integration/api.test.js tests/integration/api.test.ts
```

Update the file contents:

```ts
import request from 'supertest';
import app from '../../src/app.js';
import { ddbDocClient, ddbClient } from '../../src/config/dynamodb.js';
import { DeleteCommand, PutCommand } from '@aws-sdk/lib-dynamodb';
import { CreateTableCommand, DescribeTableCommand } from '@aws-sdk/client-dynamodb';

const TABLE_NAME = 'NotificationPreferences';

describe('API Integration Tests', () => {
  const testUserId = 'usr_test_123';
  const testEventId = 'evt_test_456';

  beforeAll(async () => {
    try {
      await ddbClient.send(new DescribeTableCommand({ TableName: TABLE_NAME }));
    } catch (e: unknown) {
      if (e instanceof Error && e.name === 'ResourceNotFoundException') {
        await ddbClient.send(
          new CreateTableCommand({
            TableName: TABLE_NAME,
            KeySchema: [{ AttributeName: 'userId', KeyType: 'HASH' }],
            AttributeDefinitions: [{ AttributeName: 'userId', AttributeType: 'S' }],
            ProvisionedThroughput: { ReadCapacityUnits: 1, WriteCapacityUnits: 1 },
          }),
        );
        await new Promise((resolve) => setTimeout(resolve, 3000));
      } else {
        throw e;
      }
    }
  });

  beforeEach(async () => {
    await ddbDocClient.send(
      new DeleteCommand({
        TableName: TABLE_NAME,
        Key: { userId: testUserId },
      }),
    );
  });

  describe('POST /events', () => {
    it('should return 202 PROCESS_NOTIFICATION when preferences allow', async () => {
      await ddbDocClient.send(
        new PutCommand({
          TableName: TABLE_NAME,
          Item: {
            userId: testUserId,
            preferences: {
              item_shipped: { enabled: true, channels: ['email', 'push'] },
            },
            dndWindows: [],
          },
        }),
      );

      const eventPayload = {
        eventId: testEventId,
        userId: testUserId,
        eventType: 'item_shipped',
        timestamp: '2024-07-15T10:00:00Z',
        payload: { orderId: 'ord_1' },
      };

      const res = await request(app).post('/events').send(eventPayload).expect(202);

      expect(res.body.decision).toBe('PROCESS_NOTIFICATION');
      expect(res.body.channels).toEqual(['email', 'push']);
    });

    it('should return 200 DO_NOT_NOTIFY when DND is active', async () => {
      await ddbDocClient.send(
        new PutCommand({
          TableName: TABLE_NAME,
          Item: {
            userId: testUserId,
            preferences: {
              item_shipped: { enabled: true, channels: ['email'] },
            },
            dndWindows: [{ dayOfWeek: 'Monday', startTime: '09:00', endTime: '17:00', isFullDay: false }],
          },
        }),
      );

      const eventPayload = {
        eventId: testEventId,
        userId: testUserId,
        eventType: 'item_shipped',
        timestamp: '2024-07-15T10:00:00Z',
        payload: { orderId: 'ord_2' },
      };

      const res = await request(app).post('/events').send(eventPayload).expect(200);

      expect(res.body.decision).toBe('DO_NOT_NOTIFY');
      expect(res.body.reason).toBe('DND_ACTIVE');
    });

    it('should return 400 for invalid event payload', async () => {
      const invalidPayload = {
        userId: testUserId,
        eventType: 'item_shipped',
      };

      const res = await request(app).post('/events').send(invalidPayload).expect(400);

      expect(res.body.message).toBeDefined();
    });
  });

  describe('GET /preferences/:userId', () => {
    it('should return 200 with user preferences if found', async () => {
      const userPref = {
        userId: testUserId,
        preferences: {
          security_alert: { enabled: true, channels: ['sms'] },
        },
        dndWindows: [],
      };
      await ddbDocClient.send(new PutCommand({ TableName: TABLE_NAME, Item: userPref }));

      const res = await request(app).get(`/preferences/${testUserId}`).expect(200);

      expect(res.body.userId).toBe(testUserId);
      expect(res.body.preferences).toMatchObject(userPref.preferences);
    });

    it('should return 404 if user preferences not found', async () => {
      await request(app).get('/preferences/non_existent_user').expect(404);
    });
  });

  describe('POST /preferences/:userId', () => {
    it('should return 201 and set new user preferences', async () => {
      const newPreferences = {
        preferences: {
          new_feature_announcement: { enabled: true, channels: ['email'] },
        },
        dndWindows: [{ dayOfWeek: 'Tuesday', isFullDay: true }],
      };

      const res = await request(app).post(`/preferences/${testUserId}`).send(newPreferences).expect(201);

      expect(res.body.userId).toBe(testUserId);
      expect(res.body.preferences).toEqual(newPreferences.preferences);
    });

    it('should return 400 for invalid preference payload', async () => {
      const invalidPayload = {
        preferences: {
          item_shipped: { enabled: 'not_a_boolean' },
        },
      };

      const res = await request(app).post(`/preferences/${testUserId}`).send(invalidPayload).expect(400);

      expect(res.body.message).toBeDefined();
    });
  });

  describe('PUT /preferences/:userId', () => {
    beforeEach(async () => {
      await ddbDocClient.send(
        new PutCommand({
          TableName: TABLE_NAME,
          Item: {
            userId: testUserId,
            preferences: {
              item_shipped: { enabled: true, channels: ['email'] },
              security_alert: { enabled: true, channels: ['push'] },
            },
            dndWindows: [{ dayOfWeek: 'Monday', startTime: '00:00', endTime: '08:00', isFullDay: false }],
          },
        }),
      );
    });

    it('should return 200 and update specific preferences', async () => {
      const updatePayload = {
        preferences: {
          item_shipped: { enabled: true, channels: ['email', 'sms'] },
        },
      };

      const res = await request(app).put(`/preferences/${testUserId}`).send(updatePayload).expect(200);

      expect(res.body.userId).toBe(testUserId);
      expect(res.body.preferences.item_shipped.channels).toContain('sms');
      expect(res.body.preferences.security_alert.channels).toEqual(['push']);
    });

    it('should return 200 and update dnd windows', async () => {
      const updatePayload = {
        dndWindows: [{ dayOfWeek: 'Tuesday', isFullDay: true }],
      };

      const res = await request(app).put(`/preferences/${testUserId}`).send(updatePayload).expect(200);

      expect(res.body.userId).toBe(testUserId);
      expect(res.body.dndWindows).toEqual(updatePayload.dndWindows);
    });

    it('should return 400 for invalid update payload', async () => {
      const invalidPayload = {
        preferences: {
          item_shipped: { channels: ['invalid_channel'] },
        },
      };

      const res = await request(app).put(`/preferences/${testUserId}`).send(invalidPayload).expect(400);

      expect(res.body.message).toBeDefined();
    });
  });
});
```

- [ ] **Step 4: Build and run tests**

```bash
npm run build
npm test
```

Expected: `tsc` produces `dist/` with no errors. All 10 integration tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/app.ts src/lambda.ts tests/integration/api.test.ts
git commit -m "chore: port entrypoints and integration tests to TypeScript"
```

---

### Task 7: Update deployment and Docker tooling

**Files:**
- Modify: `Dockerfile`
- Modify: `docker-compose.yml`
- Modify: `deploy.sh`
- Modify: `infra/lambda.tf`

**Interfaces:**
- Consumes: compiled `dist/` output from `npm run build`
- Produces: working Docker dev environment, working Lambda deployment zip

- [ ] **Step 1: Update `Dockerfile`**

```dockerfile
FROM node:lts-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

- [ ] **Step 2: Update `docker-compose.yml` app service command**

Change the `command` line in the `app` service:

```yaml
    command: npm run dev
```

No change needed -- `npm run dev` in the updated `package.json` already runs `tsc --watch` + `nodemon dist/app.js`.

- [ ] **Step 3: Update `deploy.sh` to zip `dist/` instead of `src/`**

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "Building TypeScript..."
npm run build

echo "Packaging Lambda function..."
zip -rq lambda.zip dist/ node_modules/ package.json -x "node_modules/.cache/*"
echo "Created lambda.zip ($(du -h lambda.zip | cut -f1))"

cd infra

echo "Initializing Terraform..."
terraform init

echo "Planning infrastructure changes..."
terraform plan -out=tfplan

echo ""
echo "Review the plan above. To apply:"
echo "  cd infra && terraform apply tfplan"
```

- [ ] **Step 4: Update Lambda handler path in `infra/lambda.tf`**

Change line 57:

```hcl
  handler = "dist/lambda.handler"
```

- [ ] **Step 5: Verify Docker build**

```bash
docker compose build app
```

Expected: builds successfully, TypeScript compiles during image build.

- [ ] **Step 6: Commit**

```bash
git add Dockerfile deploy.sh docker-compose.yml infra/lambda.tf
git commit -m "chore: update deployment tooling for TypeScript compiled output"
```

---

### Task 8: Final cleanup and full verification

**Files:**
- Verify: no `.js` files remain in `src/` or `tests/`
- Verify: all tests pass
- Verify: build produces correct output

- [ ] **Step 1: Verify no JS source files remain**

```bash
find src/ tests/ -name '*.js' -not -path 'dist/*'
```

Expected: no output (no `.js` files remaining).

- [ ] **Step 2: Full build from clean state**

```bash
rm -rf dist/
npm run build
```

Expected: `dist/` is populated with `.js`, `.d.ts`, `.js.map` files mirroring `src/` structure.

- [ ] **Step 3: Run full test suite**

```bash
npm test
```

Expected: all integration tests pass.

- [ ] **Step 4: Verify Lambda entry point exists**

```bash
ls dist/lambda.js dist/lambda.d.ts
```

Expected: both files exist.

- [ ] **Step 5: Commit final state**

```bash
git add -A
git commit -m "chore: complete TypeScript migration"
```
