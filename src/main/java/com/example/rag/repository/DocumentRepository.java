package com.example.rag.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.rag.entity.DocumentEntity;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    /**
     * Находит документ по имени файла.
     *
     * @param fileName имя файла
     * @return Optional с документом или пустой Optional
     */
    Optional<DocumentEntity> findByFileName(String fileName);

    /**
     * Удаляет документ по имени файла.
     *
     * @param fileName имя файла
     */
    void deleteByFileName(String fileName);
}