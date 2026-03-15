###############################################################################
#  CRM Platform - Real Estate Business Demo Seed Script
#  Populates the CRM with a full real-estate brokerage scenario.
#
#  Usage:   .\demo\seed-real-estate.ps1
#  Prereqs: All Docker services running (docker compose up -d)
###############################################################################

$ErrorActionPreference = "Continue"
$BASE = "http://localhost"

function Write-Step  { param($msg) Write-Host "`n> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "  + $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "  ! $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "  X $msg" -ForegroundColor Red }

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
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers; ErrorAction = "Stop" }
    if ($Body -and $Method -ne "GET") {
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
    }
    try {
        return Invoke-RestMethod @params
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Warn "$Method $Uri -> $status"
        return $null
    }
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Magenta
Write-Host "  Real Estate CRM - End-to-End Business Demo Seed" -ForegroundColor Magenta
Write-Host "  Pipeline: Inquiry > Viewing > Offer > Negotiation > Closed" -ForegroundColor Magenta
Write-Host "================================================================" -ForegroundColor Magenta

###############################################################################
# 1. REGISTER / LOGIN REAL ESTATE AGENTS
###############################################################################
Write-Step "Registering real estate team members..."

$users = @(
    @{ email='rachel.morgan@premierrealty.com';  password='Demo@2026!'; firstName='Rachel';  lastName='Morgan';   role='Broker / Admin' },
    @{ email='david.kim@premierrealty.com';      password='Demo@2026!'; firstName='David';   lastName='Kim';      role='Sales Manager' },
    @{ email='sofia.martinez@premierrealty.com'; password='Demo@2026!'; firstName='Sofia';   lastName='Martinez'; role='Buyers Agent' },
    @{ email='jason.carter@premierrealty.com';   password='Demo@2026!'; firstName='Jason';   lastName='Carter';   role='Listing Agent' },
    @{ email='priya.patel@premierrealty.com';    password='Demo@2026!'; firstName='Priya';   lastName='Patel';    role='Leasing Agent' }
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

    $res = Invoke-Api -Method POST -Uri "${BASE}:8081/api/v1/auth/register" -Body $body
    if ($res -and $res.data) {
        $tokens[$u.email]  = $res.data.accessToken
        $userIds[$u.email] = $res.data.userId
        Write-Ok "$($u.firstName) $($u.lastName) registered ($($u.role))"
    } else {
        $loginBody = @{ email = $u.email; password = $u.password; tenantId = "default" } | ConvertTo-Json
        $res = Invoke-Api -Method POST -Uri "${BASE}:8081/api/v1/auth/login" -Body $loginBody
        if ($res -and $res.data) {
            $tokens[$u.email]  = $res.data.accessToken
            $userIds[$u.email] = $res.data.userId
            Write-Ok "$($u.firstName) $($u.lastName) logged in ($($u.role))"
        } else {
            Write-Err "Failed to auth $($u.email)"
        }
    }
}

$T       = $tokens['rachel.morgan@premierrealty.com']
$adminId = $userIds['rachel.morgan@premierrealty.com']
$mgrId   = $userIds['david.kim@premierrealty.com']
$agent1  = $userIds['sofia.martinez@premierrealty.com']
$agent2  = $userIds['jason.carter@premierrealty.com']
$agent3  = $userIds['priya.patel@premierrealty.com']

if (-not $T) { Write-Err "Cannot proceed without admin token"; exit 1 }

###############################################################################
# 2. ACCOUNTS - Real Estate Agencies and Developers
###############################################################################
Write-Step "Creating real estate accounts..."

$accountData = @(
    @{ name='Premier Realty Group';         industry='Real Estate';            website='https://premierrealty.com';     phone='+1-310-555-1001'; annualRevenue=12000000;  numberOfEmployees=45;  type='CUSTOMER'; territory='Los Angeles';  segment='MID_MARKET'; lifecycleStage='ACTIVE';     billingAddress='8500 Wilshire Blvd Beverly Hills CA 90211'; description='Full-service residential brokerage covering LA metro area with 200+ listings annually' },
    @{ name='Skyline Development Corp';     industry='Real Estate';            website='https://skylinedev.com';       phone='+1-305-555-2002'; annualRevenue=85000000;  numberOfEmployees=120; type='CUSTOMER'; territory='Miami';        segment='ENTERPRISE'; lifecycleStage='ACTIVE';     billingAddress='1200 Brickell Ave Miami FL 33131';          description='Luxury condo developer specializing in waterfront high-rises in Miami' },
    @{ name='Oakwood Property Management';  industry='Property Management';    website='https://oakwoodpm.com';        phone='+1-512-555-3003'; annualRevenue=8000000;   numberOfEmployees=60;  type='CUSTOMER'; territory='Austin';       segment='MID_MARKET'; lifecycleStage='ACTIVE';     billingAddress='600 Congress Ave Austin TX 78701';          description='Manages 2500+ rental units across Central Texas' },
    @{ name='Greenfield Homes';             industry='Home Building';          website='https://greenfieldhomes.com';  phone='+1-602-555-4004'; annualRevenue=45000000;  numberOfEmployees=200; type='PROSPECT'; territory='Phoenix';      segment='ENTERPRISE'; lifecycleStage='EVALUATION'; billingAddress='4400 N Scottsdale Rd Scottsdale AZ 85251';  description='New construction builder with 5 active communities in Greater Phoenix' },
    @{ name='Harbor View Realty';           industry='Real Estate';            website='https://harborviewrealty.com'; phone='+1-619-555-5005'; annualRevenue=6500000;   numberOfEmployees=25;  type='CUSTOMER'; territory='San Diego';    segment='SMB';        lifecycleStage='ACTIVE';     billingAddress='725 5th Ave San Diego CA 92101';            description='Boutique brokerage specializing in coastal properties in San Diego' },
    @{ name='Summit Commercial Realty';     industry='Commercial Real Estate'; website='https://summitcre.com';        phone='+1-212-555-6006'; annualRevenue=32000000;  numberOfEmployees=80;  type='PARTNER';  territory='New York';     segment='ENTERPRISE'; lifecycleStage='ACTIVE';     billingAddress='350 5th Ave New York NY 10118';             description='Commercial and mixed-use property specialists for Manhattan and Brooklyn' },
    @{ name='Sunrise Senior Living';        industry='Senior Living';          website='https://sunrisesenior.com';    phone='+1-703-555-7007'; annualRevenue=15000000;  numberOfEmployees=150; type='PROSPECT'; territory='Virginia';     segment='MID_MARKET'; lifecycleStage='ONBOARDING'; billingAddress='7900 Westpark Dr McLean VA 22102';          description='55-plus communities developer looking for CRM to manage buyer leads' },
    @{ name='Pacific Coast Mortgage';       industry='Mortgage Lending';       website='https://pacificcoastmtg.com'; phone='+1-949-555-8008'; annualRevenue=22000000;  numberOfEmployees=95;  type='PARTNER';  territory='Orange County'; segment='MID_MARKET'; lifecycleStage='ACTIVE';     billingAddress='19800 MacArthur Blvd Irvine CA 92612';     description='Preferred mortgage partner for pre-qualification and buyer financing' }
)

$accountIds = @()
foreach ($a in $accountData) {
    $body = $a | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8083/api/v1/accounts" -Body $body -Token $T
    if ($res -and $res.data -and $res.data.id) {
        $accountIds += $res.data.id
        Write-Ok "$($a.name) ($($a.type))"
    } else {
        $accountIds += 'skip'
        Write-Warn "Skipped $($a.name)"
    }
}

###############################################################################
# 3. CONTACTS - Buyers Sellers Investors with Property Preferences
###############################################################################
Write-Step "Creating contacts with property preferences..."

