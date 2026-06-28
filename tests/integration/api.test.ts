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
