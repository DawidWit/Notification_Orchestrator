import type { Request, Response } from 'express';
import {
  getUserPreferences,
  upsertUserPreferences,
  updateSpecificUserPreferences,
} from '../services/preferenceService.js';

export const getPreferencesForUser = async (req: Request<{ userId: string }>, res: Response): Promise<void> => {
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

export const setPreferencesForUser = async (req: Request<{ userId: string }>, res: Response): Promise<void> => {
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

export const updatePreferencesForUser = async (req: Request<{ userId: string }>, res: Response): Promise<void> => {
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