$contactData = @(
    @{ firstName='Michael';  lastName='Zhang';      email='michael.zhang@gmail.com';       phone='+1-310-555-2101'; title='Home Buyer';         department='Residential';        accountIdx=0; lifecycleStage='OPPORTUNITY'; segment='DECISION_MAKER'; description='Looking for 4BR single-family in Beverly Hills. Budget 2.5M-3.5M. Must have pool and home office. Pre-approved with Pacific Coast Mortgage.' },
    @{ firstName='Jennifer'; lastName='Okafor';     email='jennifer.okafor@outlook.com';   phone='+1-305-555-2102'; title='Luxury Buyer';       department='Luxury Residential';  accountIdx=1; lifecycleStage='OPPORTUNITY'; segment='DECISION_MAKER'; description='Seeking 3BR waterfront condo in Miami. Budget 1.8M-2.5M. Ocean view mandatory. Interested in Skyline Tower pre-construction.' },
    @{ firstName='Robert';   lastName='Henderson';  email='robert.henderson@yahoo.com';    phone='+1-512-555-2103'; title='Property Investor';  department='Investment';          accountIdx=2; lifecycleStage='CUSTOMER';    segment='DECISION_MAKER'; description='Portfolio investor with 12 rental units in Austin. Looking to acquire 5-10 more SFR properties. Budget 300K-500K per unit. Cash buyer.' },
    @{ firstName='Sarah';    lastName='Nakamura';   email='sarah.nakamura@icloud.com';     phone='+1-602-555-2104'; title='First-Time Buyer';   department='New Construction';    accountIdx=3; lifecycleStage='LEAD';        segment='INFLUENCER';     description='First-time buyer interested in Greenfield Homes communities. Budget 400K-550K. 3BR min prefers modern farmhouse style. Needs FHA financing.' },
    @{ firstName='David';    lastName='Reeves';     email='david.reeves@me.com';           phone='+1-619-555-2105'; title='Seller';             department='Residential';        accountIdx=4; lifecycleStage='CUSTOMER';    segment='DECISION_MAKER'; description='Listing 5BR coastal property at 1840 Ocean Blvd La Jolla. Asking 4.2M. Property has ocean views and renovated kitchen. Motivated seller relocating in 60 days.' },
    @{ firstName='Amanda';   lastName='Collins';    email='amanda.collins@protonmail.com'; phone='+1-212-555-2106'; title='Commercial Buyer';   department='Commercial';          accountIdx=5; lifecycleStage='OPPORTUNITY'; segment='EXECUTIVE';      description='Seeking 10000+ sqft retail/office space in Manhattan. Budget 5M-8M. Needs ground-floor retail with upper-floor offices. 1031 exchange timeline.' },
    @{ firstName='Marcus';   lastName='Williams';   email='marcus.williams@gmail.com';     phone='+1-703-555-2107'; title='Retirement Buyer';   department='Senior Living';       accountIdx=6; lifecycleStage='SQL';         segment='DECISION_MAKER'; description='Downsizing to 55-plus community in McLean VA. Budget 650K-800K. 2BR plus den single-story preferred. Wife needs accessibility features.' },
    @{ firstName='Olivia';   lastName='Park';       email='olivia.park@hotmail.com';       phone='+1-415-555-2108'; title='Relocation Buyer';   department='Residential';        accountIdx=0; lifecycleStage='MQL';         segment='INFLUENCER';     description='Relocating from NYC to LA for work. Budget 1.2M-1.8M. Needs good school district. 3-4BR modern finishes. Timeline 90 days.' },
    @{ firstName='Carlos';   lastName='Rivera';     email='carlos.rivera@live.com';        phone='+1-949-555-2109'; title='Investor';           department='Investment';          accountIdx=7; lifecycleStage='CUSTOMER';    segment='CHAMPION';       description='Fix-and-flip investor in Orange County. Budget 500K-800K per property. Seeks distressed SFR with rehab potential. 3+ deals per year.' },
    @{ firstName='Emily';    lastName='Thompson';   email='emily.thompson@gmail.com';      phone='+1-310-555-2110'; title='Luxury Seller';      department='Luxury Residential';  accountIdx=0; lifecycleStage='CUSTOMER';    segment='DECISION_MAKER'; description='Listing 6BR estate in Bel Air. Asking 12.5M. 8500 sqft infinity pool guest house. Celebrity clientele requires NDA and private showings only.' },
    @{ firstName='James';    lastName='OBrien';     email='james.obrien@comcast.net';      phone='+1-305-555-2111'; title='Vacation Buyer';     department='Vacation Home';       accountIdx=1; lifecycleStage='LEAD';        segment='INFLUENCER';     description='Looking for vacation condo in Miami Beach. Budget 800K-1.2M. 2BR furnished preferred. Plans to use as Airbnb rental when not visiting.' },
    @{ firstName='Linda';    lastName='Foster';     email='linda.foster@gmail.com';        phone='+1-512-555-2112'; title='Tenant Buyer';       department='Residential';        accountIdx=2; lifecycleStage='SUBSCRIBER';  segment='USER';           description='Currently renting in East Austin. Interested in rent-to-own or first-time buyer programs. Budget 250K-350K. Needs 2BR+ near transit.' }
)

$contactIds = @()
foreach ($c in $contactData) {
    $bodyObj = @{
        firstName      = $c.firstName
        lastName       = $c.lastName
        email          = $c.email
        phone          = $c.phone
        title          = $c.title
        department     = $c.department
        lifecycleStage = $c.lifecycleStage
        segment        = $c.segment
        emailOptIn     = $true
        description    = $c.description
    }
    if ($accountIds[$c.accountIdx] -ne 'skip') {
        $bodyObj['accountId'] = $accountIds[$c.accountIdx]
    }
    $body = $bodyObj | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8084/api/v1/contacts" -Body $body -Token $T
    if ($res -and $res.data -and $res.data.id) {
        $contactIds += $res.data.id
        $desc = $c.description
        if ($desc.Length -gt 50) { $desc = $desc.Substring(0, 50) + '...' }
        Write-Ok "$($c.firstName) $($c.lastName) - $($c.title) ($desc)"
    } else {
        $contactIds += 'skip'
        Write-Warn "Skipped $($c.firstName) $($c.lastName)"
    }
}

###############################################################################
# 4. LEADS - From Property Portals and Social Media
###############################################################################
Write-Step "Creating leads from property portals and social media..."

