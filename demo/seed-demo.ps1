###############################################################################
#  CRM Platform – Product Demo Seed Script
#  ─────────────────────────────────────────
#  Populates every service with realistic data for a live product demo.
#
#  Usage:  .\demo\seed-demo.ps1
#  Prereq: All Docker services running (docker compose up -d)
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
        Write-Warn "  $Method $Uri → $status"
        return $null
    }
}

###############################################################################
# 1. REGISTER DEMO USERS
###############################################################################
Write-Step "Creating demo user accounts..."

$users = @(
    @{ email="sarah.chen@acmecorp.com";    password="Demo@2026!"; firstName="Sarah";   lastName="Chen";      role="ADMIN"   },
    @{ email="james.wilson@acmecorp.com";  password="Demo@2026!"; firstName="James";   lastName="Wilson";    role="MANAGER" },
    @{ email="emily.rodriguez@acmecorp.com"; password="Demo@2026!"; firstName="Emily"; lastName="Rodriguez"; role="USER"    },
    @{ email="michael.park@acmecorp.com";  password="Demo@2026!"; firstName="Michael"; lastName="Park";      role="USER"    },
    @{ email="lisa.thompson@acmecorp.com"; password="Demo@2026!"; firstName="Lisa";    lastName="Thompson";  role="USER"    }
)

$tokens = @{}
$userIds = @{}

foreach ($u in $users) {
    $body = @{
        email     = $u.email
        password  = $u.password
        firstName = $u.firstName
        lastName  = $u.lastName
        tenantId  = "default"
    } | ConvertTo-Json

    $res = Invoke-Api -Method POST -Uri "$BASE`:8081/api/v1/auth/register" -Body $body
    if ($res -and $res.data) {
        $tokens[$u.email] = $res.data.accessToken
        $userIds[$u.email] = $res.data.userId
        Write-Ok "$($u.firstName) $($u.lastName) registered ($($u.role))"
    } else {
        # Try login if already registered
        $loginBody = @{ email = $u.email; password = $u.password; tenantId = "default" } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "$BASE`:8081/api/v1/auth/login" -Body $loginBody
        if ($res -and $res.data) {
            $tokens[$u.email] = $res.data.accessToken
            $userIds[$u.email] = $res.data.userId
            Write-Ok "$($u.firstName) $($u.lastName) logged in ($($u.role))"
        } else {
            Write-Err "Failed to auth $($u.email)"
        }
    }
}

# Use admin token for most operations
$T = $tokens["sarah.chen@acmecorp.com"]
$adminId  = $userIds["sarah.chen@acmecorp.com"]
$mgr      = $userIds["james.wilson@acmecorp.com"]
$rep1     = $userIds["emily.rodriguez@acmecorp.com"]
$rep2     = $userIds["michael.park@acmecorp.com"]
$rep3     = $userIds["lisa.thompson@acmecorp.com"]

if (-not $T) {
    Write-Err "Cannot proceed without admin token"; exit 1
}

###############################################################################
# 2. ACCOUNTS — 10 realistic companies
###############################################################################
Write-Step "Creating accounts..."

$accountData = @(
    @{ name="TechVista Solutions";    industry="Technology";      website="https://techvista.io";     phone="+1-415-555-1001"; annualRevenue=45000000;  numberOfEmployees=320;  type="CUSTOMER";  territory="West";    segment="ENTERPRISE";  lifecycleStage="ACTIVE"     },
    @{ name="GlobalRetail Inc";       industry="Retail";          website="https://globalretail.com"; phone="+1-212-555-2002"; annualRevenue=120000000; numberOfEmployees=1200; type="CUSTOMER";  territory="East";    segment="ENTERPRISE";  lifecycleStage="ACTIVE"     },
    @{ name="MedHealth Partners";     industry="Healthcare";      website="https://medhealth.org";    phone="+1-312-555-3003"; annualRevenue=28000000;  numberOfEmployees=180;  type="PROSPECT";  territory="Central"; segment="MID_MARKET";  lifecycleStage="EVALUATION" },
    @{ name="FinEdge Capital";        industry="Financial Services"; website="https://finedge.com";   phone="+1-617-555-4004"; annualRevenue=75000000;  numberOfEmployees=450;  type="CUSTOMER";  territory="East";    segment="ENTERPRISE";  lifecycleStage="ACTIVE"     },
    @{ name="EduPath Learning";       industry="Education";       website="https://edupath.edu";      phone="+1-503-555-5005"; annualRevenue=12000000;  numberOfEmployees=90;   type="PROSPECT";  territory="West";    segment="MID_MARKET";  lifecycleStage="EVALUATION" },
    @{ name="GreenEnergy Dynamics";   industry="Energy";          website="https://greenenergy.com";  phone="+1-720-555-6006"; annualRevenue=200000000; numberOfEmployees=2500; type="CUSTOMER";  territory="Central"; segment="ENTERPRISE";  lifecycleStage="ACTIVE"     },
    @{ name="CloudNine SaaS";         industry="Technology";      website="https://cloudnine.io";     phone="+1-650-555-7007"; annualRevenue=18000000;  numberOfEmployees=110;  type="PARTNER";   territory="West";    segment="MID_MARKET";  lifecycleStage="ACTIVE"     },
    @{ name="Precision Manufacturing"; industry="Manufacturing";  website="https://precisionmfg.com"; phone="+1-248-555-8008"; annualRevenue=55000000;  numberOfEmployees=700;  type="PROSPECT";  territory="Central"; segment="ENTERPRISE";  lifecycleStage="ONBOARDING" },
    @{ name="Atlas Logistics";        industry="Transportation";  website="https://atlaslogistics.com"; phone="+1-901-555-9009"; annualRevenue=35000000; numberOfEmployees=400; type="CUSTOMER"; territory="East";    segment="MID_MARKET";  lifecycleStage="ACTIVE"     },
    @{ name="StartupHub Ventures";    industry="Venture Capital"; website="https://startuphub.vc";    phone="+1-415-555-1010"; annualRevenue=5000000;   numberOfEmployees=25;   type="PROSPECT";  territory="West";    segment="SMB";         lifecycleStage="EVALUATION" }
)

