import express from 'express';
import dotenv from 'dotenv';
import eventRoutes from './routes/eventRoutes.js';
import preferenceRoutes from './routes/preferenceRoutes.js';

dotenv.config();

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get('/', (_req, res) => {
  res.send('Notification Orchestrator is running!');
});

app.use('/events', eventRoutes);
app.use('/preferences', preferenceRoutes);

const PORT = process.env.PORT || 3000;

if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, () => {
    console.log(`Notification orchestrator microservice running on port ${PORT}`);
  });
}

export default app;
