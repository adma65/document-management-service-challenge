--The script to initialize the schema was sourced from the Spring Batch Core dependency: org.springframework.batch.core.

CREATE SCHEMA document_schema;
SET SCHEMA 'document_schema';

CREATE TABLE document (
    id SERIAL PRIMARY KEY,
    user_name VARCHAR(255),
    document_name VARCHAR(255),
    tags TEXT[],
    minio_path VARCHAR(255),
    file_size BIGINT,
    file_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


