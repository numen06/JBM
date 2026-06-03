--liquibase formatted sql
--changeset jbm:openapi-docs-v21-tables splitStatements:true

CREATE TABLE IF NOT EXISTS open_api_document (
    doc_id          BIGINT       NOT NULL PRIMARY KEY,
    service_id      VARCHAR(128) NOT NULL,
    title           VARCHAR(256),
    version         VARCHAR(64),
    source_url      VARCHAR(512),
    spec_version    VARCHAR(32),
    raw_spec        LONGTEXT,
    source_hash     VARCHAR(128),
    sync_status     VARCHAR(32),
    sync_message    VARCHAR(1024),
    sync_time       TIMESTAMP,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS open_api_operation (
    operation_id        BIGINT       NOT NULL PRIMARY KEY,
    doc_id              BIGINT,
    api_id              BIGINT,
    service_id          VARCHAR(128) NOT NULL,
    path                VARCHAR(512) NOT NULL,
    request_method      VARCHAR(16)  NOT NULL,
    tags                VARCHAR(512),
    summary             VARCHAR(512),
    description         LONGTEXT,
    operation_key       VARCHAR(768) NOT NULL,
    parameters_json     LONGTEXT,
    request_body_json   LONGTEXT,
    responses_json      LONGTEXT,
    schemas_json        LONGTEXT,
    security_json       LONGTEXT,
    examples_json       LONGTEXT,
    raw_operation_json  LONGTEXT,
    deprecated          INT          DEFAULT 0,
    is_open             INT          DEFAULT 0,
    is_auth             INT          DEFAULT 1,
    status              INT          DEFAULT 1,
    sync_state          VARCHAR(32),
    first_seen_time     TIMESTAMP,
    last_seen_time      TIMESTAMP,
    removed_time        TIMESTAMP,
    change_type         VARCHAR(32),
    source_hash         VARCHAR(128),
    sync_time           TIMESTAMP,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP
);

CREATE TABLE IF NOT EXISTS published_api_doc (
    published_id        BIGINT       NOT NULL PRIMARY KEY,
    doc_key             VARCHAR(128) NOT NULL,
    title               VARCHAR(256),
    version             VARCHAR(64),
    content_type        VARCHAR(64),
    published_spec      LONGTEXT,
    published_summary   LONGTEXT,
    source_hash         VARCHAR(128),
    publisher_user_id   BIGINT,
    published_at        TIMESTAMP,
    status              INT          DEFAULT 1,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP
);

--changeset jbm:openapi-docs-v21-indexes dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'open_api_document' AND index_name = 'idx_open_api_document_service_id'
CREATE INDEX idx_open_api_document_service_id ON open_api_document (service_id);
CREATE INDEX idx_open_api_document_source_hash ON open_api_document (source_hash);
CREATE INDEX idx_open_api_operation_service_path_method ON open_api_operation (service_id, path, request_method);
CREATE INDEX idx_open_api_operation_api_id ON open_api_operation (api_id);
CREATE UNIQUE INDEX idx_open_api_operation_operation_key ON open_api_operation (operation_key);
CREATE INDEX idx_open_api_operation_sync_state ON open_api_operation (sync_state);
CREATE UNIQUE INDEX idx_published_api_doc_doc_key ON published_api_doc (doc_key);
CREATE INDEX idx_published_api_doc_status ON published_api_doc (status);

--changeset jbm:openapi-docs-v21-indexes-h2 dbms:h2
CREATE INDEX IF NOT EXISTS idx_open_api_document_service_id ON open_api_document (service_id);
CREATE INDEX IF NOT EXISTS idx_open_api_document_source_hash ON open_api_document (source_hash);
CREATE INDEX IF NOT EXISTS idx_open_api_operation_service_path_method ON open_api_operation (service_id, path, request_method);
CREATE INDEX IF NOT EXISTS idx_open_api_operation_api_id ON open_api_operation (api_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_open_api_operation_operation_key ON open_api_operation (operation_key);
CREATE INDEX IF NOT EXISTS idx_open_api_operation_sync_state ON open_api_operation (sync_state);
CREATE UNIQUE INDEX IF NOT EXISTS idx_published_api_doc_doc_key ON published_api_doc (doc_key);
CREATE INDEX IF NOT EXISTS idx_published_api_doc_status ON published_api_doc (status);
