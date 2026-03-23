/* ============================================================
   Types – shared TypeScript interfaces for the CRM platform
   ============================================================ */

// ── Auth ────────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
  tenantId: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  tenantId: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  mfaRequired?: boolean;
  mfaToken?: string;
  userId?: string;
  email?: string;
  tenantId?: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  tenantId: string;
  roles: string[];
  planName?: string;
}

export interface TenantPlan {
  tenantId: string;
  planName: string;
  maxUsers: number;
  maxCustomObjects: number;
  maxWorkflows: number;
  maxDashboards: number;
  maxPipelines: number;
  maxRoles: number;
  maxRecordsPerObject: number;
  aiConfigEnabled: boolean;
  aiInsightsEnabled: boolean;
  emailTrackingEnabled: boolean;
  apiAccessEnabled: boolean;
  integrationsEnabled: boolean;
  startedAt: string;
  expiresAt: string | null;
  active: boolean;
}

// ── Generic API wrappers ────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: { code: string; message: string };
  timestamp: string;
}

export interface PagedData<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

// ── Leads ───────────────────────────────────────────────────
export type LeadStatus = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'UNQUALIFIED' | 'CONVERTED' | 'LOST';
export type LeadSource = 'WEB' | 'PHONE' | 'EMAIL' | 'REFERRAL' | 'SOCIAL_MEDIA' | 'TRADE_SHOW' | 'EVENT' | 'OTHER';

