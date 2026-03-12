###############################################################################
#  CRM Platform – Communications Demo Seed Script
#  ────────────────────────────────────────────────
#  Populates SMS, WhatsApp, Calls, and Unified Inbox with realistic data.
#  Also runs AI Transcription and Sentiment Analysis demos.
#
#  Usage:  .\demo\seed-communications.ps1
#  Prereq: All Docker services running + .\demo\seed-demo.ps1 already run
###############################################################################

$ErrorActionPreference = "Continue"
$BASE = "http://localhost"

# ─── Colours ──────────────────────────────────────────────────────────────────
function Write-Step  { param($msg) Write-Host "`n▶ $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "  ✓ $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "  ⚠ $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "  ✗ $msg" -ForegroundColor Red }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Body = $null,
        [string]$Token = $null,
        [string]$ContentType = "application/json"
    )
    $headers = @{ "Content-Type" = $ContentType }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    $params = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $headers
        ErrorAction = "Stop"
    }
    if ($Body -and $Method -ne "GET") {
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
    }
    try {
        $resp = Invoke-RestMethod @params
        return $resp
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Warn "$Method $Uri → $status"
        return $null
    }
}

###############################################################################
# 0. AUTHENTICATE
###############################################################################
Write-Step "Authenticating admin user..."

$loginBody = @{
    email    = "sarah.chen@acmecorp.com"
    password = "Demo@2026!"
    tenantId = "default"
} | ConvertTo-Json

$loginRes = Invoke-Api -Method POST -Uri "$BASE`:8081/api/v1/auth/login" -Body $loginBody
if ($loginRes -and $loginRes.data) {
    $T = $loginRes.data.accessToken
    Write-Ok "Authenticated as Sarah Chen"
} else {
    Write-Err "Authentication failed. Run seed-demo.ps1 first."
    exit 1
}

###############################################################################
# 1. SMS MESSAGES — 10 messages across 4 conversations
###############################################################################
Write-Step "Creating SMS messages..."

$smsMessages = @(
    @{ from="+1-555-100-0001"; to="+1-415-555-1101"; body="Hi David, this is Sarah from CRMS. Just wanted to confirm our discovery call tomorrow at 10 AM. Looking forward to it!" },
    @{ from="+1-415-555-1101"; to="+1-555-100-0001"; body="Hi Sarah! Yes, confirmed for 10 AM. I will have our CTO Rachel on the call as well." },
    @{ from="+1-555-100-0001"; to="+1-415-555-1101"; body="Perfect! I will send the meeting link shortly. See you both tomorrow." },

    @{ from="+1-555-100-0002"; to="+1-212-555-2101"; body="Hi Amanda, Emily here from CRMS. Quick reminder about the pipeline review meeting at 2 PM today." },
    @{ from="+1-212-555-2101"; to="+1-555-100-0002"; body="Thanks Emily! Running 5 mins late but will be there." },

    @{ from="+1-555-100-0003"; to="+1-312-555-3101"; body="Dr. Sharma, Michael from CRMS. The HIPAA compliance documentation you requested is ready. Shall I email it over?" },
    @{ from="+1-312-555-3101"; to="+1-555-100-0003"; body="Yes please, send it to my email. Also include the data residency policy." },
    @{ from="+1-555-100-0003"; to="+1-312-555-3101"; body="Done! Sent both documents to priya.sharma@medhealth.org. Let me know if you need anything else." },

    @{ from="+1-555-100-0001"; to="+1-720-555-6101"; body="Thomas, Sarah here. The GreenEnergy fleet management proposal is ready. Can we schedule a call this week to review?" },
    @{ from="+1-720-555-6101"; to="+1-555-100-0001"; body="Great news! Thursday 3 PM works for me. Please send a calendar invite." }
)

foreach ($sms in $smsMessages) {
    $body = @{
        fromNumber = $sms.from
        toNumber   = $sms.to
        body       = $sms.body
    } | ConvertTo-Json

    $res = Invoke-Api -Method POST -Uri "$BASE`:9087/api/v1/communications/sms/send" -Body $body -Token $T
    if ($res) {
        $preview = if ($sms.body.Length -gt 50) { $sms.body.Substring(0, 50) + "..." } else { $sms.body }
        Write-Ok "SMS: $($sms.from) → $($sms.to): $preview"
    }
}

