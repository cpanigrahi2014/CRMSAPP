import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { config } from './config';
import { logger } from './utils/logger';
import aiRoutes from './routes/aiRoutes';
import prisma from './models/prismaClient';

const app = express();

// ─── Global Middleware ───────────────────────────────────────────────────────

app.use(helmet());
app.use(cors({
  origin: ['http://localhost:3000', 'http://localhost:3003', 'http://localhost:5173'],
  credentials: true,
}));
app.use(express.json({ limit: '1mb' }));

// Trust proxy (behind nginx)
app.set('trust proxy', 1);

// Rate limiter with premium support
app.use('/api/ai', (req, res, next) => {
  // Assume req.user is populated by authentication middleware
  let maxRequests = config.rateLimit.max;
  if (req.user && (req.user.roles?.includes('premium'))) {
    maxRequests = config.rateLimit.premiumMax;
  }
  return rateLimit({
    windowMs: config.rateLimit.windowMs,
    max: maxRequests,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests, please try again later.' },
  })(req, res, next);
});

// ─── Health Check ────────────────────────────────────────────────────────────

app.get('/health', (_req, res) => {
  res.json({ status: 'UP', service: 'crm-ai-agent', timestamp: new Date().toISOString() });
});

app.get('/actuator/health', (_req, res) => {
  res.json({ status: 'UP' });
});

// ─── API Routes ──────────────────────────────────────────────────────────────

app.use('/api/ai', aiRoutes);

// ─── 404 ─────────────────────────────────────────────────────────────────────

app.use((_req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// ─── Global Error Handler ────────────────────────────────────────────────────

app.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  logger.error('Unhandled error', err);
  res.status(500).json({ error: 'Internal server error' });
});

// ─── Start Server ────────────────────────────────────────────────────────────

async function start() {
  try {
    // Verify DB connection
    await prisma.$connect();
    logger.info('Database connected');

    app.listen(config.port, () => {
      logger.info(`CRM AI Agent running on port ${config.port}`, {
        env: config.env,
        confirmationMode: config.ai.confirmationMode,
      });
    });
  } catch (err) {
    logger.error('Failed to start server', err);
    process.exit(1);
  }
}

// Graceful shutdown
process.on('SIGTERM', async () => {
  logger.info('SIGTERM received — shutting down');
  await prisma.$disconnect();
  process.exit(0);
});

process.on('SIGINT', async () => {
  logger.info('SIGINT received — shutting down');
  await prisma.$disconnect();
  process.exit(0);
});

start();

export default app;
