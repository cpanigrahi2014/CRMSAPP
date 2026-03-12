import api from './api';
import type { ApiResponse } from '../types';
import type {
  PredictiveLeadScore, WinProbability, SalesForecast, ChurnPrediction,
  NextBestAction, AiReportInsight, DataEntrySuggestion, AiSalesInsight,
  EmailReply, MeetingSummary, AutoLead,
} from '../types';

const INSIGHTS = '/api/v1/ai/insights';

/* ── localStorage helpers ────────────────────────────────────── */
function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch { return fallback; }
}
function saveJson<T>(key: string, data: T): void {
  localStorage.setItem(key, JSON.stringify(data));
}

/* ── Async helper: try API, fall back to localStorage ──────── */
async function tryApi<T>(apiFn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    const result = await apiFn();
    return result ?? fallback;
  } catch { return fallback; }
}

const K = {
  leads: 'crm_ai_lead_scores',
  winProb: 'crm_ai_win_prob',
  forecast: 'crm_ai_forecast',
  churn: 'crm_ai_churn',
  nba: 'crm_ai_next_action',
  reports: 'crm_ai_reports',
  suggestions: 'crm_ai_suggestions',
  insights: 'crm_ai_insights',
  emailReplies: 'crm_ai_email_replies',
  meetingSummaries: 'crm_ai_meeting_summaries',
  autoLeads: 'crm_ai_auto_leads',
};

/* ── Default Lead Scores ─────────────────────────────────────── */
const DEFAULT_LEAD_SCORES: PredictiveLeadScore[] = [
  { id: '1', leadId: 'l1', leadName: 'Sarah Johnson', email: 'sarah@techcorp.com', company: 'TechCorp', currentScore: 72, predictedScore: 88, trend: 'RISING', conversionProbability: 0.78, topFactors: [{ factor: 'Opened 5 emails in 7 days', impact: 15, direction: 'positive' }, { factor: 'Visited pricing page 3x', impact: 12, direction: 'positive' }, { factor: 'Company revenue >$10M', impact: 8, direction: 'positive' }], lastUpdated: new Date(Date.now() - 1800000).toISOString() },
  { id: '2', leadId: 'l2', leadName: 'Michael Chen', email: 'mchen@globalsoft.io', company: 'GlobalSoft', currentScore: 65, predictedScore: 71, trend: 'RISING', conversionProbability: 0.62, topFactors: [{ factor: 'Downloaded whitepaper', impact: 10, direction: 'positive' }, { factor: 'Attended webinar', impact: 8, direction: 'positive' }, { factor: 'No phone number', impact: -5, direction: 'negative' }], lastUpdated: new Date(Date.now() - 3600000).toISOString() },
  { id: '3', leadId: 'l3', leadName: 'Emily Rodriguez', email: 'emily@startupxyz.com', company: 'StartupXYZ', currentScore: 45, predictedScore: 38, trend: 'DECLINING', conversionProbability: 0.28, topFactors: [{ factor: 'No engagement in 14 days', impact: -18, direction: 'negative' }, { factor: 'Small company size', impact: -8, direction: 'negative' }, { factor: 'Attended demo', impact: 6, direction: 'positive' }], lastUpdated: new Date(Date.now() - 7200000).toISOString() },
  { id: '4', leadId: 'l4', leadName: 'David Kim', email: 'dkim@enterprise.co', company: 'Enterprise Co', currentScore: 91, predictedScore: 94, trend: 'RISING', conversionProbability: 0.92, topFactors: [{ factor: 'Requested proposal', impact: 25, direction: 'positive' }, { factor: 'Budget confirmed', impact: 20, direction: 'positive' }, { factor: 'Decision maker role', impact: 15, direction: 'positive' }], lastUpdated: new Date(Date.now() - 900000).toISOString() },
  { id: '5', leadId: 'l5', leadName: 'Lisa Wang', email: 'lwang@mediaco.com', company: 'MediaCo', currentScore: 55, predictedScore: 55, trend: 'STABLE', conversionProbability: 0.45, topFactors: [{ factor: 'Moderate email engagement', impact: 5, direction: 'positive' }, { factor: 'Good industry fit', impact: 7, direction: 'positive' }, { factor: 'Long sales cycle typical', impact: -3, direction: 'negative' }], lastUpdated: new Date(Date.now() - 5400000).toISOString() },
  { id: '6', leadId: 'l6', leadName: 'James Foster', email: 'jfoster@bigretail.com', company: 'BigRetail', currentScore: 82, predictedScore: 79, trend: 'STABLE', conversionProbability: 0.74, topFactors: [{ factor: 'Multi-user trial active', impact: 18, direction: 'positive' }, { factor: 'Integration request', impact: 10, direction: 'positive' }, { factor: 'Competitor evaluation', impact: -8, direction: 'negative' }], lastUpdated: new Date(Date.now() - 2700000).toISOString() },
];

