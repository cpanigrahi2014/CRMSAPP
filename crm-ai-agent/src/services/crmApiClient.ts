import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { config } from '../config';
import { logger } from '../utils/logger';

// ─── CRM Backend API Client ─────────────────────────────────────────────────
// Makes authenticated HTTP calls to CRM microservices to create actual records.

const SERVICE_URLS: Record<string, string> = {
  lead: config.crm.leadUrl,
  account: config.crm.accountUrl,
  contact: config.crm.contactUrl,
  opportunity: config.crm.opportunityUrl,
  activity: config.crm.activityUrl,
};

const API_PATHS: Record<string, string> = {
  lead: '/api/v1/leads',
  account: '/api/v1/accounts',
  contact: '/api/v1/contacts',
  opportunity: '/api/v1/opportunities',
  activity: '/api/v1/activities',
};

export interface CrmApiResult {
  success: boolean;
  data?: Record<string, unknown>;
  error?: string;
}

/**
 * Create a CRM record by calling the appropriate microservice.
 * @param objectType - e.g. 'lead', 'account', 'contact', 'opportunity'
 * @param fields - Record data to send in the POST body
 * @param authToken - Bearer token to forward to the microservice
 */
export async function createCrmRecord(
  objectType: string,
  fields: Record<string, unknown>,
  authToken: string,
): Promise<CrmApiResult> {
  const type = objectType.toLowerCase();
  const baseUrl = SERVICE_URLS[type];
  const path = API_PATHS[type];

  if (!baseUrl || !path) {
    return {
      success: false,
      error: `Unsupported record type: "${objectType}". Supported types: ${Object.keys(SERVICE_URLS).join(', ')}`,
    };
  }

  const url = `${baseUrl}${path}`;

  try {
    logger.info('Creating CRM record', { objectType: type, url, fields });

    const response = await axios.post(url, fields, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${authToken}`,
      },
      timeout: 15000,
    });

    const body = response.data;

    if (body.success === false) {
      return { success: false, error: body.message || 'Backend returned failure' };
    }

    return {
      success: true,
      data: body.data ?? body,
    };
  } catch (err: any) {
    const status = err.response?.status;
    const msg = err.response?.data?.message || err.response?.data?.error || err.message;
    logger.error('CRM API call failed', { objectType: type, url, status, error: msg });
    return { success: false, error: `${type}-service error (${status ?? 'network'}): ${msg}` };
  }
}
