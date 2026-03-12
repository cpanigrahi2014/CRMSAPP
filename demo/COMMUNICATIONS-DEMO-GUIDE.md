# CRMS Platform — Communications Features: Testing & Demo Guide

> Covers: Built-in Email, WhatsApp, SMS, Calling & Call Recording, AI Transcription, AI Sentiment Analysis, Conversation Timeline, Multi-Channel Inbox

---

## Prerequisites

1. **All services running**: `docker compose up -d`
2. **Demo data seeded**: `.\demo\seed-demo.ps1`
3. **Communication data seeded**: `.\demo\seed-communications.ps1`
4. **App URL**: [http://localhost:3000](http://localhost:3000)
5. **Login**: `sarah.chen@acmecorp.com` / `Demo@2026!` / Tenant: `default`

### Service Ports Reference

| Service              | Port  | Base URL                             |
|----------------------|-------|--------------------------------------|
| Auth Service         | 8081  | `http://localhost:8081/api/v1/auth`  |
| Notification Service | 9087  | `http://localhost:9087/api/v1/communications` |
| Email Service        | 9090  | `http://localhost:9090/api/v1/email` |
| AI Service           | 9089  | `http://localhost:9089/api/v1/ai`    |
| Contact Service      | 9084  | `http://localhost:9084/api/v1/contacts` |

---

## Feature 1: Built-in Email System

### API Testing (cURL / Postman)

#### Step 1 — Get Auth Token
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sarah.chen@acmecorp.com","password":"Demo@2026!","tenantId":"default"}'
```
Save the `accessToken` from response as `$TOKEN`.

#### Step 2 — Send an Email
```bash
curl -X POST http://localhost:9090/api/v1/email/messages/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAddress": "sarah.chen@acmecorp.com",
    "toAddresses": ["david.kim@techvista.com"],
    "subject": "Partnership Proposal - TechVista x CRMS",
    "bodyHtml": "<h2>Hi David,</h2><p>Following up on our discovery call. Attached is the partnership proposal.</p><p>Best, Sarah</p>",
    "bodyText": "Hi David, Following up on our discovery call. Attached is the partnership proposal. Best, Sarah",
    "relatedEntityType": "CONTACT",
    "relatedEntityId": "<contactId>"
  }'
```

#### Step 3 — View Inbox
```bash
curl -X GET "http://localhost:9090/api/v1/email/messages/inbox?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 4 — View Sent Items
```bash
curl -X GET "http://localhost:9090/api/v1/email/messages/sent?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 5 — View Email Thread
```bash
curl -X GET "http://localhost:9090/api/v1/email/messages/thread/{threadId}" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 6 — Search Emails
```bash
curl -X GET "http://localhost:9090/api/v1/email/messages/search?query=proposal" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 7 — View Email Analytics
```bash
curl -X GET "http://localhost:9090/api/v1/email/analytics" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 8 — Email Templates
```bash
# List templates
curl -X GET "http://localhost:9090/api/v1/email/templates" \
  -H "Authorization: Bearer $TOKEN"

# Create a template
curl -X POST http://localhost:9090/api/v1/email/templates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo Follow-Up",
    "subject": "Great connecting at the demo, {{contactName}}!",
    "bodyHtml": "<p>Hi {{contactName}},</p><p>Thanks for attending the demo. Here are the next steps for {{companyName}}...</p>",
    "category": "SALES"
  }'
```

### UI Demo Steps
1. Navigate to **Email** in the sidebar
2. View **Inbox** tab — shows all received emails
3. View **Sent** tab — shows all outbound emails
4. Click **Compose** → fill in To, Subject, Body → click **Send**
5. Click on any email to view the full thread
6. Navigate to **Templates** tab → show pre-built templates
7. Navigate to **Analytics** tab → show open rates, click rates, delivery stats

### What to Verify
- [ ] Email appears in Sent items after sending
- [ ] Email threads group related messages correctly
- [ ] Templates list all 5 seeded templates
- [ ] Analytics show send/open/click metrics
- [ ] Search finds emails by keyword
- [ ] Emails linked to contacts/accounts appear in entity view

---

## Feature 2: Built-in WhatsApp Integration

### API Testing

