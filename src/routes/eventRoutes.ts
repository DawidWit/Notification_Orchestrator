import { Router } from 'express';
import { ingestEvent } from '../controllers/eventController.js';
import { validatePayload, eventSchema } from '../middleware/validatePayload.js';

const router = Router();

router.post('/', validatePayload(eventSchema), ingestEvent);

export default router;
