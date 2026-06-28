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
