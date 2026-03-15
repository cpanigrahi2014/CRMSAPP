###############################################################################
#  CRM Platform – End-to-End Test Data Seed Script
#  ────────────────────────────────────────────────
#  Seeds ALL modules with realistic data for comprehensive E2E testing.
#  Works through nginx proxy (port 80) or direct service ports.
#
#  Usage:
#    .\demo\seed-e2e.ps1                              # localhost
#    .\demo\seed-e2e.ps1 -BaseUrl http://34.193.72.13 # EC2 instance
#
#  Prereqs:
#    - All Docker services running
#    - Auth service reachable
###############################################################################

param(
    [string]$BaseUrl = "http://34.193.72.13"
)

$ErrorActionPreference = "Continue"
$BASE = $BaseUrl.TrimEnd("/")

# ─── Colours ──────────────────────────────────────────────────────────────────
function Write-Step  { param($msg) Write-Host "`n===== $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "  [WARN] $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "  [ERR] $msg" -ForegroundColor Red }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Body = $null,
        [string]$Token = $null,
        [string]$ContentType = "application/json"
    )
    $headers = @{ "Content-Type" = $ContentType; "X-Tenant-ID" = "default" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers; ErrorAction = "Stop" }
    if ($Body -and $Method -ne "GET") {
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
    }
    try {
        $resp = Invoke-RestMethod @params
        return $resp
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Warn "$Method $Uri -> $status"
        return $null
    }
}

###############################################################################
# 1. AUTH — Login or Register demo user
###############################################################################
Write-Step "1. Authenticating..."

$adminEmail = "demo@crm.com"
$adminPass  = "Demo@2026!"

# Try login first
$loginBody = @{ email=$adminEmail; password=$adminPass; tenantId="default" } | ConvertTo-Json
$res = Invoke-Api -Method POST -Uri "$BASE/api/v1/auth/login" -Body $loginBody
if ($res -and $res.data -and $res.data.accessToken) {
    $T = $res.data.accessToken
    $adminId = $res.data.userId
    Write-Ok "Logged in as $adminEmail (userId=$adminId)"
} else {
    # Try register
    $regBody = @{ email=$adminEmail; password=$adminPass; firstName="Demo"; lastName="Admin"; tenantId="default" } | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/auth/register" -Body $regBody
    if ($res -and $res.data) {
        $T = $res.data.accessToken
        $adminId = $res.data.userId
        Write-Ok "Registered $adminEmail"
    } else {
        Write-Err "Cannot authenticate. Aborting."; exit 1
    }
}

# Register additional test users
$testUsers = @(
    @{ email="sarah.chen@crm.com"; password="Demo@2026!"; firstName="Sarah"; lastName="Chen" },
    @{ email="james.wilson@crm.com"; password="Demo@2026!"; firstName="James"; lastName="Wilson" },
    @{ email="emily.rodriguez@crm.com"; password="Demo@2026!"; firstName="Emily"; lastName="Rodriguez" }
)
$userTokens = @{}; $userIds = @{}
$userTokens[$adminEmail] = $T; $userIds[$adminEmail] = $adminId

foreach ($u in $testUsers) {
    $body = @{ email=$u.email; password=$u.password; firstName=$u.firstName; lastName=$u.lastName; tenantId="default" } | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/auth/register" -Body $body
    if (-not ($res -and $res.data)) {
        $lb = @{ email=$u.email; password=$u.password; tenantId="default" } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/auth/login" -Body $lb
    }
    if ($res -and $res.data) {
        $userTokens[$u.email] = $res.data.accessToken
        $userIds[$u.email] = $res.data.userId
        Write-Ok "$($u.firstName) $($u.lastName) ($($u.email))"
    }
}

$sarahId = $userIds["sarah.chen@crm.com"]
$jamesId = $userIds["james.wilson@crm.com"]
$emilyId = $userIds["emily.rodriguez@crm.com"]

###############################################################################
# 2. ACCOUNTS — 8 companies
###############################################################################
Write-Step "2. Creating accounts..."