#### Step 1 — Send a WhatsApp Message
```bash
curl -X POST http://localhost:9087/api/v1/communications/whatsapp/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromNumber": "+1-555-100-0001",
    "toNumber": "+1-415-555-1001",
    "body": "Hi David, this is Sarah from CRMS. Just wanted to confirm our meeting tomorrow at 10 AM. Looking forward to it!",
    "messageType": "TEXT",
    "relatedEntityType": "CONTACT",
    "relatedEntityId": "<contactId>"
  }'
```

#### Step 2 — Send a WhatsApp Media Message
```bash
curl -X POST http://localhost:9087/api/v1/communications/whatsapp/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromNumber": "+1-555-100-0001",
    "toNumber": "+1-415-555-1001",
    "body": "Here is the proposal document we discussed.",
    "messageType": "DOCUMENT",
    "mediaUrl": "https://example.com/docs/proposal.pdf",
    "mediaType": "application/pdf",
    "relatedEntityType": "OPPORTUNITY",
    "relatedEntityId": "<opportunityId>"
  }'
```

#### Step 3 — List WhatsApp Messages
```bash
curl -X GET "http://localhost:9087/api/v1/communications/whatsapp?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 4 — View Conversation by Phone Number
```bash
curl -X GET "http://localhost:9087/api/v1/communications/whatsapp/number/+1-415-555-1001" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 5 — Mark Message as Read
```bash
curl -X POST "http://localhost:9087/api/v1/communications/whatsapp/{messageId}/read" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Communications** in the sidebar
2. Click the **WhatsApp** tab
3. View list of WhatsApp conversations with delivery status indicators
4. Click **New Message** → enter phone number + message → Send
5. Click on a conversation to see the chat-style message thread
6. Demonstrate read receipts (✓✓ blue ticks for READ status)
7. Send a media message (image/document) and verify media type icon

### What to Verify
- [ ] Outbound messages show status: PENDING → SENT → DELIVERED
- [ ] Messages grouped by phone number in conversation view
- [ ] Media messages display correct type (IMAGE, DOCUMENT, etc.)
- [ ] Read receipts update when marked as read
- [ ] Messages are linked to the correct contact/entity

---

## Feature 3: Built-in SMS Messaging

### API Testing

#### Step 1 — Send an SMS
```bash
curl -X POST http://localhost:9087/api/v1/communications/sms/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromNumber": "+1-555-100-0001",
    "toNumber": "+1-310-555-2001",
    "body": "Hi Amanda, quick reminder about our pipeline review meeting tomorrow at 2 PM. See you there! - Sarah",
    "relatedEntityType": "CONTACT",
    "relatedEntityId": "<contactId>"
  }'
```

#### Step 2 — List All SMS Messages
```bash
curl -X GET "http://localhost:9087/api/v1/communications/sms?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 3 — View SMS Conversation by Number
```bash
curl -X GET "http://localhost:9087/api/v1/communications/sms/number/+1-310-555-2001" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 4 — Get SMS by ID
```bash
curl -X GET "http://localhost:9087/api/v1/communications/sms/{messageId}" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Communications** → **SMS** tab
2. View all SMS messages with status badges (SENT, DELIVERED, FAILED)
3. Click **New SMS** → enter phone number + message text → Send
4. View conversation thread grouped by phone number
5. Show direction indicators (↗ outbound, ↙ inbound)
6. Demonstrate delivery status tracking

### What to Verify
- [ ] SMS sent successfully and appears in list
- [ ] Status transitions: PENDING → SENDING → SENT → DELIVERED
- [ ] Conversation view groups messages by phone number
- [ ] Inbound vs outbound direction is clearly displayed
- [ ] Messages are linked to correct entity (Contact/Account)

---

## Feature 4: Built-in Calling & Call Recording

### API Testing

#### Step 1 — Initiate a Call
```bash
curl -X POST http://localhost:9087/api/v1/communications/calls/initiate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromNumber": "+1-555-100-0001",
    "toNumber": "+1-415-555-1001",
    "direction": "OUTBOUND",
    "relatedEntityType": "CONTACT",
    "relatedEntityId": "<contactId>",
    "notes": "Discovery call with David Kim - TechVista platform licensing"
  }'
```

