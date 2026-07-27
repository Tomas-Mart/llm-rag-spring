package com.example.rag.dto;

import com.example.rag.entity.DocumentEntity;

/**
 * DTO (Data Transfer Object) для документа.
 *
 * <p>Представляет собой неизменяемый объект для передачи данных о документе
 * между слоями приложения. Использует Record для обеспечения иммутабельности
 * и автоматической генерации методов.</p>
 *
 * <h2>Преимущества использования Record</h2>
 * <ul>
 *   <li><b>Неизменяемость (immutable)</b> - все поля final, безопасность в многопоточной среде</li>
 *   <li><b>Автоматическая генерация</b> - конструктор, геттеры, equals(), hashCode(), toString()</li>
 *   <li><b>Минимальный код</b> - меньше шаблонного кода, меньше ошибок</li>
 *   <li><b>Идеально для DTO</b> - предназначен для передачи данных</li>
 * </ul>
 *
 * <h2>Преимущества использования DTO</h2>
 * <ul>
 *   <li><b>Контроль данных</b> - передаются только нужные поля, скрываются внутренние детали</li>
 *   <li><b>Изоляция</b> - изменения в сущности не влияют на API</li>
 *   <li><b>Оптимизация</b> - снижение нагрузки на сеть и память</li>
 *   <li><b>Безопасность</b> - не раскрываются внутренние структуры БД</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 * <pre>{@code
 * // Преобразование из сущности в DTO
 * DocumentDto dto = DocumentDto.fromEntity(documentEntity);
 *
 * // Создание DTO
 * DocumentDto dto = new DocumentDto(1L, "file.txt", "content", "{}");
 *
 * // Проверка валидности
 * if (dto.isValid()) {
 *     // Обработка валидного DTO
 * }
 *
 * // Создание копии с обновленным содержимым
 * DocumentDto updated = dto.withContent("New content");
 *
 * // Преобразование обратно в сущность
 * DocumentEntity entity = dto.toEntity();
 * }</pre>
 *
 * @author Amina
 * @version 1.0
 * @see DocumentEntity
 * @since 27.07.2026
 */
public record DocumentDto(
        /**
         * Уникальный идентификатор документа.
         * Может быть {@code null} для новых документов.
         */
        Long id,

        /**
         * Имя файла документа.
         * Не может быть {@code null} или пустым для валидного DTO.
         */
        String fileName,

        /**
         * Содержимое документа.
         * Не может быть {@code null} или пустым для валидного DTO.
         */
        String content,

        /**
         * Метаданные документа в формате JSON.
         * Может быть {@code null} или пустой строкой.
         * Пример: {@code {"author":"John","category":"documentation"}}
         */
        String metadata
) {

    /**
     * Преобразует сущность {@link DocumentEntity} в DTO.
     *
     * <p>Позволяет безопасно преобразовывать объекты из слоя данных
     * в объекты для передачи по сети.</p>
     *
     * @param entity сущность документа, может быть {@code null}
     * @return DTO документа или {@code null}, если сущность равна {@code null}
     */
    public static DocumentDto fromEntity(DocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DocumentDto(
                entity.getId(),
                entity.getFileName(),
                entity.getContent(),
                entity.getMetadata()
        );
    }

    /**
     * Преобразует DTO в сущность {@link DocumentEntity}.
     *
     * <p>Создает новую сущность на основе данных DTO.
     * Полезно при создании или обновлении документов в БД.</p>
     *
     * @return сущность документа с заполненными полями из DTO
     */
    public DocumentEntity toEntity() {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(this.id());
        entity.setFileName(this.fileName());
        entity.setContent(this.content());
        entity.setMetadata(this.metadata());
        return entity;
    }

    /**
     * Проверяет валидность DTO.
     *
     * <p>DTO считается валидным, если:
     * <ul>
     *   <li>Имя файла не {@code null} и не пустое</li>
     *   <li>Содержимое не {@code null} и не пустое</li>
     * </ul>
     * </p>
     *
     * @return {@code true} если DTO валиден, {@code false} в противном случае
     */
    public boolean isValid() {
        return fileName != null && !fileName.isBlank()
               && content != null && !content.isBlank();
    }

    /**
     * Создает копию DTO с обновленным содержимым.
     *
     * <p>Используется для создания новых версий DTO без изменения оригинала.
     * Сохраняет все остальные поля (id, fileName, metadata).</p>
     *
     * @param newContent новое содержимое документа
     * @return новый DTO с обновленным содержимым
     * @throws IllegalArgumentException если {@code newContent} равен {@code null}
     */
    public DocumentDto withContent(String newContent) {
        if (newContent == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        return new DocumentDto(this.id(), this.fileName(), newContent, this.metadata());
    }

    /**
     * Создает копию DTO с обновленными метаданными.
     *
     * <p>Используется для создания новых версий DTO без изменения оригинала.
     * Сохраняет все остальные поля (id, fileName, content).</p>
     *
     * @param newMetadata новые метаданные в формате JSON
     * @return новый DTO с обновленными метаданными
     */
    public DocumentDto withMetadata(String newMetadata) {
        return new DocumentDto(this.id(), this.fileName(), this.content(), newMetadata);
    }

    /**
     * Возвращает краткое строковое представление DTO.
     *
     * <p>В отличие от стандартного {@code toString()}, этот метод
     * не включает полное содержимое документа, что полезно для логирования.</p>
     *
     * <p>Формат вывода:
     * {@code DocumentDto{id=1, fileName='file.txt', contentLength=1024, metadata='{"author":"John"}'}}</p>
     *
     * @return краткое представление DTO
     */
    public String toShortString() {
        return String.format("DocumentDto{id=%d, fileName='%s', contentLength=%d, metadata='%s'}",
                id(), fileName(), content() != null ? content().length() : 0, metadata());
    }

    /**
     * Возвращает DTO без содержимого (только метаданные).
     *
     * <p>Полезно для операций, где нужно только метаданные без полного текста.</p>
     *
     * @return новый DTO с тем же id, fileName, metadata, но с пустым содержимым
     */
    public DocumentDto withoutContent() {
        return new DocumentDto(this.id(), this.fileName(), "", this.metadata());
    }

    /**
     * Проверяет, содержит ли DTO метаданные.
     *
     * @return {@code true} если метаданные не {@code null} и не пустые
     */
    public boolean hasMetadata() {
        return metadata != null && !metadata.isBlank();
    }

    /**
     * Проверяет, содержит ли DTO содержимое.
     *
     * @return {@code true} если содержимое не {@code null} и не пустое
     */
    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    /**
     * Возвращает размер содержимого в символах.
     *
     * @return размер содержимого или 0, если содержимое {@code null}
     */
    public int contentLength() {
        return content != null ? content.length() : 0;
    }

    /**
     * Создает новый DTO с указанным ID (для обновления).
     *
     * @param newId новый ID документа
     * @return новый DTO с обновленным ID
     */
    public DocumentDto withId(Long newId) {
        return new DocumentDto(newId, this.fileName(), this.content(), this.metadata());
    }

    /**
     * Проверяет, является ли документ новым (без ID).
     *
     * @return {@code true} если ID равен {@code null}
     */
    public boolean isNew() {
        return id == null;
    }
}