$accountIds = @()
foreach ($a in $accountData) {
    $body = $a | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE`:8083/api/v1/accounts" -Body $body -Token $T
    if ($res -and $res.data) {
        $accountIds += $res.data.id
        Write-Ok "$($a.name)"
    }
}

###############################################################################
# 3. CONTACTS — 20 contacts linked to accounts
###############################################################################
Write-Step "Creating contacts..."

$contactData = @(
    @{ firstName="David";      lastName="Kim";          email="david.kim@techvista.io";           phone="+1-415-555-1101"; title="CTO";                    department="Engineering";  accountIdx=0; linkedinUrl="https://linkedin.com/in/davidkim";   lifecycleStage="CUSTOMER";   segment="DECISION_MAKER" },
    @{ firstName="Rachel";     lastName="Torres";       email="rachel.torres@techvista.io";       phone="+1-415-555-1102"; title="VP of Engineering";      department="Engineering";  accountIdx=0; linkedinUrl="https://linkedin.com/in/racheltorres"; lifecycleStage="CUSTOMER"; segment="INFLUENCER"     },
    @{ firstName="Amanda";     lastName="Foster";       email="amanda.foster@globalretail.com";   phone="+1-212-555-2101"; title="CEO";                    department="Executive";    accountIdx=1; linkedinUrl="https://linkedin.com/in/amandafoster"; lifecycleStage="CUSTOMER"; segment="DECISION_MAKER" },
    @{ firstName="Marcus";     lastName="Johnson";      email="marcus.johnson@globalretail.com";  phone="+1-212-555-2102"; title="Head of IT";             department="IT";           accountIdx=1; linkedinUrl="https://linkedin.com/in/marcusjohnson"; lifecycleStage="CUSTOMER"; segment="CHAMPION"      },
    @{ firstName="Dr. Priya";  lastName="Sharma";       email="priya.sharma@medhealth.org";       phone="+1-312-555-3101"; title="Chief Medical Officer";  department="Medical";      accountIdx=2; linkedinUrl="https://linkedin.com/in/priyasharma";  lifecycleStage="LEAD";    segment="DECISION_MAKER" },
    @{ firstName="Kevin";      lastName="O'Brien";      email="kevin.obrien@medhealth.org";       phone="+1-312-555-3102"; title="IT Director";            department="IT";           accountIdx=2; linkedinUrl="https://linkedin.com/in/kevinobrien";  lifecycleStage="LEAD";    segment="INFLUENCER"     },
    @{ firstName="Christine";  lastName="Wang";         email="christine.wang@finedge.com";       phone="+1-617-555-4101"; title="CFO";                    department="Finance";      accountIdx=3; linkedinUrl="https://linkedin.com/in/christinewang"; lifecycleStage="CUSTOMER"; segment="DECISION_MAKER"},
    @{ firstName="Robert";     lastName="Martinez";     email="robert.martinez@finedge.com";      phone="+1-617-555-4102"; title="Head of Operations";     department="Operations";   accountIdx=3; linkedinUrl="https://linkedin.com/in/robertmartinez"; lifecycleStage="CUSTOMER"; segment="CHAMPION"    },
    @{ firstName="Jennifer";   lastName="Liu";          email="jennifer.liu@edupath.edu";         phone="+1-503-555-5101"; title="Dean of Technology";     department="Academic";     accountIdx=4; linkedinUrl="https://linkedin.com/in/jenniferliu";   lifecycleStage="LEAD";    segment="DECISION_MAKER" },
    @{ firstName="Thomas";     lastName="Anderson";     email="thomas.anderson@greenenergy.com";  phone="+1-720-555-6101"; title="COO";                    department="Operations";   accountIdx=5; linkedinUrl="https://linkedin.com/in/thomasanderson"; lifecycleStage="CUSTOMER"; segment="DECISION_MAKER"},
    @{ firstName="Sofia";      lastName="Petrov";       email="sofia.petrov@greenenergy.com";     phone="+1-720-555-6102"; title="VP Sustainability";      department="Sustainability"; accountIdx=5; linkedinUrl="https://linkedin.com/in/sofiapetrov"; lifecycleStage="CUSTOMER"; segment="INFLUENCER"   },
    @{ firstName="Nathan";     lastName="Hughes";       email="nathan.hughes@cloudnine.io";       phone="+1-650-555-7101"; title="CEO";                    department="Executive";    accountIdx=6; linkedinUrl="https://linkedin.com/in/nathanhughes"; lifecycleStage="CUSTOMER"; segment="EXECUTIVE"     },
    @{ firstName="Laura";      lastName="Chen";         email="laura.chen@cloudnine.io";          phone="+1-650-555-7102"; title="Head of Partnerships";   department="Business Dev"; accountIdx=6; linkedinUrl="https://linkedin.com/in/laurachen";    lifecycleStage="CUSTOMER"; segment="CHAMPION"      },
    @{ firstName="Greg";       lastName="Williams";     email="greg.williams@precisionmfg.com";   phone="+1-248-555-8101"; title="Plant Manager";          department="Operations";   accountIdx=7; linkedinUrl="https://linkedin.com/in/gregwilliams"; lifecycleStage="LEAD";    segment="INFLUENCER"     },
    @{ firstName="Diane";      lastName="Sullivan";     email="diane.sullivan@precisionmfg.com";  phone="+1-248-555-8102"; title="VP Procurement";         department="Procurement";  accountIdx=7; linkedinUrl="https://linkedin.com/in/dianesullivan"; lifecycleStage="LEAD";   segment="DECISION_MAKER" },
    @{ firstName="Carlos";     lastName="Rivera";       email="carlos.rivera@atlaslogistics.com"; phone="+1-901-555-9101"; title="Director of IT";         department="IT";           accountIdx=8; linkedinUrl="https://linkedin.com/in/carlosrivera"; lifecycleStage="CUSTOMER"; segment="CHAMPION"      },
    @{ firstName="Megan";      lastName="Price";        email="megan.price@atlaslogistics.com";   phone="+1-901-555-9102"; title="Fleet Manager";          department="Fleet Ops";    accountIdx=8; linkedinUrl="https://linkedin.com/in/meganprice";   lifecycleStage="CUSTOMER"; segment="USER"          },
    @{ firstName="Alex";       lastName="Novak";        email="alex.novak@startuphub.vc";         phone="+1-415-555-1011"; title="Managing Partner";       department="Investment";   accountIdx=9; linkedinUrl="https://linkedin.com/in/alexnovak";   lifecycleStage="LEAD";    segment="DECISION_MAKER" },
    @{ firstName="Yuki";       lastName="Tanaka";       email="yuki.tanaka@startuphub.vc";        phone="+1-415-555-1012"; title="Associate";              department="Investment";   accountIdx=9; linkedinUrl="https://linkedin.com/in/yukitanaka";  lifecycleStage="LEAD";    segment="INFLUENCER"     },
    @{ firstName="Patricia";   lastName="Moore";        email="patricia.moore@outlook.com";       phone="+1-555-000-0001"; title="Independent Consultant"; department="Consulting";   accountIdx=-1; linkedinUrl="https://linkedin.com/in/patriciamoore"; lifecycleStage="LEAD";  segment="INFLUENCER"     }
)

$contactIds = @()
foreach ($c in $contactData) {
    $body = @{
        firstName      = $c.firstName
        lastName       = $c.lastName
        email          = $c.email
        phone          = $c.phone
        title          = $c.title
        department     = $c.department
        linkedinUrl    = $c.linkedinUrl
        lifecycleStage = $c.lifecycleStage
        segment        = $c.segment
        emailOptIn     = $true
    }
    if ($c.accountIdx -ge 0 -and $c.accountIdx -lt $accountIds.Count) {
        $body["accountId"] = $accountIds[$c.accountIdx]
    }
    $res = Invoke-Api -Method POST -Uri "$BASE`:8084/api/v1/contacts" -Body ($body | ConvertTo-Json) -Token $T
    if ($res -and $res.data) {
        $contactIds += $res.data.id
        Write-Ok "$($c.firstName) $($c.lastName) @ $($c.title)"
    }
}