export interface Lead {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  company: string;
  title: string;
  status: LeadStatus;
  source: LeadSource;
  leadScore: number;
  assignedTo: string | null;
  description: string;
  notes?: string;
  converted: boolean;
  opportunityId: string | null;
  campaignId: string | null;
  territory: string | null;
  slaDueDate: string | null;
  firstResponseAt: string | null;
  accountId: string | null;
  contactId: string | null;
  tags: LeadTag[];
  tenantId: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateLeadRequest {
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  company?: string;
  title?: string;
  source?: LeadSource;
  description?: string;
  assignedTo?: string;
}

export interface UpdateLeadRequest extends Partial<CreateLeadRequest> {
  status?: LeadStatus;
  leadScore?: number;
  campaignId?: string;
  territory?: string;
  slaDueDate?: string;
}

export interface ConvertLeadRequest {
  opportunityName: string;
  amount?: number;
  stage?: OpportunityStage;
  createAccount?: boolean;
  createContact?: boolean;
}

// ── Lead sub-entities ───────────────────────────────────────
export interface LeadNote {
  id: string;
  leadId: string;
  content: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface LeadTag {
  id: string;
  name: string;
  color: string;
  createdAt: string;
}

export interface LeadAttachment {
  id: string;
  leadId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  createdBy: string;
  createdAt: string;
}

export interface LeadActivity {
  id: string;
  leadId: string;
  activityType: string;
  title: string;
  description: string;
  metadata: string | null;
  createdBy: string;
  createdAt: string;
}

export interface AssignmentRule {
  id: string;
  name: string;
  criteriaField: string;
  criteriaOperator: string;
  criteriaValue: string;
  assignTo: string;
  priority: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScoringRule {
  id: string;
  name: string;
  criteriaField: string;
  criteriaOperator: string;
  criteriaValue: string;
  scoreDelta: number;
  active: boolean;
  tenantId: string;
  createdAt: string;
}

export interface BulkUpdateRequest {
  leadIds: string[];
  status?: LeadStatus;
  assignTo?: string;
  territory?: string;
  delete?: boolean;
}

export interface LeadAnalytics {
  totalLeads: number;
  convertedLeads: number;
  conversionRate: number;
  averageScore: number | null;
  byStatus: Record<string, number>;
  bySource: Record<string, number>;
  slaBreached: number;
}

export interface WebForm {
  id: string;
  name: string;
  fields: string;
  source: string;
  assignTo: string | null;
  active: boolean;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

// ── Accounts ────────────────────────────────────────────────
export type AccountType = 'PROSPECT' | 'CUSTOMER' | 'PARTNER' | 'VENDOR' | 'OTHER';
export type LifecycleStage = 'NEW' | 'ACTIVE' | 'INACTIVE' | 'CHURNED';

export interface Account {
  id: string;
  name: string;
  type?: AccountType;
  industry: string;
  website: string;
  phone: string;
  billingAddress: string | null;
  shippingAddress: string | null;
  annualRevenue: number;
  numberOfEmployees: number;
  parentAccountId: string | null;
  description: string | null;
  ownerId: string | null;
  territory: string | null;
  lifecycleStage: string | null;
  healthScore: number | null;
  segment: string | null;
  engagementScore: number | null;
  tags?: AccountTag[];
  tenantId: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateAccountRequest {
  name: string;
  industry?: string;
  website?: string;
  phone?: string;
  billingAddress?: string;
  shippingAddress?: string;
  annualRevenue?: number;
  numberOfEmployees?: number;
  parentAccountId?: string;
  description?: string;
  type?: string;
  ownerId?: string;
  territory?: string;
  lifecycleStage?: string;
  segment?: string;
}

export interface UpdateAccountRequest {
  name?: string;
  industry?: string;
  website?: string;
  phone?: string;
  billingAddress?: string;
  shippingAddress?: string;
  annualRevenue?: number;
  numberOfEmployees?: number;
  parentAccountId?: string;
  description?: string;
  type?: string;
  ownerId?: string;
  territory?: string;
  lifecycleStage?: string;
  segment?: string;
  healthScore?: number;
  engagementScore?: number;
}

export interface AccountNote {
  id: string;
  accountId: string;
  content: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccountTag {
  id: string;
  name: string;
  color: string;
  createdAt: string;
}

export interface AccountAttachment {
  id: string;
  accountId: string;
  fileName: string;
  fileUrl: string;
  fileSize: number | null;
  fileType: string | null;
  createdBy: string;
  createdAt: string;
}

export interface AccountActivity {
  id: string;
  accountId: string;
  type: string;
  description: string;
  performedBy: string;
  createdBy: string;
  createdAt: string;
}

export interface AccountAnalytics {
  totalAccounts: number;
  activeAccounts: number;
  newAccounts: number;
  churnedAccounts: number;
  totalRevenue: number;
  averageRevenue: number;
  averageHealthScore: number;
  averageEngagementScore: number;
  byType: Record<string, number>;
  byIndustry: Record<string, number>;
  byLifecycleStage: Record<string, number>;
  byTerritory: Record<string, number>;
  bySegment: Record<string, number>;
}

export interface BulkAccountUpdateRequest {
  accountIds: string[];
  ownerId?: string;
  territory?: string;
  type?: string;
  lifecycleStage?: string;
  segment?: string;
}

// ── Contacts ────────────────────────────────────────────────
export interface Contact {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  mobilePhone: string | null;
  title: string;
  department: string;
  accountId: string | null;
  mailingAddress: string | null;
  description: string | null;
  ownerId: string | null;
  linkedinUrl: string | null;
  twitterUrl: string | null;
  facebookUrl: string | null;
  otherSocialUrl: string | null;
  leadSource: string | null;
  lifecycleStage: string | null;
  segment: string | null;
  emailOptIn: boolean;
  smsOptIn: boolean;
  phoneOptIn: boolean;
  consentDate: string | null;
  consentSource: string | null;
  doNotCall: boolean;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateContactRequest {
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  mobilePhone?: string;
  title?: string;
  department?: string;
  accountId?: string;
  mailingAddress?: string;
  description?: string;
  ownerId?: string;
  linkedinUrl?: string;
  twitterUrl?: string;
  facebookUrl?: string;
  otherSocialUrl?: string;
  leadSource?: string;
  lifecycleStage?: string;
  segment?: string;
  emailOptIn?: boolean;
  smsOptIn?: boolean;
  phoneOptIn?: boolean;
  consentSource?: string;
  doNotCall?: boolean;
}

export interface UpdateConsentRequest {
  emailOptIn?: boolean;
  smsOptIn?: boolean;
  phoneOptIn?: boolean;
  doNotCall?: boolean;
  consentSource?: string;
}

export interface ContactCommunication {
  id: string;
  contactId: string;
  commType: string;
  subject: string | null;
  body: string | null;
  direction: string;
  status: string;
  communicationDate: string;
  tenantId: string;
  createdAt: string;
  createdBy: string;
}

export interface CreateCommunicationRequest {
  commType: string;
  subject?: string;
  body?: string;
  direction: string;
  status?: string;
  communicationDate?: string;
}

export interface ContactActivity {
  id: string;
  contactId: string;
  activityType: string;
  description: string | null;
  metadata: string | null;
  createdAt: string;
  createdBy: string;
}

export interface ContactTag {
  id: string;
  contactId: string;
  tagName: string;
  createdAt: string;
}

export interface ContactNote {
  id: string;
  contactId: string;
  content: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContactAttachment {
  id: string;
  contactId: string;
  fileName: string;
  fileUrl: string;
  fileSize: number | null;
  fileType: string | null;
  createdBy: string;
  createdAt: string;
}

export interface DuplicateContactGroup {
  matchField: string;
  matchValue: string;
  contacts: Contact[];
}

export interface ContactAnalytics {
  totalContacts: number;
  contactsWithEmail: number;
  contactsWithPhone: number;
  contactsWithAccount: number;
  emailOptInCount: number;
  smsOptInCount: number;
  doNotCallCount: number;
  bySegment: Record<string, number>;
  byLifecycleStage: Record<string, number>;
  byLeadSource: Record<string, number>;
  byDepartment: Record<string, number>;
  communicationsByType?: Record<string, number>;
  totalCommunications?: number;
  totalTags?: number;
}

// ── Opportunities ───────────────────────────────────────────
export type OpportunityStage =
  | 'PROSPECTING'
  | 'QUALIFICATION'
  | 'NEEDS_ANALYSIS'
  | 'PROPOSAL'
  | 'NEGOTIATION'
  | 'CLOSED_WON'
  | 'CLOSED_LOST'
  | string; // Allow custom stages

export type ForecastCategory = 'PIPELINE' | 'BEST_CASE' | 'COMMIT' | 'CLOSED';

export interface PipelineStage {
  id: string;
  name: string;
  displayName: string;
  displayOrder: number;
  color: string;
  defaultProbability: number;
  forecastCategory: string;
  closedWon: boolean;
  closedLost: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PipelineStageRequest {
  name: string;
  displayName: string;
  displayOrder: number;
  color?: string;
  defaultProbability?: number;
  forecastCategory?: string;
  closedWon?: boolean;
  closedLost?: boolean;
  active?: boolean;
}

export interface Opportunity {
  id: string;
  name: string;
  accountId: string | null;
  accountName?: string;
  contactId: string | null;
  stage: OpportunityStage;
  amount: number;
  probability: number;
  closeDate: string;
  description: string | null;
  assignedTo: string | null;
  forecastCategory: ForecastCategory | null;
  lostReason: string | null;
  wonDate: string | null;
  lostDate: string | null;
  currency: string;
  nextStep: string | null;
  leadSource: string | null;
  campaignId: string | null;
  predictedCloseDate: string | null;
  confidenceScore: number | null;
  ownerId: string | null;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateOpportunityRequest {
  name: string;
  accountId?: string;
  contactId?: string;
  stage?: OpportunityStage;
  amount?: number;
  probability?: number;
  closeDate?: string;
  description?: string;
  assignedTo?: string;
  currency?: string;
  nextStep?: string;
  leadSource?: string;
  campaignId?: string;
  ownerId?: string;
}

export interface UpdateOpportunityRequest {
  name?: string;
  accountId?: string;
  contactId?: string;
  stage?: OpportunityStage;
  amount?: number;
  probability?: number;
  closeDate?: string;
  description?: string;
  assignedTo?: string;
  currency?: string;
  nextStep?: string;
  leadSource?: string;
  campaignId?: string;
  ownerId?: string;
}

// ── Opportunity sub-entities ────────────────────────────────
export interface OpportunityProduct {
  id: string;
  opportunityId: string;
  productName: string;
  productCode: string | null;
  quantity: number;
  unitPrice: number;
  discount: number;
  totalPrice: number;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  productName: string;
  productCode?: string;
  quantity?: number;
  unitPrice?: number;
  discount?: number;
  description?: string;
}

export interface OpportunityCompetitor {
  id: string;
  opportunityId: string;
  competitorName: string;
  strengths: string | null;
  weaknesses: string | null;
  strategy: string | null;
  threatLevel: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCompetitorRequest {
  competitorName: string;
  strengths?: string;
  weaknesses?: string;
  strategy?: string;
  threatLevel?: string;
}

export interface OpportunityActivity {
  id: string;
  opportunityId: string;
  activityType: string;
  description: string;
  metadata: string | null;
  createdAt: string;
  createdBy: string;
}

export interface OpportunityCollaborator {
  id: string;
  opportunityId: string;
  userId: string;
  role: string;
  createdAt: string;
}

export interface OpportunityNote {
  id: string;
  opportunityId: string;
  content: string;
  isPinned: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateNoteRequest {
  content: string;
  isPinned?: boolean;
}

export interface OpportunityReminder {
  id: string;
  opportunityId: string;
  reminderType: string;
  message: string;
  remindAt: string;
  isCompleted: boolean;
  completedAt: string | null;
  createdAt: string;
  createdBy: string;
}

export interface CreateReminderRequest {
  reminderType: string;
  message: string;
  remindAt: string;
}

export interface StageSummary {
  stage: string;
  count: number;
  totalAmount: number;
  weightedAmount: number;
}

export interface ForecastSummary {
  pipelineValue: number;
  bestCaseValue: number;
  commitValue: number;
  closedValue: number;
  weightedPipeline: number;
  byStage: StageSummary[];
}

export interface RevenueAnalytics {
  totalOpportunities: number;
  openOpportunities: number;
  closedWonOpportunities: number;
  closedLostOpportunities: number;
  totalRevenue: number;
  totalPipeline: number;
  averageDealSize: number;
  winRate: number;
  totalWeightedPipeline: number;
  revenueByStage: Record<string, number>;
  countByStage: Record<string, number>;
  revenueByLeadSource: Record<string, number>;
  stageBreakdown: StageSummary[];
}

export interface WinLossAnalysis {
  totalClosedWon: number;
  totalClosedLost: number;
  winRate: number;
  averageWonDealSize: number;
  averageLostDealSize: number;
  totalWonRevenue: number;
  totalLostRevenue: number;
  averageDaysToClose: number;
  lostReasonBreakdown: Record<string, number>;
}

export interface RevenueTrend {
  monthly: MonthlyRevenueData[];
}

export interface MonthlyRevenueData {
  month: string;
  wonRevenue: number;
  pipeline: number;
  dealsWon: number;
  dealsClosed: number;
  dealsCreated: number;
}

export interface OpportunityAlert {
  type: string;
  message: string;
  opportunityId: string;
  opportunityName: string;
  severity: string;
}

// ── Activities ──────────────────────────────────────────────
export type ActivityType = 'TASK' | 'CALL' | 'MEETING' | 'EMAIL';
export type ActivityStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type ActivityPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type RecurrenceRule = 'DAILY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY';

export interface Activity {
  id: string;
  type: ActivityType;
  subject: string;
  description: string | null;
  status: ActivityStatus;
  priority: ActivityPriority;
  dueDate: string | null;
  startTime: string | null;
  endTime: string | null;
  completedAt: string | null;
  relatedEntityType: string | null;
  relatedEntityId: string | null;
  assignedTo: string | null;
  reminderAt: string | null;
  reminderSent: boolean;
  recurrenceRule: RecurrenceRule | null;
  recurrenceEnd: string | null;
  parentActivityId: string | null;
  location: string | null;
  callDurationMinutes: number | null;
  callOutcome: string | null;
  emailTo: string | null;
  emailCc: string | null;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateActivityRequest {
  type: ActivityType;
  subject: string;
  description?: string;
  status?: ActivityStatus;
  priority?: ActivityPriority;
  dueDate?: string;
  startTime?: string;
  endTime?: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  assignedTo?: string;
  reminderAt?: string;
  recurrenceRule?: RecurrenceRule;
  recurrenceEnd?: string;
  location?: string;
  callDurationMinutes?: number;
  callOutcome?: string;
  emailTo?: string;
  emailCc?: string;
}

export interface UpdateActivityRequest extends Partial<CreateActivityRequest> {}

export interface ActivityAnalytics {
  totalActivities: number;
  completedActivities: number;
  overdueActivities: number;
  completionRate: number;
  avgCompletionDays: number;
  countByType: Record<string, number>;
  countByStatus: Record<string, number>;
  countByPriority: Record<string, number>;
  countByAssignee: Record<string, number>;
}

// ── Cases ───────────────────────────────────────────────────
export type CasePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type CaseStatus = 'NEW' | 'OPEN' | 'IN_PROGRESS' | 'ESCALATED' | 'RESOLVED' | 'CLOSED';

export interface SupportCase {
  id: string;
  caseNumber?: string;
  subject: string;
  description: string;
  customerId?: string;
  customerName?: string;
  contactName?: string;
  accountName?: string;
  priority: CasePriority;
  status: CaseStatus;
  assignedAgent?: string | null;
  sla?: string;
  createdDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ── Campaigns ───────────────────────────────────────────────
export type CampaignStatus = 'DRAFT' | 'PLANNED' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ABORTED';
export type CampaignType = 'EMAIL' | 'SOCIAL' | 'EVENT' | 'WEBINAR' | 'CONTENT' | 'PAID_ADS';

export interface Campaign {
  id: string;
  name: string;
  type: CampaignType;
  status: CampaignStatus;
  startDate?: string;
  endDate?: string;
  budget?: number;
  spent?: number;
  actualCost?: number;
  expectedRevenue?: number;
  leads?: number;
  leadsGenerated?: number;
  conversions?: number;
  description?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

// ── Dashboard ───────────────────────────────────────────────
export interface DashboardMetrics {
  totalLeads: number;
  qualifiedLeads: number;
  totalOpportunities: number;
  pipelineValue: number;
  wonDeals: number;
  wonAmount: number;
  conversionRate: number;
  avgDealSize: number;
}

export interface ChartDataPoint {
  name: string;
  value: number;
  [key: string]: string | number;
}

// ── Pipeline Analytics ──────────────────────────────────────
export interface StageConversionRate {
  fromStage: string;
  toStage: string;
  transitioned: number;
  total: number;
  conversionPct: number;
}

export interface StageTransition {
  fromStage: string;
  toStage: string;
  count: number;
}

export interface StageConversionAnalytics {
  conversionRates: Record<string, StageConversionRate>;
  avgTimeInStage: Record<string, number>;
  transitions: StageTransition[];
  overallConversionRate: number;
}

export interface PipelineDashboard {
  totalPipelineValue: number;
  totalOpenDeals: number;
  totalClosedWon: number;
  totalClosedLost: number;
  totalRevenue: number;
  avgDealSize: number;
  winRate: number;
  weightedPipeline: number;
  stageBreakdown: StageSummary[];
  revenueByLeadSource: Record<string, number>;
  forecastPipeline: number;
  forecastBestCase: number;
  forecastCommit: number;
  forecastClosed: number;
  overdueDeals: number;
  closingSoonDeals: number;
  staleDeals: number;
  activeReminders: number;
}

export interface RepPerformance {
  userId: string;
  totalDeals: number;
  wonDeals: number;
  lostDeals: number;
  openDeals: number;
  totalRevenue: number;
  avgDealSize: number;
  winRate: number;
  quotaAttainment: number;
}

export interface PipelinePerformance {
  pipelineVelocity: number;
  avgDealSize: number;
  winRate: number;
  avgCycleDays: number;
  totalDeals: number;
  repPerformances: RepPerformance[];
}

export interface SalesQuota {
  id: string;
  userId: string;
  periodType: string;
  periodStart: string;
  periodEnd: string;
  targetAmount: number;
  targetDeals: number;
  actualAmount: number;
  actualDeals: number;
  attainmentPct: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSalesQuotaRequest {
  userId: string;
  periodType: string;
  periodStart: string;
  periodEnd: string;
  targetAmount: number;
  targetDeals: number;
}

// ── Email Integration ───────────────────────────────────────
export type EmailProvider = 'GMAIL' | 'OUTLOOK' | 'SMTP';
export type EmailDirection = 'INBOUND' | 'OUTBOUND';
export type EmailStatus = 'DRAFT' | 'QUEUED' | 'SENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'RECEIVED';
export type TrackingEventType = 'SENT' | 'DELIVERED' | 'OPENED' | 'CLICKED' | 'BOUNCED' | 'UNSUBSCRIBED';
export type ScheduleStatus = 'PENDING' | 'SENT' | 'CANCELLED' | 'FAILED';

export interface EmailAccount {
  id: string;
  provider: EmailProvider;
  email: string;
  displayName?: string;
  isDefault: boolean;
  connected: boolean;
  lastSyncAt?: string;
  smtpHost?: string;
  smtpPort?: number;
  createdAt: string;
}

export interface CreateEmailAccountRequest {
  provider: EmailProvider;
  email: string;
  displayName?: string;
  isDefault: boolean;
  smtpHost?: string;
  smtpPort?: number;
  smtpUsername?: string;
  smtpPassword?: string;
  authCode?: string;
}

export interface EmailTemplate {
  id: string;
  name: string;
  subject: string;
  bodyHtml?: string;
  bodyText?: string;
  category?: string;
  variables?: string;
  isActive: boolean;
  usageCount: number;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateEmailTemplateRequest {
  name: string;
  subject: string;
  bodyHtml?: string;
  bodyText?: string;
  category?: string;
  variables?: string;
}

export interface UpdateEmailTemplateRequest {
  name?: string;
  subject?: string;
  bodyHtml?: string;
  bodyText?: string;
  category?: string;
  variables?: string;
  isActive?: boolean;
}

export interface EmailMessage {
  id: string;
  accountId?: string;
  fromAddress: string;
  toAddresses: string;
  ccAddresses?: string;
  bccAddresses?: string;
  subject: string;
  bodyHtml?: string;
  bodyText?: string;
  direction: EmailDirection;
  status: EmailStatus;
  threadId?: string;
  inReplyTo?: string;
  templateId?: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  opened: boolean;
  openCount: number;
  clickCount: number;
  firstOpenedAt?: string;
  sentAt?: string;
  scheduledAt?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface SendEmailRequest {
  to: string;
  cc?: string;
  bcc?: string;
  subject: string;
  bodyHtml?: string;
  bodyText?: string;
  templateId?: string;
  templateVars?: Record<string, string>;
  accountId?: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  inReplyTo?: string;
  scheduledAt?: string;
  trackOpens: boolean;
  trackClicks: boolean;
}

export interface EmailTrackingEvent {
  id: string;
  messageId: string;
  eventType: TrackingEventType;
  linkUrl?: string;
  userAgent?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface EmailSchedule {
  id: string;
  toAddresses: string;
  ccAddresses?: string;
  subject: string;
  bodyHtml?: string;
  templateId?: string;
  scheduledAt: string;
  status: ScheduleStatus;
  sentAt?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface EmailAnalytics {
  totalSent: number;
  totalDelivered: number;
  totalOpened: number;
  totalClicked: number;
  totalBounced: number;
  totalFailed: number;
  openRate: number;
  clickRate: number;
  bounceRate: number;
  deliveryRate: number;
  sentByDay: Record<string, number>;
  opensByDay: Record<string, number>;
  clicksByDay: Record<string, number>;
  sentByTemplate: Record<string, number>;
}

export interface OAuthConnectResponse {
  authorizationUrl: string;
  provider: string;
  state: string;
}

// ── Workflow Automation ─────────────────────────────────────
export type ActionType = 'SEND_EMAIL' | 'CREATE_TASK' | 'UPDATE_FIELD' | 'SEND_NOTIFICATION' | 'ASSIGN_TO';
export type ConditionOperator = 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'LESS_THAN' | 'CONTAINS' | 'IN' | 'IS_NULL' | 'IS_NOT_NULL';
export type LogicalOperator = 'AND' | 'OR';
export type ExecutionStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';

export interface WorkflowCondition {
  id?: string;
  fieldName: string;
  operator: ConditionOperator;
  value: string;
  logicalOperator: LogicalOperator;
}

export interface WorkflowAction {
  id?: string;
  actionType: ActionType;
  targetField: string;
  targetValue: string;
  actionOrder: number;
}

export interface WorkflowRule {
  id: string;
  name: string;
  description: string | null;
  entityType: string;
  triggerEvent: string;
  active: boolean;
  conditions: WorkflowCondition[];
  actions: WorkflowAction[];
  tenantId: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateWorkflowRuleRequest {
  name: string;
  description?: string;
  entityType: string;
  triggerEvent: string;
  conditions: Omit<WorkflowCondition, 'id'>[];
  actions: Omit<WorkflowAction, 'id'>[];
}

export interface UpdateWorkflowRuleRequest {
  name?: string;
  description?: string;
  entityType?: string;
  triggerEvent?: string;
  conditions?: Omit<WorkflowCondition, 'id'>[];
  actions?: Omit<WorkflowAction, 'id'>[];
}

export interface WorkflowExecutionLog {
  id: string;
  ruleId: string;
  ruleName: string;
  triggerEntityType: string;
  triggerEntityId: string;
  status: ExecutionStatus;
  executionDetails: string | null;
  executedAt: string;
}

// ── Security & User Management ──────────────────────────────
export type RoleLevel = 'ADMIN' | 'MANAGER' | 'USER' | 'VIEWER';

export interface RoleDefinition {
  id: string;
  name: string;
  description: string;
  level: number;
  permissions: string[];
  tenantId: string;
  createdAt?: string;
}

export interface PermissionSet {
  id: string;
  name: string;
  description: string;
  permissions: Permission[];
}

export interface Permission {
  resource: string;
  actions: ('create' | 'read' | 'update' | 'delete')[];
}

export interface FieldSecurity {
  id: string;
  entityType: string;
  fieldName: string;
  role: string;
  visible: boolean;
  editable: boolean;
}

export interface RecordAccessRule {
  id: string;
  entityType: string;
  accessType: 'OWNER' | 'TEAM' | 'ROLE' | 'PUBLIC';
  role?: string;
  canRead: boolean;
  canEdit: boolean;
  canDelete: boolean;
}

export interface SsoProvider {
  id: string;
  name: string;
  protocol: 'SAML' | 'OIDC';
  issuerUrl: string;
  clientId: string;
  enabled: boolean;
  createdAt: string;
}

export interface MfaSetup {
  enabled: boolean;
  method: 'TOTP' | 'SMS' | 'EMAIL';
  verifiedAt?: string;
}

export interface AuditLogEntry {
  id: string;
  userId: string;
  userEmail: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  ipAddress: string;
  timestamp: string;
}

export interface UserManagement {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  enabled: boolean;
  roles: string[];
  tenantId: string;
  lastLogin?: string;
  mfaEnabled?: boolean;
  createdAt?: string;
}

// ── Integration Platform ────────────────────────────────────
export type IntegrationStatus = 'ACTIVE' | 'INACTIVE' | 'ERROR' | 'PENDING';
export type WebhookEvent = 'LEAD_CREATED' | 'LEAD_UPDATED' | 'LEAD_DELETED' | 'CONTACT_CREATED' | 'CONTACT_UPDATED'
  | 'ACCOUNT_CREATED' | 'ACCOUNT_UPDATED' | 'OPPORTUNITY_CREATED' | 'OPPORTUNITY_UPDATED' | 'OPPORTUNITY_WON'
  | 'OPPORTUNITY_LOST' | 'ACTIVITY_COMPLETED' | 'CUSTOM';
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
export type AuthType = 'API_KEY' | 'OAUTH2' | 'BASIC' | 'BEARER' | 'NONE';
export type SyncDirection = 'INBOUND' | 'OUTBOUND' | 'BIDIRECTIONAL';
export type SyncStatus = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';
export type ConnectorType = 'DATABASE' | 'REST_API' | 'FILE' | 'SALESFORCE' | 'HUBSPOT' | 'ZAPIER' | 'SLACK' | 'CUSTOM';
export type IntegrationLogLevel = 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL';

export interface RestApiEndpoint {
  id: string;
  name: string;
  path: string;
  method: HttpMethod;
  description: string;
  authRequired: boolean;
  rateLimit: number;
  enabled: boolean;
  version: string;
  requestSchema?: string;
  responseSchema?: string;
  lastCalledAt?: string;
  totalCalls: number;
  createdAt: string;
}

export interface WebhookConfig {
  id: string;
  name: string;
  url: string;
  events: WebhookEvent[];
  secret?: string;
  active: boolean;
  retryCount: number;
  retryDelayMs: number;
  headers?: Record<string, string>;
  lastTriggeredAt?: string;
  successCount: number;
  failureCount: number;
  createdAt: string;
}

export interface ThirdPartyIntegration {
  id: string;
  name: string;
  provider: string;
  type: ConnectorType;
  status: IntegrationStatus;
  description: string;
  icon?: string;
  authType: AuthType;
  configJson?: string;
  lastSyncAt?: string;
  enabled: boolean;
  createdAt: string;
}

export interface DataSync {
  id: string;
  name: string;
  integrationId: string;
  integrationName: string;
  entityType: string;
  direction: SyncDirection;
  status: SyncStatus;
  schedule?: string;
  lastRunAt?: string;
  lastRunDuration?: number;
  recordsSynced: number;
  recordsFailed: number;
  fieldMapping: Record<string, string>;
  enabled: boolean;
  createdAt: string;
}

export interface ExternalConnector {
  id: string;
  name: string;
  type: ConnectorType;
  connectionString?: string;
  host?: string;
  port?: number;
  database?: string;
  baseUrl?: string;
  status: IntegrationStatus;
  lastTestAt?: string;
  enabled: boolean;
  createdAt: string;
}

export interface ApiAuthConfig {
  id: string;
  name: string;
  authType: AuthType;
  apiKey?: string;
  clientId?: string;
  clientSecret?: string;
  tokenUrl?: string;
  scopes?: string[];
  expiresAt?: string;
  active: boolean;
  createdAt: string;
  lastUsedAt?: string;
}

export interface IntegrationHealth {
  id: string;
  integrationId: string;
  integrationName: string;
  status: IntegrationStatus;
  uptime: number;
  avgResponseMs: number;
  successRate: number;
  totalRequests: number;
  lastCheckedAt: string;
  alertsCount: number;
}

export interface IntegrationError {
  id: string;
  integrationId: string;
  integrationName: string;
  level: IntegrationLogLevel;
  message: string;
  stackTrace?: string;
  endpoint?: string;
  httpStatus?: number;
  requestPayload?: string;
  responsePayload?: string;
  resolvedAt?: string;
  createdAt: string;
}

// ── AI & Intelligence ───────────────────────────────────────
export type ScoreTrend = 'RISING' | 'STABLE' | 'DECLINING';
export type ChurnRisk = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ActionCategory = 'EMAIL' | 'CALL' | 'MEETING' | 'PROPOSAL' | 'FOLLOW_UP' | 'UPSELL' | 'RETENTION';
export type InsightType = 'TREND' | 'ANOMALY' | 'RECOMMENDATION' | 'PREDICTION' | 'ALERT';
export type ForecastConfidence = 'LOW' | 'MEDIUM' | 'HIGH';

export interface PredictiveLeadScore {
  id: string;
  leadId: string;
  leadName: string;
  email: string;
  company: string;
  currentScore: number;
  predictedScore: number;
  trend: ScoreTrend;
  conversionProbability: number;
  topFactors: ScoringFactor[];
  lastUpdated: string;
}

export interface ScoringFactor {
  factor: string;
  impact: number;
  direction: 'positive' | 'negative';
}

export interface WinProbability {
  id: string;
  opportunityId: string;
  opportunityName: string;
  accountName: string;
  amount: number;
  stage: string;
  winProbability: number;
  historicalWinRate: number;
  daysInStage: number;
  riskFactors: string[];
  positiveSignals: string[];
  recommendation: string;
  lastUpdated: string;
}

export interface SalesForecast {
  id: string;
  period: string;
  periodLabel: string;
  predictedRevenue: number;
  bestCase: number;
  worstCase: number;
  confidence: ForecastConfidence;
  pipelineValue: number;
  weightedPipeline: number;
  closedToDate: number;
  quota: number;
  attainmentPct: number;
  factors: string[];
}

export interface ChurnPrediction {
  id: string;
  accountId: string;
  accountName: string;
  industry: string;
  annualRevenue: number;
  riskLevel: ChurnRisk;
  churnProbability: number;
  riskFactors: string[];
  lastActivityDays: number;
  healthScore: number;
  recommendedActions: string[];
  predictedChurnDate?: string;
}

export interface NextBestAction {
  id: string;
  entityType: string;
  entityId: string;
  entityName: string;
  category: ActionCategory;
  action: string;
  reason: string;
  priority: number;
  expectedImpact: string;
  dueDate?: string;
  completed: boolean;
  createdAt: string;
}

export interface AiReportInsight {
  id: string;
  reportName: string;
  insightType: InsightType;
  title: string;
  description: string;
  metric: string;
  currentValue: number;
  previousValue: number;
  changePct: number;
  recommendation?: string;
  generatedAt: string;
}

export interface DataEntrySuggestion {
  id: string;
  entityType: string;
  entityId: string;
  entityName: string;
  field: string;
  currentValue?: string;
  suggestedValue: string;
  confidence: number;
  source: string;
  accepted?: boolean;
  createdAt: string;
}

export interface AiSalesInsight {
  id: string;
  insightType: InsightType;
  title: string;
  summary: string;
  details: string;
  impactArea: string;
  severity: 'low' | 'medium' | 'high';
  actionable: boolean;
  relatedEntities: { type: string; id: string; name: string }[];
  generatedAt: string;
}

// ── AI Email Reply Generation ───────────────────────────────
export interface EmailReply {
  id: string;
  originalFrom: string;
  originalSubject: string;
  replySubject: string;
  replyBody: string;
  tone: string;
  suggestions: string[];
  createdAt: string;
}

// ── AI Meeting Summary ──────────────────────────────────────
export interface CrmUpdateSuggestion {
  entityType: string;
  entityId: string;
  field: string;
  suggestedValue: string;
  reason: string;
}

export interface MeetingSummary {
  id: string;
  meetingTitle: string;
  meetingDate?: string;
  participants: string[];
  summary: string;
  actionItems: string[];
  keyDecisions: string[];
  crmUpdates: CrmUpdateSuggestion[];
  relatedEntityType?: string;
  relatedEntityId?: string;
  createdAt: string;
}

// ── AI Auto-Lead from Email / Meeting ───────────────────────
export type AutoLeadStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CREATED';
export type AutoLeadSource = 'EMAIL' | 'MEETING';

export interface AutoLead {
  id: string;
  sourceType: AutoLeadSource;
  sourceReference?: string;
  leadName: string;
  email?: string;
  company?: string;
  title?: string;
  phone?: string;
  notes?: string;
  confidence: number;
  status: AutoLeadStatus;
  createdAt: string;
}

// ── Zero-Configuration CRM Types ────────────────────────────

export interface CsvFieldMapping {
  csvHeader: string;
  crmField: string;
  dataType: string;
  confidence: number;
  sampleValue: string;
  /** True if this field is from an industry template */
  isIndustryField?: boolean;
  /** True if this field is a custom field from AI Config */
  isCustomField?: boolean;
}

export interface IndustryFieldInfo {
  fieldName: string;
  label: string;
  fieldType: string;
  required: boolean;
}

export interface CsvFieldDetectionResult {
  entityType: string;
  fieldMappings: CsvFieldMapping[];
  unmappedColumns: string[];
  totalColumns: number;
  mappedColumns: number;
  /** Industry used for detection */
  industry?: string;
  /** Industry-specific fields available for this entity type */
  industryFields?: IndustryFieldInfo[];
}

export interface EnrichedField {
  field: string;
  currentValue: string;
  suggestedValue: string;
  confidence: number;
  source: string;
}

export interface ContactEnrichmentResult {
  contactId: string;
  enrichedFields: EnrichedField[];
  overallConfidence: number;
  enrichmentSource: string;
}

export interface OnboardingStep {
  id: string;
  title: string;
  description: string;
  category: string;
  completed: boolean;
  actionUrl: string;
  aiHint: string;
}

export interface OnboardingStatus {
  completedSteps: number;
  totalSteps: number;
  progressPercent: number;
  steps: OnboardingStep[];
  nextRecommendation: string;
}

// ── Unified Communications ──────────────────────────────────
export interface SmsMessage {
  id: string;
  fromNumber: string;
  toNumber: string;
  body: string;
  direction: 'INBOUND' | 'OUTBOUND';
  status: 'PENDING' | 'SENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'RECEIVED';
  externalId?: string;
  errorMessage?: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  sentAt?: string;
  deliveredAt?: string;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface WhatsAppMessage {
  id: string;
  fromNumber: string;
  toNumber: string;
  body: string;
  mediaUrl?: string;
  mediaType?: string;
  messageType: 'TEXT' | 'IMAGE' | 'DOCUMENT' | 'AUDIO' | 'VIDEO' | 'TEMPLATE';
  direction: 'INBOUND' | 'OUTBOUND';
  status: 'PENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';
  externalId?: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  readAt?: string;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CallRecord {
  id: string;
  fromNumber: string;
  toNumber: string;
  direction: 'INBOUND' | 'OUTBOUND';
  status: 'INITIATED' | 'RINGING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'NO_ANSWER' | 'BUSY' | 'VOICEMAIL';
  durationSeconds?: number;
  recordingUrl?: string;
  recordingDurationSeconds?: number;
  voicemailUrl?: string;
  callOutcome?: string;
  notes?: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  startedAt?: string;
  answeredAt?: string;
  endedAt?: string;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface UnifiedInboxMessage {
  id: string;
  channel: string;
  direction: string;
  sender: string;
  recipient: string;
  subject?: string;
  body: string;
  status: string;
  sourceId: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  tenantId: string;
  createdAt: string;
}

export interface TranscriptionResult {
  id: string;
  sourceType: string;
  sourceId?: string;
  fullTranscript: string;
  segments: { speaker: string; text: string; timestamp: string }[];
  keyTopics: string[];
  summary: string;
  language: string;
  createdAt: string;
}

export interface SentimentAnalysisResult {
  id: string;
  sourceType: string;
  sourceId?: string;
  overallSentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL' | 'MIXED';
  sentimentScore: number;
  confidence: number;
  summary: string;
  emotions: { emotion: string; score: number }[];
  keyPhrases: string[];
  concerns: string[];
  positiveIndicators: string[];
  recommendation: string;
  createdAt: string;
}

// ── Real-Time Collaboration ─────────────────────────────────
export interface DealChatMessage {
  id: string;
  opportunityId: string;
  senderId: string;
  senderName: string;
  message: string;
  messageType: string;
  parentMessageId?: string;
  isEdited: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MentionRecord {
  id: string;
  recordType: string;
  recordId: string;
  sourceType: string;
  sourceId: string;
  mentionedUserId: string;
  mentionedUserName: string;
  mentionedById: string;
  mentionedByName: string;
  isRead: boolean;
  createdAt: string;
}

export type ApprovalType = 'DISCOUNT' | 'STAGE_CHANGE' | 'CLOSE_DEAL' | 'PRICING' | 'CUSTOM';
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED';
export type ApprovalPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

export interface DealApproval {
  id: string;
  opportunityId: string;
  requestedById: string;
  requestedByName: string;
  approverId: string;
  approverName: string;
  approvalType: ApprovalType;
  status: ApprovalStatus;
  title: string;
  description?: string;
  currentValue?: string;
  requestedValue?: string;
  approverComment?: string;
  priority: ApprovalPriority;
  dueDate?: string;
  decidedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RecordComment {
  id: string;
  recordType: string;
  recordId: string;
  authorId: string;
  authorName: string;
  content: string;
  parentCommentId?: string;
  isInternal: boolean;
  isEdited: boolean;
  isPinned: boolean;
  replies?: RecordComment[];
  createdAt: string;
  updatedAt: string;
}

export interface ActivityStreamEvent {
  id: string;
  eventType: string;
  entityType: string;
  entityId?: string;
  entityName?: string;
  description?: string;
  performedBy: string;
  performedByName: string;
  metadata?: string;
  createdAt: string;
}

// ── Smart Automation ────────────────────────────────────────
export interface WorkflowTemplate {
  id: string;
  name: string;
  description?: string;
  category: string;
  entityType: string;
  triggerEvent: string;
  conditionsJson?: string;
  actionsJson?: string;
  canvasLayout?: string;
  popularity: number;
  isSystem: boolean;
  tenantId?: string;
  createdAt: string;
  updatedAt: string;
}

export type SuggestionType = 'PATTERN_DETECTED' | 'BEST_PRACTICE' | 'OPTIMIZATION';
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'DISMISSED';

export interface WorkflowSuggestion {
  id: string;
  suggestionType: SuggestionType;
  name: string;
  description?: string;
  entityType: string;
  triggerEvent: string;
  conditionsJson?: string;
  actionsJson?: string;
  confidence: number;
  reason?: string;
  status: SuggestionStatus;
  acceptedRuleId?: string;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProposalTemplate {
  id: string;
  name: string;
  description?: string;
  contentTemplate: string;
  category?: string;
  isDefault: boolean;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export type ProposalStatus = 'DRAFT' | 'SENT' | 'VIEWED' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';

export interface ProposalLineItem {
  id?: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  totalPrice: number;
  sortOrder: number;
}

export interface Proposal {
  id: string;
  opportunityId: string;
  templateId?: string;
  title: string;
  content?: string;
  status: ProposalStatus;
  amount: number;
  validUntil?: string;
  sentAt?: string;
  viewedAt?: string;
  respondedAt?: string;
  recipientEmail?: string;
  recipientName?: string;
  version: number;
  lineItems: ProposalLineItem[];
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProposalRequest {
  opportunityId: string;
  templateId?: string;
  title: string;
  content?: string;
  recipientEmail?: string;
  recipientName?: string;
  validUntil?: string;
  lineItems?: { productName: string; quantity: number; unitPrice: number; discount?: number; sortOrder?: number }[];
}

export type ContractStatus = 'DRAFT' | 'SENT' | 'VIEWED' | 'SIGNED' | 'EXECUTED' | 'EXPIRED' | 'CANCELLED';

// ── Calendar Sync ───────────────────────────────────────────
export interface CalendarFeedToken {
  id: string;
  name: string;
  token: string;
  active: boolean;
  createdAt: string;
  lastAccessedAt?: string;
}

export interface CalendarSyncConfig {
  id: string;
  provider: 'GOOGLE' | 'OUTLOOK' | 'APPLE';
  status: 'DISCONNECTED' | 'CONNECTED' | 'SYNCING' | 'ERROR';
  calendarId?: string;
  syncDirection: 'BIDIRECTIONAL' | 'TO_CALENDAR' | 'FROM_CALENDAR';
  syncIntervalMinutes: number;
  lastSyncAt?: string;
  lastSyncStatus?: string;
  eventsSynced: number;
  enabled: boolean;
  createdAt: string;
}

export interface Contract {
  id: string;
  opportunityId: string;
  proposalId?: string;
  title: string;
  content?: string;
  status: ContractStatus;
  amount: number;
  startDate?: string;
  endDate?: string;
  sentAt?: string;
  viewedAt?: string;
  signedAt?: string;
  executedAt?: string;
  signerName?: string;
  signerEmail?: string;
  version: number;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateContractRequest {
  opportunityId: string;
  proposalId?: string;
  title: string;
  content?: string;
  amount: number;
  startDate?: string;
  endDate?: string;
}

export interface SignContractRequest {
  signerName: string;
  signerEmail: string;
  signatureData: string;
}

// ── Developer & Integration Platform ────────────────────────────
export interface WebhookDeliveryLog {
  id: string;
  webhookId: string;
  webhookName: string;
  eventType: string;
  payload: string;
  responseStatus: number;
  responseBody?: string;
  attempt: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'RETRYING';
  errorMessage?: string;
  deliveredAt?: string;
  createdAt: string;
}

export interface DeveloperApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  rawKey?: string;
  scopes: string[];
  rateLimit: number;
  callsToday: number;
  totalCalls: number;
  active: boolean;
  expiresAt?: string;
  lastUsedAt?: string;
  createdBy: string;
  createdAt: string;
}

export interface CreateApiKeyRequest {
  name: string;
  scopes?: string[];
  rateLimit?: number;
  expiresAt?: string;
}

export interface MarketplacePlugin {
  id: string;
  name: string;
  slug: string;
  description?: string;
  longDescription?: string;
  category: string;
  author: string;
  version: string;
  iconUrl?: string;
  screenshots?: string[];
  downloadUrl?: string;
  documentationUrl?: string;
  status: 'DRAFT' | 'PUBLISHED' | 'DEPRECATED' | 'REMOVED';
  pricing: 'FREE' | 'PAID' | 'FREEMIUM';
  priceAmount?: number;
  installCount: number;
  rating: number;
  ratingCount: number;
  requiredScopes?: string[];
  configSchema?: Record<string, unknown>;
  isVerified: boolean;
  installed: boolean;
  createdAt: string;
}

export interface CreatePluginRequest {
  name: string;
  description?: string;
  longDescription?: string;
  category: string;
  author: string;
  version?: string;
  iconUrl?: string;
  screenshots?: string[];
  downloadUrl?: string;
  documentationUrl?: string;
  pricing?: string;
  priceAmount?: number;
  requiredScopes?: string[];
  configSchema?: Record<string, unknown>;
}

export interface PluginInstallation {
  id: string;
  pluginId: string;
  pluginName: string;
  pluginSlug: string;
  status: 'ACTIVE' | 'DISABLED' | 'UNINSTALLED';
  config?: Record<string, unknown>;
  installedBy: string;
  createdAt: string;
}

export interface EmbeddableWidget {
  id: string;
  name: string;
  widgetType: 'FORM' | 'TABLE' | 'CHART' | 'METRIC' | 'TIMELINE' | 'CUSTOM';
  description?: string;
  config?: Record<string, unknown>;
  embedToken: string;
  embedCode: string;
  allowedDomains?: string[];
  active: boolean;
  viewCount: number;
  createdAt: string;
}

export interface CreateWidgetRequest {
  name: string;
  widgetType?: string;
  description?: string;
  config?: Record<string, unknown>;
  allowedDomains?: string[];
}

export interface CustomApp {
  id: string;
  name: string;
  slug: string;
  description?: string;
  appType: 'FORM' | 'DASHBOARD' | 'PAGE' | 'WORKFLOW';
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  layout?: Record<string, unknown>;
  dataSource?: Record<string, unknown>;
  style?: Record<string, unknown>;
  publishedVersion?: string;
  createdBy: string;
  createdAt: string;
}

export interface CreateCustomAppRequest {
  name: string;
  description?: string;
  appType?: string;
  layout?: Record<string, unknown>;
  dataSource?: Record<string, unknown>;
  style?: Record<string, unknown>;
}