#### Step 2 — Update Call (Simulate Answering)
```bash
curl -X PUT "http://localhost:9087/api/v1/communications/calls/{callId}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "answeredAt": "2026-03-09T10:01:30"
  }'
```

#### Step 3 — End the Call (With Recording)
```bash
curl -X POST "http://localhost:9087/api/v1/communications/calls/{callId}/end" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "durationSeconds": 1845,
    "recordingUrl": "https://recordings.crms.io/calls/2026-03-09-david-kim.mp3",
    "recordingDurationSeconds": 1845,
    "callOutcome": "Positive - Interested in enterprise plan, requesting proposal",
    "notes": "David confirmed budget approval. Wants custom API integration. Follow up with technical proposal by Friday."
  }'
```

#### Step 4 — List All Calls
```bash
curl -X GET "http://localhost:9087/api/v1/communications/calls?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 5 — View Call History by Phone Number
```bash
curl -X GET "http://localhost:9087/api/v1/communications/calls/number/+1-415-555-1001" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Communications** → **Calls** tab
2. View call log with duration, status, and outcome columns
3. Click **New Call** → enter number → click **Initiate Call**
4. Show the call in RINGING → IN_PROGRESS status
5. Click **End Call** → fill in outcome and notes
6. Show the completed call with recording link (🔴 icon)
7. Click recording link to play back the call audio
8. Demonstrate call history filtered by phone number

### What to Verify
- [ ] Call lifecycle: INITIATED → RINGING → IN_PROGRESS → COMPLETED
- [ ] Duration tracked accurately (startedAt, answeredAt, endedAt)
- [ ] Recording URL saved and accessible
- [ ] Call outcome and notes persisted
- [ ] Voicemail entries shown for NO_ANSWER/VOICEMAIL status
- [ ] Call history shows all calls for a given number

---

## Feature 5: AI Conversation Transcription

### API Testing

#### Step 1 — Transcribe a Call Recording
```bash
curl -X POST http://localhost:9089/api/v1/ai/transcribe \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Sarah: Good morning David, thanks for taking the time to chat today. David: Of course, we have been looking forward to this. Sarah: So, I wanted to go over the platform licensing options. We have three tiers - Starter, Professional, and Enterprise. Based on your team size of 200 users, I would recommend our Enterprise plan. David: That sounds right. What does the pricing look like for enterprise? Sarah: For 200 seats, we are looking at $450,000 annually. That includes premium support, custom API access, and dedicated account management. David: That is within our budget range. Our CTO has already approved up to $500K for this initiative. Sarah: Wonderful! I will have a detailed proposal sent over by Friday. David: Perfect. Can we also discuss the API integration timeline? We need to connect with our existing ERP system. Sarah: Absolutely. Our integration team can have a custom connector built within 6 weeks of contract signing.",
    "sourceType": "CALL_RECORDING",
    "speakers": ["Sarah Chen", "David Kim"],
    "language": "en"
  }'
```
**Expected Response**:
```json
{
  "id": "uuid",
  "fullTranscript": "Formatted transcript...",
  "segments": [
    { "speaker": "Sarah Chen", "text": "Good morning David...", "timestamp": "00:00:00" },
    { "speaker": "David Kim", "text": "Of course, we have been...", "timestamp": "00:00:15" }
  ],
  "keyTopics": ["Platform licensing", "Enterprise plan", "Pricing", "API integration", "ERP connector"],
  "summary": "Sarah presented enterprise licensing at $450K/year for 200 seats. David confirmed budget approval up to $500K. Proposal to be sent by Friday, with 6-week API integration timeline discussed.",
  "language": "en"
}
```

#### Step 2 — Transcribe a Meeting
```bash
curl -X POST http://localhost:9089/api/v1/ai/transcribe \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "James: Let us review the Q1 pipeline. Emily, how is the GreenEnergy deal looking? Emily: GreenEnergy is in negotiation at $1.2 million. They want volume discounts on the fleet management module. James: What is our floor price? Emily: We can go down to $1.05 million and still maintain 65% margin. James: Good. Michael, status on GlobalRetail? Michael: They are at proposal stage - $850K. Main blocker is their IT team wants a security audit first. James: Schedule that for next week. Lisa, any updates on the EduPath deal? Lisa: Still in prospecting. Budget approval is pending on their end. I am following up weekly.",
    "sourceType": "MEETING",
    "speakers": ["James Wilson", "Emily Rodriguez", "Michael Park", "Lisa Thompson"],
    "language": "en"
  }'
```

