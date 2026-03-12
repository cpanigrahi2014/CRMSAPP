import { describe, it, expect, vi, beforeEach } from 'vitest';
import { buildExecutionPlan } from '../agents/instructionPlanner';
import type { ConfigCommand } from '../utils/schemaValidator';

// Mock the validation service
vi.mock('../services/validationService', () => ({
  validateCommand: vi.fn().mockResolvedValue({ valid: true, errors: [], warnings: [] }),
}));

describe('InstructionPlanner', () => {
  describe('buildExecutionPlan', () => {
    it('should build a plan for a create_field command', async () => {
      const command: ConfigCommand = {
        action: 'create_field',
        object: 'Lead',
        field_name: 'Budget',
        field_type: 'currency',
      };

      const plan = await buildExecutionPlan(command, false);

      expect(plan.command).toEqual(command);
      expect(plan.validation.valid).toBe(true);
      expect(plan.description).toContain('Budget');
      expect(plan.description).toContain('Lead');
      expect(plan.requiresConfirmation).toBe(false);
    });

    it('should require confirmation when confirmationMode is true', async () => {
      const command: ConfigCommand = {
        action: 'create_object',
        name: 'Product',
        label: 'Product',
      };

      const plan = await buildExecutionPlan(command, true);
      expect(plan.requiresConfirmation).toBe(true);
    });

    it('should produce descriptions for all action types', async () => {
      const commands: ConfigCommand[] = [
        { action: 'create_object', name: 'Test', label: 'Test' },
        { action: 'create_field', object: 'Lead', field_name: 'X', field_type: 'text' },
        { action: 'create_relationship', name: 'R', source_object: 'A', target_object: 'B', relationship_type: 'one_to_many' },
        { action: 'create_workflow', name: 'W', object: 'Lead', trigger_type: 'on_create', actions: [{ type: 'send_email', config: {} }] },
        { action: 'create_pipeline', name: 'P', object: 'Opportunity' },
        { action: 'create_pipeline_stage', pipeline: 'P', name: 'S' },
        { action: 'create_dashboard', name: 'D' },
        { action: 'create_role', name: 'R' },
        { action: 'create_permission', role: 'Admin', object: 'Lead' },
        { action: 'create_automation_rule', name: 'AR', object: 'Lead', rule_type: 'validation', conditions: [{}], actions: [{}] },
      ];

      for (const cmd of commands) {
        const plan = await buildExecutionPlan(cmd, false);
        expect(plan.description.length).toBeGreaterThan(0);
      }
    });
  });
});