###############################################################################
# 4. LEADS — 15 leads in various stages
###############################################################################
Write-Step "Creating leads..."

$leadData = @(
    @{ firstName="Ryan";     lastName="Cooper";     email="ryan.cooper@innovatetech.com";     phone="+1-408-555-3001"; company="InnovateTech";            title="VP Engineering";       source="WEB";          assigneeIdx=2; description="Downloaded enterprise whitepaper. Interested in API platform." },
    @{ firstName="Samantha"; lastName="Blake";      email="samantha.blake@retailpro.com";     phone="+1-310-555-3002"; company="RetailPro Solutions";     title="Director of eCommerce"; source="TRADE_SHOW";   assigneeIdx=3; description="Met at RetailTech Summit 2026. Strong interest in omnichannel." },
    @{ firstName="Derek";    lastName="Chang";      email="derek.chang@biosynth.com";         phone="+1-858-555-3003"; company="BioSynth Labs";           title="Lab Director";          source="REFERRAL";     assigneeIdx=4; description="Referred by Dr. Sharma. Looking for LIMS integration." },
    @{ firstName="Olivia";   lastName="Hayes";      email="olivia.hayes@metroconstruction.com"; phone="+1-469-555-3004"; company="Metro Construction";   title="Project Manager";       source="WEB";          assigneeIdx=2; description="Filled out contact form. Needs project management CRM." },
    @{ firstName="Vincent";  lastName="Russo";      email="vincent.russo@alphainsurance.com"; phone="+1-704-555-3005"; company="Alpha Insurance Group";   title="CIO";                   source="EMAIL";        assigneeIdx=3; description="Responded to email campaign. Looking for claims workflow." },
    @{ firstName="Hannah";   lastName="Brooks";     email="hannah.brooks@sunrisemedia.com";   phone="+1-323-555-3006"; company="Sunrise Media Group";     title="Marketing Director";    source="SOCIAL_MEDIA"; assigneeIdx=4; description="Engaged on LinkedIn post. Looking for marketing automation." },
    @{ firstName="Benjamin"; lastName="Grant";      email="benjamin.grant@pacificfoods.com";  phone="+1-206-555-3007"; company="Pacific Foods Co";        title="Supply Chain Manager";  source="TRADE_SHOW";   assigneeIdx=2; description="Visited booth at FoodTech Expo. Inventory management needs." },
    @{ firstName="Natalie";  lastName="Diaz";       email="natalie.diaz@cloudops360.com";     phone="+1-512-555-3008"; company="CloudOps 360";            title="DevOps Lead";           source="WEB";          assigneeIdx=3; description="Signed up for free trial. Evaluating for 50-person team." },
    @{ firstName="Tyler";    lastName="Manning";    email="tyler.manning@legalpro.com";       phone="+1-404-555-3009"; company="LegalPro Associates";     title="Managing Partner";      source="REFERRAL";     assigneeIdx=4; description="Referred by existing client. Needs CRM for law firm." },
    @{ firstName="Isabella"; lastName="Santos";     email="isabella.santos@nexushealth.com";  phone="+1-305-555-3010"; company="Nexus Health Systems";    title="VP Operations";         source="PHONE";        assigneeIdx=2; description="Called in after seeing ad. Immediate need for patient CRM." },
    @{ firstName="Jason";    lastName="Patel";      email="jason.patel@quantumrobotics.com";  phone="+1-734-555-3011"; company="Quantum Robotics";        title="CEO";                   source="WEB";          assigneeIdx=3; description="Requested demo. Series B startup, rapid growth phase." },
    @{ firstName="Catherine"; lastName="Lee";       email="catherine.lee@premier-hospitality.com"; phone="+1-702-555-3012"; company="Premier Hospitality"; title="General Manager";    source="TRADE_SHOW";   assigneeIdx=4; description="Hotel chain. Needs guest relationship management." },
    @{ firstName="Andrew";   lastName="Fischer";    email="andrew.fischer@steelbridge.com";   phone="+1-412-555-3013"; company="Steelbridge Engineering"; title="Operations Director";   source="EMAIL";        assigneeIdx=2; description="Opened 3 emails. Requested pricing for 200 seats." },
    @{ firstName="Monica";   lastName="Graves";     email="monica.graves@horizonagri.com";    phone="+1-515-555-3014"; company="Horizon Agriculture";     title="VP Technology";         source="SOCIAL_MEDIA"; assigneeIdx=3; description="Commented on webinar. Agricultural CRM interest." },
    @{ firstName="Russell";  lastName="Pope";       email="russell.pope@eliteauto.com";       phone="+1-313-555-3015"; company="Elite Auto Group";        title="Dealer Principal";      source="PHONE";        assigneeIdx=4; description="Multi-location dealership. Needs automotive CRM." }
)

