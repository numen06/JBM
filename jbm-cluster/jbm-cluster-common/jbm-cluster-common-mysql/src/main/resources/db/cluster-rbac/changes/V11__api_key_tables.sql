--liquibase formatted sql
--changeset jbm:api-key-v11-tables splitStatements:true

CREATE TABLE IF NOT EXISTS base_api_key (
    key_id            BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    developer_id      BIGINT       NOT NULL,
    biz_app_id        BIGINT,
    api_key           VARCHAR(50)  NOT NULL,
    secret_key        VARCHAR(256),
    public_key        LONGTEXT,
    private_key       LONGTEXT,
    key_name          VARCHAR(100),
    key_desc          VARCHAR(500),
    client_name       VARCHAR(100),
    scope_modules     VARCHAR(500),
    expire_time       TIMESTAMP,
    status            INT          DEFAULT 1,
    revoke_time       TIMESTAMP,
    last_used_time    TIMESTAMP,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_authority_apikey (
    id                BIGINT       NOT NULL PRIMARY KEY,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    key_id            BIGINT       NOT NULL,
    authority_id      BIGINT       NOT NULL,
    expire_time       TIMESTAMP,
    auth_status       INT          DEFAULT 1,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

--changeset jbm:api-key-v11-indexes dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'base_api_key' AND index_name = 'idx_base_api_key_api_key'
CREATE UNIQUE INDEX idx_base_api_key_api_key ON base_api_key (api_key);
CREATE INDEX idx_base_api_key_developer_id ON base_api_key (developer_id);
CREATE INDEX idx_base_api_key_biz_app_id ON base_api_key (biz_app_id);
CREATE INDEX idx_base_authority_apikey_key_id ON base_authority_apikey (key_id);

--changeset jbm:api-key-v11-indexes-h2 dbms:h2
CREATE UNIQUE INDEX IF NOT EXISTS idx_base_api_key_api_key ON base_api_key (api_key);
CREATE INDEX IF NOT EXISTS idx_base_api_key_developer_id ON base_api_key (developer_id);
CREATE INDEX IF NOT EXISTS idx_base_api_key_biz_app_id ON base_api_key (biz_app_id);
CREATE INDEX IF NOT EXISTS idx_base_authority_apikey_key_id ON base_authority_apikey (key_id);