/* ── Default Win Probabilities ───────────────────────────────── */
const DEFAULT_WIN_PROB: WinProbability[] = [
  { id: '1', opportunityId: 'o1', opportunityName: 'TechCorp Enterprise Deal', accountName: 'TechCorp', amount: 125000, stage: 'NEGOTIATION', winProbability: 82, historicalWinRate: 68, daysInStage: 12, riskFactors: ['Competitor also in final round'], positiveSignals: ['Champion identified', 'Budget approved', 'Legal review started'], recommendation: 'Schedule executive alignment call to close within 2 weeks', lastUpdated: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', opportunityId: 'o2', opportunityName: 'GlobalSoft Expansion', accountName: 'GlobalSoft', amount: 85000, stage: 'PROPOSAL', winProbability: 58, historicalWinRate: 52, daysInStage: 8, riskFactors: ['No response to last email', 'Price concerns raised'], positiveSignals: ['Good product fit', 'Multi-year interest'], recommendation: 'Address pricing concerns with ROI analysis and case study', lastUpdated: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', opportunityId: 'o3', opportunityName: 'MediaCo Platform License', accountName: 'MediaCo', amount: 210000, stage: 'NEEDS_ANALYSIS', winProbability: 35, historicalWinRate: 31, daysInStage: 22, riskFactors: ['Stalled in discovery', 'Multiple stakeholders', 'Budget not confirmed'], positiveSignals: ['Strong use case'], recommendation: 'Re-engage with updated discovery call; identify economic buyer', lastUpdated: new Date(Date.now() - 14400000).toISOString() },
  { id: '4', opportunityId: 'o4', opportunityName: 'BigRetail CRM Migration', accountName: 'BigRetail', amount: 340000, stage: 'QUALIFICATION', winProbability: 45, historicalWinRate: 40, daysInStage: 5, riskFactors: ['Early stage', 'Large org with slow procurement'], positiveSignals: ['Executive sponsor', 'Existing vendor dissatisfied', 'Timeline defined'], recommendation: 'Accelerate with POC proposal and reference customers', lastUpdated: new Date(Date.now() - 5400000).toISOString() },
  { id: '5', opportunityId: 'o5', opportunityName: 'StartupXYZ Starter Plan', accountName: 'StartupXYZ', amount: 18000, stage: 'PROSPECTING', winProbability: 22, historicalWinRate: 18, daysInStage: 30, riskFactors: ['Low engagement', 'Small budget', 'No urgency'], positiveSignals: ['Good initial interest'], recommendation: 'Nurture with content marketing; revisit in 30 days', lastUpdated: new Date(Date.now() - 21600000).toISOString() },
];

/* ── Default Forecasts ───────────────────────────────────────── */
const DEFAULT_FORECAST: SalesForecast[] = [
  { id: '1', period: '2026-Q1', periodLabel: 'Q1 2026', predictedRevenue: 1250000, bestCase: 1450000, worstCase: 980000, confidence: 'HIGH', pipelineValue: 2800000, weightedPipeline: 1680000, closedToDate: 890000, quota: 1500000, attainmentPct: 59, factors: ['Strong pipeline Q4 carry-over', '3 large deals in negotiation', 'Seasonal uptick expected'] },
  { id: '2', period: '2026-Q2', periodLabel: 'Q2 2026', predictedRevenue: 1380000, bestCase: 1620000, worstCase: 1050000, confidence: 'MEDIUM', pipelineValue: 3200000, weightedPipeline: 1920000, closedToDate: 0, quota: 1600000, attainmentPct: 0, factors: ['New product launch driving interest', 'Marketing campaign ramp-up', 'Hiring 2 new reps'] },
  { id: '3', period: '2026-Q3', periodLabel: 'Q3 2026', predictedRevenue: 1150000, bestCase: 1400000, worstCase: 850000, confidence: 'LOW', pipelineValue: 1800000, weightedPipeline: 1080000, closedToDate: 0, quota: 1600000, attainmentPct: 0, factors: ['Summer seasonality expected', 'Early pipeline still building', 'Depends on Q2 conversion'] },
  { id: '4', period: '2026-Q4', periodLabel: 'Q4 2026', predictedRevenue: 1550000, bestCase: 1850000, worstCase: 1100000, confidence: 'LOW', pipelineValue: 1200000, weightedPipeline: 720000, closedToDate: 0, quota: 1800000, attainmentPct: 0, factors: ['Year-end budget flush typical', 'Enterprise deals closing', 'Renewal cycle peak'] },
];