$leadData = @(
    @{ firstName='Tyler';    lastName='Brooks';     email='tyler.brooks@gmail.com';       phone='+1-310-555-3001'; company='Self';                 title='Home Buyer';       source='WEB';          description='Zillow inquiry: Interested in 3BR homes in Santa Monica under 1.5M. Clicked on 12 listings in past week.' },
    @{ firstName='Hannah';   lastName='Lee';        email='hannah.lee@yahoo.com';         phone='+1-305-555-3002'; company='Lee Consulting';       title='Investor';         source='WEB';          description='Realtor.com lead: Searched multi-family properties in Miami-Dade 800K-1.5M range. Downloaded investment ROI calculator.' },
    @{ firstName='Derek';    lastName='Sullivan';   email='derek.sullivan@outlook.com';   phone='+1-512-555-3003'; company='Sullivan Family Trust'; title='Trust Buyer';      source='REFERRAL';     description='Referred by Robert Henderson. Looking for 4BR in Westlake Hills Austin. Budget 700K-900K. Pre-approved.' },
    @{ firstName='Aisha';    lastName='Mohammed';   email='aisha.mohammed@gmail.com';     phone='+1-602-555-3004'; company='Self';                 title='First-Time Buyer'; source='SOCIAL_MEDIA'; description='Facebook ad response: Interested in Greenfield Homes Cactus Ridge community. Wants 3BR under 480K. Asked about HOA fees.' },
    @{ firstName='Ryan';     lastName='Costa';      email='ryan.costa@protonmail.com';    phone='+1-619-555-3005'; company='Costa Ventures LLC';   title='Investor';         source='WEB';          description='Zillow inquiry: Price-reduced coastal lots in Encinitas. Budget 1M-2M. Interested in building custom home.' },
    @{ firstName='Megan';    lastName='Whitfield';  email='megan.whitfield@icloud.com';   phone='+1-212-555-3006'; company='Self';                 title='Renter to Buyer';  source='SOCIAL_MEDIA'; description='Instagram DM: Saw our Manhattan loft listing Reel. First-time buyer budget 600K-900K. Wants open-concept in SoHo or Tribeca.' },
    @{ firstName='Kevin';    lastName='Tran';       email='kevin.tran@live.com';          phone='+1-949-555-3007'; company='Tran Holdings';        title='Flipper';          source='REFERRAL';     description='Referred by Carlos Rivera. Seeks distressed properties in OC for fix-and-flip. Budget 400K-600K. Cash ready.' },
    @{ firstName='Patricia'; lastName='Gomez';      email='patricia.gomez@gmail.com';     phone='+1-310-555-3008'; company='Self';                 title='Relocator';        source='WEB';          description='Redfin inquiry: Relocating from Chicago. Wants 4BR in Pasadena near Caltech. Budget 1.3M-1.7M. Moving in 45 days.' },
    @{ firstName='Brandon';  lastName='Hayes';      email='brandon.hayes@outlook.com';    phone='+1-703-555-3009'; company='Hayes Associates';     title='Downsizer';        source='EMAIL';        description='Email inquiry from open house: Selling 5BR colonial in McLean want to downsize. Also interested in 55-plus communities.' },
    @{ firstName='Samantha'; lastName='Reed';       email='samantha.reed@gmail.com';      phone='+1-305-555-3010'; company='Reed Family';          title='Vacation Buyer';   source='SOCIAL_MEDIA'; description='TikTok comment: Loved our Miami Beach condo tour video. Looking for furnished 2BR for winter getaways. Budget 700K-1M.' },
    @{ firstName='Nathan';   lastName='Wright';     email='nathan.wright@yahoo.com';      phone='+1-415-555-3011'; company='Wright Tech';          title='Tech Exec Buyer';  source='WEB';          description='Zillow Premier Agent lead: CTO looking for modern home in Palo Alto. Budget 3M-5M. Wants smart home features and EV charging.' },
    @{ firstName='Grace';    lastName='Chen';       email='grace.chen@hotmail.com';       phone='+1-512-555-3012'; company='Self';                 title='Grad Student';     source='SOCIAL_MEDIA'; description='Facebook Marketplace response: Looking for affordable condo near UT Austin campus. Budget 180K-250K. 1-2BR.' },
    @{ firstName='Victor';   lastName='Santos';     email='victor.santos@gmail.com';      phone='+1-619-555-3013'; company='Santos Development';   title='Developer';        source='PHONE';        description='Cold call follow-up: Commercial developer seeking entitled land parcels in San Diego county. 2-5 acres. Budget 2M-5M.' },
    @{ firstName='Diana';    lastName='Kowalski';   email='diana.kowalski@live.com';      phone='+1-602-555-3014'; company='Self';                 title='Luxury Buyer';     source='WEB';          description='Realtor.com: Searching for estates in Paradise Valley AZ. Budget 4M+. Wants minimum 1-acre lot mountain views wine cellar.' },
    @{ firstName='Chris';    lastName='Taylor';     email='chris.taylor@gmail.com';       phone='+1-212-555-3015'; company='Taylor Capital';       title='Investor';         source='REFERRAL';     description='Referred by Summit Commercial. Looking for mixed-use buildings in Brooklyn. Budget 3M-7M. 1031 exchange deadline in 120 days.' }
)

$leadIds = @()
$assignees = @($agent1, $agent2, $agent3, $mgrId)
$i = 0
foreach ($l in $leadData) {
    $bodyObj = @{
        firstName   = $l.firstName
        lastName    = $l.lastName
        email       = $l.email
        phone       = $l.phone
        company     = $l.company
        title       = $l.title
        source      = $l.source
        description = $l.description
    }
    if ($assignees[$i % $assignees.Count]) {
        $bodyObj['assignedTo'] = $assignees[$i % $assignees.Count]
    }
    $body = $bodyObj | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8082/api/v1/leads" -Body $body -Token $T
    if ($res -and $res.data -and $res.data.id) {
        $leadIds += $res.data.id
        Write-Ok "$($l.firstName) $($l.lastName) - $($l.source) - $($l.title)"
    } else {
        $leadIds += 'skip'
        Write-Warn "Skipped $($l.firstName) $($l.lastName)"
    }
    $i++
}

###############################################################################
# 5. OPPORTUNITIES - Property Deals
#    PROSPECTING=Inquiry  QUALIFICATION=Viewing  PROPOSAL=Offer
#    NEGOTIATION=Negotiation  CLOSED_WON=Closed  CLOSED_LOST=Lost
###############################################################################
Write-Step "Creating property deal opportunities..."

