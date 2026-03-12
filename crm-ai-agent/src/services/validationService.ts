import { ConfigCommand } from '../utils/schemaValidator';
import { logger } from '../utils/logger';
import {
  getObjectByName,
  getFieldByObjectAndName,
  getPipelineByName,
  getRoleByName,
} from '../models/crmMetadata';

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

/**
 * Validate a parsed config command against live CRM metadata before execution.
 */
export async function validateCommand(command: ConfigCommand): Promise<ValidationResult> {
  const errors: string[] = [];
  const warnings: string[] = [];

  switch (command.action) {
    // ── create_object ──────────────────────────────────────────────────────
    case 'create_object': {
      const existing = await getObjectByName(command.name);
      if (existing) errors.push(`Object "${command.name}" already exists.`);
      break;
    }

    // ── create_field ───────────────────────────────────────────────────────
    case 'create_field': {
      const obj = await getObjectByName(command.object);
      if (!obj) {
        errors.push(`Object "${command.object}" not found.`);
        break;
      }
      const existingField = await getFieldByObjectAndName(command.object, command.field_name);
      if (existingField) {
        errors.push(`Field "${command.field_name}" already exists on "${command.object}".`);
      }
      if (command.field_type === 'picklist' && (!command.options || command.options.length === 0)) {
        warnings.push('Picklist field created without options. You can add options later.');
      }
      break;
    }

    // ── create_relationship ────────────────────────────────────────────────
    case 'create_relationship': {
      const src = await getObjectByName(command.source_object);
      const tgt = await getObjectByName(command.target_object);
      if (!src) errors.push(`Source object "${command.source_object}" not found.`);
      if (!tgt) errors.push(`Target object "${command.target_object}" not found.`);
      break;
    }

    // ── create_workflow ────────────────────────────────────────────────────
    case 'create_workflow': {
      const obj = await getObjectByName(command.object);
      if (!obj) errors.push(`Object "${command.object}" not found.`);
      if (command.actions.length === 0) errors.push('Workflow must have at least one action.');
      const validTriggers = ['on_create', 'on_update', 'field_change', 'scheduled'];
      if (!validTriggers.includes(command.trigger_type)) {
        errors.push(`Invalid trigger type "${command.trigger_type}".`);
      }
      break;
    }

    // ── create_pipeline ────────────────────────────────────────────────────
    case 'create_pipeline': {
      const existing = await getPipelineByName(command.name);
      if (existing) errors.push(`Pipeline "${command.name}" already exists.`);
      const obj = await getObjectByName(command.object);
      if (!obj) warnings.push(`Object "${command.object}" not found — pipeline will be created anyway.`);
      break;
    }

    // ── create_pipeline_stage ──────────────────────────────────────────────
    case 'create_pipeline_stage': {
      const pipeline = await getPipelineByName(command.pipeline);
      if (!pipeline) {
        errors.push(`Pipeline "${command.pipeline}" not found.`);
        break;
      }
      const stageExists = pipeline.stages.some(
        (s) => s.name.toLowerCase() === command.name.toLowerCase(),
      );
      if (stageExists) {
        errors.push(`Stage "${command.name}" already exists in pipeline "${command.pipeline}".`);
      }
      break;
    }

    // ── create_dashboard ───────────────────────────────────────────────────
    case 'create_dashboard': {
      if (!command.name || command.name.trim().length === 0) {
        errors.push('Dashboard must have a name.');
      }
      break;
    }

    // ── create_role ────────────────────────────────────────────────────────
    case 'create_role': {
      const existing = await getRoleByName(command.name);
      if (existing) errors.push(`Role "${command.name}" already exists.`);
      break;
    }

    // ── create_permission ──────────────────────────────────────────────────
    case 'create_permission': {
      const role = await getRoleByName(command.role);
      if (!role) errors.push(`Role "${command.role}" not found.`);
      const obj = await getObjectByName(command.object);
      if (!obj) errors.push(`Object "${command.object}" not found.`);
      break;
    }

    // ── create_automation_rule ─────────────────────────────────────────────
    case 'create_automation_rule': {
      const obj = await getObjectByName(command.object);
      if (!obj) errors.push(`Object "${command.object}" not found.`);
      break;
    }

    // ── create_record ────────────────────────────────────────────────────
    case 'create_record': {
      const rt = command.record_type;
      const f = command.fields as Record<string, unknown>;
      if (rt === 'lead') {
        if (!f.firstName) errors.push('Lead requires "firstName".');
        if (!f.lastName) errors.push('Lead requires "lastName".');
      } else if (rt === 'account') {
        if (!f.name) errors.push('Account requires "name".');
      } else if (rt === 'contact') {
        if (!f.firstName) errors.push('Contact requires "firstName".');
        if (!f.lastName) errors.push('Contact requires "lastName".');
      } else if (rt === 'opportunity') {
        if (!f.name) errors.push('Opportunity requires "name".');
      }
      break;
    }

    default:
      errors.push(`Unknown action: ${(command as Record<string, unknown>).action}`);
  }

  logger.debug('Validation result', { action: command.action, errors, warnings });
  return { valid: errors.length === 0, errors, warnings };
}