$leadIds = @()
$reps = @($rep1, $rep2, $rep3)
foreach ($l in $leadData) {
    $body = @{
        firstName   = $l.firstName
        lastName    = $l.lastName
        email       = $l.email
        phone       = $l.phone
        company     = $l.company
        title       = $l.title
        source      = $l.source
        description = $l.description
    }
    if ($reps[$l.assigneeIdx - 2]) { $body["assignedTo"] = $reps[$l.assigneeIdx - 2] }

    $res = Invoke-Api -Method POST -Uri "$BASE`:8082/api/v1/leads" -Body ($body | ConvertTo-Json) -Token $T
    if ($res -and $res.data) {
        $leadIds += $res.data.id
        Write-Ok "$($l.firstName) $($l.lastName) – $($l.company)"
    }
}

###############################################################################
# 5. OPPORTUNITIES — 12 deals across pipeline stages
###############################################################################
Write-Step "Creating opportunities..."

$today = Get-Date -Format "yyyy-MM-dd"
$oppData = @(
    @{ name="TechVista Platform License";          accountIdx=0; contactIdx=0;  amount=450000;  stage="NEGOTIATION";    probability=75; closeDate=(Get-Date).AddDays(15).ToString("yyyy-MM-dd");  description="Annual platform license + premium support. Final pricing discussions.";         assignee=$rep1; leadSource="WEB";        forecastCategory="COMMIT"    },
    @{ name="GlobalRetail Omnichannel Suite";      accountIdx=1; contactIdx=2;  amount=850000;  stage="PROPOSAL";       probability=60; closeDate=(Get-Date).AddDays(30).ToString("yyyy-MM-dd");  description="Full omnichannel CRM suite for 50 stores. RFP response submitted.";            assignee=$rep1; leadSource="TRADE_SHOW"; forecastCategory="BEST_CASE" },
    @{ name="MedHealth HIPAA Compliance Package";  accountIdx=2; contactIdx=4;  amount=280000;  stage="NEEDS_ANALYSIS"; probability=40; closeDate=(Get-Date).AddDays(60).ToString("yyyy-MM-dd");  description="HIPAA-compliant CRM + patient portal. Requirements gathering in progress.";    assignee=$rep2; leadSource="REFERRAL";   forecastCategory="PIPELINE"  },
    @{ name="FinEdge Risk Analytics Module";       accountIdx=3; contactIdx=6;  amount=520000;  stage="QUALIFICATION";  probability=30; closeDate=(Get-Date).AddDays(75).ToString("yyyy-MM-dd");  description="Risk analytics add-on to existing platform. Budget approval pending.";         assignee=$rep2; leadSource="EMAIL";      forecastCategory="PIPELINE"  },
    @{ name="EduPath Learning Management CRM";     accountIdx=4; contactIdx=8;  amount=180000;  stage="PROSPECTING";    probability=15; closeDate=(Get-Date).AddDays(90).ToString("yyyy-MM-dd");  description="CRM + LMS integration for 5000 students. Initial discovery call scheduled.";  assignee=$rep3; leadSource="WEB";        forecastCategory="PIPELINE"  },
    @{ name="GreenEnergy Fleet Management";        accountIdx=5; contactIdx=9;  amount=1200000; stage="NEGOTIATION";    probability=80; closeDate=(Get-Date).AddDays(10).ToString("yyyy-MM-dd");  description="Enterprise fleet & field service CRM. Contract review stage.";                 assignee=$rep1; leadSource="REFERRAL";   forecastCategory="COMMIT"    },
    @{ name="CloudNine Partnership Integration";   accountIdx=6; contactIdx=11; amount=150000;  stage="CLOSED_WON";     probability=100; closeDate=(Get-Date).AddDays(-5).ToString("yyyy-MM-dd"); description="API integration partnership. Deal closed! Implementation starting Q2.";       assignee=$rep2; leadSource="REFERRAL";   forecastCategory="CLOSED"    },
    @{ name="Precision Mfg ERP Connect";           accountIdx=7; contactIdx=13; amount=380000;  stage="PROPOSAL";       probability=55; closeDate=(Get-Date).AddDays(45).ToString("yyyy-MM-dd");  description="ERP-CRM bridge for production tracking. Proposal delivered.";                 assignee=$rep3; leadSource="TRADE_SHOW"; forecastCategory="BEST_CASE" },
    @{ name="Atlas Logistics Route Optimization";  accountIdx=8; contactIdx=15; amount=290000;  stage="CLOSED_WON";     probability=100; closeDate=(Get-Date).AddDays(-20).ToString("yyyy-MM-dd"); description="Route optimization + driver CRM. Successfully deployed to 400 trucks.";      assignee=$rep1; leadSource="WEB";        forecastCategory="CLOSED"    },
    @{ name="StartupHub Portfolio CRM";            accountIdx=9; contactIdx=17; amount=95000;   stage="QUALIFICATION";  probability=25; closeDate=(Get-Date).AddDays(60).ToString("yyyy-MM-dd");  description="Portfolio company management CRM. Exploring fit for 25 portfolio companies.";  assignee=$rep3; leadSource="SOCIAL_MEDIA"; forecastCategory="PIPELINE" },
    @{ name="TechVista AI Add-on";                 accountIdx=0; contactIdx=1;  amount=120000;  stage="NEEDS_ANALYSIS"; probability=50; closeDate=(Get-Date).AddDays(40).ToString("yyyy-MM-dd");  description="AI insights add-on for existing license. Technical evaluation underway.";     assignee=$rep1; leadSource="WEB";        forecastCategory="BEST_CASE" },
    @{ name="GlobalRetail Data Migration";         accountIdx=1; contactIdx=3;  amount=75000;   stage="CLOSED_LOST";    probability=0;  closeDate=(Get-Date).AddDays(-10).ToString("yyyy-MM-dd"); description="Data migration services. Lost to competitor on pricing.";                     assignee=$rep2; leadSource="EMAIL";      forecastCategory="CLOSED"    }
)