###############################################################################
# 2. WHATSAPP MESSAGES — 12 messages across 3 conversations
###############################################################################
Write-Step "Creating WhatsApp messages..."

$waMessages = @(
    @{ from="+1-555-100-0001"; to="+1-415-555-1101"; body="Hi David! Sarah from CRMS here. Just shared the enterprise proposal via email. Let me know if the PDF opens OK."; type="TEXT"     },
    @{ from="+1-415-555-1101"; to="+1-555-100-0001"; body="Got it, thanks Sarah! The proposal looks comprehensive. A few questions about the API integration section."; type="TEXT"        },
    @{ from="+1-555-100-0001"; to="+1-415-555-1101"; body="Of course! Here is the API architecture diagram for reference."; type="IMAGE"; mediaUrl="https://example.com/docs/api-diagram.png"; mediaType="image/png" },
    @{ from="+1-415-555-1101"; to="+1-555-100-0001"; body="This is exactly what we need. Let me share this with Rachel and get back to you by EOD."; type="TEXT"                           },

    @{ from="+1-555-100-0002"; to="+1-617-555-4101"; body="Hi Christine, Emily from CRMS. Hope you are doing well! Quick update on the FinEdge risk analytics module."; type="TEXT"        },
    @{ from="+1-617-555-4101"; to="+1-555-100-0002"; body="Hi Emily! Yes, we have been discussing it internally. The board wants to see the ROI analysis."; type="TEXT"                    },
    @{ from="+1-555-100-0002"; to="+1-617-555-4101"; body="Absolutely! Here is the detailed ROI calculator spreadsheet."; type="DOCUMENT"; mediaUrl="https://example.com/docs/roi-calc.xlsx"; mediaType="application/vnd.ms-excel" },
    @{ from="+1-617-555-4101"; to="+1-555-100-0002"; body="Received. I will present this at the board meeting next Tuesday."; type="TEXT"                                                  },

    @{ from="+1-555-100-0003"; to="+1-720-555-6102"; body="Hi Sofia, Michael here. Following up on the sustainability dashboard customization for GreenEnergy."; type="TEXT"               },
    @{ from="+1-720-555-6102"; to="+1-555-100-0003"; body="Hi Michael! We love the initial mockups. Can you send the interactive prototype link?"; type="TEXT"                              },
    @{ from="+1-555-100-0003"; to="+1-720-555-6102"; body="Here you go! Click to explore the prototype."; type="TEXT"                                                                       },
    @{ from="+1-720-555-6102"; to="+1-555-100-0003"; body="Impressive work! Thomas and I will review together tomorrow morning."; type="TEXT"                                               }
)

