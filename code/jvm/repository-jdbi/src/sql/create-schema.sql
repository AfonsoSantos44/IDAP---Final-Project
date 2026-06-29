
-- USERS
CREATE TABLE users (
                       user_id SERIAL PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin'))
);

-- ACCIDENT CASE
CREATE TABLE accident_case (
                               case_id SERIAL PRIMARY KEY,
                               user_id INT NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               case_status VARCHAR(50) NOT NULL DEFAULT 'open',
                               accident_description TEXT,
                               FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- TOKENS
CREATE TABLE tokens (
                        user_id INT NOT NULL,
                        token_hash VARCHAR(255) NOT NULL UNIQUE,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        expires_at TIMESTAMP NOT NULL,
                        revoked BOOLEAN NOT NULL DEFAULT FALSE,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- WEATHER
CREATE TABLE weather_conditions (
                                    weather_id SERIAL PRIMARY KEY,
                                    case_id INT NOT NULL UNIQUE,
                                    condition_type VARCHAR(255),
                                    temperature FLOAT,
                                    visibility FLOAT,
                                    precipitation VARCHAR(255),
                                    FOREIGN KEY (case_id) REFERENCES accident_case(case_id) ON DELETE CASCADE
);

-- SCENE
CREATE TABLE accident_scene (
                                scene_id SERIAL PRIMARY KEY,
                                case_id INT NOT NULL UNIQUE,
                                latitude DOUBLE PRECISION NOT NULL,
                                longitude DOUBLE PRECISION NOT NULL,
                                terrain_inclination FLOAT NOT NULL,
                                road_gradient FLOAT NOT NULL,
                                road_type VARCHAR(255) NOT NULL,
                                spatial_description TEXT NOT NULL,
                                vehicle_positioning_notes TEXT NOT NULL,
                                FOREIGN KEY (case_id) REFERENCES accident_case(case_id) ON DELETE CASCADE
);

-- VEHICLE
CREATE TABLE vehicle (
                         vehicle_id SERIAL PRIMARY KEY,
                         case_id INT NOT NULL,
                         brand VARCHAR(255) NOT NULL,
                         model VARCHAR(255) NOT NULL,
                         year_of_fabrication INT NOT NULL,
                         license_plate VARCHAR(50) NOT NULL,
                         role VARCHAR(50),
                         UNIQUE (case_id, license_plate),
                         FOREIGN KEY (case_id) REFERENCES accident_case(case_id) ON DELETE CASCADE
);

-- DAMAGE
CREATE TABLE damage (
                        damage_id SERIAL PRIMARY KEY,
                        vehicle_id INT NOT NULL,
                        contact_zone VARCHAR(255) NOT NULL,
                        deformation_type VARCHAR(255) NOT NULL,
                        direction VARCHAR(255) NOT NULL,
                        height_cm FLOAT,
                        damage_description TEXT NOT NULL,
                        FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE CASCADE
);

-- EVIDENCE
CREATE TABLE evidence (
                          evidence_id SERIAL PRIMARY KEY,
                          case_id INT NOT NULL,
                          evidence_type VARCHAR(50) NOT NULL,
                          evidence_description TEXT NOT NULL,
                          uploaded_by INT NOT NULL,
                          uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (case_id) REFERENCES accident_case(case_id) ON DELETE CASCADE,
                          FOREIGN KEY (uploaded_by) REFERENCES users(user_id) ON DELETE NO ACTION
);

-- IMAGE EVIDENCE
-- file_path holds the object-storage key (MinIO/S3), not the image bytes nor a local
-- filesystem path. The binary content lives in the object store; only the key is in Postgres.
CREATE TABLE image_evidence (
                                image_evidence_id SERIAL PRIMARY KEY,
                                evidence_id INT NOT NULL UNIQUE,
                                file_path VARCHAR(255) NOT NULL,
                                width INT NOT NULL,
                                height INT NOT NULL,
                                metadata TEXT,
                                FOREIGN KEY (evidence_id) REFERENCES evidence(evidence_id) ON DELETE CASCADE
);

-- ANALYSIS
CREATE TABLE analysis (
                          analysis_id SERIAL PRIMARY KEY,
                          case_id INT NOT NULL,
                          analyst_id INT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          FOREIGN KEY (case_id) REFERENCES accident_case(case_id) ON DELETE CASCADE,
                          FOREIGN KEY (analyst_id) REFERENCES users(user_id) ON DELETE NO ACTION
);

-- ANALYSIS IMAGE
CREATE TABLE analysis_image (
                                analysis_id INT NOT NULL,
                                evidence_id INT NOT NULL,
                                purpose VARCHAR(50),
                                PRIMARY KEY (analysis_id, evidence_id),
                                FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE,
                                FOREIGN KEY (evidence_id) REFERENCES evidence(evidence_id) ON DELETE CASCADE
);

-- MEASUREMENT
CREATE TABLE measurement (
                             measurement_id SERIAL PRIMARY KEY,
                             analysis_id INT NOT NULL,
                             evidence_id INT NOT NULL,
                             damage_id INT NOT NULL,

                             ref_obj_length_cm FLOAT NOT NULL,
                             ref_obj_x1 FLOAT NOT NULL,
                             ref_obj_y1 FLOAT NOT NULL,
                             ref_obj_x2 FLOAT NOT NULL,
                             ref_obj_y2 FLOAT NOT NULL,

                             damage_area_x1 FLOAT NOT NULL,
                             damage_area_y1 FLOAT NOT NULL,
                             damage_area_x2 FLOAT NOT NULL,
                             damage_area_y2 FLOAT NOT NULL,
                             calculated_height_cm FLOAT NOT NULL,
                             damage_min_height_cm FLOAT NOT NULL,
                             damage_max_height_cm FLOAT NOT NULL,
                             scale_cm_per_pixel FLOAT NOT NULL,
                             confidence FLOAT NOT NULL,
                             calibration_method VARCHAR(50) NOT NULL,
                             -- Object-storage key (MinIO/S3) of the generated annotated comparison image.
                             comparison_image_path VARCHAR(255),
                             processed_at TIMESTAMP NOT NULL DEFAULT NOW(),

                             FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE,
                             FOREIGN KEY (evidence_id) REFERENCES evidence(evidence_id) ON DELETE CASCADE,
                             FOREIGN KEY (damage_id) REFERENCES damage(damage_id) ON DELETE CASCADE
);

-- DAMAGE COMPARISON
CREATE TABLE damage_comparison (
                                   comparison_id SERIAL PRIMARY KEY,
                                   analysis_id INT NOT NULL,
                                   damage_source_id INT NOT NULL,
                                   damage_target_id INT NOT NULL,
                                   compatibility_status VARCHAR(50) NOT NULL,
                                   notes TEXT,
                                   FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE,
                                   FOREIGN KEY (damage_source_id) REFERENCES damage(damage_id) ON DELETE CASCADE,
                                   FOREIGN KEY (damage_target_id) REFERENCES damage(damage_id) ON DELETE CASCADE
);

-- ANALYSIS CONCLUSION
CREATE TABLE analysis_conclusion (
                                     conclusion_id SERIAL PRIMARY KEY,
                                     analysis_id INT NOT NULL UNIQUE,
                                     compatibility_result VARCHAR(50) NOT NULL,
                                     justification TEXT NOT NULL,
                                     FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE
);

-- REPORT
CREATE TABLE report (
                        report_id SERIAL PRIMARY KEY,
                        analysis_id INT NOT NULL,
                        generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        file_path VARCHAR(255) NOT NULL,
                        FOREIGN KEY (analysis_id) REFERENCES analysis(analysis_id) ON DELETE CASCADE
);
-- Create indexes for better query performance
CREATE INDEX idx_token_user ON tokens(user_id);
CREATE INDEX idx_case_user ON accident_case(user_id);
CREATE INDEX idx_vehicle_case ON vehicle(case_id);
CREATE INDEX idx_damage_vehicle ON damage(vehicle_id);
CREATE INDEX idx_evidence_case ON evidence(case_id);
CREATE INDEX idx_evidence_uploaded_by ON evidence(uploaded_by);
CREATE INDEX idx_analysis_case ON analysis(case_id);
CREATE INDEX idx_analysis_analyst ON analysis(analyst_id);
CREATE INDEX idx_measurement_analysis ON measurement(analysis_id);
CREATE INDEX idx_measurement_evidence ON measurement(evidence_id);
CREATE INDEX idx_measurement_damage ON measurement(damage_id);
CREATE INDEX idx_comparison_analysis ON damage_comparison(analysis_id);
CREATE INDEX idx_report_analysis ON report(analysis_id);
