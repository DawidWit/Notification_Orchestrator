import type { Request, Response, NextFunction } from 'express';
import { evaluateNotificationDecision } from '../services/notificationService.js';
import type { EventPayload } from '../types/index.js';

export const ingestEvent = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
  try {
    const decision = await evaluateNotificationDecision(req.body as EventPayload);

    if (decision.decision === 'PROCESS_NOTIFICATION') {
      res.status(202).json(decision);
    } else {
      res.status(200).json(decision);
    }
  } catch (error) {
    next(error);
  }
};
