package com.example.rag.nplusone;

import java.util.List;
import java.util.stream.Collectors;
import com.example.rag.dto.DocumentDto;
import com.example.rag.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Тестовый сервис для проверки N+1 проблемы в запросах к базе данных.
 *
 * <p>Этот сервис используется исключительно в тестовом окружении для:
 * <ul>
 *   <li>Обнаружения N+1 проблемы при выполнении запросов</li>
 *   <li>Тестирования производительности различных подходов к загрузке данных</li>
 *   <li>Сравнения оптимизированных и неоптимизированных запросов</li>
 *   <li>Валидации работы {@link DocumentRepository} с различными методами доступа</li>
 * </ul>
 * </p>
 *
 * <p><b>Внимание:</b> Этот класс не должен использоваться в производственном коде.
 * Он предназначен только для тестирования и анализа производительности.</p>
 *
 * <h2>Методы</h2>
 * <ul>
 *   <li>{@link #getAllDocuments()} - обычный запрос (может вызвать N+1)</li>
 *   <li>{@link #getAllDocumentsOptimized()} - оптимизированный запрос (предотвращает N+1)</li>
 *   <li>{@link #getDocumentByFileName(String)} - поиск по имени файла</li>
 *   <li>{@link #getDocumentById(Long)} - поиск по идентификатору</li>
 *   <li>{@link #getAllDocumentIds()} - получение только идентификаторов</li>
 *   <li>{@link #existsByFileName(String)} - проверка существования по имени</li>
 *   <li>{@link #existsById(Long)} - проверка существования по идентификатору</li>
 *   <li>{@link #countAllDocuments()} - получение количества документов</li>
 *   <li>{@link #getTopDocuments(int)} - получение первых N документов</li>
 *   <li>{@link #getDocumentsSortedByFileName()} - получение документов с сортировкой</li>
 *   <li>{@link #isEmpty()} - проверка пустоты базы данных</li>
 * </ul>
 *
 * <p><b>Использование в тестах:</b>
 * Этот сервис используется в {@code DocumentServiceNPlusOneTest} для проверки
 * производительности запросов и обнаружения N+1 проблемы.</p>
 *
 * @author Amina
 * @version 1.0
 * @see DocumentDto
 * @see DocumentRepository
 * @since 27.07.2026
 */
@Slf4j
public class DocumentServiceForTest {

    /**
     * Репозиторий для работы с документами.
     */
    private final DocumentRepository documentRepository;

    /**
     * Конструктор с внедрением зависимости.
     *
     * @param documentRepository репозиторий документов
     * @throws IllegalArgumentException если {@code documentRepository} равен {@code null}
     */
    public DocumentServiceForTest(DocumentRepository documentRepository) {
        if (documentRepository == null) {
            throw new IllegalArgumentException("DocumentRepository cannot be null");
        }
        this.documentRepository = documentRepository;
        log.debug("DocumentServiceForTest initialized successfully");
    }

    // ============================================================
    // ОСНОВНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Получает все документы в виде DTO.
     *
     * <p><b>Внимание:</b> Этот метод использует {@code findAll()} без оптимизации.
     * При наличии связанных сущностей может возникнуть N+1 проблема.</p>
     *
     * <p>Для оптимизированной версии используйте {@link #getAllDocumentsOptimized()}.</p>
     *
     * @return список DTO всех документов (может быть пустым)
     * @throws RuntimeException если ошибка при выполнении запроса
     */
    public List<DocumentDto> getAllDocuments() {
        log.debug("Fetching all documents without optimization");
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentDto> result = documentRepository.findAll()
                    .stream()
                    .map(DocumentDto::fromEntity)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Fetched {} documents in {} ms", result.size(), duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching all documents: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch documents", exception);
        }
    }

    /**
     * Получает все документы с оптимизированной загрузкой.
     *
     * <p>Использует {@code findAllWithOptimized()} для предотвращения N+1 проблемы.
     * Рекомендуется для использования в тестах производительности.</p>
     *
     * @return список DTO всех документов с оптимизированной загрузкой
     * @throws RuntimeException если ошибка при выполнении запроса
     */
    public List<DocumentDto> getAllDocumentsOptimized() {
        log.debug("Fetching all documents with optimization");
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentDto> result = documentRepository.findAllWithOptimized()
                    .stream()
                    .map(DocumentDto::fromEntity)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Fetched {} documents in {} ms with optimization", result.size(), duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching optimized documents: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch optimized documents", exception);
        }
    }

    /**
     * Получает документ по имени файла.
     *
     * <p>Использует {@code findByFileName()} для поиска конкретного документа.</p>
     *
     * @param fileName имя файла для поиска (не может быть {@code null} или пустым)
     * @return DTO документа или {@code null}, если документ не найден
     * @throws IllegalArgumentException если {@code fileName} равен {@code null} или пустой
     */
    public DocumentDto getDocumentByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        log.debug("Fetching document by file name: {}", fileName);
        long startTime = System.currentTimeMillis();

        try {
            DocumentDto result = documentRepository.findByFileName(fileName)
                    .map(DocumentDto::fromEntity)
                    .orElse(null);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Document found: {} in {} ms", result != null, duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching document by file name '{}': {}", fileName, exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch document by file name", exception);
        }
    }

    /**
     * Получает документ по идентификатору.
     *
     * <p>Использует {@code findById()} для поиска конкретного документа.</p>
     *
     * @param id идентификатор документа (не может быть {@code null})
     * @return DTO документа или {@code null}, если документ не найден
     * @throws IllegalArgumentException если {@code id} равен {@code null}
     */
    public DocumentDto getDocumentById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Document identifier cannot be null");
        }

        log.debug("Fetching document by identifier: {}", id);
        long startTime = System.currentTimeMillis();

        try {
            DocumentDto result = documentRepository.findById(id)
                    .map(DocumentDto::fromEntity)
                    .orElse(null);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Document found: {} in {} ms", result != null, duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching document by identifier '{}': {}", id, exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch document by identifier", exception);
        }
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Получает идентификаторы всех документов.
     *
     * <p>Легковесный запрос, возвращает только идентификаторы без загрузки полных сущностей.
     * Полезно для массовых операций, где нужны только идентификаторы.</p>
     *
     * @return список идентификаторов всех документов (может быть пустым)
     * @throws RuntimeException если ошибка при выполнении запроса
     */
    public List<Long> getAllDocumentIds() {
        log.debug("Fetching all document identifiers");
        long startTime = System.currentTimeMillis();

        try {
            List<Long> result = documentRepository.findAllIds();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Fetched {} identifiers in {} ms", result.size(), duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching document identifiers: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch document identifiers", exception);
        }
    }

    /**
     * Проверяет существование документа по имени файла.
     *
     * <p>Использует оптимизированный запрос с COUNT вместо загрузки всей сущности.</p>
     *
     * @param fileName имя файла для проверки (не может быть {@code null} или пустым)
     * @return {@code true} если документ существует, {@code false} в противном случае
     * @throws IllegalArgumentException если {@code fileName} равен {@code null} или пустой
     */
    public boolean existsByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        log.debug("Checking existence of document by file name: {}", fileName);
        long startTime = System.currentTimeMillis();

        try {
            boolean result = documentRepository.existsByFileNameOptimized(fileName);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Document exists: {} in {} ms", result, duration);

            return result;
        } catch (Exception exception) {
            log.error("Error checking document existence by file name '{}': {}", fileName, exception.getMessage(), exception);
            throw new RuntimeException("Failed to check document existence by file name", exception);
        }
    }

    /**
     * Проверяет существование документа по идентификатору.
     *
     * <p>Использует {@code existsById()} для проверки существования.</p>
     *
     * @param id идентификатор документа
     * @return {@code true} если документ существует, {@code false} в противном случае
     */
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }

        log.debug("Checking existence of document by identifier: {}", id);
        long startTime = System.currentTimeMillis();

        try {
            boolean result = documentRepository.existsById(id);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Document exists by identifier: {} in {} ms", result, duration);

            return result;
        } catch (Exception exception) {
            log.error("Error checking document existence by identifier '{}': {}", id, exception.getMessage(), exception);
            throw new RuntimeException("Failed to check document existence by identifier", exception);
        }
    }

    /**
     * Получает количество всех документов.
     *
     * <p>Использует {@code count()} для получения общего количества.</p>
     *
     * @return количество документов в базе данных
     */
    public long countAllDocuments() {
        log.debug("Counting all documents");
        long startTime = System.currentTimeMillis();

        try {
            long count = documentRepository.count();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Total documents: {} in {} ms", count, duration);

            return count;
        } catch (Exception exception) {
            log.error("Error counting documents: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to count documents", exception);
        }
    }

    /**
     * Получает первые N документов.
     *
     * <p>Полезно для тестирования с ограниченным набором данных.</p>
     *
     * @param limit максимальное количество документов (должно быть больше 0)
     * @return список DTO документов (не более {@code limit} штук)
     * @throws IllegalArgumentException если {@code limit} меньше 1
     */
    public List<DocumentDto> getTopDocuments(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        log.debug("Fetching top {} documents", limit);
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentDto> result = documentRepository.findAll()
                    .stream()
                    .limit(limit)
                    .map(DocumentDto::fromEntity)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Fetched {} documents in {} ms", result.size(), duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching top documents: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch top documents", exception);
        }
    }

    /**
     * Получает документы, отсортированные по имени файла в алфавитном порядке.
     *
     * <p>Использует {@code findAllOrderedByFileName()} для получения отсортированного списка.</p>
     *
     * @return список DTO документов, отсортированных по имени файла
     * @throws RuntimeException если ошибка при выполнении запроса
     */
    public List<DocumentDto> getDocumentsSortedByFileName() {
        log.debug("Fetching documents sorted by file name");
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentDto> result = documentRepository.findAllOrderedByFileName()
                    .stream()
                    .map(DocumentDto::fromEntity)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Fetched {} sorted documents in {} ms", result.size(), duration);

            return result;
        } catch (Exception exception) {
            log.error("Error fetching sorted documents: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to fetch sorted documents", exception);
        }
    }

    /**
     * Проверяет, пуста ли база данных документов.
     *
     * <p>Использует {@link #countAllDocuments()} для определения наличия документов.</p>
     *
     * @return {@code true} если документов нет, {@code false} если есть хотя бы один документ
     */
    public boolean isEmpty() {
        return countAllDocuments() == 0;
    }
}