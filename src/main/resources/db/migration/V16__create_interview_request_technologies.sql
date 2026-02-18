-- src/main/resources/db/migration/V16__create_interview_request_technologies.sql
CREATE TABLE IF NOT EXISTS interview_request_technologies (
    interview_request_id BIGINT NOT NULL,
    technology_id        BIGINT NOT NULL,
    PRIMARY KEY (interview_request_id, technology_id),
    CONSTRAINT fk_irt_interview_request
        FOREIGN KEY (interview_request_id)
        REFERENCES interview_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_irt_technology
        FOREIGN KEY (technology_id)
        REFERENCES technologies(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_irt_interview_request_id ON interview_request_technologies(interview_request_id);
CREATE INDEX IF NOT EXISTS idx_irt_technology_id        ON interview_request_technologies(technology_id);