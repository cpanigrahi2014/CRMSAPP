import { config } from '../config';
import { logger } from '../utils/logger';
import { ConfigCommand, ConfigureResponse } from '../utils/schemaValidator';
import { parseInstruction, generateConfirmation } from '../services/aiService';
import { classifyIntent } from './intentClassifier';
import { buildExecutionPlan, ExecutionPlan } from './instructionPlanner';
import { executeCommand } from '../services/crmConfigService';
import { checkPlanLimits } from './planEnforcer';
import { createAuditLog, updateAuditLogStatus, getAuditLogById } from '../models/auditLog';
import { ConfigCommand as ConfigCommandSchema } from '../utils/schemaValidator';
import prisma from '../models/prismaClient';
import type { ConversationHistoryItem } from '../utils/schemaValidator';

// ─── Main Orchestrator ───────────────────────────────────────────────────────

export async function processInstruction(
  instruction: string,
  userId: string,
  sessionId?: string,
  authToken?: string,
  tenantId?: string,
): Promise<ConfigureResponse> {
  logger.info('Processing instruction', { instruction, userId, sessionId });

  // 1. Fetch conversation history for context (if session exists)
  const history = sessionId ? await getSessionHistory(sessionId) : [];

  // 2. Parse instruction via AI
  const rawConfig = await parseInstruction(instruction, history);

  // 3. Classify / normalise intent
  const normalised = classifyIntent(rawConfig);

  // 4. Validate schema with zod
  const parseResult = ConfigCommandSchema.safeParse(normalised);
  if (!parseResult.success) {
    const errorMsg = parseResult.error.issues.map((i) => i.message).join('; ');
    logger.warn('Schema validation failed', { errors: errorMsg, raw: normalised });

    const auditLog = await createAuditLog({
      userId,
      instruction,
      generatedConfig: normalised as Record<string, unknown>,
      executionStatus: 'failed',
      errorMessage: `Invalid config: ${errorMsg}`,
    });

    return {
      action: String(normalised.action ?? 'unknown'),
      status: 'failed',
      details: { errors: parseResult.error.issues },
      message: `Invalid configuration: ${errorMsg}`,
      auditLogId: auditLog.id,
    };
  }

  const command: ConfigCommand = parseResult.data;

  // 5. Build execution plan (validate against live metadata)
  const plan: ExecutionPlan = await buildExecutionPlan(command, config.ai.confirmationMode);

  if (!plan.validation.valid) {
    const auditLog = await createAuditLog({
      userId,
      instruction,
      generatedConfig: command as unknown as Record<string, unknown>,
      executionStatus: 'failed',
      errorMessage: plan.validation.errors.join('; '),
    });

    return {
      action: command.action,
      status: 'failed',
      details: { errors: plan.validation.errors, warnings: plan.validation.warnings },
      message: plan.validation.errors.join(' '),
      auditLogId: auditLog.id,
    };
  }

  // 6. If confirmation mode → save pending and ask
  if (plan.requiresConfirmation) {
    const confirmation = await generateConfirmation(command as unknown as Record<string, unknown>);

    const auditLog = await createAuditLog({
      userId,
      instruction,
      generatedConfig: command as unknown as Record<string, unknown>,
      executionStatus: 'pending',
    });

    // Save to conversation
    if (sessionId) {
      await saveMessage(sessionId, 'user', instruction);
      await saveMessage(sessionId, 'assistant', confirmation, {
        auditLogId: auditLog.id,
        command,
      });
    }

    return {
      action: command.action,
      status: 'pending_confirmation',
      details: {
        ...command as unknown as Record<string, unknown>,
        warnings: plan.validation.warnings,
        plan: plan.description,
      },
      message: `${confirmation}\n\nConfirm?`,
      auditLogId: auditLog.id,
    };
  }

  // 7. Check plan limits before executing
  if (tenantId) {
    const planCheck = await checkPlanLimits(command, tenantId, authToken);
    if (!planCheck.allowed) {
      const auditLog = await createAuditLog({
        userId,
        instruction,
        generatedConfig: command as unknown as Record<string, unknown>,
        executionStatus: 'failed',
        errorMessage: planCheck.message,
      });

      return {
        action: command.action,
        status: 'failed',
        details: { planLimit: true },
        message: planCheck.message,
        auditLogId: auditLog.id,
      };
    }
  }

  // 8. Execute immediately
  return await executeAndLog(command, instruction, userId, sessionId, undefined, authToken);
}

// ─── Confirm & Execute a Pending Command ─────────────────────────────────────

