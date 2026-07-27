package com.example.rag.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.rag.entity.DocumentEntity;

/**
 * Репозиторий для работы с сущностями документов.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *   <li>Поиска документов по имени файла</li>
 *   <li>Проверки существования документа</li>
 *   <li>Удаления документа по имени файла</li>
 *   <li>Стандартные CRUD операции (наследуются от JpaRepository)</li>
 *   <li>Оптимизированные запросы для предотвращения N+1 проблемы</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentEntity
 * @since 1.0
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    // ==================== БАЗОВЫЕ МЕТОДЫ ====================

    /**
     * Находит документ по имени файла.
     *
     * @param fileName имя файла (чувствительно к регистру)
     * @return Optional с документом или пустой Optional, если документ не найден
     */
    Optional<DocumentEntity> findByFileName(String fileName);

    /**
     * Проверяет существование документа по имени файла.
     *
     * @param fileName имя файла (чувствительно к регистру)
     * @return true если документ существует, false в противном случае
     */
    boolean existsByFileName(String fileName);

    /**
     * Удаляет документ по имени файла.
     *
     * @param fileName имя файла (чувствительно к регистру)
     */
    void deleteByFileName(String fileName);

    // ==================== ОПТИМИЗИРОВАННЫЕ МЕТОДЫ ====================

    /**
     * Находит все документы с оптимизированной загрузкой.
     * Использует простой запрос без дополнительных связей.
     *
     * @return список документов
     */
    @Query("SELECT d FROM DocumentEntity d")
    List<DocumentEntity> findAllWithOptimized();

    /**
     * Находит документ по имени файла с оптимизированной загрузкой.
     *
     * @param fileName имя файла (чувствительно к регистру)
     * @return Optional с документом или пустой Optional
     */
    @Query("SELECT d FROM DocumentEntity d WHERE d.fileName = :fileName")
    Optional<DocumentEntity> findByFileNameWithOptimized(@Param("fileName") String fileName);

    /**
     * Находит все документы, отсортированные по имени файла.
     *
     * @return отсортированный список документов
     */
    @Query("SELECT d FROM DocumentEntity d ORDER BY d.fileName ASC")
    List<DocumentEntity> findAllOrderedByFileName();

    // ==================== МЕТОДЫ ДЛЯ ПРОВЕРКИ СУЩЕСТВОВАНИЯ ====================

    /**
     * Проверяет существование документа с определенным именем файла.
     * Использует COUNT запрос вместо загрузки всей сущности.
     *
     * @param fileName имя файла
     * @return true если документ существует
     */
    @Query("SELECT COUNT(d) > 0 FROM DocumentEntity d WHERE d.fileName = :fileName")
    boolean existsByFileNameOptimized(@Param("fileName") String fileName);

    // ==================== МЕТОДЫ ДЛЯ СТАТИСТИКИ ====================

    /**
     * Получает ID всех документов для массовых операций.
     * Легковесный запрос без загрузки полных сущностей.
     *
     * @return список ID документов
     */
    @Query("SELECT d.id FROM DocumentEntity d")
    List<Long> findAllIds();

    /**
     * Находит документы по списку ID.
     *
     * @param ids список ID документов
     * @return список документов
     */
    @Query("SELECT d FROM DocumentEntity d WHERE d.id IN :ids")
    List<DocumentEntity> findAllByIdInWithOptimized(@Param("ids") List<Long> ids);

    // ==================== МЕТОДЫ ДЛЯ ПОИСКА С ФИЛЬТРАЦИЕЙ ====================

    /**
     * Находит документы по содержанию (поиск по тексту).
     *
     * @param content часть текста для поиска
     * @return список документов
     */
    @Query("SELECT d FROM DocumentEntity d WHERE LOWER(d.content) LIKE LOWER(CONCAT('%', :content, '%'))")
    List<DocumentEntity> findByContentContainingIgnoreCase(@Param("content") String content);

    /**
     * Находит документы по содержанию с оптимизацией.
     *
     * @param content часть текста для поиска
     * @return список документов
     */
    @Query("SELECT d FROM DocumentEntity d WHERE LOWER(d.content) LIKE LOWER(CONCAT('%', :content, '%'))")
    List<DocumentEntity> findByContentContainingIgnoreCaseWithOptimized(@Param("content") String content);

    /**
     * Находит документы по имени файла, содержащему указанную подстроку.
     *
     * @param fileName часть имени файла
     * @return список документов
     */
    List<DocumentEntity> findByFileNameContainingIgnoreCase(String fileName);
}