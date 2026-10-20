CREATE TABLE IF NOT EXISTS recovery_checkins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recorded_on DATE NOT NULL,
    recorded_at DATETIME NOT NULL,
    sleep_hours DOUBLE,
    sleep_quality INTEGER,
    fatigue_level INTEGER,
    soreness_level INTEGER,
    soreness_areas VARCHAR(255),
    resting_heart_rate INTEGER,
    symptoms TEXT,
    limitations TEXT,
    notes TEXT,
    source VARCHAR(50) NOT NULL DEFAULT 'SELF_REPORT',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_recovery_checkins_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_recovery_checkins_user_date ON recovery_checkins (user_id, recorded_on, recorded_at);

CREATE TABLE IF NOT EXISTS recovery_assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    generated_at DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    classification VARCHAR(80),
    recommended_action VARCHAR(80),
    confidence VARCHAR(20),
    explanation TEXT,
    next_session_guidance TEXT,
    safety_guidance TEXT,
    safety_notice TEXT,
    recovery_suggestions TEXT,
    missing_information TEXT,
    supporting_evidence TEXT,
    uncertainties TEXT,
    safety_flags TEXT,
    indicators_json TEXT,
    context_analysis_json TEXT,
    assessment_json TEXT,
    recommendation_json TEXT,
    prompt_a_version VARCHAR(40),
    prompt_b_version VARCHAR(40),
    prompt_c_version VARCHAR(40),
    data_freshness_hours INTEGER,
    requires_professional_evaluation BOOLEAN NOT NULL DEFAULT FALSE,
    plan_change_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    provider_error TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_recovery_assessments_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_recovery_assessments_user_generated ON recovery_assessments (user_id, generated_at);