foreach ($wa in $waMessages) {
    $bodyObj = @{
        fromNumber  = $wa.from
        toNumber    = $wa.to
        body        = $wa.body
        messageType = $wa.type
    }
    if ($wa.mediaUrl)  { $bodyObj["mediaUrl"]  = $wa.mediaUrl }
    if ($wa.mediaType) { $bodyObj["mediaType"] = $wa.mediaType }

    $body = $bodyObj | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE`:9087/api/v1/communications/whatsapp/send" -Body $body -Token $T
    if ($res) {
        $preview = if ($wa.body.Length -gt 50) { $wa.body.Substring(0, 50) + "..." } else { $wa.body }
        Write-Ok "WhatsApp ($($wa.type)): $($wa.from) → $($wa.to): $preview"
    }
}

###############################################################################
# 3. CALL RECORDS — 8 calls with various outcomes
###############################################################################
Write-Step "Creating call records..."

$calls = @(
    @{
        from="+1-555-100-0001"; to="+1-415-555-1101"; direction="OUTBOUND"
        notes="Discovery call with David Kim - TechVista platform licensing discussion"
        outcome="Positive - Interested in enterprise plan, requesting proposal"
        duration=1845; recording="https://recordings.crms.io/calls/2026-03-06-techvista-discovery.mp3"
    },
    @{
        from="+1-555-100-0002"; to="+1-212-555-2101"; direction="OUTBOUND"
        notes="Pipeline review with Amanda Foster - GlobalRetail omnichannel suite"
        outcome="Positive - Moving to contract negotiation phase"
        duration=2210; recording="https://recordings.crms.io/calls/2026-03-06-globalretail-review.mp3"
    },
    @{
        from="+1-555-100-0003"; to="+1-312-555-3101"; direction="OUTBOUND"
        notes="HIPAA compliance review with Dr. Sharma - MedHealth package"
        outcome="Concerns - Timeline and data residency issues raised"
        duration=1520; recording="https://recordings.crms.io/calls/2026-03-07-medhealth-hipaa.mp3"
    },
    @{
        from="+1-617-555-4101"; to="+1-555-100-0002"; direction="INBOUND"
        notes="Christine Wang called about FinEdge risk analytics pricing"
        outcome="Neutral - Waiting on board approval, follow up next week"
        duration=980; recording="https://recordings.crms.io/calls/2026-03-07-finedge-pricing.mp3"
    },
    @{
        from="+1-555-100-0001"; to="+1-720-555-6101"; direction="OUTBOUND"
        notes="GreenEnergy fleet management proposal walkthrough with Thomas Anderson"
        outcome="Positive - Volume discount requested, ready to negotiate"
        duration=2640; recording="https://recordings.crms.io/calls/2026-03-08-greenenergy-proposal.mp3"
    },
    @{
        from="+1-555-100-0003"; to="+1-650-555-7101"; direction="OUTBOUND"
        notes="CloudNine partnership integration kickoff with Nathan Hughes"
        outcome="Positive - Agreement signed, implementation starting next month"
        duration=1200; recording="https://recordings.crms.io/calls/2026-03-08-cloudnine-kickoff.mp3"
    },
    @{
        from="+1-503-555-5101"; to="+1-555-100-0001"; direction="INBOUND"
        notes="Jennifer Liu inquired about EduPath LMS integration capabilities"
        outcome="Neutral - Still in evaluation, budget pending"
        duration=720; recording=$null
    },
    @{
        from="+1-555-100-0002"; to="+1-248-555-8008"; direction="OUTBOUND"
        notes="Precision Manufacturing ERP connector technical discussion"
        outcome="Positive - Technical requirements aligned, sending SOW"
        duration=1560; recording="https://recordings.crms.io/calls/2026-03-09-precision-erp.mp3"
    }
)

foreach ($call in $calls) {
    # Initiate call
    $initBody = @{
        fromNumber        = $call.from
        toNumber          = $call.to
        direction         = $call.direction
        notes             = $call.notes
    } | ConvertTo-Json

    $res = Invoke-Api -Method POST -Uri "$BASE`:9087/api/v1/communications/calls/initiate" -Body $initBody -Token $T
    if ($res -and $res.data) {
        $callId = $res.data.id

        # End call with outcome and recording
        $endBody = @{
            durationSeconds          = $call.duration
            callOutcome              = $call.outcome
            notes                    = $call.notes
        }
        if ($call.recording) {
            $endBody["recordingUrl"]              = $call.recording
            $endBody["recordingDurationSeconds"]   = $call.duration
        }

        $endJson = $endBody | ConvertTo-Json
        Invoke-Api -Method POST -Uri "$BASE`:9087/api/v1/communications/calls/$callId/end" -Body $endJson -Token $T | Out-Null

        $durationMin = [math]::Floor($call.duration / 60)
        Write-Ok "Call ($($call.direction)): $($call.from) → $($call.to) [${durationMin}m] — $($call.outcome.Substring(0, [Math]::Min(50, $call.outcome.Length)))"
    }
}

###############################################################################
# 4. AI TRANSCRIPTION — 2 demo transcriptions
###############################################################################
Write-Step "Running AI transcription demos..."

