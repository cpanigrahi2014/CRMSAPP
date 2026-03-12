import { z } from 'zod';

// ─── Action Enum ─────────────────────────────────────────────────────────────

export const ConfigAction = z.enum([
  'create_object',
  'create_field',
  'create_relationship',
  'create_workflow',
  'create_pipeline',
  'create_pipeline_stage',
  'create_dashboard',
  'create_role',
  'create_permission',
  'create_automation_rule',
  'create_record',
]);
export type ConfigAction = z.infer<typeof ConfigAction>;

// ─── Individual action payloads ──────────────────────────────────────────────

export const ObjectFieldDef = z.object({
  name: z.string().optional(),
  field_name: z.string().optional(),
  type: z.string().optional(),
  field_type: z.string().optional(),
  label: z.string().optional(),
  options: z.array(z.string()).optional(),
}).passthrough();

export const CreateObjectPayload = z.object({
  action: z.literal('create_object'),
  name: z.string().min(1),
  label: z.string().optional(),
  plural_label: z.string().optional(),
  description: z.string().optional(),
  icon: z.string().optional(),
  fields: z.array(ObjectFieldDef).optional(),
});

export const FieldType = z.enum([
  'text', 'textarea', 'number', 'currency', 'percent',
  'date', 'datetime', 'boolean', 'email', 'phone', 'url',
  'picklist', 'multi_picklist', 'lookup', 'formula', 'auto_number',
]);

export const CreateFieldPayload = z.object({
  action: z.literal('create_field'),
  object: z.string().min(1),
  field_name: z.string().min(1),
  field_type: FieldType,
  label: z.string().optional(),
  is_required: z.boolean().optional(),
  is_unique: z.boolean().optional(),
  default_value: z.string().optional(),
  options: z.array(z.string()).optional(), // for picklist
  description: z.string().optional(),
});

export const RelationshipType = z.enum(['one_to_one', 'one_to_many', 'many_to_many']);

export const CreateRelationshipPayload = z.object({
  action: z.literal('create_relationship'),
  name: z.string().min(1),
  source_object: z.string().min(1),
  target_object: z.string().min(1),
  relationship_type: RelationshipType,
  description: z.string().optional(),
});

export const TriggerType = z.enum(['on_create', 'on_update', 'field_change', 'scheduled']);

export const WorkflowActionSchema = z.object({
  type: z.enum(['send_email', 'update_field', 'create_task', 'notify', 'webhook']),
  config: z.record(z.unknown()),
});

export const CreateWorkflowPayload = z.object({
  action: z.literal('create_workflow'),
  name: z.string().min(1),
  object: z.string().min(1),
  trigger_type: TriggerType,
  trigger_config: z.record(z.unknown()).optional(),
  conditions: z.array(z.record(z.unknown())).optional(),
  actions: z.array(WorkflowActionSchema).min(1),
  description: z.string().optional(),
});

export const CreatePipelinePayload = z.object({
  action: z.literal('create_pipeline'),
  name: z.string().min(1),
  object: z.string().min(1),
  description: z.string().optional(),
  stages: z.array(z.object({
    name: z.string().min(1),
    probability: z.number().min(0).max(100).optional(),
    is_closed: z.boolean().optional(),
    is_won: z.boolean().optional(),
    color: z.string().optional(),
  })).optional(),
});

export const CreatePipelineStagePayload = z.object({
  action: z.literal('create_pipeline_stage'),
  pipeline: z.string().min(1),
  name: z.string().min(1),
  probability: z.number().min(0).max(100).optional(),
  sort_order: z.number().optional(),
  is_closed: z.boolean().optional(),
  is_won: z.boolean().optional(),
  color: z.string().optional(),
});

export const WidgetType = z.enum(['chart', 'metric', 'table', 'list']);

export const CreateDashboardPayload = z.object({
  action: z.literal('create_dashboard'),
  name: z.string().min(1),
  description: z.string().optional(),
  widgets: z.array(z.object({
    title: z.string().min(1),
    widget_type: WidgetType,
    config: z.record(z.unknown()),
    position: z.object({ x: z.number(), y: z.number(), w: z.number(), h: z.number() }).optional(),
  })).optional(),
});

export const CreateRolePayload = z.object({
  action: z.literal('create_role'),
  name: z.string().min(1),
  description: z.string().optional(),
  permissions: z.array(z.object({
    object: z.string().min(1),
    can_create: z.boolean().optional(),
    can_read: z.boolean().optional(),
    can_update: z.boolean().optional(),
    can_delete: z.boolean().optional(),
  })).optional(),
});

export const CreatePermissionPayload = z.object({
  action: z.literal('create_permission'),
  role: z.string().min(1),
  object: z.string().min(1),
  can_create: z.boolean().optional(),
  can_read: z.boolean().optional(),
  can_update: z.boolean().optional(),
  can_delete: z.boolean().optional(),
});

export const CreateAutomationRulePayload = z.object({
  action: z.literal('create_automation_rule'),
  name: z.string().min(1),
  object: z.string().min(1),
  rule_type: z.enum(['validation', 'assignment', 'escalation', 'auto_response']),
  conditions: z.array(z.record(z.unknown())).min(1),
  actions: z.array(z.record(z.unknown())).min(1),
  priority: z.number().optional(),
  description: z.string().optional(),
});

// ─── Create CRM Record (lead, contact, account, opportunity) ─────────────────

export const CrmRecordType = z.enum(['lead', 'account', 'contact', 'opportunity', 'activity']);

export const CreateRecordPayload = z.object({
  action: z.literal('create_record'),
  record_type: CrmRecordType,
  fields: z.record(z.unknown()),
});

// ─── Discriminated union ─────────────────────────────────────────────────────

export const ConfigCommand = z.discriminatedUnion('action', [
  CreateObjectPayload,
  CreateFieldPayload,
  CreateRelationshipPayload,
  CreateWorkflowPayload,
  CreatePipelinePayload,
  CreatePipelineStagePayload,
  CreateDashboardPayload,
  CreateRolePayload,
  CreatePermissionPayload,
  CreateAutomationRulePayload,
  CreateRecordPayload,
]);
export type ConfigCommand = z.infer<typeof ConfigCommand>;

// ─── API response types ──────────────────────────────────────────────────────

export interface ConfigureRequest {
  instruction: string;
  sessionId?: string;
  confirm?: boolean; // true → execute a pending command
}

export interface ConfigureResponse {
  action: string;
  status: 'pending_confirmation' | 'created' | 'failed';
  details: Record<string, unknown>;
  message: string;
  auditLogId: string;
}

export interface ConversationHistoryItem {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

// ─── JWT payload ─────────────────────────────────────────────────────────────

export interface JwtPayload {
  sub: string;       // userId
  email: string;
  tenantId: string;
  roles: string[];
  iat?: number;
  exp?: number;
}