$accountData = @(
    @{ name="Acme Corporation";        industry="Technology";         website="https://acme.com";         phone="+1-415-555-1001"; annualRevenue=50000000;  numberOfEmployees=400;  type="CUSTOMER"; territory="West";    segment="ENTERPRISE";  lifecycleStage="ACTIVE" },
    @{ name="Globex Industries";       industry="Manufacturing";      website="https://globex.com";       phone="+1-212-555-2002"; annualRevenue=120000000; numberOfEmployees=1500; type="CUSTOMER"; territory="East";    segment="ENTERPRISE";  lifecycleStage="ACTIVE" },
    @{ name="Initech Solutions";       industry="Financial Services"; website="https://initech.com";      phone="+1-312-555-3003"; annualRevenue=30000000;  numberOfEmployees=200;  type="PROSPECT"; territory="Central"; segment="MID_MARKET";  lifecycleStage="EVALUATION" },
    @{ name="Hooli Tech";             industry="Technology";         website="https://hooli.com";        phone="+1-650-555-4004"; annualRevenue=80000000;  numberOfEmployees=600;  type="CUSTOMER"; territory="West";    segment="ENTERPRISE";  lifecycleStage="ACTIVE" },
    @{ name="Stark Industries";       industry="Defense";            website="https://starkindustries.com"; phone="+1-310-555-5005"; annualRevenue=200000000; numberOfEmployees=3000; type="CUSTOMER"; territory="West"; segment="ENTERPRISE"; lifecycleStage="ACTIVE" },
    @{ name="Wayne Enterprises";      industry="Conglomerate";       website="https://wayne-ent.com";    phone="+1-617-555-6006"; annualRevenue=95000000;  numberOfEmployees=800;  type="PROSPECT"; territory="East";    segment="ENTERPRISE";  lifecycleStage="ONBOARDING" },
    @{ name="Oscorp Technologies";    industry="Biotechnology";      website="https://oscorp.com";       phone="+1-718-555-7007"; annualRevenue=45000000;  numberOfEmployees=350;  type="PARTNER";  territory="East";    segment="MID_MARKET";  lifecycleStage="ACTIVE" },
    @{ name="Umbrella Analytics";     industry="Data Analytics";     website="https://umbrella-ai.com";  phone="+1-503-555-8008"; annualRevenue=15000000;  numberOfEmployees=80;   type="PROSPECT"; territory="West";    segment="SMB";         lifecycleStage="EVALUATION" }
)

$accountIds = @()
foreach ($a in $accountData) {
    $body = $a | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/accounts" -Body $body -Token $T
    if ($res -and $res.data) {
        $accountIds += $res.data.id
        Write-Ok "$($a.name) ($($a.type))"
    } else {
        $accountIds += "skip"
    }
}

###############################################################################
# 3. CONTACTS — 16 contacts linked to accounts
###############################################################################
Write-Step "3. Creating contacts..."

$contactData = @(
    @{ firstName="Alice";     lastName="Johnson";   email="alice.johnson@acme.com";        phone="+1-415-555-1101"; title="CTO";                   department="Engineering";  accountIdx=0 },
    @{ firstName="Bob";       lastName="Smith";     email="bob.smith@acme.com";            phone="+1-415-555-1102"; title="VP Sales";               department="Sales";        accountIdx=0 },
    @{ firstName="Carol";     lastName="Davis";     email="carol.davis@globex.com";        phone="+1-212-555-2101"; title="CEO";                    department="Executive";    accountIdx=1 },
    @{ firstName="David";     lastName="Wilson";    email="david.wilson@globex.com";       phone="+1-212-555-2102"; title="Head of Procurement";    department="Procurement";  accountIdx=1 },
    @{ firstName="Eve";       lastName="Miller";    email="eve.miller@initech.com";        phone="+1-312-555-3101"; title="CFO";                    department="Finance";      accountIdx=2 },
    @{ firstName="Frank";     lastName="Garcia";    email="frank.garcia@initech.com";      phone="+1-312-555-3102"; title="IT Director";            department="IT";           accountIdx=2 },
    @{ firstName="Grace";     lastName="Lee";       email="grace.lee@hooli.com";           phone="+1-650-555-4101"; title="VP Engineering";         department="Engineering";  accountIdx=3 },
    @{ firstName="Henry";     lastName="Taylor";    email="henry.taylor@hooli.com";        phone="+1-650-555-4102"; title="Product Manager";        department="Product";      accountIdx=3 },
    @{ firstName="Irene";     lastName="Stark";     email="irene.stark@starkindustries.com"; phone="+1-310-555-5101"; title="Director of Innovation"; department="R&D";        accountIdx=4 },
    @{ firstName="Jack";      lastName="Wayne";     email="jack.wayne@wayne-ent.com";      phone="+1-617-555-6101"; title="COO";                    department="Operations";   accountIdx=5 },
    @{ firstName="Karen";     lastName="Osborn";    email="karen.osborn@oscorp.com";       phone="+1-718-555-7101"; title="Head of Research";       department="R&D";          accountIdx=6 },
    @{ firstName="Leo";       lastName="Umbrella";  email="leo.umbrella@umbrella-ai.com";  phone="+1-503-555-8101"; title="Founder & CEO";          department="Executive";    accountIdx=7 },
    @{ firstName="Mia";       lastName="Chen";      email="mia.chen@acme.com";             phone="+1-415-555-1103"; title="Marketing Director";     department="Marketing";    accountIdx=0 },
    @{ firstName="Nathan";    lastName="Brown";     email="nathan.brown@globex.com";       phone="+1-212-555-2103"; title="Supply Chain Lead";      department="Supply Chain"; accountIdx=1 },
    @{ firstName="Olivia";    lastName="Martinez";  email="olivia.martinez@hooli.com";     phone="+1-650-555-4103"; title="Data Science Lead";      department="Data";         accountIdx=3 },
    @{ firstName="Peter";     lastName="Anderson";  email="peter.anderson@starkindustries.com"; phone="+1-310-555-5102"; title="Security Director"; department="Security";     accountIdx=4 }
)