$transcriptions = @(
    @{
        content = "Sarah: Good morning David thanks for taking the time to chat today. David: Of course we have been looking forward to this. Sarah: So I wanted to go over the platform licensing options. We have three tiers Starter Professional and Enterprise. Based on your team size of 200 users I would recommend our Enterprise plan. David: That sounds right. What does the pricing look like for enterprise? Sarah: For 200 seats we are looking at 450000 dollars annually. That includes premium support custom API access and dedicated account management. David: That is within our budget range. Our CTO has already approved up to 500K for this initiative. Sarah: Wonderful I will have a detailed proposal sent over by Friday. David: Perfect. Can we also discuss the API integration timeline? We need to connect with our existing ERP system. Sarah: Absolutely our integration team can have a custom connector built within 6 weeks of contract signing."
        sourceType = "CALL_RECORDING"
        speakers   = @("Sarah Chen", "David Kim")
        label      = "TechVista Discovery Call"
    },
    @{
        content = "James: Let us review the Q1 pipeline. Emily how is the GreenEnergy deal looking? Emily: GreenEnergy is in negotiation at 1.2 million dollars. They want volume discounts on the fleet management module. James: What is our floor price? Emily: We can go down to 1.05 million and still maintain 65 percent margin. James: Good. Michael status on GlobalRetail? Michael: They are at proposal stage 850K. Main blocker is their IT team wants a security audit first. James: Schedule that for next week. Lisa any updates on the EduPath deal? Lisa: Still in prospecting. Budget approval is pending on their end. I am following up weekly."
        sourceType = "MEETING"
        speakers   = @("James Wilson", "Emily Rodriguez", "Michael Park", "Lisa Thompson")
        label      = "Q1 Pipeline Review Meeting"
    }
)

foreach ($tx in $transcriptions) {
    $body = @{
        content    = $tx.content
        sourceType = $tx.sourceType
        speakers   = $tx.speakers
        language   = "en"
    } | ConvertTo-Json

    $res = Invoke-Api -Method POST -Uri "$BASE`:9089/api/v1/ai/transcribe" -Body $body -Token $T
    if ($res) {
        Write-Ok "Transcription: $($tx.label) — Topics: $(if ($res.data.keyTopics) { $res.data.keyTopics -join ', ' } else { 'generated' })"
    }
}

###############################################################################
# 5. AI SENTIMENT ANALYSIS — 3 demo analyses
###############################################################################
Write-Step "Running AI sentiment analysis demos..."

$sentiments = @(
    @{
        content = "Sarah: Good morning David thanks for taking the time to chat today. David: Of course we have been really excited about this partnership. Your platform demo last week was impressive. Sarah: That is great to hear. Let me walk you through the pricing. David: Perfect. Honestly the features you showed align exactly with what our CTO has been requesting. Sarah: For 200 seats at the Enterprise tier we are looking at 450K annually. David: That is within our approved budget. Our CTO has signed off on up to 500K. When can we get the paperwork started? Sarah: I can have the contract ready by EOD tomorrow. David: Excellent let us finalize this week."
        sourceType  = "CALL_RECORDING"
        contactName = "David Kim"
        label       = "Positive — David Kim (TechVista)"
    },
    @{
        content = "Emily: Hi Dr. Sharma following up on the HIPAA compliance package. Dr. Sharma: Yes we have some concerns. Our legal team reviewed the data residency terms and they are not comfortable with cloud storage outside the US. Emily: I understand. We do offer US-only data centers. Dr. Sharma: That helps but the implementation timeline of 12 weeks is too long. We have a compliance audit in 8 weeks and need everything in place before that. Emily: We could potentially accelerate to 8 weeks with a dedicated implementation team. Dr. Sharma: The cost would need to stay within our original budget though. We cannot approve additional expenses for expedited work. Emily: Let me discuss with my manager and get back to you with options by Thursday. Dr. Sharma: Fine but if we cannot resolve this soon we may need to evaluate other vendors."
        sourceType  = "CALL_RECORDING"
        contactName = "Dr. Priya Sharma"
        label       = "Negative — Dr. Sharma (MedHealth)"
    },
    @{
        content = "Michael: Thomas thanks for joining. I have the updated fleet management pricing. Thomas: Thanks Michael. We have reviewed internally and the solution looks solid but we need to discuss the volume pricing. We manage 2500 vehicles and need per-unit economics to work. Michael: Understood. What range are you targeting? Thomas: Ideally under 400 per vehicle annually. Your current quote is around 480. Michael: I think we can work with that. Let me build out a volume tier proposal. Thomas: That would be helpful. Also Sofia from our sustainability team will want to review the carbon tracking module. Michael: Absolutely I will include that in the proposal with a separate line item. Thomas: Good. We are motivated to move forward but need the numbers to align."
        sourceType  = "CALL_RECORDING"
        contactName = "Thomas Anderson"
        label       = "Mixed — Thomas Anderson (GreenEnergy)"
    }
)

