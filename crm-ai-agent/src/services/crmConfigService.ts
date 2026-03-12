import prisma from '../models/prismaClient';
import { logger } from '../utils/logger';
import type { ConfigCommand } from '../utils/schemaValidator';
import type { Prisma } from '@prisma/client';
import { createCrmRecord } from './crmApiClient';

// ─── Execution result ────────────────────────────────────────────────────────

export interface ExecutionResult {
  success: boolean;
  data?: Record<string, unknown>;
  error?: string;
}

// ─── CRM Configuration Service ──────────────────────────────────────────────

export async function executeCommand(
  command: ConfigCommand,
  userId: string,
  authToken?: string,
): Promise<ExecutionResult> {
  try {
    switch (command.action) {
      case 'create_object':
        return await createObject(command, userId);
      case 'create_field':
        return await createField(command, userId);
      case 'create_relationship':
        return await createRelationship(command, userId);
      case 'create_workflow':
        return await createWorkflow(command, userId);
      case 'create_pipeline':
        return await createPipeline(command, userId);
      case 'create_pipeline_stage':
        return await createPipelineStage(command);
      case 'create_dashboard':
        return await createDashboard(command, userId);
      case 'create_role':
        return await createRole(command);
      case 'create_permission':
        return await createPermission(command);
      case 'create_automation_rule':
        return await createAutomationRule(command, userId);
      case 'create_record':
        if (!authToken) return { success: false, error: 'Auth token required for record creation' };
        return await handleCreateRecord(command, authToken);
      default:
        return { success: false, error: `Unsupported action: ${(command as any).action}` };
    }
  } catch (err: any) {
    logger.error('CRM config execution error', { action: command.action, error: err.message });
    return { success: false, error: err.message };
  }
}

// ─── Implementations ─────────────────────────────────────────────────────────

async function createObject(
  cmd: Extract<ConfigCommand, { action: 'create_object' }>,
  userId: string,
): Promise<ExecutionResult> {
  const obj = await prisma.crmObject.create({
    data: {
      name: cmd.name,
      label: cmd.label || cmd.name,
      pluralLabel: cmd.plural_label || `${cmd.label || cmd.name}s`,
      description: cmd.description,
      icon: cmd.icon,
      createdBy: userId,
    },
  });

  // Create inline fields if provided
  const fields = (cmd as any).fields;
  if (Array.isArray(fields) && fields.length > 0) {
    await prisma.crmField.createMany({
      data: fields.map((f: any) => ({
        objectId: obj.id,
        name: f.name || f.field_name,
        label: f.label || f.name || f.field_name,
        fieldType: f.type || f.field_type || 'text',
        isRequired: f.required === true || f.is_required === true,
        isUnique: f.unique === true || f.is_unique === true,
        defaultValue: f.default_value ?? null,
        options: f.options ? JSON.parse(JSON.stringify(f.options)) : undefined,
        description: f.description ?? null,
        createdBy: userId,
      })),
    });
  }

  return { success: true, data: { id: obj.id, name: obj.name, label: obj.label, fieldsCreated: fields?.length ?? 0 } };
}

async function createField(
  cmd: Extract<ConfigCommand, { action: 'create_field' }>,
  userId: string,
): Promise<ExecutionResult> {
  const obj = await prisma.crmObject.findFirst({
    where: { name: { equals: cmd.object, mode: 'insensitive' } },
  });
  if (!obj) return { success: false, error: `Object "${cmd.object}" not found` };

  const field = await prisma.crmField.create({
    data: {
      objectId: obj.id,
      name: cmd.field_name,
      label: cmd.label || cmd.field_name,
      fieldType: cmd.field_type,
      isRequired: cmd.is_required ?? false,
      isUnique: cmd.is_unique ?? false,
      defaultValue: cmd.default_value,
      options: cmd.options ? cmd.options : undefined,
      description: cmd.description,
      createdBy: userId,
    },
  });
  return {
    success: true,
    data: { id: field.id, object: cmd.object, field_name: field.name, field_type: field.fieldType },
  };
}

async function createRelationship(
  cmd: Extract<ConfigCommand, { action: 'create_relationship' }>,
  userId: string,
): Promise<ExecutionResult> {
  const source = await prisma.crmObject.findFirst({
    where: { name: { equals: cmd.source_object, mode: 'insensitive' } },
  });
  const target = await prisma.crmObject.findFirst({
    where: { name: { equals: cmd.target_object, mode: 'insensitive' } },
  });
  if (!source) return { success: false, error: `Source object "${cmd.source_object}" not found` };
  if (!target) return { success: false, error: `Target object "${cmd.target_object}" not found` };

  const rel = await prisma.crmRelationship.create({
    data: {
      name: cmd.name,
      sourceObjectId: source.id,
      targetObjectId: target.id,
      relationshipType: cmd.relationship_type,
      description: cmd.description,
      createdBy: userId,
    },
  });
  return { success: true, data: { id: rel.id, name: rel.name, type: rel.relationshipType } };
}

async function createWorkflow(
  cmd: Extract<ConfigCommand, { action: 'create_workflow' }>,
  userId: string,
): Promise<ExecutionResult> {
  const wf = await prisma.workflow.create({
    data: {
      name: cmd.name,
      objectName: cmd.object,
      triggerType: cmd.trigger_type,
      triggerConfig: (cmd.trigger_config ?? {}) as Prisma.InputJsonValue,
      conditions: (cmd.conditions ?? []) as unknown as Prisma.InputJsonValue,
      actions: cmd.actions as unknown as Prisma.InputJsonValue,
      description: cmd.description,
      createdBy: userId,
    },
  });
  return { success: true, data: { id: wf.id, name: wf.name, trigger: wf.triggerType } };
}

