import OpenAI from 'openai';
import { config } from '../config';
import { logger } from '../utils/logger';
import type { ConversationHistoryItem } from '../utils/schemaValidator';
import { getMetadataSummary } from '../models/crmMetadata';

// ─── OpenAI Client ───────────────────────────────────────────────────────────

const openai = new OpenAI({ apiKey: config.openai.apiKey });

// ─── System Prompt ───────────────────────────────────────────────────────────

const BASE_SYSTEM_PROMPT = `You are an AI CRM configuration and operations agent.

Convert user instructions into structured JSON commands.
Always return ONLY valid JSON — no markdown, no backticks, no explanation outside JSON.

Supported actions:

  --- CRM RECORD OPERATIONS ---
  create_record        — Create an actual CRM record (lead, contact, account, opportunity, activity)

  --- SCHEMA / CONFIGURATION ---
  create_object        — Create a new CRM object/entity definition
  create_field         — Add a field to an existing object
  create_relationship  — Link two objects
  create_workflow      — Create an automation workflow with triggers and actions
  create_pipeline      — Create a new pipeline
  create_pipeline_stage— Add a stage to an existing pipeline
  create_dashboard     — Create a dashboard with widgets
  create_role          — Create a role
  create_permission    — Set permissions for a role on an object
  create_automation_rule — Create a validation/assignment/escalation rule

Record types for create_record: lead, account, contact, opportunity, activity

Lead fields:     firstName (required), lastName (required), email, phone, company, title, source (WEB|PHONE|EMAIL|REFERRAL|SOCIAL_MEDIA|TRADE_SHOW|OTHER), description
Account fields:  name (required), industry, website, phone, billingAddress, shippingAddress, annualRevenue, numberOfEmployees, description
Contact fields:  firstName (required), lastName (required), email, phone, mobilePhone, title, department, accountId, mailingAddress, description
Opportunity fields: name (required), accountId, contactId, amount, stage (PROSPECTING|QUALIFICATION|NEEDS_ANALYSIS|PROPOSAL|NEGOTIATION|CLOSED_WON|CLOSED_LOST), probability, closeDate (YYYY-MM-DD), description, forecastCategory (PIPELINE|BEST_CASE|COMMIT|CLOSED)

Field types: text, textarea, number, currency, percent, date, datetime, boolean,
             email, phone, url, picklist, multi_picklist, lookup, formula, auto_number

Trigger types: on_create, on_update, field_change, scheduled

Workflow action types: send_email, update_field, create_task, notify, webhook

Rule types: validation, assignment, escalation, auto_response

Widget types: chart, metric, table, list

Relationship types: one_to_one, one_to_many, many_to_many

Examples:

User: Create a lead with name John Doe and email john@example.com
Output:
{"action":"create_record","record_type":"lead","fields":{"firstName":"John","lastName":"Doe","email":"john@example.com"}}

User: Create a lead John Smith from Acme Corp, phone 555-1234
Output:
{"action":"create_record","record_type":"lead","fields":{"firstName":"John","lastName":"Smith","company":"Acme Corp","phone":"555-1234"}}

User: Create an account named Acme Corporation in Technology industry
Output:
{"action":"create_record","record_type":"account","fields":{"name":"Acme Corporation","industry":"Technology"}}

User: Create a contact Jane Doe at jane@acme.com
Output:
{"action":"create_record","record_type":"contact","fields":{"firstName":"Jane","lastName":"Doe","email":"jane@acme.com"}}

User: Create an opportunity Big Deal worth 50000
Output:
{"action":"create_record","record_type":"opportunity","fields":{"name":"Big Deal","amount":50000,"stage":"PROSPECTING"}}

User: Create a custom object called Project with fields for name, budget, start date, status, and priority
Output:
{"action":"create_object","name":"Project","label":"Project","fields":[{"name":"name","type":"text","label":"Name"},{"name":"budget","type":"currency","label":"Budget"},{"name":"start_date","type":"date","label":"Start Date"},{"name":"status","type":"picklist","label":"Status","options":["Planning","Active","On Hold","Completed"]},{"name":"priority","type":"picklist","label":"Priority","options":["Low","Medium","High","Critical"]}]}

User: Create a custom object called Billing with fields: patient_name (text, required), appointment_date (date, required), service_description (text), amount (currency, required), payment_status (picklist with options Pending, Paid, Overdue), insurance_claim_id (text), payment_method (picklist with options Cash, Card, Insurance)
Output:
{"action":"create_object","name":"Billing","label":"Billing","fields":[{"name":"patient_name","type":"text","label":"Patient Name","required":true},{"name":"appointment_date","type":"date","label":"Appointment Date","required":true},{"name":"service_description","type":"text","label":"Service Description"},{"name":"amount","type":"currency","label":"Amount","required":true},{"name":"payment_status","type":"picklist","label":"Payment Status","options":["Pending","Paid","Overdue"]},{"name":"insurance_claim_id","type":"text","label":"Insurance Claim ID"},{"name":"payment_method","type":"picklist","label":"Payment Method","options":["Cash","Card","Insurance"]}]}

User: Create a field called Budget in Leads
Output:
{"action":"create_field","object":"Lead","field_name":"Budget","field_type":"currency","label":"Budget"}

User: Add pipeline stage Technical Review
Output:
{"action":"create_pipeline_stage","pipeline":"Sales Pipeline","name":"Technical Review","probability":40}

User: Create a workflow that sends email when lead becomes qualified
Output:
{"action":"create_workflow","name":"Email on Lead Qualified","object":"Lead","trigger_type":"field_change","trigger_config":{"field":"status","new_value":"qualified"},"conditions":[],"actions":[{"type":"send_email","config":{"template":"lead_qualified","to":"{{owner_email}}"}}]}

User: Create dashboard showing monthly revenue
Output:
{"action":"create_dashboard","name":"Monthly Revenue Dashboard","description":"Shows monthly revenue metrics","widgets":[{"title":"Monthly Revenue","widget_type":"chart","config":{"chart_type":"bar","data_source":"Opportunity","metric":"amount","group_by":"close_date","period":"monthly","filter":{"stage":"Closed Won"}},"position":{"x":0,"y":0,"w":12,"h":4}}]}

IMPORTANT: When the user says "create a lead", "add a contact", "new account", etc., they want to create an actual DATA RECORD, not a schema object. Use create_record for these. Only use create_object when they explicitly say "create object" or "create entity".
`;

