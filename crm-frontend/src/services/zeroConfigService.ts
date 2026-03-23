import api from './api';
import type {
  ApiResponse,
  CsvFieldDetectionResult,
  ContactEnrichmentResult,
  OnboardingStatus,
  IndustryFieldInfo,
} from '../types';

const AI_BASE = '/api/v1/ai';

/* ===== CSV Field Detection ===== */

export async function detectCsvFields(
  csvContent: string,
  entityType: string,
  industry?: string,
  customFields?: string[]
): Promise<CsvFieldDetectionResult> {
  const { data } = await api.post<ApiResponse<CsvFieldDetectionResult>>(
    `${AI_BASE}/csv-detect-fields`,
    { csvContent, entityType, industry, customFields }
  );
  return data.data;
}

/** Get industry-specific fields for an entity type */
export async function getIndustryFields(
  industry: string,
  entityType: string
): Promise<{ industry: string; entityType: string; fields: IndustryFieldInfo[]; supportedIndustries: string[] }> {
  const { data } = await api.get<ApiResponse<{ industry: string; entityType: string; fields: IndustryFieldInfo[]; supportedIndustries: string[] }>>(
    `${AI_BASE}/csv-industry-fields`,
    { params: { industry, entityType } }
  );
  return data.data;
}

/** Get list of supported industries */
export async function getSupportedIndustries(): Promise<string[]> {
  const { data } = await api.get<ApiResponse<string[]>>(
    `${AI_BASE}/csv-supported-industries`
  );
  return data.data;
}

/* ===== Contact Enrichment ===== */

export async function enrichContact(request: {
  contactId: string;
  name?: string;
  email?: string;
  company?: string;
  title?: string;
  phone?: string;
  linkedInUrl?: string;
}): Promise<ContactEnrichmentResult> {
  const { data } = await api.post<ApiResponse<ContactEnrichmentResult>>(
    `${AI_BASE}/enrich-contact`,
    request
  );
  return data.data;
}

/* ===== AI Onboarding Assistant ===== */

export async function getOnboardingStatus(): Promise<OnboardingStatus> {
  const { data } = await api.get<ApiResponse<OnboardingStatus>>(
    `${AI_BASE}/onboarding/status`
  );
  return data.data;
}

export async function completeOnboardingStep(
  stepId: string
): Promise<OnboardingStatus> {
  const { data } = await api.post<ApiResponse<OnboardingStatus>>(
    `${AI_BASE}/onboarding/complete-step`,
    { stepId }
  );
  return data.data;
}

export async function resetOnboarding(): Promise<OnboardingStatus> {
  const { data } = await api.post<ApiResponse<OnboardingStatus>>(
    `${AI_BASE}/onboarding/reset`
  );
  return data.data;
}

export async function getOnboardingGuidance(
  stepId: string
): Promise<string> {
  const { data } = await api.get<ApiResponse<string>>(
    `${AI_BASE}/onboarding/guidance/${stepId}`
  );
  return data.data;
}

/* ===== Default / fallback data ===== */