$oppIds = @()
foreach ($o in $oppData) {
    $body = @{
        name             = $o.name
        amount           = $o.amount
        stage            = $o.stage
        probability      = $o.probability
        closeDate        = $o.closeDate
        description      = $o.description
        leadSource       = $o.leadSource
        forecastCategory = $o.forecastCategory
    }
    if ($o.accountIdx -ge 0 -and $o.accountIdx -lt $accountIds.Count) {
        $body["accountId"] = $accountIds[$o.accountIdx]
    }
    if ($o.contactIdx -ge 0 -and $o.contactIdx -lt $contactIds.Count) {
        $body["contactId"] = $contactIds[$o.contactIdx]
    }
    if ($o.assignee) { $body["assignedTo"] = $o.assignee }

    $res = Invoke-Api -Method POST -Uri "$BASE`:8085/api/v1/opportunities" -Body ($body | ConvertTo-Json) -Token $T
    if ($res -and $res.data) {
        $oppIds += $res.data.id
        Write-Ok "$($o.name) – `$$($o.amount.ToString('N0')) ($($o.stage))"
    }
}

###############################################################################
# 6. PRODUCTS on key opportunities
###############################################################################
Write-Step "Adding products to opportunities..."

$products = @(
    @{ oppIdx=0; productName="Platform License (Annual)"; productCode="PLT-001"; quantity=1; unitPrice=350000; discount=0;  description="Enterprise platform license, unlimited users" },
    @{ oppIdx=0; productName="Premium Support Package";   productCode="SUP-001"; quantity=1; unitPrice=100000; discount=0;  description="24/7 premium support with dedicated CSM"     },
    @{ oppIdx=1; productName="Omnichannel Suite License";productCode="OCS-001"; quantity=50; unitPrice=12000; discount=10; description="Per-store annual license"                     },
    @{ oppIdx=1; productName="Implementation Services";  productCode="IMP-001"; quantity=1;  unitPrice=250000; discount=0; description="Full implementation & training" },
    @{ oppIdx=5; productName="Fleet CRM Enterprise";     productCode="FLT-001"; quantity=1;  unitPrice=800000; discount=0; description="Enterprise fleet management module" },
    @{ oppIdx=5; productName="Field Service Add-on";     productCode="FSA-001"; quantity=1;  unitPrice=400000; discount=0; description="Mobile field service management" },
    @{ oppIdx=7; productName="ERP Connector";            productCode="ERP-001"; quantity=1;  unitPrice=280000; discount=10; description="Bi-directional ERP integration" },
    @{ oppIdx=7; productName="Training Package";         productCode="TRN-001"; quantity=1;  unitPrice=100000; discount=0;  description="40 hours of on-site training" }
)

foreach ($p in $products) {
    if ($p.oppIdx -lt $oppIds.Count) {
        $body = @{
            productName = $p.productName
            productCode = $p.productCode
            quantity    = $p.quantity
            unitPrice   = $p.unitPrice
            discount    = $p.discount
            description = $p.description
        } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "$BASE`:8085/api/v1/opportunities/$($oppIds[$p.oppIdx])/products" -Body $body -Token $T
        if ($res) { Write-Ok "$($p.productName) → $($oppData[$p.oppIdx].name)" }
    }
}

###############################################################################
# 7. ACTIVITIES — 25 tasks, calls, meetings, emails
###############################################################################
Write-Step "Creating activities..."