$oppData = @(
    @{ name='Zhang - Beverly Hills 4BR SFR';         accountIdx=0; contactIdx=0;  amount=2850000;  stage='NEGOTIATION';   probability=75;  closeDate='2026-04-15'; leadSource='WEB';          forecastCategory='BEST_CASE'; assignee=$agent1; description='4BR/3.5BA single-family 3200 sqft. 456 Palm Dr Beverly Hills. Seller countered at 2.9M buyer at 2.75M. Splitting difference likely.' },
    @{ name='Okafor - Skyline Tower Unit 42A';        accountIdx=1; contactIdx=1;  amount=2200000;  stage='PROPOSAL';      probability=60;  closeDate='2026-05-01'; leadSource='WEB';          forecastCategory='PIPELINE';  assignee=$agent1; description='3BR/2BA pre-construction luxury condo. 42nd floor direct ocean view. Buyer submitted 2.1M offer countered at 2.2M.' },
    @{ name='Henderson - Austin SFR Portfolio x5';    accountIdx=2; contactIdx=2;  amount=2100000;  stage='QUALIFICATION'; probability=40;  closeDate='2026-06-30'; leadSource='REFERRAL';     forecastCategory='PIPELINE';  assignee=$agent2; description='5 single-family rentals in East Austin. Viewing 3 properties this week. Investor pre-qualified for 2.5M line of credit. Cap rate target 6%+.' },
    @{ name='Nakamura - Cactus Ridge Lot 24';         accountIdx=3; contactIdx=3;  amount=485000;   stage='PROSPECTING';   probability=20;  closeDate='2026-07-15'; leadSource='SOCIAL_MEDIA'; forecastCategory='PIPELINE';  assignee=$agent2; description='New construction 3BR/2BA modern farmhouse. Initial inquiry from Facebook ad. Scheduled community tour for next Saturday.' },
    @{ name='Reeves - La Jolla Coastal Estate Sale';  accountIdx=4; contactIdx=4;  amount=4200000;  stage='QUALIFICATION'; probability=45;  closeDate='2026-05-30'; leadSource='WEB';          forecastCategory='PIPELINE';  assignee=$agent2; description='Listing: 5BR/4BA 4800 sqft at 1840 Ocean Blvd. 2 showings completed 1 second viewing scheduled. 3 interested buyers.' },
    @{ name='Collins - Manhattan Mixed-Use Building'; accountIdx=5; contactIdx=5;  amount=6500000;  stage='NEGOTIATION';   probability=70;  closeDate='2026-04-30'; leadSource='REFERRAL';     forecastCategory='BEST_CASE'; assignee=$mgrId;  description='12000 sqft mixed-use at 188 Bowery. Ground-floor retail + 3 upper office floors. 1031 exchange 45-day ID deadline approaching.' },
    @{ name='Williams - Sunrise Meadows 55+ Unit';    accountIdx=6; contactIdx=6;  amount=725000;   stage='PROPOSAL';      probability=55;  closeDate='2026-05-15'; leadSource='WEB';          forecastCategory='PIPELINE';  assignee=$agent3; description='2BR+den single-story in Sunrise Meadows. ADA accessible. Offer submitted at 700K listed at 749K. Seller reviewing.' },
    @{ name='Park - Pasadena Mid-Century Modern';     accountIdx=0; contactIdx=7;  amount=1550000;  stage='QUALIFICATION'; probability=35;  closeDate='2026-06-01'; leadSource='WEB';          forecastCategory='PIPELINE';  assignee=$agent1; description='3BR/2BA mid-century modern 2100 sqft. Near Caltech. Viewed last Tuesday. Buyer loved it but wants to see 2 more comps.' },
    @{ name='Rivera - OC Fix and Flip Tustin Ranch';  accountIdx=7; contactIdx=8;  amount=650000;   stage='CLOSED_WON';    probability=100; closeDate='2026-03-01'; leadSource='REFERRAL';     forecastCategory='CLOSED';    assignee=$agent2; description='3BR/2BA distressed SFR purchased at 650K. Rehab budget 120K. ARV estimated 920K. Closed 3/1. Rehab in progress.' },
    @{ name='Thompson - Bel Air Estate Listing';      accountIdx=0; contactIdx=9;  amount=12500000; stage='QUALIFICATION'; probability=30;  closeDate='2026-08-01'; leadSource='REFERRAL';     forecastCategory='PIPELINE';  assignee=$mgrId;  description='Exclusive listing: 6BR/7BA estate 8500 sqft on 1.2 acres. Private showings only NDA required. 2 qualified buyers toured so far.' },
    @{ name='OBrien - Miami Beach Vacation Condo';    accountIdx=1; contactIdx=10; amount=950000;   stage='PROSPECTING';   probability=15;  closeDate='2026-07-01'; leadSource='SOCIAL_MEDIA'; forecastCategory='PIPELINE';  assignee=$agent1; description='Initial inquiry for furnished 2BR condo in South Beach. Budget 800K-1.2M. Interested in Airbnb rental potential.' },
    @{ name='Santos - Encinitas Land Parcel';         accountIdx=4; contactIdx=-1; amount=3200000;  stage='PROPOSAL';      probability=50;  closeDate='2026-05-20'; leadSource='PHONE';        forecastCategory='BEST_CASE'; assignee=$agent2; description='3.5 acre entitled parcel on Coast Hwy. Zoned R-3 mixed residential. Offer at 3.0M asking 3.4M. Environmental report clear.' },
    @{ name='Kowalski - Paradise Valley Estate';      accountIdx=3; contactIdx=-1; amount=5800000;  stage='PROSPECTING';   probability=10;  closeDate='2026-09-01'; leadSource='WEB';          forecastCategory='PIPELINE';  assignee=$agent3; description='Luxury estate search in Paradise Valley. 1+ acre mountain views. Touring 3 properties next month. Budget 4M-7M.' },
    @{ name='Taylor - Brooklyn Mixed-Use 1031';       accountIdx=5; contactIdx=-1; amount=4500000;  stage='NEGOTIATION';   probability=65;  closeDate='2026-04-20'; leadSource='REFERRAL';     forecastCategory='BEST_CASE'; assignee=$mgrId;  description='2-building portfolio in Williamsburg. 8 residential units + 2 commercial. 1031 exchange with 120-day closeout. Due diligence in progress.' },
    @{ name='Brooks - Santa Monica 3BR';              accountIdx=0; contactIdx=-1; amount=1350000;  stage='PROSPECTING';   probability=20;  closeDate='2026-06-15'; leadSource='WEB';          forecastCategory='PIPELINE';  assignee=$agent1; description='Zillow lead converted to opportunity. Wants 3BR under 1.5M near beach. Scheduled 3 viewings for Saturday open house tour.' }
)

$oppIds = @()
foreach ($o in $oppData) {
    $bodyObj = @{
        name             = $o.name
        amount           = $o.amount
        stage            = $o.stage
        probability      = $o.probability
        closeDate        = $o.closeDate
        description      = $o.description
        leadSource       = $o.leadSource
        forecastCategory = $o.forecastCategory
    }
    if ($o.assignee) { $bodyObj['assignedTo'] = $o.assignee }
    if ($o.accountIdx -ge 0 -and $accountIds[$o.accountIdx] -ne 'skip') {
        $bodyObj['accountId'] = $accountIds[$o.accountIdx]
    }
    if ($o.contactIdx -ge 0 -and $contactIds[$o.contactIdx] -ne 'skip') {
        $bodyObj['contactId'] = $contactIds[$o.contactIdx]
    }
    $body = $bodyObj | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8085/api/v1/opportunities" -Body $body -Token $T
    if ($res -and $res.data -and $res.data.id) {
        $oppIds += $res.data.id
        Write-Ok "$($o.name) - $($o.stage) - $($o.amount)"
    } else {
        $oppIds += 'skip'
        Write-Warn "Skipped $($o.name)"
    }
}

###############################################################################
# 6. OPPORTUNITY PRODUCTS - Property Details as Line Items
###############################################################################
Write-Step "Adding property details to opportunities..."

$products = @(
    @{ oppIdx=0;  productName='456 Palm Dr Beverly Hills';          productCode='RES-BH-456';   quantity=1; unitPrice=2850000;  discount=0; description='4BR/3.5BA SFR 3200 sqft pool home office 2-car garage' },
    @{ oppIdx=1;  productName='Skyline Tower Unit 42A';             productCode='CON-MIA-42A';  quantity=1; unitPrice=2200000;  discount=0; description='3BR/2BA pre-construction condo 42nd floor ocean view 2100 sqft' },
    @{ oppIdx=2;  productName='East Austin SFR Portfolio 5 units';  productCode='INV-AUS-5PK';  quantity=5; unitPrice=420000;   discount=0; description='5x 3BR/2BA rental homes avg 1400 sqft avg rent 2200/mo' },
    @{ oppIdx=3;  productName='Cactus Ridge Lot 24 New Build';      productCode='NEW-PHX-CR24'; quantity=1; unitPrice=485000;   discount=0; description='3BR/2BA modern farmhouse 1800 sqft 0.25 acre lot 2-car garage' },
    @{ oppIdx=4;  productName='1840 Ocean Blvd La Jolla';           productCode='LUX-SD-1840';  quantity=1; unitPrice=4200000;  discount=0; description='5BR/4BA coastal estate 4800 sqft panoramic ocean views guest suite' },
    @{ oppIdx=5;  productName='188 Bowery Manhattan';               productCode='COM-NYC-188';  quantity=1; unitPrice=6500000;  discount=0; description='12000 sqft mixed-use ground-floor retail + 3 office floors' },
    @{ oppIdx=6;  productName='Sunrise Meadows Unit 112';           productCode='SEN-VA-112';   quantity=1; unitPrice=725000;   discount=0; description='2BR+den single-story ADA accessible 55+ community 1600 sqft' },
    @{ oppIdx=8;  productName='14 Tustin Ranch Rd Tustin';          productCode='FLP-OC-14TR';  quantity=1; unitPrice=650000;   discount=0; description='3BR/2BA distressed SFR 1500 sqft needs full rehab ARV 920K' },
    @{ oppIdx=9;  productName='Bel Air Estate 100 Stone Canyon';    productCode='LUX-LA-100SC'; quantity=1; unitPrice=12500000; discount=0; description='6BR/7BA 8500 sqft 1.2 acres infinity pool guest house views' }
)