// ─── Build System Prompt with Live Metadata ──────────────────────────────────

async function buildSystemPrompt(): Promise<string> {
  try {
    const metadata = await getMetadataSummary();
    return `${BASE_SYSTEM_PROMPT}\n\n${metadata}\n\nUse the above metadata to validate object names, field names, and pipeline names. Reference existing objects by their exact names.`;
  } catch (err) {
    logger.warn('Could not load CRM metadata for system prompt', { error: err });
    return BASE_SYSTEM_PROMPT;
  }
}

// ─── Fallback Parser (when OpenAI is unavailable) ────────────────────────────

function fallbackParseInstruction(instruction: string): Record<string, unknown> {
  const lower = instruction.toLowerCase();

  // Detect "create a record" style instructions
  if (/create\s+(a\s+)?lead\b/.test(lower) && !/create\s+(a\s+)?(custom\s+)?object/.test(lower)) {
    const names = instruction.match(/(?:lead|named?)\s+(\w+)\s+(\w+)/i);
    const email = instruction.match(/email\s+(\S+@\S+)/i);
    const company = instruction.match(/(?:from|company|at)\s+([A-Z][\w\s]+?)(?:,|\.|$)/i);
    return {
      action: 'create_record', record_type: 'lead',
      fields: {
        firstName: names?.[1] ?? 'New', lastName: names?.[2] ?? 'Lead',
        ...(email ? { email: email[1] } : {}),
        ...(company ? { company: company[1].trim() } : {}),
      },
    };
  }

  // Detect pipeline creation
  if (/create\s+(a\s+)?(sales\s+)?pipeline/.test(lower)) {
    const pipelineName = instruction.match(/pipeline\s+(?:called\s+|named\s+)?(.+?)(?:\s+with|\s*$)/i)?.[1] ?? 'New Pipeline';
    const stageMatches = [...instruction.matchAll(/(\w[\w\s]+?)\s*\((\d+)%?\)/g)];
    const stages = stageMatches.length > 0
      ? stageMatches.map((m, i) => ({
          name: m[1].trim(), probability: parseInt(m[2]), sortOrder: i + 1,
          ...(parseInt(m[2]) === 100 ? { is_won: true, is_closed: true } : {}),
          ...(parseInt(m[2]) === 0 ? { is_closed: true } : {}),
        }))
      : [{ name: 'Qualification', probability: 20, sortOrder: 1 },
         { name: 'Proposal', probability: 50, sortOrder: 2 },
         { name: 'Negotiation', probability: 75, sortOrder: 3 },
         { name: 'Closed Won', probability: 100, sortOrder: 4, is_won: true, is_closed: true },
         { name: 'Closed Lost', probability: 0, sortOrder: 5, is_closed: true }];
    return { action: 'create_pipeline', name: pipelineName, object: 'Opportunity', stages };
  }

  // Detect workflow creation
  if (/create\s+(a\s+)?workflow/.test(lower) || /when\s+(a|an)\s+/.test(lower)) {
    const workflowName = instruction.match(/workflow\s+(?:called\s+|named\s+|that\s+)?(.+?)(?:\s+when|\s*$)/i)?.[1] ?? 'AI Generated Workflow';
    return {
      action: 'create_workflow', name: workflowName.length > 60 ? 'AI Generated Workflow' : workflowName,
      object: /lead/i.test(instruction) ? 'Lead' : /opportunity/i.test(instruction) ? 'Opportunity' : /contact/i.test(instruction) ? 'Contact' : 'Lead',
      trigger_type: /created|new/i.test(instruction) ? 'on_create' : /change|update/i.test(instruction) ? 'field_change' : 'on_create',
      trigger_config: {}, conditions: [],
      actions: [{ type: /email/i.test(instruction) ? 'send_email' : /task/i.test(instruction) ? 'create_task' : 'notify',
                  config: {} }],
    };
  }

  // Detect custom object creation
  if (/create\s+(a\s+)?(custom\s+)?object/.test(lower)) {
    const objectName = instruction.match(/object\s+(?:called\s+|named\s+)?(\w+)/i)?.[1] ?? 'CustomObject';
    const fieldPhrases = instruction.match(/fields?\s+(?:for\s+|:?\s*)(.+)/i)?.[1] ?? '';
    const fieldNames = fieldPhrases.split(/,\s*|\s+and\s+/).map(f => f.trim()).filter(f => f.length > 0);
    const fields = fieldNames.map(name => {
      const lower = name.toLowerCase();
      let fieldType = 'text';
      if (/budget|amount|price|cost|revenue/.test(lower)) fieldType = 'currency';
      else if (/date|start|end|due/.test(lower)) fieldType = 'date';
      else if (/status|priority|type|category/.test(lower)) fieldType = 'picklist';
      else if (/email/.test(lower)) fieldType = 'email';
      else if (/phone/.test(lower)) fieldType = 'phone';
      else if (/url|website|link/.test(lower)) fieldType = 'url';
      else if (/number|count|quantity/.test(lower)) fieldType = 'number';
      return { name: name.replace(/\s+/g, '_').toLowerCase(), type: fieldType, label: name };
    });
    return { action: 'create_object', name: objectName, fields };
  }

  // Detect field creation
  if (/add\s+(a\s+)?field|create\s+(a\s+)?field/.test(lower)) {
    const fieldName = instruction.match(/field\s+(?:called\s+|named\s+)?['"]?(\w[\w\s]*?)['"]?\s+(?:to|in|on)/i)?.[1] ?? 'new_field';
    const object = instruction.match(/(?:to|in|on)\s+(?:the\s+)?(\w+)/i)?.[1] ?? 'Lead';
    return { action: 'create_field', object, field_name: fieldName.replace(/\s+/g, '_').toLowerCase(), field_type: 'text', label: fieldName };
  }

  // Default: create_object
  return {
    action: 'create_object', name: 'CustomObject',
    fields: [{ name: 'name', type: 'text', label: 'Name' }],
  };
}

// ─── Parse Instruction ───────────────────────────────────────────────────────

export async function parseInstruction(
  instruction: string,
  history: ConversationHistoryItem[] = [],
): Promise<Record<string, unknown>> {
  const systemPrompt = await buildSystemPrompt();

  const messages: OpenAI.Chat.Completions.ChatCompletionMessageParam[] = [
    { role: 'system', content: systemPrompt },
    ...history.map((h) => ({
      role: h.role as 'user' | 'assistant' | 'system',
      content: h.content,
    })),
    { role: 'user', content: instruction },
  ];

  logger.info('Sending instruction to OpenAI', { model: config.openai.model, instruction });

  try {
    const response = await openai.chat.completions.create({
      model: config.openai.model,
      messages,
      temperature: 0.1,
      max_tokens: 4096,
      response_format: { type: 'json_object' },
    });

    const content = response.choices[0]?.message?.content;
    if (!content) {
      throw new Error('OpenAI returned empty response');
    }

    logger.debug('OpenAI raw response', { content });

    try {
      return JSON.parse(content);
    } catch {
      throw new Error(`Failed to parse AI response as JSON: ${content.slice(0, 300)}`);
    }
  } catch (err: any) {
    logger.warn('OpenAI call failed, using fallback parser', { error: err.message });
    return fallbackParseInstruction(instruction);
  }
}

// ─── Generate Human-Readable Confirmation ────────────────────────────────────

export async function generateConfirmation(command: Record<string, unknown>): Promise<string> {
  try {
    const response = await openai.chat.completions.create({
      model: config.openai.model,
      messages: [
        {
          role: 'system',
          content:
            'You are a CRM configuration assistant. Given a JSON config command, produce a short human-readable confirmation message. Be concise. Start with "I will ..."',
        },
        { role: 'user', content: JSON.stringify(command) },
      ],
      temperature: 0.3,
      max_tokens: 200,
    });

    return response.choices[0]?.message?.content ?? 'Please confirm the action.';
  } catch (err: any) {
    logger.warn('OpenAI confirmation call failed, using fallback', { error: err.message });
    const action = String(command.action ?? 'perform action');
    const name = String(command.name ?? command.record_type ?? '');
    return `I will ${action.replace(/_/g, ' ')}${name ? ': ' + name : ''}. Please confirm.`;
  }
}