/* ── Default Churn Predictions ───────────────────────────────── */
const DEFAULT_CHURN: ChurnPrediction[] = [
  { id: '1', accountId: 'a1', accountName: 'OldCorp Industries', industry: 'Manufacturing', annualRevenue: 450000, riskLevel: 'CRITICAL', churnProbability: 0.89, riskFactors: ['No login in 45 days', 'Support tickets up 300%', 'Contract renewal in 30 days', 'Key contact left company'], lastActivityDays: 45, healthScore: 15, recommendedActions: ['Executive outreach call', 'Offer renewal incentive', 'Schedule training session'], predictedChurnDate: new Date(Date.now() + 86400000 * 30).toISOString() },
  { id: '2', accountId: 'a2', accountName: 'RetailMax', industry: 'Retail', annualRevenue: 280000, riskLevel: 'HIGH', churnProbability: 0.72, riskFactors: ['Usage dropped 60% in 3 months', 'Evaluating competitor', 'Unresolved billing dispute'], lastActivityDays: 21, healthScore: 32, recommendedActions: ['Address billing issue immediately', 'Share competitive comparison', 'Offer premium features trial'], predictedChurnDate: new Date(Date.now() + 86400000 * 60).toISOString() },
  { id: '3', accountId: 'a3', accountName: 'FinanceFirst', industry: 'Financial Services', annualRevenue: 180000, riskLevel: 'MEDIUM', churnProbability: 0.45, riskFactors: ['Feature requests unfulfilled', 'NPS score dropped'], lastActivityDays: 10, healthScore: 55, recommendedActions: ['Schedule product roadmap review', 'Assign dedicated CSM'], predictedChurnDate: new Date(Date.now() + 86400000 * 120).toISOString() },
  { id: '4', accountId: 'a4', accountName: 'HealthTech Pro', industry: 'Healthcare', annualRevenue: 520000, riskLevel: 'LOW', churnProbability: 0.12, riskFactors: [], lastActivityDays: 2, healthScore: 88, recommendedActions: ['Upsell opportunity: analytics module'], predictedChurnDate: undefined },
  { id: '5', accountId: 'a5', accountName: 'EduLearn Corp', industry: 'Education', annualRevenue: 95000, riskLevel: 'HIGH', churnProbability: 0.68, riskFactors: ['Budget cuts announced', 'Low adoption rate (15%)', 'Poor onboarding completion'], lastActivityDays: 18, healthScore: 28, recommendedActions: ['Offer adoption workshop', 'Connect with new budget owner', 'Provide usage optimization guide'], predictedChurnDate: new Date(Date.now() + 86400000 * 45).toISOString() },
];