### UI Demo Steps
1. Navigate to **Communications** → **AI Transcription** tab
2. Paste or upload a call recording transcript
3. Select source type: CALL_RECORDING / MEETING / VOICEMAIL
4. Click **Transcribe**
5. View the structured output:
   - **Full Transcript** with speaker labels
   - **Segments** with timestamps per speaker
   - **Key Topics** extracted automatically
   - **Summary** of the conversation
6. Show how transcription links back to the original call record

### What to Verify
- [ ] Transcription returns structured segments with speaker labels
- [ ] Key topics are accurately extracted
- [ ] Summary is concise and captures main points
- [ ] Source type and source ID are correctly linked
- [ ] Multi-speaker conversations are correctly attributed
- [ ] Timestamps are assigned to each segment

---

## Feature 6: AI Call Sentiment Analysis

### API Testing

#### Step 1 — Analyze a Positive Call
```bash
curl -X POST http://localhost:9089/api/v1/ai/sentiment-analysis \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Sarah: Good morning David, thanks for taking the time to chat today. David: Of course, we have been really excited about this partnership. Your platform demo last week was impressive. Sarah: That is great to hear! Let me walk you through the pricing. David: Perfect. Honestly, the features you showed align exactly with what our CTO has been requesting. Sarah: For 200 seats at the Enterprise tier, we are looking at $450K annually. David: That is within our approved budget. Our CTO has signed off on up to $500K. When can we get the paperwork started? Sarah: I can have the contract ready by EOD tomorrow. David: Excellent! Let us finalize this week.",
    "sourceType": "CALL_RECORDING",
    "contactName": "David Kim"
  }'
```
**Expected Response**:
```json
{
  "overallSentiment": "POSITIVE",
  "sentimentScore": 0.85,
  "confidence": 0.92,
  "summary": "Highly positive sales conversation. Customer is enthusiastic, budget-approved, and ready to move forward quickly.",
  "emotions": [
    { "emotion": "enthusiasm", "score": 0.9 },
    { "emotion": "confidence", "score": 0.85 },
    { "emotion": "satisfaction", "score": 0.8 }
  ],
  "keyPhrases": ["really excited", "impressive", "exactly what our CTO requested", "within our approved budget"],
  "concerns": [],
  "positiveIndicators": ["Budget pre-approved", "CTO buy-in confirmed", "Urgency to finalize this week", "No objections raised"],
  "recommendation": "Fast-track contract delivery. Customer is ready to close. No blockers identified."
}
```

#### Step 2 — Analyze a Negative/Concerned Call
```bash
curl -X POST http://localhost:9089/api/v1/ai/sentiment-analysis \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Emily: Hi Dr. Sharma, following up on the HIPAA compliance package. Dr. Sharma: Yes, we have some concerns. Our legal team reviewed the data residency terms and they are not comfortable with cloud storage outside the US. Emily: I understand. We do offer US-only data centers. Dr. Sharma: That helps, but the implementation timeline of 12 weeks is too long. We have a compliance audit in 8 weeks and need everything in place before that. Emily: We could potentially accelerate to 8 weeks with a dedicated implementation team. Dr. Sharma: The cost would need to stay within our original budget though. We cannot approve additional expenses for expedited work. Emily: Let me discuss with my manager and get back to you with options by Thursday. Dr. Sharma: Fine, but if we cannot resolve this soon, we may need to evaluate other vendors.",
    "sourceType": "CALL_RECORDING",
    "contactName": "Dr. Priya Sharma"
  }'
```
**Expected Response**:
```json
{
  "overallSentiment": "NEGATIVE",
  "sentimentScore": -0.45,
  "confidence": 0.88,
  "summary": "Customer has multiple concerns: data residency, implementation timeline, and budget constraints. Risk of losing deal to competitors.",
  "emotions": [
    { "emotion": "concern", "score": 0.8 },
    { "emotion": "frustration", "score": 0.6 },
    { "emotion": "impatience", "score": 0.7 }
  ],
  "keyPhrases": ["not comfortable", "too long", "cannot approve additional expenses", "evaluate other vendors"],
  "concerns": [
    "Data residency - legal team uncomfortable with non-US cloud storage",
    "Timeline - 12 weeks too long, needs 8-week delivery",
    "Budget - cannot absorb expedited implementation costs",
    "Competitive threat - may evaluate alternatives"
  ],
  "positiveIndicators": ["Still engaged in conversation", "Open to solutions", "Gave clear deadline (Thursday)"],
  "recommendation": "URGENT: Escalate to manager. Prepare US-only data center proposal with 8-week accelerated timeline at original budget. Respond by Thursday to prevent competitive evaluation."
}
```