foreach ($p in $products) {
    if ($oppIds[$p.oppIdx] -eq 'skip') { continue }
    $body = @{
        productName = $p.productName
        productCode = $p.productCode
        quantity    = $p.quantity
        unitPrice   = $p.unitPrice
        discount    = $p.discount
        description = $p.description
    } | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8085/api/v1/opportunities/$($oppIds[$p.oppIdx])/products" -Body $body -Token $T
    if ($res) { Write-Ok "$($p.productName)" }
    else { Write-Warn "Skipped product $($p.productName)" }
}

###############################################################################
# 7. ACTIVITIES - Viewings Calls Follow-ups Open Houses
###############################################################################
Write-Step "Creating activities..."

$now = Get-Date
$activityData = @(
    @{ type='MEETING'; subject='Property Viewing: 456 Palm Dr Beverly Hills';       priority='HIGH';   dueDate=($now.AddDays(2)).ToString("yyyy-MM-dd'T'10:00:00");  entityType='OPPORTUNITY'; entityIdx=0;  assignee=$agent1; description='In-person showing for Michael Zhang. 4BR/3.5BA 3200 sqft. Prepare property brochure and comparable sales data.' },
    @{ type='MEETING'; subject='Open House: 1840 Ocean Blvd La Jolla';              priority='HIGH';   dueDate=($now.AddDays(3)).ToString("yyyy-MM-dd'T'14:00:00");  entityType='OPPORTUNITY'; entityIdx=4;  assignee=$agent2; description='Public open house 2-5 PM. Prepare sign-in sheet listing flyers and refreshments. 15+ expected visitors.' },
    @{ type='CALL';    subject='Follow-up: Skyline Tower showing with Jennifer';     priority='MEDIUM'; dueDate=($now.AddDays(1)).ToString("yyyy-MM-dd'T'09:00:00");  entityType='OPPORTUNITY'; entityIdx=1;  assignee=$agent1; description='Call Jennifer Okafor to discuss thoughts after Tuesday condo viewing. Address parking and HOA concerns.' },
    @{ type='CALL';    subject='Pre-qualification check: Sarah Nakamura';            priority='MEDIUM'; dueDate=($now.AddDays(1)).ToString("yyyy-MM-dd'T'11:00:00");  entityType='OPPORTUNITY'; entityIdx=3;  assignee=$agent2; description='Call Pacific Coast Mortgage to verify FHA pre-approval status for Sarah. Confirm max loan amount.' },
    @{ type='TASK';    subject='Prepare CMA for Reeves listing';                     priority='HIGH';   dueDate=($now.AddDays(2)).ToString("yyyy-MM-dd'T'08:00:00");  entityType='OPPORTUNITY'; entityIdx=4;  assignee=$agent2; description='Run Comparative Market Analysis for 1840 Ocean Blvd. Pull 6-month sold comps within 0.5 mile radius.' },
    @{ type='MEETING'; subject='Viewing Tour: Henderson - 3 Austin Rentals';         priority='HIGH';   dueDate=($now.AddDays(4)).ToString("yyyy-MM-dd'T'09:00:00");  entityType='OPPORTUNITY'; entityIdx=2;  assignee=$agent2; description='Tour 3 of 5 SFR rental properties with Robert Henderson. Bring rental comps and cap rate analysis.' },
    @{ type='TASK';    subject='Draft NDA for Bel Air showings';                     priority='URGENT'; dueDate=($now.AddDays(1)).ToString("yyyy-MM-dd'T'08:00:00");  entityType='OPPORTUNITY'; entityIdx=9;  assignee=$mgrId;  description='Prepare confidentiality agreement for Bel Air estate private showings. Celebrity seller requires all buyers sign before touring.' },
    @{ type='EMAIL';   subject='Send listing brochure to Tyler Brooks';              priority='LOW';    dueDate=($now.AddDays(1)).ToString("yyyy-MM-dd'T'10:00:00");  entityType='LEAD';        entityIdx=0;  assignee=$agent1; description='Email top 5 Santa Monica listings matching Tyler criteria 3BR under 1.5M with photos and virtual tour links.' },
    @{ type='MEETING'; subject='Sunrise Meadows Tour: Marcus Williams';              priority='MEDIUM'; dueDate=($now.AddDays(5)).ToString("yyyy-MM-dd'T'10:00:00");  entityType='OPPORTUNITY'; entityIdx=6;  assignee=$agent3; description='Community tour of Sunrise Meadows 55-plus with Marcus and wife. Focus on ADA-accessible units. Meet at clubhouse.' },
    @{ type='CALL';    subject='1031 Exchange Deadline Review: Amanda Collins';       priority='URGENT'; dueDate=($now.AddDays(0)).ToString("yyyy-MM-dd'T'14:00:00");  entityType='OPPORTUNITY'; entityIdx=5;  assignee=$mgrId;  description='Review 1031 exchange timeline with Amanda and her QI. 45-day identification deadline is April 3. Must finalize target properties.' },
    @{ type='TASK';    subject='Update MLS listing photos for La Jolla property';    priority='MEDIUM'; dueDate=($now.AddDays(3)).ToString("yyyy-MM-dd'T'09:00:00");  entityType='OPPORTUNITY'; entityIdx=4;  assignee=$agent2; description='Professional photographer delivering new drone and twilight shots. Upload to MLS Zillow and Realtor.com.' },
    @{ type='MEETING'; subject='Saturday Open House Tour: 3 Santa Monica properties'; priority='HIGH'; dueDate=($now.AddDays(6)).ToString("yyyy-MM-dd'T'10:00:00");  entityType='OPPORTUNITY'; entityIdx=14; assignee=$agent1; description='Take Tyler Brooks on guided tour of 3 pre-selected homes in Santa Monica. Prepare comparison sheets.' },
    @{ type='CALL';    subject='Mortgage rate lock discussion: Olivia Park';         priority='HIGH';   dueDate=($now.AddDays(2)).ToString("yyyy-MM-dd'T'15:00:00");  entityType='OPPORTUNITY'; entityIdx=7;  assignee=$agent1; description='Rates are rising. Advise Olivia to lock her rate with Pacific Coast Mortgage. Current 5.875 percent.' },
    @{ type='TASK';    subject='Process Rivera closing documents';                   priority='HIGH';   dueDate=($now.AddDays(0)).ToString("yyyy-MM-dd'T'16:00:00");  entityType='OPPORTUNITY'; entityIdx=8;  assignee=$agent2; description='Finalize closing paperwork for Tustin Ranch flip. Wire transfer confirmed. Send signed deed to title company.' },
    @{ type='EMAIL';   subject='Weekly market update newsletter';                    priority='MEDIUM'; dueDate=($now.AddDays(5)).ToString("yyyy-MM-dd'T'08:00:00");  entityType='LEAD';        entityIdx=-1; assignee=$agent3; description='Send weekly email blast to all leads: new listings price reductions market trends interest rate update and upcoming open houses.' }
)

foreach ($a in $activityData) {
    $bodyObj = @{
        type        = $a.type
        subject     = $a.subject
        description = $a.description
        priority    = $a.priority
        dueDate     = $a.dueDate
    }
    if ($a.assignee) { $bodyObj['assignedTo'] = $a.assignee }
    if ($a.entityIdx -ge 0) {
        $bodyObj['relatedEntityType'] = $a.entityType
        if ($a.entityType -eq 'OPPORTUNITY' -and $oppIds[$a.entityIdx] -ne 'skip') {
            $bodyObj['relatedEntityId'] = $oppIds[$a.entityIdx]
        }
        if ($a.entityType -eq 'LEAD' -and $leadIds[$a.entityIdx] -ne 'skip') {
            $bodyObj['relatedEntityId'] = $leadIds[$a.entityIdx]
        }
    }
    $body = $bodyObj | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8086/api/v1/activities" -Body $body -Token $T
    if ($res) { Write-Ok "$($a.type) - $($a.subject)" }
    else { Write-Warn "Skipped activity: $($a.subject)" }
}