async function createPipeline(
  cmd: Extract<ConfigCommand, { action: 'create_pipeline' }>,
  userId: string,
): Promise<ExecutionResult> {
  const pipeline = await prisma.pipeline.create({
    data: {
      name: cmd.name,
      objectName: cmd.object,
      description: cmd.description,
      createdBy: userId,
      stages: cmd.stages
        ? {
            create: cmd.stages.map((s, i) => ({
              name: s.name,
              probability: s.probability ?? 0,
              sortOrder: i,
              isClosed: s.is_closed ?? false,
              isWon: s.is_won ?? false,
              color: s.color,
            })),
          }
        : undefined,
    },
    include: { stages: true },
  });
  return {
    success: true,
    data: {
      id: pipeline.id,
      name: pipeline.name,
      stages: pipeline.stages.map((s) => s.name),
    },
  };
}

async function createPipelineStage(
  cmd: Extract<ConfigCommand, { action: 'create_pipeline_stage' }>,
): Promise<ExecutionResult> {
  const pipeline = await prisma.pipeline.findFirst({
    where: { name: { equals: cmd.pipeline, mode: 'insensitive' } },
    include: { stages: { orderBy: { sortOrder: 'desc' }, take: 1 } },
  });
  if (!pipeline) return { success: false, error: `Pipeline "${cmd.pipeline}" not found` };

  const maxOrder = pipeline.stages[0]?.sortOrder ?? -1;

  const stage = await prisma.pipelineStage.create({
    data: {
      pipelineId: pipeline.id,
      name: cmd.name,
      probability: cmd.probability ?? 0,
      sortOrder: cmd.sort_order ?? maxOrder + 1,
      isClosed: cmd.is_closed ?? false,
      isWon: cmd.is_won ?? false,
      color: cmd.color,
    },
  });
  return {
    success: true,
    data: { id: stage.id, pipeline: cmd.pipeline, stage: stage.name, sortOrder: stage.sortOrder },
  };
}

async function createDashboard(
  cmd: Extract<ConfigCommand, { action: 'create_dashboard' }>,
  userId: string,
): Promise<ExecutionResult> {
  const dashboard = await prisma.dashboard.create({
    data: {
      name: cmd.name,
      description: cmd.description,
      createdBy: userId,
      widgets: cmd.widgets
        ? {
            create: cmd.widgets.map((w) => ({
              title: w.title,
              widgetType: w.widget_type,
              config: w.config as any,
              position: w.position ?? { x: 0, y: 0, w: 6, h: 4 },
            })),
          }
        : undefined,
    },
    include: { widgets: true },
  });
  return {
    success: true,
    data: {
      id: dashboard.id,
      name: dashboard.name,
      widgetCount: dashboard.widgets.length,
    },
  };
}

async function createRole(
  cmd: Extract<ConfigCommand, { action: 'create_role' }>,
): Promise<ExecutionResult> {
  const role = await prisma.role.create({
    data: {
      name: cmd.name,
      description: cmd.description,
      permissions: cmd.permissions
        ? {
            create: cmd.permissions.map((p) => ({
              objectName: p.object,
              canCreate: p.can_create ?? false,
              canRead: p.can_read ?? true,
              canUpdate: p.can_update ?? false,
              canDelete: p.can_delete ?? false,
            })),
          }
        : undefined,
    },
    include: { permissions: true },
  });
  return { success: true, data: { id: role.id, name: role.name } };
}

async function createPermission(
  cmd: Extract<ConfigCommand, { action: 'create_permission' }>,
): Promise<ExecutionResult> {
  const role = await prisma.role.findFirst({
    where: { name: { equals: cmd.role, mode: 'insensitive' } },
  });
  if (!role) return { success: false, error: `Role "${cmd.role}" not found` };

  const perm = await prisma.permission.upsert({
    where: { roleId_objectName: { roleId: role.id, objectName: cmd.object } },
    update: {
      canCreate: cmd.can_create ?? false,
      canRead: cmd.can_read ?? true,
      canUpdate: cmd.can_update ?? false,
      canDelete: cmd.can_delete ?? false,
    },
    create: {
      roleId: role.id,
      objectName: cmd.object,
      canCreate: cmd.can_create ?? false,
      canRead: cmd.can_read ?? true,
      canUpdate: cmd.can_update ?? false,
      canDelete: cmd.can_delete ?? false,
    },
  });
  return { success: true, data: { id: perm.id, role: cmd.role, object: perm.objectName } };
}

async function createAutomationRule(
  cmd: Extract<ConfigCommand, { action: 'create_automation_rule' }>,
  userId: string,
): Promise<ExecutionResult> {
  const rule = await prisma.automationRule.create({
    data: {
      name: cmd.name,
      objectName: cmd.object,
      ruleType: cmd.rule_type,
      conditions: cmd.conditions as any,
      actions: cmd.actions as any,
      priority: cmd.priority ?? 0,
      description: cmd.description,
      createdBy: userId,
    },
  });
  return { success: true, data: { id: rule.id, name: rule.name, ruleType: rule.ruleType } };
}

// ─── Create CRM Record (calls backend microservices) ─────────────────────────

async function handleCreateRecord(
  cmd: Extract<ConfigCommand, { action: 'create_record' }>,
  authToken: string,
): Promise<ExecutionResult> {
  const result = await createCrmRecord(cmd.record_type, cmd.fields as Record<string, unknown>, authToken);
  return result;
}
