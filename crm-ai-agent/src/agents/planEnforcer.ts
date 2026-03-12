/* ============================================================
   Plan Enforcer – checks tenant plan limits before create actions
   ============================================================ */
import { config } from '../config';
import { logger } from '../utils/logger';
import prisma from '../models/prismaClient';
import type { ConfigCommand } from '../utils/schemaValidator';

interface PlanLimits {
  planName: string;
  maxCustomObjects: number;
  maxWorkflows: number;
  maxDashboards: number;
  maxPipelines: number;
  maxRoles: number;
  maxRecordsPerObject: number;
  aiConfigEnabled: boolean;
  aiInsightsEnabled: boolean;
}

/**
 * Fetches the tenant's plan limits from the auth service.
 * Returns null if the service is unavailable (fails open — allows action).
 */
async function fetchPlanLimits(authToken?: string): Promise<PlanLimits | null> {
  try {
    const url = `${config.crm.authUrl}/api/v1/auth/plan`;
    const res = await fetch(url, {
      headers: authToken ? { Authorization: `Bearer ${authToken}` } : {},
    });
    if (!res.ok) return null;
    const body = await res.json();
    return body.data ?? null;
  } catch (err) {
    logger.warn('Could not fetch plan limits — skipping enforcement', { error: String(err) });
    return null;
  }
}

/** Count resources helper */
async function countResources(tenantId: string) {
  const [objects, workflows, dashboards, pipelines, roles] = await Promise.all([
    prisma.crmObject.count({ where: { tenantId } }),
    prisma.workflow.count({ where: { tenantId } }),
    prisma.dashboard.count({ where: { tenantId } }),
    prisma.pipeline.count({ where: { tenantId } }),
    prisma.role.count({ where: { tenantId } }),
  ]);
  return { objects, workflows, dashboards, pipelines, roles };
}

type LimitCheckResult = { allowed: true } | { allowed: false; message: string };

/**
 * Checks whether the given command is allowed under the tenant's plan.
 * Returns `{ allowed: true }` if OK, or `{ allowed: false, message }` if blocked.
 */
export async function checkPlanLimits(
  command: ConfigCommand,
  tenantId: string,
  authToken?: string,
): Promise<LimitCheckResult> {
  const plan = await fetchPlanLimits(authToken);
  if (!plan) return { allowed: true }; // fail open if service unavailable

  const counts = await countResources(tenantId);

  switch (command.action) {
    case 'create_object':
      if (counts.objects >= plan.maxCustomObjects) {
        return {
          allowed: false,
          message: `Your ${plan.planName} plan allows up to ${plan.maxCustomObjects} custom objects (currently ${counts.objects}). Please upgrade your plan to create more.`,
        };
      }
      break;
    case 'create_workflow':
    case 'create_automation_rule':
      if (counts.workflows >= plan.maxWorkflows) {
        return {
          allowed: false,
          message: `Your ${plan.planName} plan allows up to ${plan.maxWorkflows} workflows (currently ${counts.workflows}). Please upgrade your plan.`,
        };
      }
      break;
    case 'create_dashboard':
      if (counts.dashboards >= plan.maxDashboards) {
        return {
          allowed: false,
          message: `Your ${plan.planName} plan allows up to ${plan.maxDashboards} dashboards (currently ${counts.dashboards}). Please upgrade your plan.`,
        };
      }
      break;
    case 'create_pipeline':
      if (counts.pipelines >= plan.maxPipelines) {
        return {
          allowed: false,
          message: `Your ${plan.planName} plan allows up to ${plan.maxPipelines} pipelines (currently ${counts.pipelines}). Please upgrade your plan.`,
        };
      }
      break;
    case 'create_role':
      if (counts.roles >= plan.maxRoles) {
        return {
          allowed: false,
          message: `Your ${plan.planName} plan allows up to ${plan.maxRoles} roles (currently ${counts.roles}). Please upgrade your plan.`,
        };
      }
      break;
  }

  return { allowed: true };
}