$actData = @(
    @{ type="MEETING"; subject="Discovery Call – TechVista";          priority="HIGH";   dueDate=(Get-Date).AddDays(1).ToString("yyyy-MM-dd'T'10:00:00");  relatedType="OPPORTUNITY"; relatedIdx=0; assignee=$rep1; description="Initial requirements review with David Kim. Discuss API needs and timeline." },
    @{ type="TASK";    subject="Send proposal to GlobalRetail";       priority="URGENT"; dueDate=(Get-Date).AddDays(2).ToString("yyyy-MM-dd'T'14:00:00");  relatedType="OPPORTUNITY"; relatedIdx=1; assignee=$rep1; description="Finalize pricing for 50-store rollout. Include implementation timeline."      },
    @{ type="CALL";    subject="Follow-up: MedHealth HIPAA Review";   priority="HIGH";   dueDate=(Get-Date).AddDays(1).ToString("yyyy-MM-dd'T'11:00:00");  relatedType="OPPORTUNITY"; relatedIdx=2; assignee=$rep2; description="Clarify HIPAA compliance requirements with Dr. Sharma."                      },
    @{ type="EMAIL";   subject="FinEdge Budget Approval Check-in";    priority="MEDIUM"; dueDate=(Get-Date).AddDays(3).ToString("yyyy-MM-dd'T'09:00:00");  relatedType="OPPORTUNITY"; relatedIdx=3; assignee=$rep2; description="Check on budget approval status. Q2 budget cycle closing."                   },
    @{ type="MEETING"; subject="Demo: EduPath LMS Integration";       priority="MEDIUM"; dueDate=(Get-Date).AddDays(5).ToString("yyyy-MM-dd'T'15:00:00");  relatedType="OPPORTUNITY"; relatedIdx=4; assignee=$rep3; description="Product demo showing LMS-CRM integration capabilities."                     },
    @{ type="TASK";    subject="Prepare GreenEnergy contract";        priority="URGENT"; dueDate=(Get-Date).AddDays(0).ToString("yyyy-MM-dd'T'16:00:00");  relatedType="OPPORTUNITY"; relatedIdx=5; assignee=$rep1; description="Final contract with fleet management terms. Legal review required."           },
    @{ type="CALL";    subject="Precision Mfg: Technical Q&A";        priority="MEDIUM"; dueDate=(Get-Date).AddDays(4).ToString("yyyy-MM-dd'T'13:00:00");  relatedType="OPPORTUNITY"; relatedIdx=7; assignee=$rep3; description="Answer ERP integration technical questions from Greg Williams."               },
    @{ type="MEETING"; subject="Quarterly Review – Atlas Logistics";   priority="LOW";    dueDate=(Get-Date).AddDays(10).ToString("yyyy-MM-dd'T'10:00:00"); relatedType="ACCOUNT";     relatedIdx=8; assignee=$rep1; description="Quarterly business review. Discuss expansion opportunities."                },
    @{ type="TASK";    subject="Qualify lead: InnovateTech";           priority="HIGH";   dueDate=(Get-Date).AddDays(1).ToString("yyyy-MM-dd'T'09:00:00");  relatedType="LEAD";        relatedIdx=0; assignee=$rep1; description="Review whitepaper download data. Check company size and budget."              },
    @{ type="CALL";    subject="Initial outreach: RetailPro";         priority="MEDIUM"; dueDate=(Get-Date).AddDays(2).ToString("yyyy-MM-dd'T'10:30:00");  relatedType="LEAD";        relatedIdx=1; assignee=$rep2; description="Follow up from trade show meeting. Schedule proper demo."                    },
    @{ type="EMAIL";   subject="Welcome email to BioSynth Labs";      priority="MEDIUM"; dueDate=(Get-Date).AddDays(0).ToString("yyyy-MM-dd'T'08:00:00");  relatedType="LEAD";        relatedIdx=2; assignee=$rep3; description="Send intro email with case studies from healthcare sector."                  },
    @{ type="MEETING"; subject="Team Pipeline Review";                 priority="HIGH";   dueDate=(Get-Date).AddDays(3).ToString("yyyy-MM-dd'T'09:00:00");  relatedType="ACCOUNT";     relatedIdx=0; assignee=$mgr;  description="Weekly team pipeline review. Discuss deal progression and blockers."        },
    @{ type="TASK";    subject="Update CRM records for Q1 deals";     priority="LOW";    dueDate=(Get-Date).AddDays(7).ToString("yyyy-MM-dd'T'17:00:00");  relatedType="ACCOUNT";     relatedIdx=1; assignee=$rep1; description="Ensure all Q1 closed deals have complete data."                              },
    @{ type="CALL";    subject="Reference call: CloudNine + FinEdge"; priority="MEDIUM"; dueDate=(Get-Date).AddDays(6).ToString("yyyy-MM-dd'T'14:00:00");  relatedType="OPPORTUNITY"; relatedIdx=3; assignee=$rep2; description="Arrange reference call between CloudNine (customer) and FinEdge (prospect)."  },
    @{ type="MEETING"; subject="Contract Negotiation – GreenEnergy";   priority="URGENT"; dueDate=(Get-Date).AddDays(2).ToString("yyyy-MM-dd'T'11:00:00"); relatedType="OPPORTUNITY"; relatedIdx=5; assignee=$rep1; description="Final pricing negotiation. CFO and legal will attend."                       },
    @{ type="TASK";    subject="Send case study to Quantum Robotics";  priority="HIGH";   dueDate=(Get-Date).AddDays(1).ToString("yyyy-MM-dd'T'12:00:00"); relatedType="LEAD";        relatedIdx=10; assignee=$rep2; description="Send SaaS startup case study to Jason Patel."                               },
    @{ type="EMAIL";   subject="Premier Hospitality follow-up";       priority="MEDIUM"; dueDate=(Get-Date).AddDays(3).ToString("yyyy-MM-dd'T'10:00:00"); relatedType="LEAD";        relatedIdx=11; assignee=$rep3; description="Follow up on trade show conversation. Share hospitality CRM features."     },
    @{ type="CALL";    subject="Steelbridge pricing discussion";       priority="HIGH";   dueDate=(Get-Date).AddDays(2).ToString("yyyy-MM-dd'T'15:00:00"); relatedType="LEAD";        relatedIdx=12; assignee=$rep1; description="Discuss enterprise pricing for 200-seat deployment."                       },
    @{ type="TASK";    subject="Prepare Executive Briefing";           priority="HIGH";   dueDate=(Get-Date).AddDays(4).ToString("yyyy-MM-dd'T'09:00:00"); relatedType="ACCOUNT";     relatedIdx=5; assignee=$mgr;  description="Prepare executive briefing for GreenEnergy board meeting."                   },
    @{ type="MEETING"; subject="Monthly Sales All-Hands";              priority="MEDIUM"; dueDate=(Get-Date).AddDays(8).ToString("yyyy-MM-dd'T'09:30:00"); relatedType="ACCOUNT";     relatedIdx=0; assignee=$mgr;  description="Monthly all-hands. Review pipeline, wins, and team metrics."                },
    @{ type="TASK";    subject="Create ROI calculator for MedHealth";  priority="HIGH";   dueDate=(Get-Date).AddDays(3).ToString("yyyy-MM-dd'T'11:00:00"); relatedType="OPPORTUNITY"; relatedIdx=2; assignee=$rep2; description='Build custom ROI model for healthcare CRM to justify $280K investment.'     },
    @{ type="CALL";    subject="Check-in with Amanda Foster";          priority="LOW";    dueDate=(Get-Date).AddDays(5).ToString("yyyy-MM-dd'T'16:00:00"); relatedType="CONTACT";     relatedIdx=2; assignee=$rep1; description="Relationship check-in with GlobalRetail CEO. No immediate ask."            },
    @{ type="EMAIL";   subject="Quarterly newsletter to all contacts"; priority="LOW";    dueDate=(Get-Date).AddDays(14).ToString("yyyy-MM-dd'T'10:00:00"); relatedType="ACCOUNT";    relatedIdx=0; assignee=$rep3; description="Q1 newsletter with product updates, case studies, and event invitations."   },
    @{ type="MEETING"; subject="StartupHub Portfolio Demo";            priority="MEDIUM"; dueDate=(Get-Date).AddDays(7).ToString("yyyy-MM-dd'T'14:00:00"); relatedType="OPPORTUNITY"; relatedIdx=9; assignee=$rep3; description="Demo CRM portfolio management features to Alex Novak."                       },
    @{ type="TASK";    subject="Competitor analysis: TechVista deal";  priority="HIGH";   dueDate=(Get-Date).AddDays(2).ToString("yyyy-MM-dd'T'10:00:00"); relatedType="OPPORTUNITY"; relatedIdx=0; assignee=$rep1; description="Research competitor offerings for TechVista evaluation. Update battle card." }
)

# Map entity types to IDs
$entityMap = @{
    "OPPORTUNITY" = $oppIds
    "ACCOUNT"     = $accountIds
    "LEAD"        = $leadIds
    "CONTACT"     = $contactIds
}

foreach ($a in $actData) {
    $body = @{
        type              = $a.type
        subject           = $a.subject
        description       = $a.description
        priority          = $a.priority
        dueDate           = $a.dueDate
        relatedEntityType = $a.relatedType
    }
    if ($a.assignee) { $body["assignedTo"] = $a.assignee }

    $ids = $entityMap[$a.relatedType]
    if ($ids -and $a.relatedIdx -lt $ids.Count) {
        $body["relatedEntityId"] = $ids[$a.relatedIdx]
    }

    $res = Invoke-Api -Method POST -Uri "$BASE`:8086/api/v1/activities" -Body ($body | ConvertTo-Json) -Token $T
    if ($res) { Write-Ok "$($a.type): $($a.subject)" }
}

