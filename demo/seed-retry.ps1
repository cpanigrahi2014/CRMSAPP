###############################################################################
#  Retry failed items: Leads & Workflows (using ADMIN token)
###############################################################################
$ErrorActionPreference = "Continue"
$BASE = "http://localhost"

function Invoke-Api {
    param([string]$Method, [string]$Uri, [string]$Body = $null, [string]$Token = $null)
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers; ErrorAction = "Stop" }
    if ($Body -and $Method -ne "GET") { $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body) }
    try { return Invoke-RestMethod @params }
    catch {
        $status = $_.Exception.Response.StatusCode.value__
        $detail = $_.ErrorDetails.Message
        Write-Host "  FAIL $Method $Uri -> $status" -ForegroundColor Red
        if ($detail) { Write-Host "  $detail" -ForegroundColor DarkYellow }
        return $null
    }
}

# Login as ADMIN Sarah
$loginRes = Invoke-Api -Method POST -Uri "$BASE`:8081/api/v1/auth/login" -Body '{"email":"sarah.chen@acmecorp.com","password":"Demo@2026!","tenantId":"default"}'
$T = $loginRes.data.accessToken
Write-Host "Logged in as Sarah (roles: $($loginRes.data.roles -join ','))" -ForegroundColor Cyan

###############################################################################
# LEADS
###############################################################################
Write-Host "`n--- Creating Leads ---" -ForegroundColor Cyan

$leadData = @(
    @{ firstName="Ryan";     lastName="Cooper";     email="ryan.cooper@innovatetech.com";     phone="+1-408-555-3001"; company="InnovateTech";            title="VP Engineering";       source="WEB";          description="Downloaded enterprise whitepaper. Interested in API platform." },
    @{ firstName="Samantha"; lastName="Blake";      email="samantha.blake@retailpro.com";     phone="+1-310-555-3002"; company="RetailPro Solutions";     title="Director of eCommerce"; source="TRADE_SHOW";   description="Met at RetailTech Summit 2026. Strong interest in omnichannel." },
    @{ firstName="Derek";    lastName="Chang";      email="derek.chang@biosynth.com";         phone="+1-858-555-3003"; company="BioSynth Labs";           title="Lab Director";          source="REFERRAL";     description="Referred by Dr. Sharma. Looking for LIMS integration." },
    @{ firstName="Olivia";   lastName="Hayes";      email="olivia.hayes@metroconstruction.com"; phone="+1-469-555-3004"; company="Metro Construction";   title="Project Manager";       source="WEB";          description="Filled out contact form. Needs project management CRM." },
    @{ firstName="Vincent";  lastName="Russo";      email="vincent.russo@alphainsurance.com"; phone="+1-704-555-3005"; company="Alpha Insurance Group";   title="CIO";                   source="EMAIL";        description="Responded to email campaign. Looking for claims workflow." },
    @{ firstName="Hannah";   lastName="Brooks";     email="hannah.brooks@sunrisemedia.com";   phone="+1-323-555-3006"; company="Sunrise Media Group";     title="Marketing Director";    source="SOCIAL_MEDIA"; description="Engaged on LinkedIn post. Looking for marketing automation." },
    @{ firstName="Benjamin"; lastName="Grant";      email="benjamin.grant@pacificfoods.com";  phone="+1-206-555-3007"; company="Pacific Foods Co";        title="Supply Chain Manager";  source="TRADE_SHOW";   description="Visited booth at FoodTech Expo. Inventory management needs." },
    @{ firstName="Natalie";  lastName="Diaz";       email="natalie.diaz@cloudops360.com";     phone="+1-512-555-3008"; company="CloudOps 360";            title="DevOps Lead";           source="WEB";          description="Signed up for free trial. Evaluating for 50-person team." },
    @{ firstName="Tyler";    lastName="Manning";    email="tyler.manning@legalpro.com";       phone="+1-404-555-3009"; company="LegalPro Associates";     title="Managing Partner";      source="REFERRAL";     description="Referred by existing client. Needs CRM for law firm." },
    @{ firstName="Isabella"; lastName="Santos";     email="isabella.santos@nexushealth.com";  phone="+1-305-555-3010"; company="Nexus Health Systems";    title="VP Operations";         source="PHONE";        description="Called in after seeing ad. Immediate need for patient CRM." },
    @{ firstName="Jason";    lastName="Patel";      email="jason.patel@quantumrobotics.com";  phone="+1-734-555-3011"; company="Quantum Robotics";        title="CEO";                   source="WEB";          description="Requested demo. Series B startup, rapid growth phase." },
    @{ firstName="Catherine"; lastName="Lee";       email="catherine.lee@premier-hospitality.com"; phone="+1-702-555-3012"; company="Premier Hospitality"; title="General Manager";    source="TRADE_SHOW";   description="Hotel chain. Needs guest relationship management." },
    @{ firstName="Andrew";   lastName="Fischer";    email="andrew.fischer@steelbridge.com";   phone="+1-412-555-3013"; company="Steelbridge Engineering"; title="Operations Director";   source="EMAIL";        description="Opened 3 emails. Requested pricing for 200 seats." },
    @{ firstName="Monica";   lastName="Graves";     email="monica.graves@horizonagri.com";    phone="+1-515-555-3014"; company="Horizon Agriculture";     title="VP Technology";         source="SOCIAL_MEDIA"; description="Commented on webinar. Agricultural CRM interest." },
    @{ firstName="Russell";  lastName="Pope";       email="russell.pope@eliteauto.com";       phone="+1-313-555-3015"; company="Elite Auto Group";        title="Dealer Principal";      source="PHONE";        description="Multi-location dealership. Needs automotive CRM." }
)

foreach ($l in $leadData) {
    $body = $l | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "$BASE`:8082/api/v1/leads" -Body $body -Token $T
    if ($res -and $res.data) { Write-Host "  OK $($l.firstName) $($l.lastName) – $($l.company)" -ForegroundColor Green }
}

###############################################################################
# WORKFLOWS
###############################################################################
Write-Host "`n--- Creating Workflows ---" -ForegroundColor Cyan

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
        name        = 'Notify manager on large deals'
        description = 'Sends notification when opportunity amount exceeds $500K'
        entityType  = "OPPORTUNITY"
        triggerEvent= "CREATED"
        conditions  = @( @{ fieldName="amount"; operator="GREATER_THAN"; value="500000" } )
        actions     = @( @{ actionType="SEND_EMAIL"; targetValue='High-value deal created' } )
    },
    @{
        name        = "Follow-up on stale opportunities"
        description = "Creates follow-up task when opportunity needs attention"
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
        actions     = @( @{ actionType="SEND_EMAIL"; targetValue="Overdue activity alert" } )
    }
)

foreach ($w in $workflows) {
    $body = $w | ConvertTo-Json -Depth 4
    $res = Invoke-Api -Method POST -Uri "$BASE`:8088/api/v1/workflows" -Body $body -Token $T
    if ($res) { Write-Host "  OK $($w.name)" -ForegroundColor Green }
}

Write-Host "`nDone!" -ForegroundColor Cyan
