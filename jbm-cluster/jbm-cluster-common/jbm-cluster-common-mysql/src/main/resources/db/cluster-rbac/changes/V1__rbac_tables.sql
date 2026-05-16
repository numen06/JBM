--liquibase formatted sql
--changeset jbm:rbac-v1-tables splitStatements:true

CREATE TABLE IF NOT EXISTS base_user (
    user_id           BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    user_name         VARCHAR(64)  NOT NULL,
    user_type         VARCHAR(32),
    company_id        BIGINT,
    department_id     BIGINT,
    nick_name         VARCHAR(128),
    real_name         VARCHAR(128),
    avatar            VARCHAR(512),
    email             VARCHAR(128),
    mobile            VARCHAR(32),
    sex               INT,
    user_desc         VARCHAR(512),
    status            INT          DEFAULT 1,
    close_time        TIMESTAMP,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_account (
    account_id        BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    user_id           BIGINT       NOT NULL,
    account           VARCHAR(128) NOT NULL,
    password          VARCHAR(256),
    account_type      VARCHAR(32),
    register_ip       VARCHAR(64),
    status            INT          DEFAULT 1,
    domain            VARCHAR(128),
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_role (
    role_id           BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    role_code         VARCHAR(64)  NOT NULL,
    role_name         VARCHAR(128),
    role_desc         VARCHAR(512),
    status            INT          DEFAULT 1,
    is_persist        INT          DEFAULT 0,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_role_user (
    id                BIGINT       NOT NULL PRIMARY KEY,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    user_id           BIGINT       NOT NULL,
    role_id           BIGINT       NOT NULL,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_menu (
    menu_id           BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    menu_code         VARCHAR(128),
    menu_name         VARCHAR(128),
    icon              VARCHAR(128),
    scheme            VARCHAR(32),
    path              VARCHAR(512),
    target            VARCHAR(32),
    priority          INT,
    menu_desc         VARCHAR(512),
    status            INT          DEFAULT 1,
    is_persist        BOOLEAN      DEFAULT FALSE,
    service_id        VARCHAR(128),
    hidden            INT          DEFAULT 1,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_action (
    action_id         BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    action_code       VARCHAR(128),
    action_name       VARCHAR(128),
    menu_id           BIGINT,
    priority          INT,
    action_desc       VARCHAR(512),
    status            INT          DEFAULT 1,
    is_persist        INT          DEFAULT 0,
    service_id        VARCHAR(128),
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_api (
    api_id            BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    api_code          VARCHAR(128),
    api_name          VARCHAR(128),
    service_id        VARCHAR(128),
    api_category      VARCHAR(64),
    path              VARCHAR(512),
    priority          INT,
    api_desc          VARCHAR(512),
    status            INT          DEFAULT 1,
    is_persist        BOOLEAN      DEFAULT FALSE,
    is_auth           BOOLEAN      DEFAULT TRUE,
    is_open           INT          DEFAULT 0,
    request_method    VARCHAR(16),
    content_type      VARCHAR(64),
    class_name        VARCHAR(256),
    method_name       VARCHAR(128),
    access_log        BOOLEAN,
    business_scope    VARCHAR(128),
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_authority (
    authority_id      BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    authority         VARCHAR(256),
    resource_type     VARCHAR(20),
    menu_id           BIGINT,
    api_id            BIGINT,
    action_id         BIGINT,
    status            INT          DEFAULT 1,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_authority_role (
    id                BIGINT       NOT NULL PRIMARY KEY,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    authority_id      BIGINT       NOT NULL,
    role_id           BIGINT       NOT NULL,
    expire_time       TIMESTAMP,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_authority_user (
    id                BIGINT       NOT NULL PRIMARY KEY,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    authority_id      BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    expire_time       TIMESTAMP,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_authority_app (
    id                BIGINT       NOT NULL PRIMARY KEY,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    authority_id      BIGINT       NOT NULL,
    expire_time       TIMESTAMP,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_authority_action (
    id                BIGINT       NOT NULL PRIMARY KEY,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    authority_id      BIGINT       NOT NULL,
    action_id         BIGINT       NOT NULL,
    expire_time       TIMESTAMP,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_developer (
    user_id           BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    app_id            BIGINT,
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    user_name         VARCHAR(64),
    user_type         VARCHAR(32),
    company_id        BIGINT,
    nick_name         VARCHAR(128),
    avatar            VARCHAR(512),
    email             VARCHAR(128),
    mobile            VARCHAR(32),
    user_desc         VARCHAR(512),
    status            INT          DEFAULT 1,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_app (
    app_id            BIGINT       NOT NULL PRIMARY KEY,
    id                BIGINT,
    code              VARCHAR(64),
    parent_id         BIGINT,
    level             INT,
    leaf_path         VARCHAR(512),
    api_key           VARCHAR(64)  NOT NULL,
    secret_key        VARCHAR(256),
    app_type          VARCHAR(32),
    app_icon          VARCHAR(512),
    app_icons         VARCHAR(1024),
    app_name          VARCHAR(128),
    app_name_en       VARCHAR(128),
    app_os            VARCHAR(32),
    developer_id      BIGINT,
    app_desc          VARCHAR(512),
    website           VARCHAR(256),
    status            INT          DEFAULT 1,
    is_persist        INT          DEFAULT 0,
    public_key        CLOB,
    private_key       CLOB,
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS api_key_index ON base_app (api_key);

CREATE TABLE IF NOT EXISTS jbm_system_init_marker (
    marker_key        VARCHAR(64)  NOT NULL PRIMARY KEY,
    initialized_at    TIMESTAMP    NOT NULL
);
