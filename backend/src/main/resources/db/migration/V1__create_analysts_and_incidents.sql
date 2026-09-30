CREATE TABLE analysts (
                          id          BIGSERIAL PRIMARY KEY,
                          name        VARCHAR(100) NOT NULL,
                          email       VARCHAR(255) NOT NULL UNIQUE,
                          active      BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE incidents (
                           id           BIGSERIAL PRIMARY KEY,
                           title        VARCHAR(200) NOT NULL,
                           description  TEXT,
                           status       VARCHAR(20)  NOT NULL,
                           severity     VARCHAR(20),
                           assignee_id  BIGINT REFERENCES analysts (id),
                           created_at   TIMESTAMPTZ  NOT NULL,
                           updated_at   TIMESTAMPTZ  NOT NULL,
                           closed_at    TIMESTAMPTZ,
                           CONSTRAINT chk_incident_status
                               CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'CLOSED')),
                           CONSTRAINT chk_incident_severity
                               CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_incidents_status   ON incidents (status);
CREATE INDEX idx_incidents_assignee ON incidents (assignee_id);