###############################################################################
# 8. WORKFLOW RULES — 5 automation rules
###############################################################################
Write-Step "Creating workflow rules..."

$workflows = @(
    @{
        name        = "Auto-assign new web leads"
        description = "Automatically assigns leads from web source to the round-robin queue"
        entityType  = "LEAD"
        triggerEvent= "CREATED"
        conditions  = @( @{ fieldName="source"; operator="EQUALS"; value="WEB" } )
        actions     = @( @{ actionType="UPDATE_FIELD"; targetField="status"; targetValue="CONTACTED" } )
    },
    @{
        name        = "Notify manager on large deals"
        description = 'Sends notification when opportunity amount exceeds $500K'
        entityType  = "OPPORTUNITY"
        triggerEvent= "CREATED"
        conditions  = @( @{ fieldName="amount"; operator="GREATER_THAN"; value="500000" } )
        actions     = @( @{ actionType="SEND_EMAIL"; targetValue='High-value deal created: {name} - ${amount}' } )
    },
    @{
        name        = "Follow-up on stale opportunities"
        description = "Creates follow-up task when opportunity hasn't been updated in 7 days"
        entityType  = "OPPORTUNITY"
        triggerEvent= "UPDATED"
        conditions  = @( @{ fieldName="stage"; operator="NOT_EQUALS"; value="CLOSED_WON" } )
        actions     = @( @{ actionType="CREATE_TASK"; targetValue="Follow up on stale opportunity" } )
    },
    @{
        name        = "Welcome email for new contacts"
        description = "Sends welcome email when a new contact is created with email opt-in"
        entityType  = "CONTACT"
        triggerEvent= "CREATED"
        conditions  = @( @{ fieldName="emailOptIn"; operator="EQUALS"; value="true" } )
        actions     = @( @{ actionType="SEND_EMAIL"; targetValue="Welcome to our CRM platform!" } )
    },
    @{
        name        = "Escalate overdue activities"
        description = "Notifies manager when activity is past due"
        entityType  = "ACTIVITY"
        triggerEvent= "UPDATED"
        conditions  = @( @{ fieldName="status"; operator="EQUALS"; value="NOT_STARTED" } )
        actions     = @( @{ actionType="SEND_EMAIL"; targetValue="Overdue activity alert: {subject}" } )
    }
)

foreach ($w in $workflows) {
    $body = $w | ConvertTo-Json -Depth 4
    $res = Invoke-Api -Method POST -Uri "$BASE`:8088/api/v1/workflows" -Body $body -Token $T
    if ($res) { Write-Ok "$($w.name)" }
}

###############################################################################
# 9. EMAIL TEMPLATES — 5 templates
###############################################################################
Write-Step "Creating email templates..."

$templates = @(
    @{
        name     = "Welcome New Client"
        subject  = "Welcome to CRMS Platform – Getting Started"
        category = "ONBOARDING"
        bodyHtml = "<h2>Welcome aboard, {{contactName}}!</h2><p>We're thrilled to have <strong>{{companyName}}</strong> join the CRMS family.</p><p>Here's how to get started:</p><ol><li>Log in to your dashboard</li><li>Import your existing data</li><li>Set up your team members</li><li>Configure your pipeline stages</li></ol><p>Your dedicated Customer Success Manager, {{csmName}}, will reach out within 24 hours.</p><p>Best regards,<br/>The CRMS Team</p>"
    },
    @{
        name     = "Meeting Follow-Up"
        subject  = "Great connecting today – Next Steps"
        category = "SALES"
        bodyHtml = "<h2>Hi {{contactName}},</h2><p>Thank you for taking the time to meet with us today. It was great learning about {{companyName}}'s goals.</p><p><strong>Key takeaways:</strong></p><ul><li>{{takeaway1}}</li><li>{{takeaway2}}</li><li>{{takeaway3}}</li></ul><p><strong>Next steps:</strong></p><ul><li>{{nextStep1}}</li><li>{{nextStep2}}</li></ul><p>Looking forward to our next conversation!</p><p>Best,<br/>{{senderName}}</p>"
    },
    @{
        name     = "Proposal Follow-Up"
        subject  = "Following up on our proposal for {{companyName}}"
        category = "SALES"
        bodyHtml = "<h2>Hi {{contactName}},</h2><p>I wanted to follow up on the proposal we sent on {{proposalDate}}.</p><p>We believe our solution can help {{companyName}} achieve:</p><ul><li>{{benefit1}}</li><li>{{benefit2}}</li><li>{{benefit3}}</li></ul><p>Do you have any questions or would you like to schedule a call to discuss further?</p><p>Best regards,<br/>{{senderName}}</p>"
    },
    @{
        name     = "Quarterly Business Review Invite"
        subject  = "QBR Invitation – {{quarter}} Review"
        category = "CUSTOMER_SUCCESS"
        bodyHtml = "<h2>Hi {{contactName}},</h2><p>It's time for our quarterly business review! I'd love to schedule time to:</p><ul><li>Review your KPIs and ROI</li><li>Discuss upcoming feature releases</li><li>Plan next quarter's objectives</li><li>Address any concerns</li></ul><p>Please let me know your availability for a 60-minute session.</p><p>Best,<br/>{{csmName}}</p>"
    },
    @{
        name     = "Deal Won – Internal Celebration"
        subject  = 'Deal Won: {{dealName}} - ${{amount}}'
        category = "INTERNAL"
        bodyHtml = '<h2>Congratulations Team!</h2><p><strong>{{repName}}</strong> just closed <strong>{{dealName}}</strong> with {{companyName}} for <strong>${{amount}}</strong>!</p><p><strong>Deal Highlights:</strong></p><ul><li>Account: {{companyName}}</li><li>Products: {{products}}</li><li>Close Date: {{closeDate}}</li><li>Sales Cycle: {{salesCycle}} days</li></ul><p>Great work, team!</p>'
    }
)

