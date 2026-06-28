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