$contactIds = @()
foreach ($c in $contactData) {
    $bodyObj = @{
        firstName  = $c.firstName; lastName = $c.lastName
        email      = $c.email;     phone    = $c.phone
        title      = $c.title;     department = $c.department
        emailOptIn = $true;        lifecycleStage = "CUSTOMER"; segment = "DECISION_MAKER"
    }
    if ($c.accountIdx -ge 0 -and $accountIds[$c.accountIdx] -ne "skip") {
        $bodyObj["accountId"] = $accountIds[$c.accountIdx]
    }
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/contacts" -Body ($bodyObj | ConvertTo-Json) -Token $T
    if ($res -and $res.data) {
        $contactIds += $res.data.id
        Write-Ok "$($c.firstName) $($c.lastName) - $($c.title)"
    } else { $contactIds += "skip" }
}

###############################################################################
# 4. LEADS — 12 leads in various stages
###############################################################################
Write-Step "4. Creating leads..."

$leadData = @(
    @{ firstName="Ryan";      lastName="Cooper";    email="ryan.cooper@innovatetech.com";     phone="+1-408-555-3001"; company="InnovateTech";        title="VP Engineering";       source="WEB";        description="Downloaded enterprise whitepaper. High interest in API platform." },
    @{ firstName="Samantha";  lastName="Blake";     email="samantha.blake@retailpro.com";     phone="+1-310-555-3002"; company="RetailPro Solutions";  title="Director of eCommerce"; source="TRADE_SHOW"; description="Met at RetailTech Summit. Strong omnichannel interest." },
    @{ firstName="Derek";     lastName="Chang";     email="derek.chang@biosynth.com";         phone="+1-858-555-3003"; company="BioSynth Labs";        title="Lab Director";          source="REFERRAL";   description="Referred by existing client. Looking for LIMS integration." },
    @{ firstName="Hannah";    lastName="Brooks";    email="hannah.brooks@sunrisemedia.com";   phone="+1-323-555-3004"; company="Sunrise Media Group";  title="Marketing Director";    source="SOCIAL_MEDIA"; description="Engaged on LinkedIn. Looking for marketing automation." },
    @{ firstName="Benjamin";  lastName="Grant";     email="benjamin.grant@pacificfoods.com";  phone="+1-206-555-3005"; company="Pacific Foods Co";     title="Supply Chain Manager";  source="TRADE_SHOW"; description="Visited booth at FoodTech Expo. Inventory management needs." },
    @{ firstName="Natalie";   lastName="Diaz";      email="natalie.diaz@cloudops360.com";     phone="+1-512-555-3006"; company="CloudOps 360";         title="DevOps Lead";           source="WEB";        description="Signed up for free trial. Evaluating for 50-person team." },
    @{ firstName="Tyler";     lastName="Manning";   email="tyler.manning@legalpro.com";       phone="+1-404-555-3007"; company="LegalPro Associates";  title="Managing Partner";      source="REFERRAL";   description="Referred by client. Needs legal practice CRM." },
    @{ firstName="Isabella";  lastName="Santos";    email="isabella.santos@nexushealth.com";  phone="+1-305-555-3008"; company="Nexus Health Systems"; title="VP Operations";         source="PHONE";      description="Called in after seeing ad. Needs patient management CRM." },
    @{ firstName="Jason";     lastName="Patel";     email="jason.patel@quantumrobotics.com";  phone="+1-734-555-3009"; company="Quantum Robotics";     title="CEO";                   source="WEB";        description="Requested demo. Series B startup, rapid growth." },
    @{ firstName="Catherine"; lastName="Lee";       email="catherine.lee@premier-hosp.com";   phone="+1-702-555-3010"; company="Premier Hospitality";  title="General Manager";       source="TRADE_SHOW"; description="Hotel chain. Guest relationship management." },
    @{ firstName="Andrew";    lastName="Fischer";   email="andrew.fischer@steelbridge.com";   phone="+1-412-555-3011"; company="Steelbridge Eng.";     title="Operations Director";   source="EMAIL";      description="Opened 3 emails. Requested pricing for 200 seats." },
    @{ firstName="Monica";    lastName="Graves";    email="monica.graves@horizonagri.com";    phone="+1-515-555-3012"; company="Horizon Agriculture";  title="VP Technology";         source="SOCIAL_MEDIA"; description="Commented on webinar. Agricultural CRM interest." }
)

$leadIds = @()
foreach ($l in $leadData) {
    $body = $l | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/leads" -Body $body -Token $T
    if ($res -and $res.data) {
        $leadIds += $res.data.id
        Write-Ok "$($l.firstName) $($l.lastName) - $($l.company)"
    } else { $leadIds += "skip" }
}

###############################################################################
# 5. OPPORTUNITIES — 10 deals across pipeline stages
###############################################################################
Write-Step "5. Creating opportunities..."

