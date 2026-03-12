import { describe, it, expect } from 'vitest';
import { classifyIntent, isSupportedAction } from '../agents/intentClassifier';

describe('IntentClassifier', () => {
  describe('classifyIntent', () => {
    it('should normalise standard actions', () => {
      const raw = { action: 'create_field', object: 'Lead', field_name: 'Budget', field_type: 'currency' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('create_field');
    });

    it('should normalise alias "add_field" → "create_field"', () => {
      const raw = { action: 'add_field', object: 'Lead', field_name: 'Budget', field_type: 'currency' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('create_field');
    });

    it('should normalise alias "add_stage" → "create_pipeline_stage"', () => {
      const raw = { action: 'add_stage', pipeline: 'Sales Pipeline', name: 'Review' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('create_pipeline_stage');
    });

    it('should normalise alias "new_dashboard" → "create_dashboard"', () => {
      const raw = { action: 'new_dashboard', name: 'Revenue Dashboard' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('create_dashboard');
    });

    it('should normalise alias "add_rule" → "create_automation_rule"', () => {
      const raw = { action: 'add_rule', name: 'Test', object: 'Lead' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('create_automation_rule');
    });

    it('should pass through unknown actions unchanged', () => {
      const raw = { action: 'delete_object', name: 'Test' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('delete_object');
    });

    it('should handle case insensitivity', () => {
      const raw = { action: 'CREATE_FIELD', object: 'Lead', field_name: 'Test', field_type: 'text' };
      const result = classifyIntent(raw);
      expect(result.action).toBe('create_field');
    });
  });

  describe('isSupportedAction', () => {
    it('should return true for supported actions', () => {
      const actions = [
        'create_object', 'create_field', 'create_relationship', 'create_workflow',
        'create_pipeline', 'create_pipeline_stage', 'create_dashboard',
        'create_role', 'create_permission', 'create_automation_rule',
      ];
      for (const action of actions) {
        expect(isSupportedAction(action)).toBe(true);
      }
    });

    it('should return false for unsupported actions', () => {
      expect(isSupportedAction('delete_object')).toBe(false);
      expect(isSupportedAction('update_field')).toBe(false);
      expect(isSupportedAction('')).toBe(false);
    });
  });
});
