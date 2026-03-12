import api from './api';
import {
  ApiResponse,
  PagedData,
  WorkflowRule,
  CreateWorkflowRuleRequest,
  UpdateWorkflowRuleRequest,
  WorkflowExecutionLog,
} from '../types';

const BASE = '/api/v1/workflows';

export const workflowService = {
  // ── CRUD ────────────────────────────────────────────────
  getAll: (page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc') =>
    api.get<ApiResponse<PagedData<WorkflowRule>>>(BASE, {
      params: { page, size, sortBy, sortDir },
    }).then((r) => r.data),

  getById: (id: string) =>
    api.get<ApiResponse<WorkflowRule>>(`${BASE}/${id}`).then((r) => r.data),

  getByEntityType: (entityType: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<WorkflowRule>>>(`${BASE}/entity/${entityType}`, {
      params: { page, size },
    }).then((r) => r.data),

  create: (data: CreateWorkflowRuleRequest) =>
    api.post<ApiResponse<WorkflowRule>>(BASE, data).then((r) => r.data),

  update: (id: string, data: UpdateWorkflowRuleRequest) =>
    api.put<ApiResponse<WorkflowRule>>(`${BASE}/${id}`, data).then((r) => r.data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`${BASE}/${id}`).then((r) => r.data),

  // ── Enable / Disable ───────────────────────────────────
  enable: (id: string) =>
    api.patch<ApiResponse<WorkflowRule>>(`${BASE}/${id}/enable`).then((r) => r.data),

  disable: (id: string) =>
    api.patch<ApiResponse<WorkflowRule>>(`${BASE}/${id}/disable`).then((r) => r.data),

  // ── Execution Logs ─────────────────────────────────────
  getExecutionLogs: (page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<WorkflowExecutionLog>>>(`${BASE}/executions`, {
      params: { page, size },
    }).then((r) => r.data),

  getExecutionLogsByRule: (ruleId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedData<WorkflowExecutionLog>>>(`${BASE}/${ruleId}/executions`, {
      params: { page, size },
    }).then((r) => r.data),
};