$oppData = @(
    @{ name="Acme Corp Platform License";       accountIdx=0; contactIdx=0; amount=450000;  stage="NEGOTIATION";    probability=75; closeInDays=15;  description="Annual platform license + support. Final pricing discussion.";    assignee=$sarahId; forecastCategory="COMMIT" },
    @{ name="Globex Manufacturing Suite";        accountIdx=1; contactIdx=2; amount=850000;  stage="PROPOSAL";       probability=60; closeInDays=30;  description="Full manufacturing CRM for 5 plants. RFP submitted.";           assignee=$sarahId; forecastCategory="BEST_CASE" },
    @{ name="Initech Financial Module";          accountIdx=2; contactIdx=4; amount=280000;  stage="NEEDS_ANALYSIS"; probability=40; closeInDays=60;  description="Risk analytics + compliance module. Requirements gathering.";    assignee=$jamesId; forecastCategory="PIPELINE" },
    @{ name="Hooli AI Analytics Add-on";         accountIdx=3; contactIdx=6; amount=520000;  stage="QUALIFICATION";  probability=30; closeInDays=75;  description="AI analytics module for existing customer. Budget pending.";     assignee=$jamesId; forecastCategory="PIPELINE" },
    @{ name="Stark Industries Enterprise ESP";   accountIdx=4; contactIdx=8; amount=1200000; stage="NEGOTIATION";    probability=80; closeInDays=10;  description="Enterprise security platform. Contract review stage.";           assignee=$emilyId; forecastCategory="COMMIT" },
    @{ name="Wayne Enterprise Integration";      accountIdx=5; contactIdx=9; amount=380000;  stage="PROPOSAL";       probability=55; closeInDays=45;  description="ERP-CRM bridge for operations tracking.";                       assignee=$emilyId; forecastCategory="BEST_CASE" },
    @{ name="Oscorp Research Portal";            accountIdx=6; contactIdx=10; amount=150000; stage="CLOSED_WON";     probability=100; closeInDays=-5; description="Research collaboration portal. Deal closed!";                   assignee=$sarahId; forecastCategory="CLOSED" },
    @{ name="Umbrella Analytics Dashboard";      accountIdx=7; contactIdx=11; amount=95000;  stage="PROSPECTING";    probability=15; closeInDays=90;  description="Custom analytics dashboard. Initial discovery call.";            assignee=$jamesId; forecastCategory="PIPELINE" },
    @{ name="Acme Corp Data Migration";          accountIdx=0; contactIdx=1; amount=75000;   stage="CLOSED_WON";     probability=100; closeInDays=-20; description="Data migration from legacy system. Successfully deployed.";     assignee=$emilyId; forecastCategory="CLOSED" },
    @{ name="Globex Supply Chain Upgrade";       accountIdx=1; contactIdx=3; amount=180000;  stage="CLOSED_LOST";    probability=0;  closeInDays=-10; description="Supply chain module. Lost to competitor on pricing.";            assignee=$jamesId; forecastCategory="CLOSED" }
)

$oppIds = @()
foreach ($o in $oppData) {
    $bodyObj = @{
        name             = $o.name
        amount           = $o.amount
        stage            = $o.stage
        probability      = $o.probability
        closeDate        = (Get-Date).AddDays($o.closeInDays).ToString("yyyy-MM-dd")
        description      = $o.description
        forecastCategory = $o.forecastCategory
    }
    if ($o.accountIdx -ge 0 -and $accountIds[$o.accountIdx] -ne "skip") { $bodyObj["accountId"] = $accountIds[$o.accountIdx] }
    if ($o.contactIdx -ge 0 -and $contactIds[$o.contactIdx] -ne "skip") { $bodyObj["contactId"] = $contactIds[$o.contactIdx] }
    if ($o.assignee) { $bodyObj["assignedTo"] = $o.assignee }

    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/opportunities" -Body ($bodyObj | ConvertTo-Json) -Token $T
    if ($res -and $res.data) {
        $oppIds += $res.data.id
        Write-Ok "$($o.name) ($($o.stage) - `$$($o.amount))"
    } else { $oppIds += "skip" }
}

###############################################################################
# 6. ACTIVITIES — 15 activities linked to entities
###############################################################################
Write-Step "6. Creating activities..."

