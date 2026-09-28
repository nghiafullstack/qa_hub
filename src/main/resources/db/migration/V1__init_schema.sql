CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE projects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    git_repo_url    VARCHAR(500),
    description     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_projects_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE api_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT       NOT NULL,
    name            VARCHAR(255),
    token_hash      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at    TIMESTAMP    NULL,
    CONSTRAINT uq_api_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_api_tokens_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_runs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT       NOT NULL,
    base_url_tested     VARCHAR(500),
    git_commit_sha      VARCHAR(100),
    source              VARCHAR(50)  NOT NULL DEFAULT 'CI',
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_tests         INT          NOT NULL DEFAULT 0,
    total_failures      INT          NOT NULL DEFAULT 0,
    total_errors        INT          NOT NULL DEFAULT 0,
    total_skipped       INT          NOT NULL DEFAULT 0,
    raw_report_format   VARCHAR(30),
    started_at          TIMESTAMP    NULL,
    finished_at         TIMESTAMP    NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_runs_project FOREIGN KEY (project_id) REFERENCES projects (id),
    INDEX idx_test_runs_project_created (project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE test_cases (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id              BIGINT        NOT NULL,
    class_name          VARCHAR(500),
    method_name         VARCHAR(500),
    display_name        VARCHAR(1000),
    status              VARCHAR(20)   NOT NULL,
    duration_ms         BIGINT,
    failure_message     TEXT,
    stack_trace         LONGTEXT,
    business_flow_tag   VARCHAR(200),
    CONSTRAINT fk_test_cases_run FOREIGN KEY (run_id) REFERENCES test_runs (id),
    INDEX idx_test_cases_run (run_id),
    INDEX idx_test_cases_tag (business_flow_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE documents (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT        NOT NULL,
    source_type         VARCHAR(20)   NOT NULL,
    source_ref          VARCHAR(1000),
    title               VARCHAR(500),
    content_text        LONGTEXT,
    doc_format          VARCHAR(30),
    content_hash        VARCHAR(64),
    last_synced_at      TIMESTAMP     NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects (id),
    INDEX idx_documents_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE analysis_runs (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id              BIGINT      NOT NULL,
    test_run_id             BIGINT      NOT NULL,
    triggered_by_user_id    BIGINT      NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    model_used              VARCHAR(100),
    error_message           TEXT,
    started_at              TIMESTAMP   NULL,
    finished_at             TIMESTAMP   NULL,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analysis_runs_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_analysis_runs_test_run FOREIGN KEY (test_run_id) REFERENCES test_runs (id),
    CONSTRAINT fk_analysis_runs_user FOREIGN KEY (triggered_by_user_id) REFERENCES users (id),
    INDEX idx_analysis_runs_test_run (test_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE findings (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_run_id         BIGINT       NOT NULL,
    category                VARCHAR(30)  NOT NULL,
    severity                VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    title                   VARCHAR(500) NOT NULL,
    description             TEXT,
    evidence_test_case_id   BIGINT       NULL,
    evidence_document_id    BIGINT       NULL,
    ai_generated            BOOLEAN      NOT NULL DEFAULT FALSE,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_findings_analysis_run FOREIGN KEY (analysis_run_id) REFERENCES analysis_runs (id),
    CONSTRAINT fk_findings_test_case FOREIGN KEY (evidence_test_case_id) REFERENCES test_cases (id),
    CONSTRAINT fk_findings_document FOREIGN KEY (evidence_document_id) REFERENCES documents (id),
    INDEX idx_findings_analysis_run (analysis_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
