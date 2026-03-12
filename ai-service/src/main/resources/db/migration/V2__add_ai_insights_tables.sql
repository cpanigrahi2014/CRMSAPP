-- AI Insights: Win Probability, Forecasts, Churn, Report Insights, Suggestions, Sales Insights

-- Win probability predictions per opportunity
CREATE TABLE IF NOT EXISTS win_probability_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    opportunity_id VARCHAR(255) NOT NULL,
    opportunity_name VARCHAR(500),
    account_name VARCHAR(500),
    amount DECIMAL(15,2),
    stage VARCHAR(100),
    win_probability INTEGER NOT NULL DEFAULT 0,
    historical_win_rate INTEGER DEFAULT 0,
    days_in_stage INTEGER DEFAULT 0,
    risk_factors TEXT,
    positive_signals TEXT,
    recommendation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Sales forecasts
CREATE TABLE IF NOT EXISTS sales_forecast_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    period VARCHAR(50) NOT NULL,
    period_label VARCHAR(100),
    predicted_revenue DECIMAL(15,2),
    best_case DECIMAL(15,2),
    worst_case DECIMAL(15,2),
    confidence VARCHAR(20) DEFAULT 'MEDIUM',
    pipeline_value DECIMAL(15,2),
    weighted_pipeline DECIMAL(15,2),
    closed_to_date DECIMAL(15,2) DEFAULT 0,
    quota DECIMAL(15,2),
    attainment_pct INTEGER DEFAULT 0,
    factors TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Churn predictions per account
CREATE TABLE IF NOT EXISTS churn_prediction_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    account_name VARCHAR(500),
    industry VARCHAR(200),
    annual_revenue DECIMAL(15,2),
    risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    churn_probability DECIMAL(5,4) NOT NULL DEFAULT 0,
    risk_factors TEXT,
    last_activity_days INTEGER DEFAULT 0,
    health_score INTEGER DEFAULT 50,
    recommended_actions TEXT,
    predicted_churn_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- AI report insights
CREATE TABLE IF NOT EXISTS ai_report_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    report_name VARCHAR(200) NOT NULL,
    insight_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    metric VARCHAR(200),
    current_value DECIMAL(15,2),
    previous_value DECIMAL(15,2),
    change_pct DECIMAL(8,2),
    recommendation TEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Data entry suggestions
CREATE TABLE IF NOT EXISTS data_entry_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    entity_name VARCHAR(500),
    field VARCHAR(100) NOT NULL,
    current_value VARCHAR(500),
    suggested_value VARCHAR(500) NOT NULL,
    confidence DECIMAL(5,4) NOT NULL DEFAULT 0,
    source VARCHAR(200),
    accepted BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- AI sales insights
CREATE TABLE IF NOT EXISTS ai_sales_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    insight_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    details TEXT,
    impact_area VARCHAR(200),
    severity VARCHAR(20) DEFAULT 'medium',
    actionable BOOLEAN DEFAULT true,
    related_entities TEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_win_prob_tenant ON win_probability_records(tenant_id);
CREATE INDEX idx_win_prob_opp ON win_probability_records(opportunity_id, tenant_id);
CREATE INDEX idx_forecast_tenant ON sales_forecast_records(tenant_id);
CREATE INDEX idx_forecast_period ON sales_forecast_records(period, tenant_id);
CREATE INDEX idx_churn_tenant ON churn_prediction_records(tenant_id);
CREATE INDEX idx_churn_account ON churn_prediction_records(account_id, tenant_id);
CREATE INDEX idx_churn_risk ON churn_prediction_records(risk_level, tenant_id);
CREATE INDEX idx_report_insights_tenant ON ai_report_insights(tenant_id);
CREATE INDEX idx_suggestions_tenant ON data_entry_suggestions(tenant_id);
CREATE INDEX idx_suggestions_entity ON data_entry_suggestions(entity_type, entity_id, tenant_id);
CREATE INDEX idx_sales_insights_tenant ON ai_sales_insights(tenant_id);
CREATE INDEX idx_sales_insights_type ON ai_sales_insights(insight_type, tenant_id);