###############################################################################
# 8. WORKFLOWS - Automated Follow-Up After Viewings
###############################################################################
Write-Step "Creating automated real estate workflows..."

$workflows = @(
    @{
        name        = 'Post-Viewing Follow-Up 24hr'
        description = 'Automatically sends a follow-up email 24 hours after a property viewing is completed. Helps maintain buyer engagement and collect feedback.'
        entityType  = 'ACTIVITY'
        triggerEvent = 'STATUS_CHANGED'
        conditions  = @(
            @{ fieldName = 'type';   operator = 'EQUALS'; value = 'MEETING' },
            @{ fieldName = 'status'; operator = 'EQUALS'; value = 'COMPLETED' }
        )
        actions     = @(
            @{ actionType = 'SEND_EMAIL';        targetField = 'email'; targetValue = 'Thank you for attending the property viewing! We would love to hear your thoughts. Reply to this email or call us to discuss next steps.' },
            @{ actionType = 'CREATE_TASK';       targetField = 'subject'; targetValue = 'Follow-up call: Get feedback on property viewing' },
            @{ actionType = 'SEND_NOTIFICATION'; targetField = 'message'; targetValue = 'Viewing follow-up needed - buyer viewed property yesterday' }
        )
    },
    @{
        name        = 'New Portal Lead Auto-Assign and Notify'
        description = 'When a new lead arrives from Zillow Realtor.com or web portal automatically assign to the on-duty agent and send an instant notification.'
        entityType  = 'LEAD'
        triggerEvent = 'CREATED'
        conditions  = @(
            @{ fieldName = 'source'; operator = 'IN'; value = 'WEB,SOCIAL_MEDIA' }
        )
        actions     = @(
            @{ actionType = 'SEND_NOTIFICATION'; targetField = 'message'; targetValue = 'New property lead from portal! Contact within 5 minutes for best conversion rate.' },
            @{ actionType = 'CREATE_TASK';       targetField = 'subject'; targetValue = 'Call new portal lead within 5 minutes' },
            @{ actionType = 'SEND_EMAIL';        targetField = 'email'; targetValue = 'Thank you for your interest! A Premier Realty agent will contact you within minutes to help with your property search.' }
        )
    },
    @{
        name        = 'Offer Submitted - Manager Alert'
        description = 'When a deal moves to PROPOSAL stage automatically notify the sales manager for review and approval tracking.'
        entityType  = 'OPPORTUNITY'
        triggerEvent = 'STAGE_CHANGED'
        conditions  = @(
            @{ fieldName = 'stage'; operator = 'EQUALS'; value = 'PROPOSAL' }
        )
        actions     = @(
            @{ actionType = 'SEND_NOTIFICATION'; targetField = 'message'; targetValue = 'New offer submitted! Review deal terms and approve before counter-offer deadline.' },
            @{ actionType = 'CREATE_TASK';       targetField = 'subject'; targetValue = 'Review and approve offer terms' }
        )
    },
    @{
        name        = 'Stale Lead Re-Engagement 7 days'
        description = 'If a lead status has not changed from NEW in 7 days trigger a re-engagement sequence with new listings matching their criteria.'
        entityType  = 'LEAD'
        triggerEvent = 'UPDATED'
        conditions  = @(
            @{ fieldName = 'status'; operator = 'EQUALS'; value = 'NEW' }
        )
        actions     = @(
            @{ actionType = 'SEND_EMAIL';        targetField = 'email'; targetValue = 'We found new properties matching your search! Check out this weeks fresh listings in your preferred area.' },
            @{ actionType = 'UPDATE_FIELD';      targetField = 'status'; targetValue = 'CONTACTED' },
            @{ actionType = 'SEND_NOTIFICATION'; targetField = 'message'; targetValue = 'Stale lead re-engaged with new listings email' }
        )
    },
    @{
        name        = 'Closed Deal - Celebration and Referral Request'
        description = 'When a deal is marked CLOSED_WON notify the team and create a task to send a closing gift and request a referral or review.'
        entityType  = 'OPPORTUNITY'
        triggerEvent = 'STAGE_CHANGED'
        conditions  = @(
            @{ fieldName = 'stage'; operator = 'EQUALS'; value = 'CLOSED_WON' }
        )
        actions     = @(
            @{ actionType = 'SEND_NOTIFICATION'; targetField = 'message'; targetValue = 'Deal closed! Congratulations to the team. Time to celebrate and request a client review.' },
            @{ actionType = 'CREATE_TASK';       targetField = 'subject'; targetValue = 'Send closing gift basket and request Zillow or Google review' },
            @{ actionType = 'SEND_EMAIL';        targetField = 'email'; targetValue = 'Congratulations on your new home! It was a pleasure helping you. If you know anyone else looking we offer a referral bonus!' }
        )
    },
    @{
        name        = 'Negotiation Stage - Urgency Tasks'
        description = 'When a deal enters NEGOTIATION create urgent tasks for counter-offer deadlines inspection scheduling and appraisal ordering.'
        entityType  = 'OPPORTUNITY'
        triggerEvent = 'STAGE_CHANGED'
        conditions  = @(
            @{ fieldName = 'stage'; operator = 'EQUALS'; value = 'NEGOTIATION' }
        )
        actions     = @(
            @{ actionType = 'CREATE_TASK';       targetField = 'subject'; targetValue = 'Schedule home inspection within 10-day contingency window' },
            @{ actionType = 'CREATE_TASK';       targetField = 'subject'; targetValue = 'Order property appraisal for lender' },
            @{ actionType = 'SEND_NOTIFICATION'; targetField = 'message'; targetValue = 'Deal in negotiation - track counter-offer deadlines and contingencies closely' }
        )
    }
)

foreach ($w in $workflows) {
    $body = $w | ConvertTo-Json -Depth 5
    $res = Invoke-Api -Method POST -Uri "${BASE}:8088/api/v1/workflows" -Body $body -Token $T
    if ($res) { Write-Ok "$($w.name)" }
    else { Write-Warn "Skipped workflow: $($w.name)" }
}

###############################################################################
# 9. CASES - Support Requests and Issues
###############################################################################
Write-Step "Creating support cases..."

$caseData = @(
    @{ subject='Home inspection contingency dispute';         priority='HIGH';     origin='PHONE';        contactIdx=0; accountIdx=0; description='Buyer Michael Zhang inspector found foundation micro-cracks at 456 Palm Dr. Seller disputes severity. Need independent structural engineer report. Contingency deadline April 5.' },
    @{ subject='HOA document review delay - Skyline Tower';   priority='MEDIUM';   origin='EMAIL';        contactIdx=1; accountIdx=1; description='Jennifer Okafor requested HOA financials and meeting minutes for Skyline Tower. Management company delayed 2 weeks. Need HOA docs before buyer can remove contingency.' },
    @{ subject='Title search issue - Austin portfolio';       priority='HIGH';     origin='PORTAL';       contactIdx=2; accountIdx=2; description='Title company found a lien on one of the 5 rental properties 567 Holly St. Previous owner contractor lien for 8500. Needs resolution before closing.' },
    @{ subject='Listing photo complaint - La Jolla';          priority='LOW';      origin='SOCIAL_MEDIA'; contactIdx=4; accountIdx=4; description='Neighboring homeowner complained that drone photos captured their backyard. Requesting photos be re-edited to blur neighboring properties.' },
    @{ subject='Appraisal came in low - Sunrise Meadows';     priority='CRITICAL'; origin='PHONE';        contactIdx=6; accountIdx=6; description='Appraisal for Unit 112 came in at 680K vs 725K offer price. 45K gap. Need to renegotiate price ask seller for credit or buyer brings additional cash.' },
    @{ subject='Lead routing failure - Zillow leads';         priority='HIGH';     origin='PORTAL';       contactIdx=-1; accountIdx=-1; description='Last 3 Zillow leads were not auto-assigned to agents. Speed-to-lead SLA of 5 minutes was violated. Check workflow and integration status.' }
)

