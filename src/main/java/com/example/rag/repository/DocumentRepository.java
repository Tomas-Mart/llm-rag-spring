package com.example.rag.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentEntity
 * @since 1.0
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

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
}