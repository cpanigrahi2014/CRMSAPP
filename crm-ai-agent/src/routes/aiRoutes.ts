import { Router } from 'express';
import { authMiddleware, checkAdminRole } from '../middleware/authMiddleware';
import {
  configure,
  confirm,
  reject,
  auditLogs,
  sessions,
  sessionMessages,
  metadata,
  metadataWorkflows,
  metadataDashboards,
} from '../controllers/aiController';

const router = Router();

// All routes require authentication + admin role
router.use(authMiddleware);
router.use(checkAdminRole);

// Core AI configuration
router.post('/configure', configure);
router.post('/confirm', confirm);
router.post('/reject', reject);

// Audit & history
router.get('/audit-logs', auditLogs);
router.get('/sessions', sessions);
router.get('/sessions/:id/messages', sessionMessages);

// CRM metadata introspection
router.get('/metadata', metadata);
router.get('/metadata/workflows', metadataWorkflows);
router.get('/metadata/dashboards', metadataDashboards);

export default router;