export const DEFAULT_ONBOARDING_STATUS: OnboardingStatus = {
  completedSteps: 0,
  totalSteps: 10,
  progressPercent: 0,
  steps: [
    { id: 'create_pipeline', title: 'Create Your Sales Pipeline', description: 'Set up your first pipeline with stages that match your sales process', category: 'Setup', completed: false, actionUrl: '/ai-config', aiHint: "Tell the AI: 'Create a pipeline called Main Sales with stages: Prospecting, Discovery, Proposal, Negotiation, Closed Won, Closed Lost'" },
    { id: 'add_first_lead', title: 'Add Your First Lead', description: 'Create a new lead or import from CSV', category: 'Data', completed: false, actionUrl: '/leads', aiHint: "Go to Leads and click 'Add Lead', or use CSV import for bulk upload" },
    { id: 'add_first_contact', title: 'Add Your First Contact', description: 'Create a contact record for a key person', category: 'Data', completed: false, actionUrl: '/contacts', aiHint: 'Go to Contacts and add a key contact from your network' },
    { id: 'add_first_account', title: 'Create Your First Account', description: 'Set up a company account to track', category: 'Data', completed: false, actionUrl: '/accounts', aiHint: "Go to Accounts and add a prospect company you're working with" },
    { id: 'create_opportunity', title: 'Create an Opportunity', description: 'Track a deal in your pipeline', category: 'Sales', completed: false, actionUrl: '/opportunities', aiHint: 'Go to Opportunities and create a deal linked to your account' },
    { id: 'setup_workflow', title: 'Set Up an Automation', description: 'Create a workflow to automate a repetitive task', category: 'Automation', completed: false, actionUrl: '/ai-config', aiHint: "Tell the AI: 'Create a workflow that sends an email notification when a lead status changes to Qualified'" },
    { id: 'explore_dashboard', title: 'Explore Your Dashboard', description: 'Check out the smart default dashboard with pipeline and revenue insights', category: 'Insights', completed: false, actionUrl: '/dashboard', aiHint: 'Visit the Dashboard to see pipeline stages, revenue charts, and forecasts' },
    { id: 'try_ai_insights', title: 'Try AI Insights', description: 'Explore AI-powered lead scoring, forecasts, and recommendations', category: 'AI', completed: false, actionUrl: '/ai-insights', aiHint: 'Go to AI Insights to see lead scores, win probabilities, and next best actions' },
    { id: 'invite_team', title: 'Invite Your Team', description: 'Add team members for collaboration', category: 'Team', completed: false, actionUrl: '/settings', aiHint: 'Go to Settings to manage user roles and invite team members' },
    { id: 'configure_integrations', title: 'Connect Integrations', description: 'Connect email, calendar, or other tools', category: 'Integrations', completed: false, actionUrl: '/integrations', aiHint: 'Go to Integrations to connect your email provider, calendar, or other CRM tools' },
  ],
  nextRecommendation: "Let's start by creating your sales pipeline. Tell the AI Config assistant what stages your sales process uses!",
};

export const DEFAULT_CSV_DETECTION: CsvFieldDetectionResult = {
  entityType: 'account',
  fieldMappings: [
    { csvHeader: 'Company Name', crmField: 'name', dataType: 'string', confidence: 0.95, sampleValue: 'Acme Corp' },
    { csvHeader: 'Industry', crmField: 'industry', dataType: 'string', confidence: 0.98, sampleValue: 'Technology' },
    { csvHeader: 'Website URL', crmField: 'website', dataType: 'url', confidence: 0.92, sampleValue: 'https://acme.com' },
    { csvHeader: 'Phone Number', crmField: 'phone', dataType: 'phone', confidence: 0.94, sampleValue: '+1-555-0123' },
    { csvHeader: 'Annual Rev', crmField: 'annual_revenue', dataType: 'currency', confidence: 0.88, sampleValue: '$5,000,000' },
    { csvHeader: '# Employees', crmField: 'number_of_employees', dataType: 'number', confidence: 0.91, sampleValue: '250' },
  ],
  unmappedColumns: ['Custom Field 1', 'Internal Code'],
  totalColumns: 8,
  mappedColumns: 6,
};

export const DEFAULT_ENRICHMENT: ContactEnrichmentResult = {
  contactId: 'sample-contact-1',
  enrichedFields: [
    { field: 'industry', currentValue: '', suggestedValue: 'Software & Technology', confidence: 0.88, source: 'Inferred from company domain' },
    { field: 'company_size', currentValue: '', suggestedValue: 'Enterprise (1000+)', confidence: 0.82, source: 'Company data analysis' },
    { field: 'department', currentValue: '', suggestedValue: 'Engineering', confidence: 0.75, source: 'Inferred from job title: Senior Developer' },
    { field: 'seniority_level', currentValue: '', suggestedValue: 'Senior', confidence: 0.91, source: 'Title contains "Senior"' },
    { field: 'timezone', currentValue: '', suggestedValue: 'America/New_York (EST)', confidence: 0.70, source: 'Inferred from phone area code' },
    { field: 'professional_summary', currentValue: '', suggestedValue: 'Experienced senior developer at a major tech company with focus on enterprise solutions.', confidence: 0.65, source: 'Synthesized from title + company' },
  ],
  overallConfidence: 0.79,
  enrichmentSource: 'AI-powered enrichment',
};
