ALTER TABLE measurement
    ADD COLUMN IF NOT EXISTS damage_min_height_cm FLOAT,
    ADD COLUMN IF NOT EXISTS damage_max_height_cm FLOAT,
    ADD COLUMN IF NOT EXISTS calibration_method VARCHAR(50);

UPDATE measurement
SET damage_min_height_cm = COALESCE(damage_min_height_cm, calculated_height_cm),
    damage_max_height_cm = COALESCE(damage_max_height_cm, calculated_height_cm),
    calibration_method = COALESCE(calibration_method, 'legacy-heuristic');

ALTER TABLE measurement
    ALTER COLUMN damage_min_height_cm SET NOT NULL,
    ALTER COLUMN damage_max_height_cm SET NOT NULL,
    ALTER COLUMN calibration_method SET NOT NULL;
