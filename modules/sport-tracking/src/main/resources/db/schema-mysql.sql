-- Sélectionner la base sport_tracking avant d'exécuter ce script.
-- À appliquer uniquement à une base neuve ou vide.
CREATE TABLE goals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goal_type VARCHAR(32) NOT NULL,
    target_value DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(24) NOT NULL,
    start_date DATE NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    KEY idx_goals_user_id (user_id),
    CONSTRAINT chk_goals_user_id_positive CHECK (user_id > 0),
    CONSTRAINT chk_goals_target_value_positive CHECK (target_value > 0),
    CONSTRAINT chk_goals_dates CHECK (target_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE progress_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    measurement_date DATE NOT NULL,
    weight DECIMAL(6, 2),
    waist_circumference DECIMAL(6, 2),
    sleep_hours DECIMAL(4, 2),
    energy_level INT,
    PRIMARY KEY (id),
    KEY idx_progress_entries_user_date (user_id, measurement_date),
    CONSTRAINT chk_progress_user_id_positive CHECK (user_id > 0),
    CONSTRAINT chk_progress_weight CHECK (weight IS NULL OR (weight > 0 AND weight <= 1000)),
    CONSTRAINT chk_progress_waist CHECK (waist_circumference IS NULL OR (waist_circumference > 0 AND waist_circumference <= 1000)),
    CONSTRAINT chk_progress_sleep CHECK (sleep_hours IS NULL OR (sleep_hours >= 0 AND sleep_hours <= 24)),
    CONSTRAINT chk_progress_energy CHECK (energy_level IS NULL OR energy_level BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
