import Joi from 'joi';
import type { Request, Response, NextFunction } from 'express';

const eventTypePreferenceSchema = Joi.object({
  enabled: Joi.boolean().required(),
  channels: Joi.array().items(Joi.string().valid('email', 'sms', 'push')).required(),
});

const dndWindowSchema = Joi.object({
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
  .xor('endTime', 'isFullDay');

const preferenceFields = {
  preferences: Joi.object().pattern(Joi.string(), eventTypePreferenceSchema).optional(),
  dndWindows: Joi.array().items(dndWindowSchema).optional(),
};

const eventSchema = Joi.object({
  eventId: Joi.string().required(),
  userId: Joi.string().required(),
  eventType: Joi.string().required(),
  timestamp: Joi.string().isoDate().required(),
  payload: Joi.object().optional(),
});

const preferenceSchema = Joi.object(preferenceFields);

const preferenceUpdateSchema = Joi.object(preferenceFields).min(1);

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