$actData = @(
    @{ subject="Discovery Call with Acme";      type="CALL";    status="COMPLETED"; relatedTo="OPPORTUNITY"; entityIdx=0; dueInDays=-3; description="Initial discovery call. Discussed requirements and timeline." },
    @{ subject="Product Demo - Globex";         type="MEETING"; status="COMPLETED"; relatedTo="OPPORTUNITY"; entityIdx=1; dueInDays=-5; description="Full product demo to procurement team. Very positive feedback." },
    @{ subject="Follow-up Email - Initech";     type="EMAIL";   status="COMPLETED"; relatedTo="OPPORTUNITY"; entityIdx=2; dueInDays=-1; description="Sent pricing comparison and ROI analysis." },
    @{ subject="Proposal Review - Hooli";       type="MEETING"; status="SCHEDULED"; relatedTo="OPPORTUNITY"; entityIdx=3; dueInDays=3;  description="Review proposal with VP Engineering and Product Manager." },
    @{ subject="Contract Negotiation - Stark";  type="MEETING"; status="SCHEDULED"; relatedTo="OPPORTUNITY"; entityIdx=4; dueInDays=2;  description="Final contract terms negotiation with procurement." },
    @{ subject="Quarterly Business Review";     type="MEETING"; status="SCHEDULED"; relatedTo="ACCOUNT";     entityIdx=0; dueInDays=7;  description="Q1 QBR with Acme Corp. Review usage, roadmap, renewal." },
    @{ subject="Follow up on lead: Ryan";       type="CALL";    status="SCHEDULED"; relatedTo="LEAD";        entityIdx=0; dueInDays=1;  description="Schedule technical demo for InnovateTech team." },
    @{ subject="Send whitepaper to Samantha";   type="TASK";    status="COMPLETED"; relatedTo="LEAD";        entityIdx=1; dueInDays=-2; description="Sent omnichannel retail whitepaper as discussed." },
    @{ subject="Technical Assessment - BioSynth"; type="MEETING"; status="SCHEDULED"; relatedTo="LEAD";      entityIdx=2; dueInDays=5;  description="On-site technical assessment for LIMS integration." },
    @{ subject="Check Wayne Onboarding";        type="TASK";    status="IN_PROGRESS"; relatedTo="ACCOUNT";   entityIdx=5; dueInDays=0;  description="Verify Wayne Enterprise onboarding milestones are on track." },
    @{ subject="Renewal Discussion - Hooli";    type="CALL";    status="SCHEDULED"; relatedTo="ACCOUNT";     entityIdx=3; dueInDays=14; description="Annual renewal discussion. May upsell AI module." },
    @{ subject="LinkedIn follow-up Hannah";     type="TASK";    status="COMPLETED"; relatedTo="LEAD";        entityIdx=3; dueInDays=-4; description="Connected and sent personalized message on LinkedIn." },
    @{ subject="Demo prep for Pacific Foods";   type="TASK";    status="SCHEDULED"; relatedTo="LEAD";        entityIdx=4; dueInDays=2;  description="Prepare custom demo with supply chain workflows." },
    @{ subject="Send proposal to CloudOps";     type="EMAIL";   status="SCHEDULED"; relatedTo="LEAD";        entityIdx=5; dueInDays=1;  description="Prepare and send custom pricing proposal." },
    @{ subject="Oscorp Implementation Kickoff"; type="MEETING"; status="SCHEDULED"; relatedTo="OPPORTUNITY"; entityIdx=6; dueInDays=7;  description="Kickoff meeting for Oscorp Research Portal implementation." }
)

foreach ($a in $actData) {
    $bodyObj = @{
        subject     = $a.subject
        type        = $a.type
        status      = $a.status
        dueDate     = (Get-Date).AddDays($a.dueInDays).ToString("yyyy-MM-ddTHH:mm:ss")
        description = $a.description
    }
    # Map entity references
    if ($a.relatedTo -eq "OPPORTUNITY" -and $oppIds[$a.entityIdx] -ne "skip") {
        $bodyObj["entityType"] = "OPPORTUNITY"; $bodyObj["entityId"] = $oppIds[$a.entityIdx]
    } elseif ($a.relatedTo -eq "ACCOUNT" -and $accountIds[$a.entityIdx] -ne "skip") {
        $bodyObj["entityType"] = "ACCOUNT"; $bodyObj["entityId"] = $accountIds[$a.entityIdx]
    } elseif ($a.relatedTo -eq "LEAD" -and $leadIds[$a.entityIdx] -ne "skip") {
        $bodyObj["entityType"] = "LEAD"; $bodyObj["entityId"] = $leadIds[$a.entityIdx]
    }

    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/activities" -Body ($bodyObj | ConvertTo-Json) -Token $T
    if ($res -and $res.data) { Write-Ok "$($a.type) - $($a.subject)" }
}

###############################################################################
# 7. CASES — 8 support cases
###############################################################################
Write-Step "7. Creating support cases..."

$caseData = @(
    @{ subject="Login issues with SSO integration";     priority="HIGH";     accountIdx=0; contactIdx=0; description="User Alice Johnson reports SSO login fails intermittently. Error: Invalid redirect URI. Affects 5+ users at Acme Corp. Browser: Chrome 120+." },
    @{ subject="Billing discrepancy on Q1 invoice";     priority="MEDIUM";   accountIdx=1; contactIdx=3; description="Invoice #INV-2026-Q1 shows $12,500 but contract states $11,800. Difference is from unapproved add-on charges. Need credit memo." },
    @{ subject="Feature request: Bulk export to CSV";   priority="LOW";      accountIdx=2; contactIdx=4; description="Eve Miller requests ability to export 10K+ financial records at once. Currently limited to 1000. Needed for quarterly compliance reporting." },
    @{ subject="Performance degradation on reports";    priority="CRITICAL"; accountIdx=3; contactIdx=6; description="Report dashboard takes 30+ seconds to load since last update. Affects all users at Hooli Tech. Query optimization needed urgently." },
    @{ subject="Unable to attach files larger than 5MB"; priority="MEDIUM";  accountIdx=4; contactIdx=8; description="Irene Stark reports file upload fails silently for files over 5MB. Need to support up to 25MB for engineering documents." },
    @{ subject="Email integration sync failure";        priority="HIGH";     accountIdx=0; contactIdx=1; description="Bob Smith's email sync stopped 3 days ago. Emails sent from CRM not appearing in Outlook. OAuth token may need refresh." },
    @{ subject="Mobile app crash on Android 14";        priority="HIGH";     accountIdx=5; contactIdx=9; description="Jack Wayne reports mobile app crashes when opening opportunity detail on Samsung Galaxy S24 with Android 14. Reproducible every time." },
    @{ subject="API rate limiting too aggressive";      priority="MEDIUM";   accountIdx=6; contactIdx=10; description="Karen Osborn's integration hitting rate limits at 50 req/min. Their data sync requires 200+ req/min during batch processing windows." }
)