foreach ($c in $caseData) {
    $bodyObj = @{
        subject     = $c.subject
        description = $c.description
        priority    = $c.priority
        origin      = $c.origin
    }
    if ($c.contactIdx -ge 0 -and $contactIds[$c.contactIdx] -ne 'skip') {
        $bodyObj['contactId']    = $contactIds[$c.contactIdx]
        $bodyObj['contactName']  = "$($contactData[$c.contactIdx].firstName) $($contactData[$c.contactIdx].lastName)"
        $bodyObj['contactEmail'] = $contactData[$c.contactIdx].email
    }
    if ($c.accountIdx -ge 0 -and $accountIds[$c.accountIdx] -ne 'skip') {
        $bodyObj['accountId']   = $accountIds[$c.accountIdx]
        $bodyObj['accountName'] = $accountData[$c.accountIdx].name
    }
    $body = $bodyObj | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8092/api/v1/cases" -Body $body -Token $T
    if ($res) { Write-Ok "$($c.priority) - $($c.subject)" }
    else { Write-Warn "Skipped case: $($c.subject)" }
}

###############################################################################
# 10. CAMPAIGNS - Marketing Campaigns for Real Estate
###############################################################################
Write-Step "Creating marketing campaigns..."

$campaignData = @(
    @{ name='Spring Open House Weekend';             type='EVENT';     status='ACTIVE';  startDate='2026-03-21'; endDate='2026-03-22'; budget=5000;  expectedRevenue=500000;   description='Weekend open house events across 8 listings in LA San Diego and Miami. Social media promotion print flyers and Zillow featured placement.' },
    @{ name='Zillow Premier Agent Boost Q2';         type='PAID_ADS';  status='ACTIVE';  startDate='2026-04-01'; endDate='2026-06-30'; budget=15000; expectedRevenue=2000000;  description='Zillow Premier Agent advertising in LA Miami San Diego Austin and NYC zip codes. Target 200+ new leads per month.' },
    @{ name='Instagram Luxury Property Showcase';    type='SOCIAL';    status='ACTIVE';  startDate='2026-03-01'; endDate='2026-05-31'; budget=3000;  expectedRevenue=1500000;  description='Weekly Instagram Reels showcasing luxury listings 2M+. Professional video tours drone footage lifestyle content. Target 50K impressions per week.' },
    @{ name='First-Time Buyer Webinar Series';       type='WEBINAR';   status='PLANNED'; startDate='2026-04-15'; endDate='2026-06-15'; budget=2000;  expectedRevenue=800000;   description='Monthly webinar on How to Buy Your First Home in 2026. Covers pre-approval market timing negotiation tips. Lead magnet: free buyer guide PDF.' },
    @{ name='Sellers Market Report Email Drip';      type='EMAIL';     status='ACTIVE';  startDate='2026-03-01'; endDate='2026-12-31'; budget=1200;  expectedRevenue=3000000;  description='Bi-weekly email to homeowner database: neighborhood price trends days-on-market stats and What Is Your Home Worth CTA linked to valuation tool.' },
    @{ name='New Construction Cactus Ridge Launch';  type='EVENT';     status='PLANNED'; startDate='2026-04-10'; endDate='2026-04-12'; budget=8000;  expectedRevenue=5000000;  description='Grand opening event for Greenfield Homes Cactus Ridge community. Model home tours live music food trucks. Target 200+ attendees 50+ qualified leads.' },
    @{ name='Investor Networking Mixer';             type='EVENT';     status='PLANNED'; startDate='2026-05-08'; endDate='2026-05-08'; budget=4000;  expectedRevenue=10000000; description='Exclusive evening event for qualified investors. Presentations on ROI analysis 1031 exchanges and off-market deals. Cap at 50 attendees.' },
    @{ name='Facebook Google Retargeting Leads';     type='PAID_ADS';  status='ACTIVE';  startDate='2026-03-10'; endDate='2026-06-30'; budget=6000;  expectedRevenue=1200000;  description='Retarget website visitors and Zillow leads who did not convert. Display ads featuring their previously viewed properties with price update alerts.' }
)

foreach ($c in $campaignData) {
    $body = $c | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8093/api/v1/campaigns" -Body $body -Token $T
    if ($res) { Write-Ok "$($c.name) ($($c.type) - $($c.status))" }
    else { Write-Warn "Skipped campaign: $($c.name)" }
}

###############################################################################
# 11. EMAIL TEMPLATES - Real Estate Specific
###############################################################################
Write-Step "Creating email templates..."

$templates = @(
    @{
        name     = 'Post-Viewing Thank You'
        subject  = 'Thank you for touring the property!'
        category = 'SALES'
        bodyHtml = '<h2>Thank You for Visiting!</h2><p>Hi there,</p><p>It was great showing you the property today. I would love to hear your thoughts!</p><ul><li><strong>Love it?</strong> Let us discuss making an offer.</li><li><strong>Want to see more?</strong> I have similar properties that may interest you.</li><li><strong>Not quite right?</strong> Tell me what was missing and I will refine your search.</li></ul><p>The market moves fast. Do not hesitate to reach out!</p><p>Best regards,<br>Premier Realty Group</p>'
    },
    @{
        name     = 'New Listing Alert'
        subject  = 'New listing matching your search criteria!'
        category = 'SALES'
        bodyHtml = '<h2>A Property You Will Love Just Hit the Market!</h2><p>Based on your search criteria I think you will be excited about this new listing.</p><p>Want to schedule a showing? Reply to this email or call us.</p><p>Premier Realty Group</p>'
    },
    @{
        name     = 'Offer Submitted Confirmation'
        subject  = 'Your offer has been submitted!'
        category = 'SALES'
        bodyHtml = '<h2>Offer Submitted Successfully</h2><p>Great news. Your offer has been officially submitted to the seller agent.</p><h3>What Happens Next?</h3><ol><li>The seller will review and respond</li><li>They may accept counter or decline</li><li>We will call you immediately when we hear back</li></ol><p>Stay positive. Your offer is strong!</p>'
    },
    @{
        name     = 'Weekly Market Update'
        subject  = 'Your Weekly Real Estate Market Update'
        category = 'CUSTOMER_SUCCESS'
        bodyHtml = '<h2>Real Estate Market Update</h2><p>Here are this week''s key market numbers for your area.</p><p>New listings median prices average days on market and price trends are all covered.</p><p>Contact us if you want to discuss how these trends affect your buying or selling plans.</p><p>Premier Realty Group</p>'
    },
    @{
        name     = 'Closing Congratulations'
        subject  = 'Congratulations on your new home!'
        category = 'CUSTOMER_SUCCESS'
        bodyHtml = '<h2>Congratulations!</h2><p>It is official. Your new home is now yours!</p><p>It has been a pleasure guiding you through this journey.</p><h3>One Small Favor?</h3><p>If you had a great experience a review on Zillow or Google helps us help more buyers like you!</p><p>And remember if you know anyone looking to buy or sell we offer a referral bonus.</p><p>Enjoy your new home!</p><p>Premier Realty Group</p>'
    }
)