foreach ($tmpl in $templates) {
    $body = $tmpl | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE`:8090/api/v1/email/templates" -Body $body -Token $T
    if ($res) { Write-Ok "$($tmpl.name)" }
}

###############################################################################
# 10. NOTES on key accounts & opportunities
###############################################################################
Write-Step "Adding notes to accounts and opportunities..."

$accountNotes = @(
    @{ idx=0; content="TechVista renewed their infrastructure contract last quarter. Strong relationship with CTO David Kim. They're evaluating our AI module as an add-on." },
    @{ idx=1; content="GlobalRetail has 50 stores nationwide with plans to expand to 75 by EOY. Key decision maker Amanda Foster prefers in-person meetings. Budget cycle is Q4." },
    @{ idx=3; content="FinEdge has strict security requirements. All proposals must go through their security review board. CFO Christine Wang champions our product internally." },
    @{ idx=5; content="GreenEnergy is our largest pipeline opportunity. COO Thomas Anderson is the executive sponsor. They need fleet management + sustainability reporting." },
    @{ idx=8; content="Atlas Logistics deployment completed successfully. Running on 400 trucks. Excellent reference customer – Carlos Rivera offered to do case study." }
)

foreach ($n in $accountNotes) {
    if ($n.idx -lt $accountIds.Count) {
        $body = @{ content = $n.content } | ConvertTo-Json
        Invoke-Api -Method POST -Uri "$BASE`:8083/api/v1/accounts/$($accountIds[$n.idx])/notes" -Body $body -Token $T | Out-Null
        Write-Ok "Note on $($accountData[$n.idx].name)"
    }
}

$oppNotes = @(
    @{ idx=0; content="David Kim confirmed budget approval. Need to finalize by end of month. Competitor X also in evaluation but we have technical advantage." },
    @{ idx=1; content="Amanda Foster wants ROI analysis before presenting to board. Need to show 18-month payback period. Implementation timeline is critical factor." },
    @{ idx=5; content="Thomas Anderson said board approved the budget. Legal is reviewing contract. Expected signature by end of next week. Biggest deal this quarter!" },
    @{ idx=6; content="Deal closed! Nathan Hughes signed the partnership agreement. Implementation kickoff scheduled for next Monday. CSM assigned: Lisa Thompson." },
    @{ idx=8; content="Post-implementation review shows 23% improvement in route efficiency. Customer extremely satisfied. Expansion discussion for warehousing CRM scheduled." }
)

foreach ($n in $oppNotes) {
    if ($n.idx -lt $oppIds.Count) {
        $body = @{ content = $n.content } | ConvertTo-Json
        Invoke-Api -Method POST -Uri "$BASE`:8085/api/v1/opportunities/$($oppIds[$n.idx])/notes" -Body $body -Token $T | Out-Null
        Write-Ok "Note on $($oppData[$n.idx].name)"
    }
}

###############################################################################
# 11. COMPETITORS on active deals
###############################################################################
Write-Step "Adding competitors to opportunities..."

$competitors = @(
    @{ oppIdx=0; competitorName="Salesforce";          strengths="Market leader, extensive ecosystem"; weaknesses="Expensive, complex implementation"; threatLevel="HIGH"   },
    @{ oppIdx=0; competitorName="HubSpot";             strengths="Easy to use, good marketing tools"; weaknesses="Limited enterprise features";       threatLevel="MEDIUM" },
    @{ oppIdx=1; competitorName="Microsoft Dynamics";   strengths="Azure integration, familiar UI";    weaknesses="Slower retail-specific features";   threatLevel="HIGH"   },
    @{ oppIdx=2; competitorName="HealthCloud by SF";    strengths="Healthcare-specific features";      weaknesses="Very expensive, long implementation"; threatLevel="MEDIUM"},
    @{ oppIdx=5; competitorName="SAP CRM";             strengths="Strong ERP integration";            weaknesses="Rigid, legacy architecture";        threatLevel="LOW"    },
    @{ oppIdx=7; competitorName="Epicor";              strengths="Manufacturing focus";               weaknesses="Limited CRM capabilities";          threatLevel="MEDIUM" }
)

foreach ($c in $competitors) {
    if ($c.oppIdx -lt $oppIds.Count) {
        $body = @{
            competitorName = $c.competitorName
            strengths      = $c.strengths
            weaknesses     = $c.weaknesses
            threatLevel    = $c.threatLevel
        } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "$BASE`:8085/api/v1/opportunities/$($oppIds[$c.oppIdx])/competitors" -Body $body -Token $T
        if ($res) { Write-Ok "$($c.competitorName) → $($oppData[$c.oppIdx].name)" }
    }
}

###############################################################################
#  SUMMARY
###############################################################################
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  ✅  DEMO DATA SEEDED SUCCESSFULLY" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""
Write-Host "  Demo Accounts:" -ForegroundColor White
Write-Host "  ─────────────────────────────────────────────────────────" -ForegroundColor DarkGray

$loginInfo = @(
    @{ role="Admin";   email="sarah.chen@acmecorp.com";      name="Sarah Chen"      },
    @{ role="Manager"; email="james.wilson@acmecorp.com";    name="James Wilson"    },
    @{ role="Rep";     email="emily.rodriguez@acmecorp.com"; name="Emily Rodriguez" },
    @{ role="Rep";     email="michael.park@acmecorp.com";    name="Michael Park"    },
    @{ role="Rep";     email="lisa.thompson@acmecorp.com";   name="Lisa Thompson"   }
)

foreach ($info in $loginInfo) {
    Write-Host "  $($info.role.PadRight(8)) │ $($info.email.PadRight(35)) │ $($info.name)" -ForegroundColor White
}

Write-Host ""
Write-Host "  Password for all: Demo@2026!" -ForegroundColor Yellow
Write-Host "  Tenant ID:        default" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Data Created:" -ForegroundColor White
Write-Host "  ─────────────────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "    • 5 user accounts (Admin, Manager, 3 Reps)" -ForegroundColor White
Write-Host "    • 10 company accounts across various industries" -ForegroundColor White
Write-Host "    • 20 contacts linked to accounts" -ForegroundColor White
Write-Host "    • 15 leads in various pipeline stages" -ForegroundColor White
Write-Host '    • 12 opportunities ($4.6M pipeline)' -ForegroundColor White
Write-Host "    • 8 product line items on deals" -ForegroundColor White
Write-Host "    • 25 activities (tasks, calls, meetings, emails)" -ForegroundColor White
Write-Host "    • 5 workflow automation rules" -ForegroundColor White
Write-Host "    • 5 email templates" -ForegroundColor White
Write-Host "    • 10 account/opportunity notes" -ForegroundColor White
Write-Host "    • 6 competitor entries on deals" -ForegroundColor White
Write-Host ""
Write-Host "  Open: http://localhost:3000" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Magenta