$caseIds = @()
foreach ($c in $caseData) {
    $bodyObj = @{
        subject     = $c.subject
        description = $c.description
        priority    = $c.priority
        origin      = "PORTAL"
    }
    if ($c.accountIdx -ge 0 -and $accountIds[$c.accountIdx] -ne "skip") {
        $bodyObj["accountId"]   = $accountIds[$c.accountIdx]
        $bodyObj["accountName"] = $accountData[$c.accountIdx].name
    }
    if ($c.contactIdx -ge 0 -and $contactIds[$c.contactIdx] -ne "skip") {
        $bodyObj["contactId"]   = $contactIds[$c.contactIdx]
        $bodyObj["contactName"] = "$($contactData[$c.contactIdx].firstName) $($contactData[$c.contactIdx].lastName)"
        $bodyObj["contactEmail"] = $contactData[$c.contactIdx].email
    }

    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/cases" -Body ($bodyObj | ConvertTo-Json) -Token $T
    if ($res -and $res.data) {
        $caseIds += $res.data.id
        Write-Ok "$($c.priority) - $($c.subject)"
    } else { $caseIds += "skip" }
}

# Resolve one case and escalate another
if ($caseIds.Count -ge 5 -and $caseIds[4] -ne "skip") {
    Invoke-Api -Method PATCH -Uri "$BASE/api/v1/cases/$($caseIds[4])/resolve?resolutionNotes=Increased+file+size+limit+to+25MB+in+config" -Token $T | Out-Null
    Write-Ok "Resolved: Unable to attach files"
}
if ($caseIds.Count -ge 4 -and $caseIds[3] -ne "skip") {
    Invoke-Api -Method PATCH -Uri "$BASE/api/v1/cases/$($caseIds[3])/escalate" -Token $T | Out-Null
    Write-Ok "Escalated: Performance degradation"
}

###############################################################################
# 8. CAMPAIGNS — 6 marketing campaigns
###############################################################################
Write-Step "8. Creating marketing campaigns..."

$campaignData = @(
    @{ name="Q1 2026 Email Nurture Series";       type="EMAIL";    status="COMPLETED"; startDate="2026-01-05"; endDate="2026-03-15"; budget=5000;  expectedRevenue=50000;  description="12-week email drip campaign targeting mid-market prospects. 8 touchpoints with case studies and ROI calculators." },
    @{ name="Product Launch Webinar - AI Suite";   type="WEBINAR";  status="ACTIVE";    startDate="2026-03-01"; endDate="2026-03-30"; budget=8000;  expectedRevenue=120000; description="Monthly webinar showcasing new AI analytics suite. Demo + Q&A format. Target: 200 registrants per session." },
    @{ name="LinkedIn Enterprise Campaign";        type="PAID_ADS"; status="ACTIVE";    startDate="2026-02-01"; endDate="2026-05-31"; budget=15000; expectedRevenue=250000; description="Sponsored content + InMail targeting C-suite at Fortune 500. Focus on analytics and automation messaging." },
    @{ name="Spring Tech Conference 2026";         type="EVENT";    status="PLANNED";   startDate="2026-05-10"; endDate="2026-05-12"; budget=25000; expectedRevenue=500000; description="Booth at TechConnect 2026. Product demos, swag, lead scanners. Expected 3000+ attendees." },
    @{ name="Content Marketing - Success Stories"; type="CONTENT";  status="ACTIVE";    startDate="2026-01-15"; endDate="2026-06-30"; budget=3500;  expectedRevenue=80000;  description="Bi-weekly blog posts + 6 customer case study videos. SEO-optimized targeting CRM-related keywords." },
    @{ name="Google Ads Retargeting";              type="PAID_ADS"; status="ACTIVE";    startDate="2026-03-01"; endDate="2026-06-30"; budget=12000; expectedRevenue=180000; description="Retarget website visitors who viewed pricing page but didn't convert. Display + search ads." }
)

$campaignIds = @()
foreach ($c in $campaignData) {
    $body = $c | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/campaigns" -Body $body -Token $T
    if ($res -and $res.data) {
        $campaignIds += $res.data.id
        Write-Ok "$($c.name) ($($c.type) - $($c.status))"
    } else { $campaignIds += "skip" }
}

###############################################################################
# 9. WORKFLOWS — 5 automation rules
###############################################################################
Write-Step "9. Creating workflow rules..."

$wfData = @(
    @{ name="Auto-assign Hot Leads";        entityType="LEAD";        triggerType="ON_CREATE"; enabled=$true;  description="Automatically assign leads with score > 80 to senior reps via round-robin." },
    @{ name="Follow-up Reminder (3 days)";  entityType="LEAD";        triggerType="ON_UPDATE"; enabled=$true;  description="Create follow-up task if lead not contacted within 3 business days." },
    @{ name="Stale Deal Alert";             entityType="OPPORTUNITY";  triggerType="SCHEDULED"; enabled=$true;  description="Alert owner when opportunity has no activity for 14+ days." },
    @{ name="Welcome Email on Signup";      entityType="CONTACT";     triggerType="ON_CREATE"; enabled=$true;  description="Send welcome email template when new contact is created from web form." },
    @{ name="Escalate P1 Cases";            entityType="CASE";        triggerType="ON_CREATE"; enabled=$false; description="Auto-escalate cases with CRITICAL priority to management queue." }
)