### UI Demo Steps
1. Navigate to **Communications** → **AI Sentiment** tab
2. Paste a call transcript or select an existing call record
3. Click **Analyze Sentiment**
4. Review the sentiment dashboard:
   - **Overall Sentiment** badge (POSITIVE / NEGATIVE / NEUTRAL / MIXED)
   - **Sentiment Score** gauge (-1.0 to +1.0)
   - **Confidence** percentage
   - **Emotions** breakdown chart
   - **Key Phrases** highlighted
   - **Concerns** listed with severity
   - **Positive Indicators** listed
   - **Recommendation** for next steps
5. Compare the positive vs. negative call analysis side by side

### What to Verify
- [ ] Sentiment correctly classified (POSITIVE/NEGATIVE/NEUTRAL/MIXED)
- [ ] Score is in range [-1.0, 1.0] and matches sentiment classification
- [ ] Confidence score is reasonable (0.7+)
- [ ] Emotions detected and scored
- [ ] Key phrases extracted from the actual content
- [ ] Concerns are specific and actionable
- [ ] Recommendation is contextually appropriate
- [ ] Different conversations produce different sentiment results

---

## Feature 7: Conversation Timeline for Customers

### API Testing

#### Step 1 — View Contact Communications
```bash
curl -X GET "http://localhost:9084/api/v1/contacts/{contactId}/communications?page=0&size=50" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 2 — Add a Communication Entry
```bash
curl -X POST "http://localhost:9084/api/v1/contacts/{contactId}/communications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commType": "CALL",
    "subject": "Pricing Discussion Follow-up",
    "body": "Discussed enterprise pricing with David. He confirmed $500K budget ceiling. Sending proposal by Friday.",
    "direction": "OUTBOUND",
    "status": "COMPLETED",
    "communicationDate": "2026-03-09T10:30:00"
  }'
```

#### Step 3 — Add Multiple Communication Types
```bash
# Email communication
curl -X POST "http://localhost:9084/api/v1/contacts/{contactId}/communications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commType": "EMAIL",
    "subject": "TechVista Partnership Proposal",
    "body": "Sent detailed enterprise proposal with API integration timeline.",
    "direction": "OUTBOUND",
    "status": "COMPLETED",
    "communicationDate": "2026-03-09T14:00:00"
  }'

# Meeting communication
curl -X POST "http://localhost:9084/api/v1/contacts/{contactId}/communications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commType": "MEETING",
    "subject": "Technical Deep Dive - API Integration",
    "body": "Met with David and TechVista CTO to review API architecture. Agreed on REST + WebSocket approach.",
    "direction": "OUTBOUND",
    "status": "COMPLETED",
    "communicationDate": "2026-03-10T15:00:00"
  }'

# SMS communication
curl -X POST "http://localhost:9084/api/v1/contacts/{contactId}/communications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commType": "SMS",
    "subject": "Quick check-in",
    "body": "Hi David, just confirming receipt of the proposal. Let me know if you have questions! - Sarah",
    "direction": "OUTBOUND",
    "status": "COMPLETED",
    "communicationDate": "2026-03-11T09:00:00"
  }'

# Note
curl -X POST "http://localhost:9084/api/v1/contacts/{contactId}/communications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commType": "NOTE",
    "subject": "Internal: Deal Strategy",
    "body": "David is our champion. Need to get CTO Maria buy-in on integration timeline. May want to involve our solutions architect.",
    "direction": "OUTBOUND",
    "status": "COMPLETED",
    "communicationDate": "2026-03-11T10:00:00"
  }'
