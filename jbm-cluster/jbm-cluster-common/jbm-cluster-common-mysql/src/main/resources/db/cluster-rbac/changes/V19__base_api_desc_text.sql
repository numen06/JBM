--liquibase formatted sql

--changeset jbm:base-api-desc-text-mysql dbms:mysql
ALTER TABLE base_api MODIFY COLUMN api_desc TEXT;

--changeset jbm:base-api-desc-text-postgresql dbms:postgresql
ALTER TABLE base_api ALTER COLUMN api_desc TYPE TEXT;

--changeset jbm:base-api-desc-text-h2 dbms:h2
ALTER TABLE base_api ALTER COLUMN api_desc TEXT;
