import { ConfigCommand, ConfigAction } from '../utils/schemaValidator';
import { logger } from '../utils/logger';
import type { z } from 'zod';

type ConfigActionValue = z.infer<typeof ConfigAction>;

/**
 * Quick local classifier that maps raw AI JSON to a known ConfigAction.
 * Falls back gracefully if the AI output is slightly off.
 */

const ACTION_ALIASES: Record<string, ConfigActionValue> = {
  create_object: 'create_object',
  add_object: 'create_object',
  new_object: 'create_object',
  create_field: 'create_field',
  add_field: 'create_field',
  new_field: 'create_field',
  create_relationship: 'create_relationship',
  add_relationship: 'create_relationship',
  create_workflow: 'create_workflow',
  add_workflow: 'create_workflow',
  new_workflow: 'create_workflow',
  create_pipeline: 'create_pipeline',
  add_pipeline: 'create_pipeline',
  new_pipeline: 'create_pipeline',
  create_pipeline_stage: 'create_pipeline_stage',
  add_pipeline_stage: 'create_pipeline_stage',
  add_stage: 'create_pipeline_stage',
  create_stage: 'create_pipeline_stage',
  create_dashboard: 'create_dashboard',
  add_dashboard: 'create_dashboard',
  new_dashboard: 'create_dashboard',
  create_role: 'create_role',
  add_role: 'create_role',
  new_role: 'create_role',
  create_permission: 'create_permission',
  add_permission: 'create_permission',
  set_permission: 'create_permission',
  create_automation_rule: 'create_automation_rule',
  add_automation_rule: 'create_automation_rule',
  create_rule: 'create_automation_rule',
  add_rule: 'create_automation_rule',
  // Record CRUD
  create_record: 'create_record',
  create_lead: 'create_record',
  add_lead: 'create_record',
  new_lead: 'create_record',
  create_contact: 'create_record',
  add_contact: 'create_record',
  new_contact: 'create_record',
  create_account: 'create_record',
  add_account: 'create_record',
  new_account: 'create_record',
  create_opportunity: 'create_record',
  add_opportunity: 'create_record',
  new_opportunity: 'create_record',
  create_activity: 'create_record',
  add_activity: 'create_record',
};

/**
 * Normalize the `action` field returned by the AI so it matches our discriminated union.
 */
export function classifyIntent(raw: Record<string, unknown>): Record<string, unknown> {
  const rawAction = String(raw.action ?? '').toLowerCase().trim();
  const normalised = ACTION_ALIASES[rawAction];

  if (!normalised) {
    logger.warn('Intent classifier: unknown action', { rawAction });
    return raw; // let zod validation catch it downstream
  }

  // If AI returned create_lead / create_contact etc. convert to create_record format
  if (normalised === 'create_record' && rawAction !== 'create_record') {
    const typeMatch = rawAction.match(/^(?:create|add|new)_(\w+)$/);
    if (typeMatch) {
      const recordType = typeMatch[1]; // lead, contact, account, opportunity, activity
      // Merge top-level fields (except action) into a `fields` object if not already present
      const fields = (raw.fields as Record<string, unknown>) ?? {};
      const { action, record_type, ...rest } = raw;
      return {
        action: 'create_record',
        record_type: (record_type as string) ?? recordType,
        fields: { ...rest, ...fields },
      };
    }
  }

  // Normalize create_object: OpenAI may return object_name instead of name
  if (normalised === 'create_object') {
    const result: Record<string, unknown> = { ...raw, action: normalised };
    if (!result.name && result.object_name) {
      result.name = result.object_name;
      delete result.object_name;
    }
    if (!result.label && result.name) {
      result.label = String(result.name);
    }
    return result;
  }

  // Normalize create_pipeline: OpenAI may return pipeline_name instead of name
  if (normalised === 'create_pipeline') {
    const result: Record<string, unknown> = { ...raw, action: normalised };
    if (!result.name && result.pipeline_name) {
      result.name = result.pipeline_name;
      delete result.pipeline_name;
    }
    if (!result.object) {
      result.object = 'Opportunity';
    }
    return result;
  }

  // Normalize create_workflow: ensure required fields
  if (normalised === 'create_workflow') {
    const result: Record<string, unknown> = { ...raw, action: normalised };
    if (!result.name && result.workflow_name) {
      result.name = result.workflow_name;
      delete result.workflow_name;
    }
    if (!result.object) {
      result.object = 'Lead';
    }
    return result;
  }

  return { ...raw, action: normalised };
}

/**
 * Validate that a classified command's action is in the supported list.
 */
export function isSupportedAction(action: string): boolean {
  return ConfigAction.safeParse(action).success;
}
