ALTER TABLE incidents
    ADD COLUMN detection_confidence DOUBLE PRECISION,
    ADD COLUMN risk_score           INTEGER,
    ADD COLUMN severity_source      VARCHAR(10);

ALTER TABLE incidents
    ADD CONSTRAINT chk_incident_detection_confidence
        CHECK (detection_confidence BETWEEN 0 AND 1),
    ADD CONSTRAINT chk_incident_risk_score
        CHECK (risk_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_incident_severity_source
        CHECK (severity_source IN ('MANUAL', 'SCORER'));

-- Every severity that exists so far was entered by hand.
UPDATE incidents SET severity_source = 'MANUAL' WHERE severity IS NOT NULL;