/* ── Default Next Best Actions ───────────────────────────────── */
const DEFAULT_NBA: NextBestAction[] = [
  { id: '1', entityType: 'OPPORTUNITY', entityId: 'o1', entityName: 'TechCorp Enterprise Deal', category: 'CALL', action: 'Schedule executive alignment call with VP of Sales', reason: 'Deal is in final negotiation; executive buy-in will accelerate close', priority: 95, expectedImpact: 'Increase win probability by ~15%', dueDate: new Date(Date.now() + 86400000 * 2).toISOString(), completed: false, createdAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', entityType: 'LEAD', entityId: 'l3', entityName: 'Emily Rodriguez', category: 'EMAIL', action: 'Send personalized re-engagement email with case study', reason: 'Lead engagement declining; case study matches their industry', priority: 75, expectedImpact: 'Reactivate lead interest', dueDate: new Date(Date.now() + 86400000).toISOString(), completed: false, createdAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', entityType: 'ACCOUNT', entityId: 'a1', entityName: 'OldCorp Industries', category: 'RETENTION', action: 'Executive outreach: schedule renewal discussion', reason: 'Critical churn risk; contract expires in 30 days', priority: 98, expectedImpact: 'Prevent $450K annual revenue loss', dueDate: new Date(Date.now() + 86400000).toISOString(), completed: false, createdAt: new Date(Date.now() - 1800000).toISOString() },
  { id: '4', entityType: 'OPPORTUNITY', entityId: 'o2', entityName: 'GlobalSoft Expansion', category: 'PROPOSAL', action: 'Send ROI analysis addressing price objections', reason: 'Prospect raised pricing concerns; ROI data can overcome objection', priority: 82, expectedImpact: 'Move deal to negotiation stage', completed: false, createdAt: new Date(Date.now() - 14400000).toISOString() },
  { id: '5', entityType: 'ACCOUNT', entityId: 'a4', entityName: 'HealthTech Pro', category: 'UPSELL', action: 'Present analytics module to power users', reason: 'High engagement and health score; strong upsell candidate', priority: 60, expectedImpact: 'Potential $120K expansion revenue', completed: false, createdAt: new Date(Date.now() - 21600000).toISOString() },
  { id: '6', entityType: 'LEAD', entityId: 'l1', entityName: 'Sarah Johnson', category: 'MEETING', action: 'Book product demo — lead score rising fast', reason: 'Score jumped 16 pts in a week; high conversion probability', priority: 88, expectedImpact: 'Convert to qualified opportunity', dueDate: new Date(Date.now() + 86400000 * 3).toISOString(), completed: false, createdAt: new Date(Date.now() - 5400000).toISOString() },
  { id: '7', entityType: 'ACCOUNT', entityId: 'a2', entityName: 'RetailMax', category: 'FOLLOW_UP', action: 'Resolve billing dispute with finance team', reason: 'Unresolved billing issue driving churn risk', priority: 90, expectedImpact: 'Reduce churn probability by 30%', completed: true, createdAt: new Date(Date.now() - 86400000 * 2).toISOString() },
];

/* ── Default AI Report Insights ──────────────────────────────── */
const DEFAULT_REPORTS: AiReportInsight[] = [
  { id: '1', reportName: 'Pipeline Velocity', insightType: 'TREND', title: 'Average deal cycle is shortening', description: 'Average days to close decreased from 45 to 38 over the past quarter, driven primarily by improved qualification processes.', metric: 'Avg Days to Close', currentValue: 38, previousValue: 45, changePct: -15.6, recommendation: 'Document and standardize the qualification improvements for all reps.', generatedAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', reportName: 'Lead Conversion', insightType: 'ANOMALY', title: 'Webinar leads converting 3x higher', description: 'Leads from the recent AI webinar series are converting at 28% vs. the 9% average from other sources.', metric: 'Conversion Rate', currentValue: 28, previousValue: 9, changePct: 211, recommendation: 'Increase investment in webinar-based lead generation campaigns.', generatedAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', reportName: 'Revenue Forecast', insightType: 'PREDICTION', title: 'Q1 quota attainment at risk', description: 'Based on current pipeline velocity and win rates, the team is projected to reach 83% of Q1 quota.', metric: 'Quota Attainment', currentValue: 83, previousValue: 95, changePct: -12.6, recommendation: 'Focus on accelerating the 3 deals in negotiation stage worth $460K combined.', generatedAt: new Date(Date.now() - 14400000).toISOString() },
  { id: '4', reportName: 'Activity Analysis', insightType: 'RECOMMENDATION', title: 'Top performers make 40% more calls', description: 'Reps in the top 20% by revenue make an average of 28 calls per day vs. 20 for the rest of the team.', metric: 'Calls per Day', currentValue: 28, previousValue: 20, changePct: 40, recommendation: 'Implement structured calling blocks and provide talk tracks for underperformers.', generatedAt: new Date(Date.now() - 21600000).toISOString() },
  { id: '5', reportName: 'Customer Health', insightType: 'ALERT', title: '4 enterprise accounts showing risk signals', description: 'Four accounts with combined ARR of $1.2M are showing declining engagement and increased support ticket volume.', metric: 'At-Risk ARR', currentValue: 1200000, previousValue: 0, changePct: 0, recommendation: 'Trigger executive business reviews for all four accounts this week.', generatedAt: new Date(Date.now() - 43200000).toISOString() },
];