foreach ($t in $templates) {
    $body = $t | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8090/api/v1/email/templates" -Body $body -Token $T
    if ($res) { Write-Ok "$($t.name)" }
    else { Write-Warn "Skipped template: $($t.name)" }
}

###############################################################################
# 12. NOTES - Deal Notes and Account Notes
###############################################################################
Write-Step "Adding notes to deals and accounts..."

$oppNotes = @(
    @{ oppIdx=0; content='3/12: Buyer toured 456 Palm Dr. Loved the pool and home office. Concerned about street noise on south side. Suggested 2.75M offer. Seller asking 2.95M. Will discuss counter-offer strategy with buyer tomorrow.' },
    @{ oppIdx=1; content='3/10: Jennifer visited Skyline Tower sales center. Impressed by amenity deck and ocean views from Unit 42A. Questions about parking (2 spots included) monthly HOA (850) and completion date (Q4 2026). She is comparing with One Park Tower.' },
    @{ oppIdx=2; content='3/8: Met with Robert Henderson to review 5-property portfolio. Properties 1-3 are in good condition cash-flow positive. Number 4 needs roof replacement about 15K. Number 5 has existing tenant lease through Dec 2026. Investor wants 6 percent cap rate minimum.' },
    @{ oppIdx=5; content='3/11: Counter-offer round 2 with Amanda attorney. She offered 6.2M we countered at 6.5M. Main sticking point: seller wants 60-day rent-back after closing. Amanda 1031 QI says feasible if structured correctly. Next call Thursday.' },
    @{ oppIdx=8; content='3/1: CLOSED! Tustin Ranch flip acquired at 650K. Rehab contractor starting 3/15. Scope: kitchen gut-reno bathrooms x2 new flooring throughout exterior paint landscaping. Budget 120K. Target list date June 1 at 920K.' },
    @{ oppIdx=9; content='3/7: Private showing for tech CEO and spouse. Very interested but want to see the guest house renovated before committing. Seller agrees to 200K renovation credit instead. NDA signed. Follow-up showing scheduled for 3/20.' }
)

foreach ($n in $oppNotes) {
    if ($oppIds[$n.oppIdx] -eq 'skip') { continue }
    $body = @{ content = $n.content } | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8085/api/v1/opportunities/$($oppIds[$n.oppIdx])/notes" -Body $body -Token $T
    if ($res) { Write-Ok "Note added to deal $($n.oppIdx + 1)" }
}

$accountNotes = @(
    @{ accIdx=0; content='Premier Realty Group is our flagship brokerage account. 45 agents handles residential sales across LA metro. Top producer: Rachel Morgan 42M GCI in 2025. Key differentiator: luxury market expertise and celebrity clientele. Renewal date December 2026.' },
    @{ accIdx=1; content='Skyline Development has 3 active condo projects in Miami. Our team handles pre-construction sales for Tower A completed and Tower B breaking ground Q2 2026. Strong relationship with VP of Sales Maria Santos. Commission structure: 3 percent on pre-construction 2.5 percent on resale.' },
    @{ accIdx=3; content='Greenfield Homes is evaluating our CRM for their 5 Phoenix-area communities. Currently using spreadsheets for lead tracking. Decision maker: COO Frank Greenfield. Opportunity to become their exclusive sales CRM. Pilot program proposed for Cactus Ridge community.' },
    @{ accIdx=7; content='Pacific Coast Mortgage is our preferred lending partner. They offer 0.25 percent rate discount for Premier Realty referrals. Key contact: VP Lending Amy Zhao. Average pre-approval turnaround 48 hours. Integration potential: auto-populate pre-approval status in CRM.' }
)

foreach ($n in $accountNotes) {
    if ($accountIds[$n.accIdx] -eq 'skip') { continue }
    $body = @{ content = $n.content } | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8083/api/v1/accounts/$($accountIds[$n.accIdx])/notes" -Body $body -Token $T
    if ($res) { Write-Ok "Note added to $($accountData[$n.accIdx].name)" }
}

###############################################################################
# 13. COMPETITORS - On Key Deals
###############################################################################
Write-Step "Adding competitors to key deals..."

$competitors = @(
    @{ oppIdx=0;  competitorName='Compass Beverly Hills';                strengths='Strong brand and large agent network and tech-forward platform';  weaknesses='Higher commission rates and impersonal service';         threatLevel='MEDIUM' },
    @{ oppIdx=0;  competitorName='Coldwell Banker Realty';               strengths='Established presence and extensive MLS access';                   weaknesses='Slower response times and less luxury market focus';     threatLevel='LOW' },
    @{ oppIdx=1;  competitorName='One Park Tower (competing property)';  strengths='Lower price point and earlier completion date';                   weaknesses='No direct ocean view and smaller units';                 threatLevel='HIGH' },
    @{ oppIdx=5;  competitorName='Douglas Elliman Manhattan';            strengths='Dominant commercial presence and deep NYC relationships';         weaknesses='Less flexible on commission and slower due diligence';   threatLevel='HIGH' },
    @{ oppIdx=9;  competitorName='The Agency Bel Air';                   strengths='Celebrity connections and Instagram marketing';                   weaknesses='Smaller team and less transaction volume';               threatLevel='MEDIUM' },
    @{ oppIdx=13; competitorName='Corcoran Group Brooklyn';              strengths='Brooklyn market dominance and established investor network';      weaknesses='Higher fees and less 1031 exchange expertise';           threatLevel='MEDIUM' }
)

foreach ($c in $competitors) {
    if ($oppIds[$c.oppIdx] -eq 'skip') { continue }
    $body = @{
        competitorName = $c.competitorName
        strengths      = $c.strengths
        weaknesses     = $c.weaknesses
        threatLevel    = $c.threatLevel
    } | ConvertTo-Json
    $res = Invoke-Api -Method POST -Uri "${BASE}:8085/api/v1/opportunities/$($oppIds[$c.oppIdx])/competitors" -Body $body -Token $T
    if ($res) { Write-Ok "$($c.competitorName) on deal $($c.oppIdx + 1)" }
}

###############################################################################
# SUMMARY
###############################################################################
Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  Real Estate CRM Demo Data - Seeded Successfully!" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host "   8 Accounts   (brokerages developers partners)" -ForegroundColor Green
Write-Host "  12 Contacts   (buyers sellers investors with preferences)" -ForegroundColor Green
Write-Host "  15 Leads      (Zillow Realtor.com social media referrals)" -ForegroundColor Green
Write-Host "  15 Deals      (across all pipeline stages)" -ForegroundColor Green
Write-Host "   9 Products   (property details as line items)" -ForegroundColor Green
Write-Host "  15 Activities  (viewings calls tasks)" -ForegroundColor Green
Write-Host "   6 Workflows  (auto follow-up lead routing etc)" -ForegroundColor Green
Write-Host "   6 Cases      (inspection disputes title issues)" -ForegroundColor Green
Write-Host "   8 Campaigns  (open houses Zillow ads webinars)" -ForegroundColor Green
Write-Host "   5 Templates  (post-viewing offers market updates)" -ForegroundColor Green
Write-Host "  10 Notes      (deal and account context)" -ForegroundColor Green
Write-Host "   6 Competitors (per-deal competitive intel)" -ForegroundColor Green
Write-Host "" -ForegroundColor Green
Write-Host "  Pipeline: Inquiry > Viewing > Offer > Negotiation > Closed" -ForegroundColor Green
Write-Host "" -ForegroundColor Green
Write-Host "  Login:  rachel.morgan@premierrealty.com / Demo@2026!" -ForegroundColor Green
Write-Host "  URL:    http://localhost:3000" -ForegroundColor Green
Write-Host "  Guide:  demo/REAL-ESTATE-DEMO-GUIDE.md" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
