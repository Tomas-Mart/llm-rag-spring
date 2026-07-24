-- ============================================
-- Инициализация PostgreSQL для интеграционных тестов
-- ============================================

-- Создаем расширение vector для pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Создаем таблицу для документов
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    metadata VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создаем таблицу для векторного хранилища
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding vector(768)
);

-- Создаем индексы для производительности
CREATE INDEX IF NOT EXISTS idx_documents_file_name ON documents(file_name);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at);

-- Создаем индекс для векторного поиска
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
ON vector_store
USING hnsw (embedding vector_cosine_ops);