foreach ($w in $wfData) {
    $body = $w | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/workflows" -Body $body -Token $T
    if ($res -and $res.data) { Write-Ok "$($w.name) (enabled=$($w.enabled))" }
}

###############################################################################
# 10. EMAIL TEMPLATES — 4 templates
###############################################################################
Write-Step "10. Creating email templates..."

$templateData = @(
    @{ name="Welcome Email"; subject="Welcome to {{companyName}}, {{firstName}}!"; body="<h2>Welcome aboard, {{firstName}}!</h2><p>We're thrilled to have {{companyName}} as a customer. Your dedicated account manager is {{repName}}.</p><p>Here are your next steps:</p><ul><li>Complete your profile setup</li><li>Import your contacts</li><li>Schedule your onboarding call</li></ul><p>Best regards,<br>The CRM Team</p>"; category="ONBOARDING" },
    @{ name="Follow-up After Demo"; subject="Thanks for the demo, {{firstName}}!"; body="<h2>Great meeting you, {{firstName}}!</h2><p>Thanks for taking the time to see our platform in action. As discussed, here's a summary:</p><ul><li>Key features: {{keyFeatures}}</li><li>Pricing: {{pricingTier}}</li><li>Next step: {{nextStep}}</li></ul><p>I'll follow up on {{followUpDate}}.</p><p>Best,<br>{{repName}}</p>"; category="SALES" },
    @{ name="Quarterly Business Review"; subject="Q{{quarter}} Business Review - {{accountName}}"; body="<h2>Q{{quarter}} Business Review</h2><p>Dear {{firstName}},</p><p>Here's your quarterly performance summary for {{accountName}}:</p><ul><li>Active users: {{activeUsers}}</li><li>Feature adoption: {{adoptionRate}}%</li><li>Support tickets: {{ticketCount}}</li></ul><p>Let's schedule a call to discuss your roadmap.</p>"; category="ACCOUNT_MANAGEMENT" },
    @{ name="Case Resolution Notification"; subject="[{{caseNumber}}] Your issue has been resolved"; body="<h2>Issue Resolved</h2><p>Hi {{firstName}},</p><p>Great news! Your support case <strong>{{caseNumber}}</strong> has been resolved.</p><p><strong>Resolution:</strong> {{resolutionNotes}}</p><p>If you have any further questions, reply to this email or open a new case.</p><p>Best,<br>Support Team</p>"; category="SUPPORT" }
)

foreach ($t in $templateData) {
    $body = $t | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/emails/templates" -Body $body -Token $T
    if ($res -and $res.data) { Write-Ok "$($t.name)" }
}

###############################################################################
# 11. NOTES — Add notes to leads, accounts, opportunities
###############################################################################
Write-Step "11. Adding notes to records..."

$noteData = @(
    @{ entityType="lead";        entityIdx=0; content="Initial discovery call went well. Ryan is very interested in the API platform. Need to schedule product demo with his engineering team." },
    @{ entityType="lead";        entityIdx=1; content="Met at RetailTech Summit. Samantha mentioned $500K budget allocated for Q2. Main competitors: Salesforce and HubSpot." },
    @{ entityType="lead";        entityIdx=4; content="Benjamin needs supply chain visibility dashboard. Requires integration with their JDE ERP. Budget decision in April." },
    @{ entityType="lead";        entityIdx=8; content="CEO directly - fast-moving decision maker. Series B raised $40M. Looking to deploy in 30 days." },
    @{ entityType="account";     entityIdx=0; content="Acme Corp is our flagship customer. Annual contract renewal in June. They want early access to AI features." },
    @{ entityType="account";     entityIdx=1; content="Globex expanded to 5 plants this quarter. Opportunity for additional seat licenses. Decision-maker is Carol Davis (CEO)." },
    @{ entityType="account";     entityIdx=4; content="Stark Industries security team passed our SOC 2 audit with flying colors. Great reference customer for enterprise security." },
    @{ entityType="opportunity"; entityIdx=0; content="Alice confirmed budget approved for $450K. Procurement reviewing contract. Expected close by end of month." },
    @{ entityType="opportunity"; entityIdx=1; content="RFP response submitted. Competing with 3 vendors. Our differentiator: manufacturing-specific workflows." },
    @{ entityType="opportunity"; entityIdx=4; content="Stark legal team reviewing MSA. Main concern: data residency requirements. We support US-only deployment." }
)

foreach ($n in $noteData) {
    $uri = $null
    if ($n.entityType -eq "lead" -and $leadIds[$n.entityIdx] -ne "skip") {
        $uri = "$BASE/api/v1/leads/$($leadIds[$n.entityIdx])/notes"
    } elseif ($n.entityType -eq "account" -and $accountIds[$n.entityIdx] -ne "skip") {
        $uri = "$BASE/api/v1/accounts/$($accountIds[$n.entityIdx])/notes"
    } elseif ($n.entityType -eq "opportunity" -and $oppIds[$n.entityIdx] -ne "skip") {
        $uri = "$BASE/api/v1/opportunities/$($oppIds[$n.entityIdx])/notes"
    }
    if ($uri) {
        $body = @{ content = $n.content } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri $uri -Body $body -Token $T
        if ($res) { Write-Ok "Note on $($n.entityType) #$($n.entityIdx)" }
    }
}

