CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
        created_at DATETIME NOT NULL,
        updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    age INTEGER,
    gender VARCHAR(50),
    height_cm DOUBLE,
    weight_kg DOUBLE,
    fitness_level VARCHAR(100),
    training_experience VARCHAR(100),
    training_days_per_week INTEGER,
    session_duration_minutes INTEGER,
    training_location VARCHAR(255),
    equipment VARCHAR(255),
    main_goal VARCHAR(255),
    target_weight_kg DOUBLE,
    target_date DATE,
    bmi DOUBLE,
    bmr DOUBLE,
    tdee DOUBLE,
    calorie_target DOUBLE,
        created_at DATETIME NOT NULL,
        updated_at DATETIME NOT NULL,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS strava_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    access_token VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255),
    expires_at BIGINT,
    athlete_id VARCHAR(255),
    connected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_strava_account_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS strava_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    name VARCHAR(255),
        start_date DATETIME,
    duration_seconds BIGINT,
    distance_meters DOUBLE,
    average_speed DOUBLE,
    elevation_gain DOUBLE,
    calories DOUBLE,
    average_heart_rate DOUBLE,
        created_at DATETIME NOT NULL,
    CONSTRAINT fk_strava_activities_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_strava_activity UNIQUE (user_id, external_id)
);

CREATE TABLE IF NOT EXISTS workout_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    weekly_strategy TEXT,
    duration_days INTEGER NOT NULL,
        generated_at DATETIME NOT NULL,
    CONSTRAINT fk_workout_plans_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS workout_weeks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    week_number INTEGER NOT NULL,
    focus TEXT,
    CONSTRAINT fk_workout_weeks_plan FOREIGN KEY (plan_id) REFERENCES workout_plans(id)
);

CREATE TABLE IF NOT EXISTS workout_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    week_id BIGINT NOT NULL,
    day_number INTEGER NOT NULL,
    type VARCHAR(100) NOT NULL,
    notes TEXT,
    CONSTRAINT fk_workout_days_week FOREIGN KEY (week_id) REFERENCES workout_weeks(id)
);

CREATE TABLE IF NOT EXISTS workout_exercises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    sets INTEGER,
    reps INTEGER,
    rest_seconds INTEGER,
    load_kg INTEGER,
    duration VARCHAR(255),
    intensity VARCHAR(50),
    notes TEXT,
    CONSTRAINT fk_workout_exercises_day FOREIGN KEY (day_id) REFERENCES workout_days(id)
);

CREATE TABLE IF NOT EXISTS progress_measurements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    measured_on DATE NOT NULL,
    weight_kg DOUBLE,
    bmi DOUBLE,
    body_fat_percent DOUBLE,
    notes TEXT,
        created_at DATETIME NOT NULL,
    CONSTRAINT fk_progress_measurements_user FOREIGN KEY (user_id) REFERENCES users(id)
);
