--liquibase formatted sql
--changeset jbm:org-v13-tables splitStatements:true

CREATE TABLE IF NOT EXISTS base_org (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    app_id              BIGINT,
    parent_id           BIGINT,
    level               INT,
    leaf_path           VARCHAR(512),
    org_name            VARCHAR(128) NOT NULL,
    org_type            VARCHAR(32),
    manager_id          BIGINT,
    source_id           VARCHAR(64),
    group_id            VARCHAR(64),
    leader_id           BIGINT,
    org_code            VARCHAR(64),
    number_of_accounts  INT,
    org_address         TEXT,
    status              INT          DEFAULT 1,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP
);

CREATE TABLE IF NOT EXISTS base_user_org (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    org_id              BIGINT       NOT NULL,
    expire_time         TIMESTAMP,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP
);

--changeset jbm:org-v13-base-app-org-id dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_app' AND column_name = 'org_id'

ALTER TABLE base_app ADD COLUMN org_id BIGINT;

--changeset jbm:org-v13-base-app-org-id-h2 dbms:h2 splitStatements:true

ALTER TABLE base_app ADD COLUMN IF NOT EXISTS org_id BIGINT;

--changeset jbm:org-v13-alter-base-org-leaf-path dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'leaf_path'
ALTER TABLE base_org ADD COLUMN leaf_path VARCHAR(512);

--changeset jbm:org-v13-alter-base-org-level dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'level'
ALTER TABLE base_org ADD COLUMN level INT;

--changeset jbm:org-v13-alter-base-org-group-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'group_id'
ALTER TABLE base_org ADD COLUMN group_id VARCHAR(64);

--changeset jbm:org-v13-alter-base-org-status dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'status'
ALTER TABLE base_org ADD COLUMN status INT DEFAULT 1;

--changeset jbm:org-v13-seed-default-org splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM base_org WHERE id = 1

INSERT INTO base_org (id, org_name, parent_id, group_id, level, leaf_path, status, create_time, update_time)
VALUES (1, '默认组织', NULL, '1', 1, '1', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

--changeset jbm:org-v13-backfill-app-org-id splitStatements:true

UPDATE base_app SET org_id = 1 WHERE org_id IS NULL;

--changeset jbm:org-v13-indexes dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'base_org' AND index_name = 'idx_base_org_parent_id'
CREATE INDEX idx_base_org_parent_id ON base_org (parent_id);
CREATE INDEX idx_base_org_group_id ON base_org (group_id);
CREATE INDEX idx_base_user_org_user_org ON base_user_org (user_id, org_id);

--changeset jbm:org-v13-indexes-h2 dbms:h2
CREATE INDEX IF NOT EXISTS idx_base_org_parent_id ON base_org (parent_id);
CREATE INDEX IF NOT EXISTS idx_base_org_group_id ON base_org (group_id);
CREATE INDEX IF NOT EXISTS idx_base_user_org_user_org ON base_user_org (user_id, org_id);