/* ── Default Data Entry Suggestions ──────────────────────────── */
const DEFAULT_SUGGESTIONS: DataEntrySuggestion[] = [
  { id: '1', entityType: 'LEAD', entityId: 'l2', entityName: 'Michael Chen', field: 'phone', suggestedValue: '+1 (415) 555-0142', confidence: 0.91, source: 'LinkedIn profile enrichment', createdAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', entityType: 'LEAD', entityId: 'l3', entityName: 'Emily Rodriguez', field: 'title', currentValue: '', suggestedValue: 'VP of Engineering', confidence: 0.87, source: 'Email signature parsing', createdAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', entityType: 'ACCOUNT', entityId: 'a3', entityName: 'FinanceFirst', field: 'numberOfEmployees', currentValue: '0', suggestedValue: '2,500', confidence: 0.82, source: 'Crunchbase data', createdAt: new Date(Date.now() - 14400000).toISOString() },
  { id: '4', entityType: 'CONTACT', entityId: 'c1', entityName: 'Anna Smith', field: 'department', currentValue: '', suggestedValue: 'Marketing', confidence: 0.78, source: 'Email domain analysis', createdAt: new Date(Date.now() - 21600000).toISOString() },
  { id: '5', entityType: 'OPPORTUNITY', entityId: 'o3', entityName: 'MediaCo Platform License', field: 'closeDate', currentValue: '2026-03-15', suggestedValue: '2026-04-15', confidence: 0.75, source: 'Stage duration analysis', createdAt: new Date(Date.now() - 43200000).toISOString() },
  { id: '6', entityType: 'LEAD', entityId: 'l5', entityName: 'Lisa Wang', field: 'company', currentValue: 'MediaCo', suggestedValue: 'MediaCo International Inc.', confidence: 0.93, source: 'Company registry lookup', createdAt: new Date(Date.now() - 54000000).toISOString() },
  { id: '7', entityType: 'ACCOUNT', entityId: 'a2', entityName: 'RetailMax', field: 'industry', currentValue: 'Retail', suggestedValue: 'E-Commerce & Retail', confidence: 0.88, source: 'Web scraping analysis', createdAt: new Date(Date.now() - 64800000).toISOString() },
];

/* ── Default Sales Insights ──────────────────────────────────── */
const DEFAULT_INSIGHTS: AiSalesInsight[] = [
  { id: '1', insightType: 'TREND', title: 'Enterprise segment growing 2x faster than SMB', summary: 'Enterprise deals ($100K+) increased 45% QoQ while SMB remained flat.', details: 'Analysis of 240 deals closed in Q4 shows the enterprise segment is outpacing SMB growth significantly. Average enterprise deal size also grew from $150K to $185K.', impactArea: 'Revenue', severity: 'high', actionable: true, relatedEntities: [{ type: 'OPPORTUNITY', id: 'o1', name: 'TechCorp Enterprise Deal' }, { type: 'OPPORTUNITY', id: 'o4', name: 'BigRetail CRM Migration' }], generatedAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', insightType: 'ANOMALY', title: 'Win rate dropped 12% for proposals sent on Fridays', summary: 'Proposals sent on Fridays have a 23% win rate vs. 35% on Tuesdays.', details: 'Timing analysis across 180 proposals shows a clear pattern: proposals sent Tuesday-Thursday have significantly higher engagement and win rates.', impactArea: 'Sales Process', severity: 'medium', actionable: true, relatedEntities: [], generatedAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', insightType: 'PREDICTION', title: 'March pipeline looks strong but Q2 needs attention', summary: 'Current March pipeline covers 120% of target, but Q2 pipeline is only at 60% coverage.', details: 'While immediate quota looks achievable, the Q2 pipeline ratio of 0.6x is below the healthy 3x benchmark. Prospecting activity needs to increase by at least 50%.', impactArea: 'Pipeline', severity: 'high', actionable: true, relatedEntities: [], generatedAt: new Date(Date.now() - 14400000).toISOString() },
  { id: '4', insightType: 'RECOMMENDATION', title: 'Cross-sell analytics module to top 20 accounts', summary: '20 accounts with high engagement scores have not adopted the analytics module.', details: 'These accounts average a health score of 85+ and have active power users. Based on similar accounts, the analytics module adoption rate is 72% when proactively offered.', impactArea: 'Expansion Revenue', severity: 'medium', actionable: true, relatedEntities: [{ type: 'ACCOUNT', id: 'a4', name: 'HealthTech Pro' }], generatedAt: new Date(Date.now() - 21600000).toISOString() },
  { id: '5', insightType: 'ALERT', title: 'Response time to new leads exceeds SLA', summary: 'Average first response time is 4.2 hours vs. the 1-hour SLA target.', details: 'Only 38% of new leads receive a response within the 1-hour window. Leads responded to within 1 hour convert at 2.5x the rate of those responded to after 4+ hours.', impactArea: 'Lead Management', severity: 'high', actionable: true, relatedEntities: [], generatedAt: new Date(Date.now() - 43200000).toISOString() },
];

