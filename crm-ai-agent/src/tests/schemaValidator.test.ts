import { describe, it, expect } from 'vitest';
import { ConfigCommand } from '../utils/schemaValidator';

describe('Schema Validator', () => {
  describe('ConfigCommand — create_field', () => {
    it('should accept a valid create_field command', () => {
      const input = {
        action: 'create_field',
        object: 'Lead',
        field_name: 'Budget',
        field_type: 'currency',
        label: 'Budget',
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.action).toBe('create_field');
      }
    });

    it('should reject missing required fields', () => {
      const input = { action: 'create_field', object: 'Lead' };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });

    it('should reject invalid field_type', () => {
      const input = {
        action: 'create_field',
        object: 'Lead',
        field_name: 'Budget',
        field_type: 'invalid_type',
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });

    it('should accept picklist with options', () => {
      const input = {
        action: 'create_field',
        object: 'Lead',
        field_name: 'Priority',
        field_type: 'picklist',
        options: ['Low', 'Medium', 'High'],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });
  });

  describe('ConfigCommand — create_object', () => {
    it('should accept a valid create_object', () => {
      const input = { action: 'create_object', name: 'Product', label: 'Product' };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });

    it('should reject empty name', () => {
      const input = { action: 'create_object', name: '', label: 'Product' };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });
  });

  describe('ConfigCommand — create_workflow', () => {
    it('should accept a valid workflow', () => {
      const input = {
        action: 'create_workflow',
        name: 'Email on Qualification',
        object: 'Lead',
        trigger_type: 'field_change',
        actions: [{ type: 'send_email', config: { template: 'lead_qualified' } }],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });

    it('should reject workflow without actions', () => {
      const input = {
        action: 'create_workflow',
        name: 'Bad Workflow',
        object: 'Lead',
        trigger_type: 'on_create',
        actions: [],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });

    it('should reject invalid trigger_type', () => {
      const input = {
        action: 'create_workflow',
        name: 'Bad Trigger',
        object: 'Lead',
        trigger_type: 'on_delete',
        actions: [{ type: 'send_email', config: {} }],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });
  });

  describe('ConfigCommand — create_pipeline_stage', () => {
    it('should accept a valid pipeline stage', () => {
      const input = {
        action: 'create_pipeline_stage',
        pipeline: 'Sales Pipeline',
        name: 'Technical Review',
        probability: 40,
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });

    it('should reject probability > 100', () => {
      const input = {
        action: 'create_pipeline_stage',
        pipeline: 'Sales Pipeline',
        name: 'Bad Stage',
        probability: 110,
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });
  });

  describe('ConfigCommand — create_dashboard', () => {
    it('should accept dashboard with widgets', () => {
      const input = {
        action: 'create_dashboard',
        name: 'Revenue Dashboard',
        widgets: [
          {
            title: 'Monthly Revenue',
            widget_type: 'chart',
            config: { chart_type: 'bar', data_source: 'Opportunity' },
            position: { x: 0, y: 0, w: 12, h: 4 },
          },
        ],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });
  });

  describe('ConfigCommand — create_role', () => {
    it('should accept role with permissions', () => {
      const input = {
        action: 'create_role',
        name: 'Marketing Manager',
        permissions: [
          { object: 'Campaign', can_create: true, can_read: true, can_update: true, can_delete: false },
        ],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });
  });

  describe('ConfigCommand — create_automation_rule', () => {
    it('should accept a valid automation rule', () => {
      const input = {
        action: 'create_automation_rule',
        name: 'Auto-assign high-value leads',
        object: 'Lead',
        rule_type: 'assignment',
        conditions: [{ field: 'estimatedValue', operator: 'greater_than', value: 50000 }],
        actions: [{ type: 'assign_to', config: { user: 'senior-rep' } }],
      };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(true);
    });
  });

  describe('ConfigCommand — unknown action', () => {
    it('should reject unknown actions', () => {
      const input = { action: 'delete_object', name: 'Lead' };
      const result = ConfigCommand.safeParse(input);
      expect(result.success).toBe(false);
    });
  });
});
