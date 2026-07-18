-- Расширение для векторов
CREATE EXTENSION IF NOT EXISTS vector;

-- Таблица для хранения эмбеддингов (Spring AI создаст автоматически,
-- но мы создаём вручную для прозрачности)
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding vector(768)
);

-- Индекс для быстрого поиска по косинусному сходству
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store
    USING hnsw (embedding vector_cosine_ops);