/* ── Default Email Replies ───────────────────────────────────── */
const DEFAULT_EMAIL_REPLIES: EmailReply[] = [
  { id: '1', originalFrom: 'john.smith@techcorp.com', originalSubject: 'Inquiry about Enterprise Plan', replySubject: 'Re: Inquiry about Enterprise Plan', replyBody: 'Hi John,\n\nThank you for your interest in our Enterprise Plan. I\'d be happy to walk you through the features and pricing.\n\nOur Enterprise Plan includes unlimited users, advanced analytics, API access, and dedicated support. I\'ve attached a detailed comparison sheet.\n\nWould you be available for a 30-minute call this week to discuss your specific needs?\n\nBest regards,\nSales Team', tone: 'professional', suggestions: ['Consider adding a specific time slot for the call', 'Mention a customer success story from their industry'], createdAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', originalFrom: 'lisa.chen@globalsoft.io', originalSubject: 'Follow up on our demo', replySubject: 'Re: Follow up on our demo', replyBody: 'Hi Lisa,\n\nGreat to hear from you! I\'m glad the demo was helpful.\n\nTo address your questions:\n1. Yes, we support SSO integration with Okta and Azure AD\n2. Data migration typically takes 2-3 business days\n3. We offer a 30-day free trial of the premium tier\n\nI\'ll send over the technical documentation you requested. Let me know if you need anything else!\n\nBest,\nSales Team', tone: 'friendly', suggestions: ['Add a link to the documentation portal', 'Propose a follow-up meeting to discuss implementation timeline'], createdAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', originalFrom: 'mark.williams@startup.co', originalSubject: 'Pricing concerns', replySubject: 'Re: Pricing concerns', replyBody: 'Hi Mark,\n\nI understand budget is a key consideration for your team. Let me share some options that might work better.\n\nWe offer flexible annual billing with 20% savings, as well as a startup program with special pricing for companies under 50 employees. I can also create a custom package based on the features you need most.\n\nWould you like to schedule a call to explore these options?\n\nRegards,\nSales Team', tone: 'empathetic', suggestions: ['Include specific pricing tiers for comparison', 'Mention ROI data from similar-sized customers'], createdAt: new Date(Date.now() - 14400000).toISOString() },
];

/* ── Default Meeting Summaries ───────────────────────────────── */
const DEFAULT_MEETING_SUMMARIES: MeetingSummary[] = [
  { id: '1', meetingTitle: 'TechCorp Enterprise Deal - Requirements Review', meetingDate: new Date(Date.now() - 86400000).toISOString(), participants: ['Sarah Johnson (TechCorp)', 'David Kim (Sales)', 'Anna Chen (Solutions)'], summary: 'Reviewed TechCorp\'s requirements for enterprise CRM deployment. They need SSO integration, custom workflows, and API access for their internal tools. Budget approved at $125K annually. Timeline: go-live by Q2.', actionItems: ['Send SOW by Friday', 'Schedule technical deep-dive with their IT team', 'Prepare SSO integration documentation', 'Create custom demo environment'], keyDecisions: ['Annual contract at $125K (3-year term)', 'Phased rollout: Phase 1 core CRM, Phase 2 integrations', 'Dedicated implementation manager assigned'], crmUpdates: [{ entityType: 'OPPORTUNITY', entityId: 'o1', field: 'amount', suggestedValue: '125000', reason: 'Budget confirmed during meeting' }, { entityType: 'OPPORTUNITY', entityId: 'o1', field: 'stage', suggestedValue: 'NEGOTIATION', reason: 'SOW to be sent, moving to negotiation' }], createdAt: new Date(Date.now() - 86400000).toISOString() },
  { id: '2', meetingTitle: 'Weekly Pipeline Review', meetingDate: new Date(Date.now() - 172800000).toISOString(), participants: ['Sales Team', 'VP Sales'], summary: 'Reviewed Q1 pipeline: $2.8M total, $1.68M weighted. Three deals in final negotiation. Identified risk in GlobalSoft deal due to competitor evaluation. Action items assigned to accelerate key deals.', actionItems: ['Follow up on GlobalSoft pricing concerns', 'Schedule exec alignment for BigRetail', 'Update forecasts in CRM', 'Prepare competitive analysis for GlobalSoft'], keyDecisions: ['Discount approval up to 15% for GlobalSoft', 'Hire additional SDR for Q2 pipeline building', 'Weekly cadence continues'], crmUpdates: [], createdAt: new Date(Date.now() - 172800000).toISOString() },
  { id: '3', meetingTitle: 'MediaCo Discovery Call', meetingDate: new Date(Date.now() - 259200000).toISOString(), participants: ['James Miller (MediaCo)', 'Emily Rodriguez (Sales)'], summary: 'Initial discovery with MediaCo. They are looking to replace their current CRM (legacy system). Key pain points: poor reporting, no automation, slow performance. 500+ users across 3 offices. Decision expected by end of Q2.', actionItems: ['Send platform overview deck', 'Schedule technical assessment', 'Connect them with reference customer in media industry', 'Prepare ROI analysis'], keyDecisions: ['MediaCo will evaluate 3 vendors including us', 'Technical POC required before final decision'], crmUpdates: [{ entityType: 'OPPORTUNITY', entityId: 'o3', field: 'notes', suggestedValue: 'Evaluating 3 vendors. POC required. 500+ users.', reason: 'Key discovery information captured' }], createdAt: new Date(Date.now() - 259200000).toISOString() },
];

/* ── Default Auto-Leads ──────────────────────────────────────── */
const DEFAULT_AUTO_LEADS: AutoLead[] = [
  { id: '1', sourceType: 'EMAIL', sourceReference: 'inquiry@company.com - Enterprise CRM Interest', leadName: 'Robert Taylor', email: 'rtaylor@innovatetech.com', company: 'InnovateTech Solutions', title: 'CTO', phone: '+1 (555) 234-5678', notes: 'Mentioned they are evaluating CRM platforms for 200+ person sales team. Currently using spreadsheets. Budget cycle starts in April.', confidence: 0.92, status: 'PENDING', createdAt: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', sourceType: 'MEETING', sourceReference: 'Industry Conference 2026 - Booth Visit', leadName: 'Jessica Park', email: 'jpark@nexusretail.com', company: 'Nexus Retail Group', title: 'VP of Operations', notes: 'Met at conference booth. Interested in CRM with POS integration. Managing 50 retail locations. Wants to centralize customer data.', confidence: 0.78, status: 'PENDING', createdAt: new Date(Date.now() - 7200000).toISOString() },
  { id: '3', sourceType: 'EMAIL', sourceReference: 'Forward from partner - Referral', leadName: 'Ahmed Hassan', email: 'ahmed.h@globalfreight.co', company: 'Global Freight Logistics', title: 'Director of Sales', phone: '+44 20 7946 0958', notes: 'Referred by consulting partner. Looking for CRM to track shipping client relationships. 80-person sales team across Europe.', confidence: 0.85, status: 'APPROVED', createdAt: new Date(Date.now() - 86400000).toISOString() },
  { id: '4', sourceType: 'MEETING', sourceReference: 'Webinar: CRM Best Practices Q1 2026', leadName: 'Sarah Mitchell', email: 'smitchell@healthplus.org', company: 'HealthPlus Networks', title: 'Head of Business Development', notes: 'Asked multiple questions during webinar about healthcare compliance features. Follow-up requested.', confidence: 0.71, status: 'PENDING', createdAt: new Date(Date.now() - 14400000).toISOString() },
  { id: '5', sourceType: 'EMAIL', sourceReference: 'Support ticket escalation', leadName: 'Unknown Contact', email: 'info@smallbiz.co', company: 'SmallBiz Co', notes: 'Generic inquiry from company email. Could not extract individual name. Low confidence.', confidence: 0.35, status: 'REJECTED', createdAt: new Date(Date.now() - 172800000).toISOString() },
];

/* ── Service ─────────────────────────────────────────────────── */
export const aiInsightsService = {
  getLeadScores: (): PredictiveLeadScore[] => loadJson(K.leads, DEFAULT_LEAD_SCORES),
  fetchLeadScores: (): Promise<PredictiveLeadScore[]> =>
    tryApi(() => api.get<ApiResponse<PredictiveLeadScore[]>>(`${INSIGHTS}/lead-scores`).then(r => r.data.data!), loadJson(K.leads, DEFAULT_LEAD_SCORES)),
  saveLeadScores: (d: PredictiveLeadScore[]) => saveJson(K.leads, d),

  getWinProbabilities: (): WinProbability[] => loadJson(K.winProb, DEFAULT_WIN_PROB),
  fetchWinProbabilities: (): Promise<WinProbability[]> =>
    tryApi(() => api.get<ApiResponse<WinProbability[]>>(`${INSIGHTS}/win-probability`).then(r => r.data.data!), loadJson(K.winProb, DEFAULT_WIN_PROB)),
  saveWinProbabilities: (d: WinProbability[]) => saveJson(K.winProb, d),

  getForecasts: (): SalesForecast[] => loadJson(K.forecast, DEFAULT_FORECAST),
  fetchForecasts: (): Promise<SalesForecast[]> =>
    tryApi(() => api.get<ApiResponse<SalesForecast[]>>(`${INSIGHTS}/forecasts`).then(r => r.data.data!), loadJson(K.forecast, DEFAULT_FORECAST)),
  saveForecasts: (d: SalesForecast[]) => saveJson(K.forecast, d),

  getChurnPredictions: (): ChurnPrediction[] => loadJson(K.churn, DEFAULT_CHURN),
  fetchChurnPredictions: (): Promise<ChurnPrediction[]> =>
    tryApi(() => api.get<ApiResponse<ChurnPrediction[]>>(`${INSIGHTS}/churn-predictions`).then(r => r.data.data!), loadJson(K.churn, DEFAULT_CHURN)),
  saveChurnPredictions: (d: ChurnPrediction[]) => saveJson(K.churn, d),

  getNextBestActions: (): NextBestAction[] => loadJson(K.nba, DEFAULT_NBA),
  saveNextBestActions: (d: NextBestAction[]) => saveJson(K.nba, d),

  getReportInsights: (): AiReportInsight[] => loadJson(K.reports, DEFAULT_REPORTS),
  fetchReportInsights: (): Promise<AiReportInsight[]> =>
    tryApi(() => api.get<ApiResponse<AiReportInsight[]>>(`${INSIGHTS}/reports`).then(r => r.data.data!), loadJson(K.reports, DEFAULT_REPORTS)),
  saveReportInsights: (d: AiReportInsight[]) => saveJson(K.reports, d),

  getSuggestions: (): DataEntrySuggestion[] => loadJson(K.suggestions, DEFAULT_SUGGESTIONS),
  fetchSuggestions: (): Promise<DataEntrySuggestion[]> =>
    tryApi(() => api.get<ApiResponse<DataEntrySuggestion[]>>(`${INSIGHTS}/suggestions`).then(r => r.data.data!), loadJson(K.suggestions, DEFAULT_SUGGESTIONS)),
  saveSuggestions: (d: DataEntrySuggestion[]) => saveJson(K.suggestions, d),

  getSalesInsights: (): AiSalesInsight[] => loadJson(K.insights, DEFAULT_INSIGHTS),
  fetchSalesInsights: (): Promise<AiSalesInsight[]> =>
    tryApi(() => api.get<ApiResponse<AiSalesInsight[]>>(`${INSIGHTS}/sales`).then(r => r.data.data!), loadJson(K.insights, DEFAULT_INSIGHTS)),
  saveSalesInsights: (d: AiSalesInsight[]) => saveJson(K.insights, d),

  // ── Email Replies ──
  getEmailReplies: (): EmailReply[] => loadJson(K.emailReplies, DEFAULT_EMAIL_REPLIES),
  fetchEmailReplies: (): Promise<EmailReply[]> =>
    tryApi(() => api.get<ApiResponse<EmailReply[]>>(`${INSIGHTS}/email-replies`).then(r => r.data.data!), loadJson(K.emailReplies, DEFAULT_EMAIL_REPLIES)),
  saveEmailReplies: (d: EmailReply[]) => saveJson(K.emailReplies, d),

  // ── Meeting Summaries ──
  getMeetingSummaries: (): MeetingSummary[] => loadJson(K.meetingSummaries, DEFAULT_MEETING_SUMMARIES),
  fetchMeetingSummaries: (): Promise<MeetingSummary[]> =>
    tryApi(() => api.get<ApiResponse<MeetingSummary[]>>(`${INSIGHTS}/meeting-summaries`).then(r => r.data.data!), loadJson(K.meetingSummaries, DEFAULT_MEETING_SUMMARIES)),
  saveMeetingSummaries: (d: MeetingSummary[]) => saveJson(K.meetingSummaries, d),

  // ── Auto-Leads ──
  getAutoLeads: (): AutoLead[] => loadJson(K.autoLeads, DEFAULT_AUTO_LEADS),
  fetchAutoLeads: (): Promise<AutoLead[]> =>
    tryApi(() => api.get<ApiResponse<AutoLead[]>>(`${INSIGHTS}/auto-leads`).then(r => r.data.data!), loadJson(K.autoLeads, DEFAULT_AUTO_LEADS)),
  saveAutoLeads: (d: AutoLead[]) => saveJson(K.autoLeads, d),
};
