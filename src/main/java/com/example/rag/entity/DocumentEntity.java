package com.example.rag.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность для хранения документов в базе данных.
 * <p>
 * Представляет собой документ, загруженный пользователем,
 * содержащий текст, метаданные и информацию о создании.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documents")
public class DocumentEntity {

    /**
     * Уникальный идентификатор документа.
     * Автоматически генерируется при сохранении.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Имя файла.
     * Должно быть уникальным для предотвращения дубликатов.
     */
    @Column(nullable = false, unique = true)
    private String fileName;

    /**
     * Содержимое документа в текстовом формате.
     * Использует тип TEXT для поддержки больших документов.
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Метаданные документа (например, категория, теги).
     * Опциональное поле.
     */
    private String metadata;

    /**
     * Дата и время создания документа.
     * Автоматически устанавливается при сохранении.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}