###############################################################################
# 12. OPPORTUNITY PRODUCTS — Line items on key deals
###############################################################################
Write-Step "12. Adding products to opportunities..."

$productData = @(
    @{ oppIdx=0; name="Platform License (Annual)";   quantity=1;  unitPrice=350000 },
    @{ oppIdx=0; name="Premium Support Package";     quantity=1;  unitPrice=100000 },
    @{ oppIdx=1; name="Manufacturing Module";        quantity=5;  unitPrice=120000 },
    @{ oppIdx=1; name="Custom Integration Services"; quantity=1;  unitPrice=250000 },
    @{ oppIdx=4; name="Enterprise Security Platform"; quantity=1; unitPrice=800000 },
    @{ oppIdx=4; name="Implementation Services";     quantity=1;  unitPrice=400000 },
    @{ oppIdx=6; name="Research Portal License";     quantity=1;  unitPrice=120000 },
    @{ oppIdx=6; name="Data Migration Services";     quantity=1;  unitPrice=30000  }
)

foreach ($p in $productData) {
    if ($oppIds[$p.oppIdx] -ne "skip") {
        $body = @{ name=$p.name; quantity=$p.quantity; unitPrice=$p.unitPrice } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/opportunities/$($oppIds[$p.oppIdx])/products" -Body $body -Token $T
        if ($res) { Write-Ok "$($p.name) x$($p.quantity) @ `$$($p.unitPrice)" }
    }
}

###############################################################################
# 13. COMPETITORS — On negotiation/proposal deals
###############################################################################
Write-Step "13. Adding competitors to opportunities..."

$compData = @(
    @{ oppIdx=0; name="Salesforce"; strengths="Market leader, ecosystem, AppExchange"; weaknesses="Expensive, complex implementation, 6-month minimum"; threatLevel="HIGH" },
    @{ oppIdx=0; name="HubSpot";   strengths="Easy to use, good marketing tools";     weaknesses="Limited enterprise features, data limits"; threatLevel="MEDIUM" },
    @{ oppIdx=1; name="SAP CRM";   strengths="ERP integration, manufacturing expertise"; weaknesses="Very expensive, long deployment, rigid"; threatLevel="HIGH" },
    @{ oppIdx=3; name="Tableau";    strengths="Best-in-class visualization";          weaknesses="No CRM, separate analytics tool"; threatLevel="LOW" },
    @{ oppIdx=4; name="Microsoft Dynamics"; strengths="Office 365 integration, Azure"; weaknesses="Complexity, licensing confusion"; threatLevel="MEDIUM" },
    @{ oppIdx=5; name="Oracle CRM"; strengths="Enterprise features, database";        weaknesses="Legacy UI, expensive, slow"; threatLevel="LOW" }
)

foreach ($c in $compData) {
    if ($oppIds[$c.oppIdx] -ne "skip") {
        $body = @{ name=$c.name; strengths=$c.strengths; weaknesses=$c.weaknesses; threatLevel=$c.threatLevel } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "$BASE/api/v1/opportunities/$($oppIds[$c.oppIdx])/competitors" -Body $body -Token $T
        if ($res) { Write-Ok "$($c.name) on deal #$($c.oppIdx)" }
    }
}

###############################################################################
# SUMMARY
###############################################################################
Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  E2E Test Data Seeding Complete!" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Created:" -ForegroundColor White
Write-Host "    4 Users        (demo admin + 3 team members)" -ForegroundColor Green
Write-Host "    8 Accounts     (enterprise, mid-market, SMB)" -ForegroundColor Green
Write-Host "   16 Contacts     (linked to accounts)" -ForegroundColor Green
Write-Host "   12 Leads        (various sources and stages)" -ForegroundColor Green
Write-Host "   10 Opportunities (full pipeline incl. won/lost)" -ForegroundColor Green
Write-Host "   15 Activities   (calls, meetings, tasks, emails)" -ForegroundColor Green
Write-Host "    8 Cases        (various priorities, 1 resolved, 1 escalated)" -ForegroundColor Green
Write-Host "    6 Campaigns    (email, webinar, ads, events, content)" -ForegroundColor Green
Write-Host "    5 Workflows    (auto-assign, follow-up, alerts)" -ForegroundColor Green
Write-Host "    4 Email Tmpl   (welcome, follow-up, QBR, resolution)" -ForegroundColor Green
Write-Host "   10 Notes        (on leads, accounts, opportunities)" -ForegroundColor Green
Write-Host "    8 Products     (line items on key deals)" -ForegroundColor Green
Write-Host "    6 Competitors  (on negotiation deals)" -ForegroundColor Green
Write-Host ""
Write-Host "  Total: 108 records across 13 modules" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Login: demo@crm.com / Demo@2026!" -ForegroundColor Yellow
Write-Host "  URL:   $BASE" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Green
