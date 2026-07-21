-- Расширение для векторов
CREATE EXTENSION IF NOT EXISTS vector;

-- Таблица для хранения эмбеддингов
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding vector(768)
);

-- Индекс для быстрого поиска по косинусному сходству
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store
    USING hnsw (embedding vector_cosine_ops);

-- Таблица для хранения метаданных документов
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    content TEXT,
    file_name VARCHAR(255),
    metadata VARCHAR(255),
    created_at TIMESTAMP
);