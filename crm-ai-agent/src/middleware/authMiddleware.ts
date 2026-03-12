import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { logger } from '../utils/logger';
import type { JwtPayload } from '../utils/schemaValidator';

// Extend Express Request
declare global {
  namespace Express {
    interface Request {
      user?: JwtPayload;
    }
  }
}

/**
 * Verify JWT and attach decoded payload to `req.user`.
 */
export function authMiddleware(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;

  if (!authHeader?.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Missing or invalid Authorization header' });
    return;
  }

  const token = authHeader.slice(7);

  try {
    // Java auth-service base64-decodes the secret before signing; replicate here
    const secretBuffer = Buffer.from(config.jwt.secret, 'base64');
    const decoded = jwt.verify(token, secretBuffer, { algorithms: ['HS512'] }) as JwtPayload;
    req.user = decoded;
    next();
  } catch (err: any) {
    logger.warn('JWT verification failed', { error: err.message });
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}

/**
 * Require the authenticated user to have the "admin" (or "ROLE_ADMIN") role.
 */
export function checkAdminRole(req: Request, res: Response, next: NextFunction): void {
  if (!req.user) {
    res.status(401).json({ error: 'Not authenticated' });
    return;
  }

  const roles = (req.user.roles ?? []).map((r) => r.toLowerCase());
  const isAdmin = roles.some((r) => r === 'admin' || r === 'role_admin');

  if (!isAdmin) {
    logger.warn('Admin role check failed', { userId: req.user.sub, roles });
    res.status(403).json({ error: 'Forbidden — admin role required' });
    return;
  }

  next();
}