foreach ($sa in $sentiments) {
    $body = @{
        content     = $sa.content
        sourceType  = $sa.sourceType
        contactName = $sa.contactName
    } | ConvertTo-Json

    $res = Invoke-Api -Method POST -Uri "$BASE`:9089/api/v1/ai/sentiment-analysis" -Body $body -Token $T
    if ($res -and $res.data) {
        $sentiment = $res.data.overallSentiment
        $score     = $res.data.sentimentScore
        Write-Ok "Sentiment: $($sa.label) → $sentiment (score: $score)"
    } elseif ($res) {
        Write-Ok "Sentiment: $($sa.label) → Analysis complete"
    }
}

###############################################################################
# 6. EMAIL MESSAGES — 6 demo emails
###############################################################################
Write-Step "Creating email messages..."

$emails = @(
    @{
        from    = "sarah.chen@acmecorp.com"
        to      = @("david.kim@techvista.io")
        subject = "TechVista Partnership Proposal - Enterprise Platform License"
        html    = "<h2>Hi David,</h2><p>Thank you for the excellent conversation today. As discussed, please find attached our Enterprise Platform License proposal for TechVista Solutions.</p><p><strong>Key highlights:</strong></p><ul><li>200-seat Enterprise license at `$450,000/year</li><li>Premium support with dedicated account manager</li><li>Custom API access and ERP integration (6-week timeline)</li><li>99.9% SLA guarantee</li></ul><p>I am available anytime this week to discuss further details.</p><p>Best regards,<br/>Sarah Chen<br/>Account Executive, CRMS Platform</p>"
        text    = "Hi David, Thank you for the excellent conversation today. Please find attached our Enterprise Platform License proposal. 200-seat Enterprise license at 450K/year with premium support. Best, Sarah"
    },
    @{
        from    = "emily.rodriguez@acmecorp.com"
        to      = @("amanda.foster@globalretail.com")
        subject = "GlobalRetail Omnichannel Suite - Updated Proposal"
        html    = "<h2>Hi Amanda,</h2><p>Following our pipeline review, here is the updated proposal for the Omnichannel Suite at `$850,000.</p><p>We have incorporated your feedback on the POS integration requirements and added a dedicated implementation timeline.</p><p>Would Thursday at 2 PM work for a walkthrough?</p><p>Best,<br/>Emily Rodriguez</p>"
        text    = "Hi Amanda, Here is the updated Omnichannel Suite proposal at 850K. Would Thursday at 2 PM work for a walkthrough? Best, Emily"
    },
    @{
        from    = "michael.park@acmecorp.com"
        to      = @("priya.sharma@medhealth.org")
        subject = "MedHealth HIPAA Compliance Package - Documentation"
        html    = "<h2>Dr. Sharma,</h2><p>As requested, please find the following compliance documentation:</p><ol><li>HIPAA BAA (Business Associate Agreement)</li><li>US-only data residency policy</li><li>SOC 2 Type II certification</li><li>Accelerated 8-week implementation plan</li></ol><p>I have also included revised pricing that stays within your original budget while accommodating the expedited timeline.</p><p>Best regards,<br/>Michael Park</p>"
        text    = "Dr. Sharma, Please find the HIPAA compliance documentation including BAA, data residency policy, SOC 2 cert, and accelerated implementation plan. Best, Michael"
    },
    @{
        from    = "sarah.chen@acmecorp.com"
        to      = @("thomas.anderson@greenenergy.com", "sofia.petrov@greenenergy.com")
        subject = "GreenEnergy Fleet Management - Volume Pricing Proposal"
        html    = "<h2>Hi Thomas and Sofia,</h2><p>Great speaking with you both. Here is the revised fleet management proposal with volume tier pricing:</p><p><strong>Tier 1:</strong> 1-1000 vehicles — `$480/vehicle/year<br/><strong>Tier 2:</strong> 1001-2000 vehicles — `$420/vehicle/year<br/><strong>Tier 3:</strong> 2001+ vehicles — `$380/vehicle/year</p><p>For your fleet of 2,500 vehicles, that comes to <strong>`$950,000/year</strong> (Tier 3 pricing).</p><p>The carbon tracking module is included as a separate line item at `$50,000/year.</p><p>Total: <strong>`$1,000,000/year</strong></p><p>Best,<br/>Sarah Chen</p>"
        text    = "Hi Thomas and Sofia, Here is the revised volume pricing for 2500 vehicles at Tier 3: 380/vehicle = 950K + 50K carbon tracking = 1M total. Best, Sarah"
    },
    @{
        from    = "david.kim@techvista.io"
        to      = @("sarah.chen@acmecorp.com")
        subject = "Re: TechVista Partnership Proposal - Enterprise Platform License"
        html    = "<h2>Hi Sarah,</h2><p>The proposal looks great. Rachel and I reviewed it with our CTO and we are aligned on the terms.</p><p>Two quick questions:</p><ol><li>Can the API integration timeline be shortened to 4 weeks? We have a product launch in Q2.</li><li>Is there a multi-year discount if we commit to a 3-year contract?</li></ol><p>Looking forward to finalizing!</p><p>Best,<br/>David Kim<br/>CTO, TechVista Solutions</p>"
        text    = "Hi Sarah, Proposal looks great. Two questions: 1) Can API integration be 4 weeks? 2) Multi-year discount for 3-year commitment? Best, David"
    },
    @{
        from    = "lisa.thompson@acmecorp.com"
        to      = @("nathan.hughes@cloudnine.io")
        subject = "CloudNine Partnership - Implementation Kickoff Details"
        html    = "<h2>Hi Nathan,</h2><p>Congratulations on finalizing the partnership integration! Here are the kickoff details:</p><p><strong>Kickoff Date:</strong> March 17, 2026<br/><strong>Implementation Lead:</strong> Lisa Thompson<br/><strong>Timeline:</strong> 8 weeks<br/><strong>Key Milestones:</strong></p><ul><li>Week 1-2: API setup and authentication</li><li>Week 3-4: Data sync and mapping</li><li>Week 5-6: UI integration and testing</li><li>Week 7-8: UAT and go-live</li></ul><p>Please confirm your technical point of contact.</p><p>Best,<br/>Lisa Thompson</p>"
        text    = "Hi Nathan, Partnership implementation kickoff is March 17. 8-week timeline with milestones. Please confirm your technical POC. Best, Lisa"
    }
)

