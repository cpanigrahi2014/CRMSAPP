import dotenv from 'dotenv';
dotenv.config();

export const config = {
  env: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '9100', 10),

  database: {
    url: process.env.DATABASE_URL || 'postgresql://postgres:postgres@127.0.0.1:5434/crm_ai_agent',
  },

  openai: {
    apiKey: process.env.OPENAI_API_KEY || '',
    model: process.env.OPENAI_MODEL || 'gpt-4o',
  },

  jwt: {
    secret: process.env.JWT_SECRET || '',
  },

  crm: {
    authUrl: process.env.CRM_AUTH_URL || 'http://localhost:9081',
    leadUrl: process.env.CRM_LEAD_URL || 'http://localhost:9082',
    accountUrl: process.env.CRM_ACCOUNT_URL || 'http://localhost:9083',
    contactUrl: process.env.CRM_CONTACT_URL || 'http://localhost:9084',
    opportunityUrl: process.env.CRM_OPPORTUNITY_URL || 'http://localhost:9085',
    activityUrl: process.env.CRM_ACTIVITY_URL || 'http://localhost:9086',
    notificationUrl: process.env.CRM_NOTIFICATION_URL || 'http://localhost:9087',
    workflowUrl: process.env.CRM_WORKFLOW_URL || 'http://localhost:9088',
  },

  ai: {
    confirmationMode: process.env.AI_CONFIRMATION_MODE === 'true',
  },

  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000', 10),
    max: parseInt(process.env.RATE_LIMIT_MAX || '30', 10),
      premiumMax: parseInt(process.env.PREMIUM_RATE_LIMIT_MAX || '100', 10), // Premium users get higher limit
  },

  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
} as const;
