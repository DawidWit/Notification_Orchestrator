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