```

### UI Demo Steps
1. Navigate to **Contacts** → click on **David Kim** (or any contact)
2. Click the **Communications** tab
3. View the chronological timeline showing all interactions:
   - 📧 Emails sent/received
   - 📞 Calls made/received with duration
   - 📅 Meetings with notes
   - 💬 SMS messages
   - 📝 Internal notes
4. Filter by communication type (Email, Call, Meeting, SMS, Note)
5. Click on any entry to expand details
6. Click the **Timeline** tab for the visual activity timeline
7. Show how communications from all channels merge into one view

### What to Verify
- [ ] All communication types appear in chronological order
- [ ] Direction indicators (INBOUND ↙ / OUTBOUND ↗) are correct
- [ ] Status badges show correctly (COMPLETED, PENDING, MISSED, SCHEDULED)
- [ ] Clicking an entry shows full details (body, timestamps, etc.)
- [ ] Timeline visual representation is clear and readable
- [ ] New communications appear immediately after creation
- [ ] Communications are scoped to the specific contact

---

## Feature 8: Multi-Channel Unified Inbox

### API Testing

#### Step 1 — View All Channels
```bash
curl -X GET "http://localhost:9087/api/v1/communications/inbox?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 2 — Filter by Channel
```bash
# SMS only
curl -X GET "http://localhost:9087/api/v1/communications/inbox/channel/SMS" \
  -H "Authorization: Bearer $TOKEN"

# WhatsApp only
curl -X GET "http://localhost:9087/api/v1/communications/inbox/channel/WHATSAPP" \
  -H "Authorization: Bearer $TOKEN"

# Calls only
curl -X GET "http://localhost:9087/api/v1/communications/inbox/channel/CALL" \
  -H "Authorization: Bearer $TOKEN"

# Emails only
curl -X GET "http://localhost:9087/api/v1/communications/inbox/channel/EMAIL" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 3 — View by Entity (Contact-specific)
```bash
curl -X GET "http://localhost:9087/api/v1/communications/inbox/entity/CONTACT/{contactId}" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Communications** → **Unified Inbox** tab
2. View the combined inbox showing messages from ALL channels:
   - 📧 Emails (blue badge)
   - 💬 SMS (primary badge)
   - 🟢 WhatsApp (green badge)
   - 📞 Calls (secondary badge)
3. Use the **channel filter** buttons to toggle:
   - Click **SMS** → only SMS messages shown
   - Click **WhatsApp** → only WhatsApp messages shown
   - Click **All** → all channels combined
4. Click on any message to view its full content
5. Show the entity links — click a contact name to navigate to their profile
6. Demonstrate how all channels feed into one unified view

### What to Verify
- [ ] All channel messages appear in a single unified list
- [ ] Channel badges (SMS, WHATSAPP, CALL, EMAIL) are color-coded correctly
- [ ] Filtering by channel works and updates the list
- [ ] Messages are sorted by date (newest first)
- [ ] Entity links navigate to the correct contact/account
- [ ] Pagination works across large message volumes
- [ ] Direction (INBOUND/OUTBOUND) is clearly visible

---

## Full Demo Script (10-Minute Walkthrough)

### Scene 1 — Email System (0:00 – 2:00)
**Narration**: *"Let's start with the built-in email system. The CRM has full email capabilities — compose, send, receive, and track, all without leaving the platform."*

1. Open **Email** → show Inbox with received messages
2. Click **Compose** → send an email to `david.kim@techvista.com`
3. Show the email appears in **Sent** items
4. Open **Templates** → show the 5 pre-built templates (Welcome, Follow-Up, Proposal, QBR, Deal Won)
5. Open **Analytics** → highlight open rates and click tracking

**Key Talking Points**:
- Gmail & Outlook OAuth2 integration
- Open & click tracking built-in
- Thread-based conversations
- Entity linking (every email connected to a contact/account/opportunity)

---

### Scene 2 — WhatsApp Integration (2:00 – 3:30)
**Narration**: *"Next, WhatsApp — the world's most popular messaging platform, integrated directly into our CRM."*

