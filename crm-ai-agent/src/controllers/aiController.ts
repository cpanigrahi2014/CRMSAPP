import { Request, Response } from 'express';
import { processInstruction, confirmAndExecute, rejectPending } from '../agents/crmAgent';
import { getAuditLogs } from '../models/auditLog';
import prisma from '../models/prismaClient';
import { logger } from '../utils/logger';

// ─── POST /api/ai/configure ─────────────────────────────────────────────────

export async function configure(req: Request, res: Response): Promise<void> {
  try {
    const { instruction, sessionId } = req.body;

    if (!instruction || typeof instruction !== 'string' || instruction.trim().length === 0) {
      res.status(400).json({ error: 'instruction is required' });
      return;
    }

    const userId = req.user!.sub;
    const tenantId = req.user!.tenantId;

    // Auto-create or reuse session
    const sid = sessionId || (await createSession(userId));

    // Save user message
    await prisma.conversationMessage.create({
      data: { sessionId: sid, role: 'user', content: instruction.trim() },
    });

    // Forward the original Bearer token for CRM API calls (create_record)
    const authToken = req.headers.authorization?.replace('Bearer ', '');

    const result = await processInstruction(instruction.trim(), userId, sid, authToken, tenantId);

    res.json({ ...result, sessionId: sid });
  } catch (err: any) {
    logger.error('Configure endpoint error', err);
    res.status(500).json({ error: 'Internal server error', message: err.message });
  }
}

// ─── POST /api/ai/confirm ───────────────────────────────────────────────────

export async function confirm(req: Request, res: Response): Promise<void> {
  try {
    const { auditLogId, sessionId } = req.body;

    if (!auditLogId) {
      res.status(400).json({ error: 'auditLogId is required' });
      return;
    }

    const userId = req.user!.sub;
    const authToken = req.headers.authorization?.replace('Bearer ', '');
    const result = await confirmAndExecute(auditLogId, userId, sessionId, authToken);

    res.json(result);
  } catch (err: any) {
    logger.error('Confirm endpoint error', err);
    res.status(500).json({ error: 'Internal server error', message: err.message });
  }
}

// ─── POST /api/ai/reject ────────────────────────────────────────────────────

export async function reject(req: Request, res: Response): Promise<void> {
  try {
    const { auditLogId } = req.body;

    if (!auditLogId) {
      res.status(400).json({ error: 'auditLogId is required' });
      return;
    }

    await rejectPending(auditLogId);
    res.json({ status: 'rejected', auditLogId });
  } catch (err: any) {
    logger.error('Reject endpoint error', err);
    res.status(500).json({ error: 'Internal server error', message: err.message });
  }
}

// ─── GET /api/ai/audit-logs ─────────────────────────────────────────────────

export async function auditLogs(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.sub;
    const limit = parseInt(req.query.limit as string) || 50;
    const logs = await getAuditLogs(userId, limit);
    res.json(logs);
  } catch (err: any) {
    logger.error('Audit logs endpoint error', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// ─── GET /api/ai/sessions ───────────────────────────────────────────────────

export async function sessions(req: Request, res: Response): Promise<void> {
  try {
    const userId = req.user!.sub;
    const list = await prisma.conversationSession.findMany({
      where: { userId, isActive: true },
      orderBy: { updatedAt: 'desc' },
      take: 20,
      include: {
        messages: { orderBy: { createdAt: 'desc' }, take: 1, select: { content: true, createdAt: true } },
      },
    });
    res.json(list);
  } catch (err: any) {
    logger.error('Sessions endpoint error', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// ─── GET /api/ai/sessions/:id/messages ───────────────────────────────────────

export async function sessionMessages(req: Request, res: Response): Promise<void> {
  try {
    const { id } = req.params;
    const messages = await prisma.conversationMessage.findMany({
      where: { sessionId: id },
      orderBy: { createdAt: 'asc' },
    });
    res.json(messages);
  } catch (err: any) {
    logger.error('Session messages endpoint error', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// ─── GET /api/ai/metadata ───────────────────────────────────────────────────

export async function metadata(req: Request, res: Response): Promise<void> {
  try {
    const objects = await prisma.crmObject.findMany({
      where: { isActive: true },
      include: { fields: { where: { isActive: true } } },
      orderBy: { name: 'asc' },
    });
    const pipelines = await prisma.pipeline.findMany({
      where: { isActive: true },
      include: { stages: { orderBy: { sortOrder: 'asc' } } },
    });
    const roles = await prisma.role.findMany({
      where: { isActive: true },
      include: { permissions: true },
    });

    res.json({ objects, pipelines, roles });
  } catch (err: any) {
    logger.error('Metadata endpoint error', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// ─── GET /api/ai/metadata/workflows ──────────────────────────────────────────

export async function metadataWorkflows(req: Request, res: Response): Promise<void> {
  try {
    const workflows = await prisma.workflow.findMany({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });
    res.json(workflows);
  } catch (err: any) {
    logger.error('Metadata workflows endpoint error', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// ─── GET /api/ai/metadata/dashboards ─────────────────────────────────────────

export async function metadataDashboards(req: Request, res: Response): Promise<void> {
  try {
    const dashboards = await prisma.dashboard.findMany({
      where: { isActive: true },
      include: { widgets: true },
      orderBy: { createdAt: 'desc' },
    });
    res.json(dashboards);
  } catch (err: any) {
    logger.error('Metadata dashboards endpoint error', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// ─── Helper ──────────────────────────────────────────────────────────────────

async function createSession(userId: string): Promise<string> {
  const session = await prisma.conversationSession.create({
    data: { userId },
  });
  return session.id;
}