foreach ($em in $emails) {
    $body = @{
        fromAddress = $em.from
        toAddresses = $em.to
        subject     = $em.subject
        bodyHtml    = $em.html
        bodyText    = $em.text
    } | ConvertTo-Json

    $res = Invoke-Api -Method POST -Uri "$BASE`:9090/api/v1/email/messages/send" -Body $body -Token $T
    if ($res) {
        Write-Ok "Email: $($em.from) → $($em.to[0]): $($em.subject.Substring(0, [Math]::Min(50, $em.subject.Length)))"
    }
}

###############################################################################
# SUMMARY
###############################################################################
Write-Host "`n" -NoNewline
Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║    Communications Demo Data Seeded Successfully!        ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║                                                          ║" -ForegroundColor Green
Write-Host "║    • 10 SMS messages (4 conversations)                   ║" -ForegroundColor White
Write-Host "║    • 12 WhatsApp messages (3 conversations)              ║" -ForegroundColor White
Write-Host "║    •  8 Call records (with recordings & outcomes)        ║" -ForegroundColor White
Write-Host "║    •  2 AI transcriptions (call + meeting)               ║" -ForegroundColor White
Write-Host "║    •  3 AI sentiment analyses (positive/negative/mixed)  ║" -ForegroundColor White
Write-Host "║    •  6 Email messages (proposals, replies, kickoffs)    ║" -ForegroundColor White
Write-Host "║                                                          ║" -ForegroundColor Green
Write-Host "║  Open: http://localhost:3000 → Communications            ║" -ForegroundColor Cyan
Write-Host "║                                                          ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
