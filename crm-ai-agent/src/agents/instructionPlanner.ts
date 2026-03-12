import { ConfigCommand, ConfigAction } from '../utils/schemaValidator';
import { validateCommand, ValidationResult } from '../services/validationService';
import { logger } from '../utils/logger';

export interface ExecutionPlan {
  command: ConfigCommand;
  validation: ValidationResult;
  description: string;
  requiresConfirmation: boolean;
}

/**
 * Build an execution plan for a parsed config command.
 *
 * 1. Validate the command against live metadata.
 * 2. Generate a human-readable description.
 * 3. Decide whether user confirmation is needed.
 */
export async function buildExecutionPlan(
  command: ConfigCommand,
  confirmationMode: boolean,
): Promise<ExecutionPlan> {
  const validation = await validateCommand(command);
  const description = describeCommand(command);

  // Always require confirmation when there are warnings, or when mode is on
  const requiresConfirmation =
    confirmationMode || validation.warnings.length > 0;

  logger.info('Execution plan built', {
    action: command.action,
    valid: validation.valid,
    requiresConfirmation,
  });

  return {
    command,
    validation,
    description,
    requiresConfirmation,
  };
}

// ─── Human-Readable Descriptions ─────────────────────────────────────────────

function describeCommand(cmd: ConfigCommand): string {
  switch (cmd.action) {
    case 'create_object':
      return `Create a new CRM object "${cmd.name}" (${cmd.label}).`;

    case 'create_field':
      return `Add a ${cmd.field_type} field "${cmd.field_name}" to ${cmd.object}.`;

    case 'create_relationship':
      return `Create a ${cmd.relationship_type} relationship "${cmd.name}" from ${cmd.source_object} to ${cmd.target_object}.`;

    case 'create_workflow':
      return `Create workflow "${cmd.name}" on ${cmd.object} triggered by ${cmd.trigger_type} with ${cmd.actions.length} action(s).`;

    case 'create_pipeline':
      return `Create pipeline "${cmd.name}" for ${cmd.object}${cmd.stages ? ` with ${cmd.stages.length} stage(s)` : ''}.`;

    case 'create_pipeline_stage':
      return `Add stage "${cmd.name}" to pipeline "${cmd.pipeline}"${cmd.probability != null ? ` (${cmd.probability}% probability)` : ''}.`;

    case 'create_dashboard':
      return `Create dashboard "${cmd.name}"${cmd.widgets ? ` with ${cmd.widgets.length} widget(s)` : ''}.`;

    case 'create_role':
      return `Create role "${cmd.name}".`;

    case 'create_permission':
      return `Set permissions on "${cmd.object}" for role "${cmd.role}".`;

    case 'create_automation_rule':
      return `Create ${cmd.rule_type} automation rule "${cmd.name}" on ${cmd.object}.`;

    case 'create_record':
      return `Create a new ${cmd.record_type} record.`;

    default:
      return `Execute action: ${(cmd as any).action}`;
  }
}
