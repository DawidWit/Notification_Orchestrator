import type { Request, Response, NextFunction } from 'express';
import {
  getUserPreferences,
  upsertUserPreferences,
  updateSpecificUserPreferences,
} from '../services/preferenceService.js';

export const getPreferencesForUser = async (req: Request<{ userId: string }>, res: Response, next: NextFunction): Promise<void> => {
  try {
    const { userId } = req.params;
    const preferences = await getUserPreferences(userId);

    if (!preferences) {
      res.status(404).json({ message: 'User preferences not found' });
      return;
    }
    res.status(200).json(preferences);
  } catch (error) {
    next(error);
  }
};

export const setPreferencesForUser = async (req: Request<{ userId: string }>, res: Response, next: NextFunction): Promise<void> => {
  try {
    const { userId } = req.params;
    const { preferences, dndWindows } = req.body;
    const newPreferences = await upsertUserPreferences(userId, preferences, dndWindows);
    res.status(201).json(newPreferences);
  } catch (error) {
    next(error);
  }
};

export const updatePreferencesForUser = async (req: Request<{ userId: string }>, res: Response, next: NextFunction): Promise<void> => {
  try {
    const { userId } = req.params;
    const updated = await updateSpecificUserPreferences(userId, req.body);

    if (!updated) {
      res.status(404).json({ message: 'User preferences not found or no changes applied' });
      return;
    }
    res.status(200).json(updated);
  } catch (error) {
    next(error);
  }
};