1. Navigate to **Communications** → **WhatsApp** tab
2. Show existing WhatsApp conversations
3. Click **New Message** → send a text message
4. Send a document message (media type)
5. Show read receipts (✓ Sent → ✓✓ Delivered → ✓✓ Read)

**Key Talking Points**:
- Supports text, image, document, audio, video messages
- Real-time delivery and read status
- Full conversation history per phone number
- All messages linked to CRM entities

---

### Scene 3 — SMS Messaging (3:30 – 4:30)
**Narration**: *"For quick outreach, SMS messaging is built right in."*

1. Navigate to **SMS** tab
2. Show SMS message list with delivery statuses
3. Click **New SMS** → compose and send
4. Show the conversation view grouped by phone number

**Key Talking Points**:
- One-click SMS from any contact record
- Delivery confirmation tracking
- Conversation threading by phone number
- Opt-in/consent tracking (linked to contact preferences)

---

### Scene 4 — Calling & Call Recording (4:30 – 6:30)
**Narration**: *"Now the calling module — initiate, record, and log calls without leaving the CRM."*

1. Navigate to **Calls** tab
2. Show the call log with recent calls
3. Click **New Call** → initiate a call to a contact
4. Show the call status change: INITIATED → RINGING → IN_PROGRESS
5. Click **End Call** → enter outcome: "Positive - ready for proposal"
6. Show the completed call with:
   - Duration: 30 min 45 sec
   - Recording link 🔴
   - Outcome notes
7. Click the recording link to play the audio

**Key Talking Points**:
- Full call lifecycle management
- Automatic recording with playback
- Call outcome tracking for pipeline analytics
- Voicemail detection and logging
- Complete call history per contact

---

### Scene 5 — AI Transcription (6:30 – 7:45)
**Narration**: *"Here is where AI transforms raw recordings into actionable intelligence."*

1. Navigate to **AI Transcription** tab
2. Paste the TechVista call transcript (Sarah + David pricing conversation)
3. Click **Transcribe**
4. Show the results:
   - **Speaker Segments**: Each speaker labeled with timestamps
   - **Key Topics**: Platform licensing, Enterprise plan, Pricing, API integration
   - **Summary**: "Sarah presented enterprise licensing at $450K/year..."
5. Highlight how this saves reps from manually noting every detail

**Key Talking Points**:
- Automatic speaker identification
- Key topic extraction
- Conversation summarization
- Works with calls, meetings, and voicemails
- Saves 15-20 minutes of manual note-taking per call

---

### Scene 6 — AI Sentiment Analysis (7:45 – 9:00)
**Narration**: *"Beyond transcription, our AI analyzes the emotional tone of every customer conversation."*

1. Navigate to **AI Sentiment** tab
2. Analyze the **positive** call (David Kim - eager to close)
   - Show: POSITIVE sentiment, 0.85 score, "enthusiasm" top emotion
   - Recommendation: "Fast-track contract delivery"
3. Analyze the **negative** call (Dr. Sharma - compliance concerns)
   - Show: NEGATIVE sentiment, -0.45 score, "concern" top emotion
   - Show concerns list: data residency, timeline, budget
   - Recommendation: "URGENT: Escalate to manager"
4. Compare side by side — show how AI prioritizes which deals need attention

**Key Talking Points**:
- Real-time sentiment scoring (-1.0 to +1.0)
- Emotion breakdown (enthusiasm, frustration, confidence)
- Automatic concern detection
- Proactive recommendations for next actions
- Helps managers identify at-risk deals instantly

---

### Scene 7 — Conversation Timeline (9:00 – 9:30)
**Narration**: *"Every interaction — email, call, SMS, WhatsApp, meeting — flows into a single customer timeline."*

1. Navigate to **Contacts** → open **David Kim**
2. Click **Communications** tab
3. Scroll through the timeline showing:
   - 📞 Discovery call (Mar 9)
   - 📧 Proposal email (Mar 9)
   - 💬 SMS check-in (Mar 11)
   - 📅 Technical deep dive meeting (Mar 10)
   - 📝 Internal strategy note

**Key Talking Points**:
- 360-degree customer view
- All channels in one chronological timeline
- No context switching between tools
- Full audit trail for compliance

---