export async function confirmAndExecute(
  auditLogId: string,
  userId: string,
  sessionId?: string,
  authToken?: string,
): Promise<ConfigureResponse> {
  const auditLog = await getAuditLogById(auditLogId);
  if (!auditLog) {
    return {
      action: 'unknown',
      status: 'failed',
      details: {},
      message: 'Audit log not found. The pending action may have expired.',
      auditLogId,
    };
  }

  if (auditLog.executionStatus !== 'pending') {
    return {
      action: String((auditLog.generatedConfig as Record<string, unknown>).action ?? 'unknown'),
      status: 'failed',
      details: {},
      message: `This action has already been ${auditLog.executionStatus}.`,
      auditLogId,
    };
  }

  const parseResult = ConfigCommandSchema.safeParse(auditLog.generatedConfig);
  if (!parseResult.success) {
    await updateAuditLogStatus(auditLogId, 'failed', 'Stored config is invalid');
    return {
      action: 'unknown',
      status: 'failed',
      details: {},
      message: 'Stored configuration is invalid.',
      auditLogId,
    };
  }

  return await executeAndLog(
    parseResult.data,
    auditLog.instruction,
    userId,
    sessionId,
    auditLogId,
    authToken,
  );
}

// ─── Reject a Pending Command ────────────────────────────────────────────────

export async function rejectPending(auditLogId: string): Promise<void> {
  await updateAuditLogStatus(auditLogId, 'rejected');
}

// ─── Internal: Execute + Audit ───────────────────────────────────────────────

async function executeAndLog(
  command: ConfigCommand,
  instruction: string,
  userId: string,
  sessionId?: string,
  existingAuditLogId?: string,
  authToken?: string,
): Promise<ConfigureResponse> {
  const result = await executeCommand(command, userId, authToken);

  const auditLogId = existingAuditLogId
    ? (await updateAuditLogStatus(
        existingAuditLogId,
        result.success ? 'executed' : 'failed',
        result.error,
      )).id
    : (await createAuditLog({
        userId,
        instruction,
        generatedConfig: command as unknown as Record<string, unknown>,
        executionStatus: result.success ? 'executed' : 'failed',
        errorMessage: result.error,
      })).id;

  // Save to conversation
  if (sessionId) {
    await saveMessage(sessionId, 'assistant', result.success
      ? `Done! ${describeResult(command, result.data)}`
      : `Failed: ${result.error}`,
    );
  }

  return {
    action: command.action,
    status: result.success ? 'created' : 'failed',
    details: result.data ?? { error: result.error },
    message: result.success
      ? describeResult(command, result.data)
      : `Failed: ${result.error}`,
    auditLogId,
  };
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function describeResult(cmd: ConfigCommand, data?: Record<string, unknown>): string {
  switch (cmd.action) {
    case 'create_field':
      return `Field "${cmd.field_name}" (${cmd.field_type}) created on ${cmd.object}.`;
    case 'create_object':
      return `Object "${cmd.name}" created.`;
    case 'create_pipeline_stage':
      return `Stage "${cmd.name}" added to pipeline "${cmd.pipeline}".`;
    case 'create_workflow':
      return `Workflow "${cmd.name}" created on ${cmd.object}.`;
    case 'create_pipeline':
      return `Pipeline "${cmd.name}" created for ${cmd.object}.`;
    case 'create_dashboard':
      return `Dashboard "${cmd.name}" created with ${(data as any)?.widgetCount ?? 0} widget(s).`;
    case 'create_role':
      return `Role "${cmd.name}" created.`;
    case 'create_permission':
      return `Permissions set on "${cmd.object}" for role "${cmd.role}".`;
    case 'create_relationship':
      return `Relationship "${cmd.name}" created between ${cmd.source_object} and ${cmd.target_object}.`;
    case 'create_automation_rule':
      return `Automation rule "${cmd.name}" created on ${cmd.object}.`;
    case 'create_record': {
      const rec = cmd as Extract<ConfigCommand, { action: 'create_record' }>;
      return `${rec.record_type.charAt(0).toUpperCase() + rec.record_type.slice(1)} record created successfully.`;
    }
    default:
      return 'Action completed.';
  }
}

async function getSessionHistory(sessionId: string): Promise<ConversationHistoryItem[]> {
  const messages = await prisma.conversationMessage.findMany({
    where: { sessionId },
    orderBy: { createdAt: 'asc' },
    take: 20, // last 20 messages for context
  });
  return messages.map((m) => ({ role: m.role as 'user' | 'assistant' | 'system', content: m.content }));
}

async function saveMessage(
  sessionId: string,
  role: string,
  content: string,
  metadata?: Record<string, unknown>,
) {
  await prisma.conversationMessage.create({
    data: { sessionId, role, content, metadata: (metadata ?? undefined) as any },
  });
}
