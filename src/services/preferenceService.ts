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