### Scene 8 — Multi-Channel Inbox (9:30 – 10:00)
**Narration**: *"Finally, the Unified Inbox — one place for everything."*

1. Navigate to **Communications** → **Unified Inbox**
2. Show all messages from all channels in one list
3. Filter: SMS → WhatsApp → Calls → Email → All
4. Click a message to open details and jump to the full conversation

**Key Talking Points**:
- Single pane of glass for all communications
- Channel-specific filtering
- Every message linked to CRM entities
- No more switching between email, phone, and messaging apps

---

## Automated API Test Script (PowerShell)

Save as `demo/test-communications.ps1` and run:

```powershell
# Quick smoke test for all 8 communication features
$BASE = "http://localhost"
$token = "<paste-your-token-here>"
$headers = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "`n=== Testing Communication Features ===" -ForegroundColor Cyan

# 1. Email - List inbox
Write-Host "`n[1] Email - Inbox" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE:9090/api/v1/email/messages/inbox?page=0&size=5" -Headers $headers | ConvertTo-Json -Depth 3

# 2. WhatsApp - Send message
Write-Host "`n[2] WhatsApp - Send" -ForegroundColor Yellow
$waBody = '{"fromNumber":"+1-555-0001","toNumber":"+1-555-0002","body":"Test WhatsApp message","messageType":"TEXT"}'
Invoke-RestMethod -Method POST -Uri "$BASE:9087/api/v1/communications/whatsapp/send" -Headers $headers -Body $waBody

# 3. SMS - Send message
Write-Host "`n[3] SMS - Send" -ForegroundColor Yellow
$smsBody = '{"fromNumber":"+1-555-0001","toNumber":"+1-555-0003","body":"Test SMS message"}'
Invoke-RestMethod -Method POST -Uri "$BASE:9087/api/v1/communications/sms/send" -Headers $headers -Body $smsBody

# 4. Calls - Initiate
Write-Host "`n[4] Calls - Initiate" -ForegroundColor Yellow
$callBody = '{"fromNumber":"+1-555-0001","toNumber":"+1-555-0004","direction":"OUTBOUND","notes":"Test call"}'
Invoke-RestMethod -Method POST -Uri "$BASE:9087/api/v1/communications/calls/initiate" -Headers $headers -Body $callBody

# 5. AI Transcription
Write-Host "`n[5] AI - Transcribe" -ForegroundColor Yellow
$txBody = '{"content":"Speaker1: Hello. Speaker2: Hi there, how are you?","sourceType":"CALL_RECORDING","speakers":["Speaker1","Speaker2"]}'
Invoke-RestMethod -Method POST -Uri "$BASE:9089/api/v1/ai/transcribe" -Headers $headers -Body $txBody

# 6. AI Sentiment
Write-Host "`n[6] AI - Sentiment Analysis" -ForegroundColor Yellow
$saBody = '{"content":"I am really excited about this partnership!","sourceType":"CALL_RECORDING","contactName":"Test Contact"}'
Invoke-RestMethod -Method POST -Uri "$BASE:9089/api/v1/ai/sentiment-analysis" -Headers $headers -Body $saBody

# 7. Unified Inbox
Write-Host "`n[7] Unified Inbox - All channels" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE:9087/api/v1/communications/inbox?page=0&size=10" -Headers $headers | ConvertTo-Json -Depth 3

# 8. Inbox filtered by channel
Write-Host "`n[8] Unified Inbox - SMS channel only" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE:9087/api/v1/communications/inbox/channel/SMS" -Headers $headers | ConvertTo-Json -Depth 3

Write-Host "`n=== All Communication Tests Complete ===" -ForegroundColor Green
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Re-authenticate: POST `/api/v1/auth/login` to get fresh token |
| 404 on communication endpoints | Verify notification-service is running: `docker ps \| grep notification` |
| Empty inbox | Run `.\demo\seed-communications.ps1` to populate demo data |
| AI endpoints timeout | Check ai-service logs: `docker logs crmsapp-ai-service-1` |
| Email not sending | Check email-service config and SMTP/OAuth credentials |
| WhatsApp status stuck on PENDING | Expected in demo mode (no live WhatsApp Business API) |
| Call recording URL not playing | Recording URLs are simulated in demo; real integration requires Twilio/VoIP provider |
