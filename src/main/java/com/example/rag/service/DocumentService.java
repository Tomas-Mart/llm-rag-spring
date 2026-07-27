package com.example.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.repository.DocumentRepository;
import com.example.rag.util.TextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для управления документами в RAG системе.
 *
 * <p>Основные функции:
 * <ul>
 *   <li><b>Загрузка и обработка документов</b> - извлечение текста из различных форматов файлов
 *       (PDF, DOCX, TXT, изображения и др.)</li>
 *   <li><b>Векторизация</b> - разбивка текста на чанки и сохранение эмбеддингов в векторную базу данных</li>
 *   <li><b>CRUD операции</b> - создание, чтение, обновление и удаление документов</li>
 *   <li><b>Поиск и фильтрация</b> - поиск документов по имени файла, содержанию и другим параметрам</li>
 *   <li><b>Управление дубликатами</b> - проверка существования документов перед загрузкой</li>
 *   <li><b>Перезагрузка</b> - обновление существующих документов</li>
 * </ul>
 * </p>
 *
 * <p>Архитектурные особенности:
 * <ul>
 *   <li>Использует {@link VectorStore} для хранения и поиска эмбеддингов</li>
 *   <li>Использует {@link DocumentRepository} для хранения метаданных в реляционной БД</li>
 *   <li>Использует {@link OcrService} для распознавания текста из изображений</li>
 *   <li>Использует {@link TextExtractor} для извлечения текста из офисных документов</li>
 *   <li>Поддерживает транзакционность через {@link Transactional}</li>
 * </ul>
 * </p>
 *
 * <p>Поддерживаемые форматы файлов:
 * <ul>
 *   <li><b>Текстовые</b>: TXT, MD, CSV, JSON, XML, HTML, PROPERTIES</li>
 *   <li><b>Офисные</b>: PDF, DOC, DOCX, XLSX, PPTX, RTF, ODT, ODS, ODP</li>
 *   <li><b>Изображения</b>: PNG, JPG, JPEG, GIF, BMP, TIFF, WebP (через OCR)</li>
 *   <li><b>Бинарные</b>: ZIP, JAR, EXE, DLL, SO, DYLIB, BIN (ограниченное извлечение)</li>
 * </ul>
 * </p>
 *
 * <p>Пример использования:
 * <pre>{@code
 * // Загрузка документа
 * documentService.ingestDocument(file, "{\"category\": \"technical\"}");
 *
 * // Получение всех документов
 * List<DocumentEntity> documents = documentService.getAllDocuments();
 *
 * // Поиск по имени файла
 * Optional<DocumentEntity> doc = documentService.getDocumentByFileName("example.pdf");
 *
 * // Удаление документа
 * documentService.deleteDocument(1L);
 * }</pre>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentEntity
 * @see DocumentRepository
 * @see VectorStore
 * @see OcrService
 * @see TextExtractor
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    /**
     * Максимальный размер файла для загрузки (10 МБ).
     * <p>Файлы больше этого размера будут отклонены.</p>
     */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /**
     * Размер пакета для сохранения чанков в векторное хранилище.
     * <p>Для больших документов чанки сохраняются пакетами для оптимизации производительности.</p>
     */
    private static final int BATCH_SIZE = 50;

    /**
     * Максимальный размер бинарного файла для извлечения текста (100 КБ).
     * <p>Ограничивает объем данных, обрабатываемых при извлечении текста из бинарных файлов.</p>
     */
    private static final int MAX_BINARY_EXTRACT_SIZE = 100000;

    /**
     * Минимальная длина слова для извлечения из бинарных файлов.
     * <p>Слова короче этого значения игнорируются при извлечении текста.</p>
     */
    private static final int MIN_WORD_LENGTH = 3;

    // ============================================================
    // ЗАВИСИМОСТИ
    // ============================================================

    /**
     * Хранилище векторов для эмбеддингов документов.
     * <p>Отвечает за сохранение и поиск векторных представлений текстов.</p>
     */
    private final VectorStore vectorStore;

    /**
     * Репозиторий для работы с метаданными документов.
     * <p>Обеспечивает CRUD операции и поиск по метаданным.</p>
     */
    private final DocumentRepository documentRepository;

    /**
     * Сервис для OCR распознавания текста из изображений.
     * <p>Использует Tesseract для извлечения текста из графических файлов.</p>
     */
    private final OcrService ocrService;

    // ============================================================
    // ПУБЛИЧНЫЕ МЕТОДЫ - УПРАВЛЕНИЕ ДОКУМЕНТАМИ
    // ============================================================

    /**
     * Загружает и обрабатывает новый документ.
     *
     * <p>Процесс загрузки включает следующие шаги:
     * <ol>
     *   <li><b>Валидация</b> - проверка размера файла и формата</li>
     *   <li><b>Проверка дубликатов</b> - если документ с таким именем уже существует, загрузка отклоняется</li>
     *   <li><b>Извлечение текста</b> - в зависимости от типа файла используется Tika, OCR или бинарный метод</li>
     *   <li><b>Создание документа</b> - формирование объекта {@link Document} с метаданными</li>
     *   <li><b>Разбивка на чанки</b> - текст разделяется на фрагменты для эффективного векторного поиска</li>
     *   <li><b>Сохранение эмбеддингов</b> - чанки векторизуются и сохраняются в {@link VectorStore}</li>
     *   <li><b>Сохранение метаданных</b> - информация о документе сохраняется в реляционную БД</li>
     * </ol>
     * </p>
     *
     * <p><b>Особенности обработки форматов:</b>
     * <ul>
     *   <li>Изображения обрабатываются через {@link OcrService}</li>
     *   <li>Офисные и текстовые документы обрабатываются через {@link TextExtractor}</li>
     *   <li>Бинарные файлы обрабатываются через {@link #extractTextFromBinary(byte[])}</li>
     * </ul>
     * </p>
     *
     * @param file     загружаемый файл {@link MultipartFile}, не может быть {@code null}
     * @param metadata метаданные документа в формате JSON или произвольная строка,
     *                 может быть {@code null} или пустой
     * @throws DocumentIngestionException если:
     *                                    <ul>
     *                                      <li>Размер файла превышает {@link #MAX_FILE_SIZE}</li>
     *                                      <li>Документ с таким именем уже существует</li>
     *                                      <li>Файл пуст или содержит только бинарные данные</li>
     *                                      <li>Ошибка при чтении или обработке файла</li>
     *                                      <li>Ошибка при сохранении в векторное хранилище</li>
     *                                    </ul>
     * @throws IllegalArgumentException   если {@code file} равен {@code null}
     * @see #reIngestDocument(MultipartFile, String)
     * @see #documentExists(String)
     */
    @Transactional
    public void ingestDocument(MultipartFile file, String metadata) throws DocumentIngestionException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new DocumentIngestionException("Имя файла отсутствует");
        }

        log.info("📄 Загружаем документ: {}", fileName);
        log.info("📄 Тип файла: {}, Размер: {} байт", file.getContentType(), file.getSize());

        try {
            // 1. Проверка размера файла
            validateFileSize(file);

            // 2. Проверяем, существует ли уже такой документ
            checkDocumentExists(fileName);

            // 3. Читаем содержимое файла (с очисткой)
            String content = readFileContent(file);

            // 4. Проверяем, что содержимое не пустое
            validateContent(content, fileName);

            // 5. Создаём документ
            Document document = createDocument(content, fileName, metadata);

            // 6. Разбиваем на чанки
            List<Document> chunks = splitDocumentIntoChunks(document);

            // 7. Сохраняем эмбеддинги
            saveEmbeddings(chunks, fileName);

            // 8. Сохраняем метаданные
            saveDocumentMetadata(content, fileName, metadata);

            log.info("✅ Документ '{}' загружен успешно", fileName);

        } catch (IOException e) {
            log.error("❌ Ошибка чтения файла: {}", fileName, e);
            throw new DocumentIngestionException("Ошибка чтения файла: " + fileName, e);
        } catch (DocumentIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка при загрузке документа: {}", fileName, e);
            throw new DocumentIngestionException("Неожиданная ошибка при загрузке документа: " + fileName, e);
        }
    }

    /**
     * Принудительная перезагрузка документа.
     *
     * <p>Удаляет существующий документ по имени файла и загружает новый.
     * Полезно для обновления содержимого документа без ручного удаления.</p>
     *
     * <p>Процесс перезагрузки:
     * <ol>
     *   <li>Поиск документа по имени файла</li>
     *   <li>Удаление старого документа (включая связанные чанки и эмбеддинги)</li>
     *   <li>Загрузка нового документа с теми же метаданными</li>
     * </ol>
     * </p>
     *
     * @param file     новый файл для загрузки
     * @param metadata метаданные документа
     * @throws DocumentIngestionException если ошибка при загрузке
     * @see #ingestDocument(MultipartFile, String)
     * @see #deleteDocumentByFileName(String)
     */
    @Transactional
    public void reIngestDocument(MultipartFile file, String metadata) throws DocumentIngestionException {
        String fileName = file.getOriginalFilename();
        log.info("🔄 Перезагрузка документа: {}", fileName);

        // Удаляем старый документ
        boolean deleted = deleteDocumentByFileName(fileName);
        if (!deleted) {
            log.warn("⚠️ Старый документ '{}' не найден, загружаем новый", fileName);
        } else {
            log.info("🗑️ Старый документ '{}' удален", fileName);
        }

        // Загружаем новый
        ingestDocument(file, metadata);
        log.info("✅ Документ '{}' перезагружен успешно", fileName);
    }

    // ============================================================
    // ПУБЛИЧНЫЕ МЕТОДЫ - CRUD ОПЕРАЦИИ
    // ============================================================

    /**
     * Возвращает все документы.
     *
     * <p><b>Внимание:</b> Для больших объемов данных рекомендуется использовать
     * пагинацию через {@link org.springframework.data.domain.Pageable}.</p>
     *
     * @return список всех документов (может быть пустым)
     */
    @Transactional(readOnly = true)
    public List<DocumentEntity> getAllDocuments() {
        log.debug("📖 Получение всех документов");
        return documentRepository.findAll();
    }

    /**
     * Возвращает документ по ID.
     *
     * @param id ID документа (не может быть {@code null})
     * @return {@link Optional} с документом или пустой {@link Optional}, если документ не найден
     * @throws IllegalArgumentException если {@code id} равен {@code null}
     */
    @Transactional(readOnly = true)
    public Optional<DocumentEntity> getDocument(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        log.debug("📖 Получение документа по ID: {}", id);
        return documentRepository.findById(id);
    }

    /**
     * Возвращает документ по имени файла.
     *
     * @param fileName имя файла (чувствительно к регистру)
     * @return {@link Optional} с документом или пустой {@link Optional}, если документ не найден
     */
    @Transactional(readOnly = true)
    public Optional<DocumentEntity> getDocumentByFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return Optional.empty();
        }
        log.debug("📖 Получение документа по имени: {}", fileName);
        return documentRepository.findByFileName(fileName);
    }

    /**
     * Проверяет существование документа по имени файла.
     *
     * @param fileName имя файла
     * @return {@code true} если документ существует, {@code false} в противном случае
     */
    @Transactional(readOnly = true)
    public boolean documentExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        return documentRepository.findByFileName(fileName).isPresent();
    }

    /**
     * Удаляет документ по ID.
     *
     * <p>При удалении документа также удаляются:
     * <ul>
     *   <li>Связанные чанки в векторном хранилище</li>
     *   <li>Метаданные в реляционной базе данных</li>
     * </ul>
     * </p>
     *
     * @param id ID документа
     * @return {@code true} если документ успешно удален, {@code false} если документ не найден
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }

        log.info("🗑️ Удаление документа по ID: {}", id);

        if (!documentRepository.existsById(id)) {
            log.warn("⚠️ Документ с ID {} не найден", id);
            return false;
        }

        documentRepository.deleteById(id);
        log.info("✅ Документ с ID {} удален", id);
        return true;
    }

    /**
     * Удаляет документ по имени файла.
     *
     * @param fileName имя файла
     * @return {@code true} если документ успешно удален, {@code false} если документ не найден
     */
    @Transactional
    public boolean deleteDocumentByFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        log.info("🗑️ Удаление документа по имени: {}", fileName);

        Optional<DocumentEntity> doc = documentRepository.findByFileName(fileName);
        if (doc.isEmpty()) {
            log.warn("⚠️ Документ с именем '{}' не найден", fileName);
            return false;
        }

        documentRepository.deleteByFileName(fileName);
        log.info("✅ Документ '{}' удален", fileName);
        return true;
    }

    /**
     * Очищает все документы.
     *
     * <p><b>Внимание:</b> Этот метод предназначен только для тестовых целей.
     * В производственной среде использовать с осторожностью!</p>
     *
     * @throws IllegalStateException если метод вызывается не в тестовом профиле
     */
    @Transactional
    public void clearAllDocuments() {
        log.warn("🧹 Очистка всех документов (только для тестов)");

        // Проверяем, что мы в тестовом профиле
        String activeProfile = System.getProperty("spring.profiles.active");
        if (activeProfile == null || !activeProfile.contains("test")) {
            throw new IllegalStateException("clearAllDocuments() can only be used in test profile");
        }

        documentRepository.deleteAll();
        log.info("✅ Все документы очищены");
    }

    // ============================================================
    // ПРИВАТНЫЕ МЕТОДЫ - ВАЛИДАЦИЯ
    // ============================================================

    /**
     * Проверяет размер файла.
     *
     * @param file файл для проверки
     * @throws DocumentIngestionException если размер превышает {@link #MAX_FILE_SIZE}
     */
    private void validateFileSize(MultipartFile file) throws DocumentIngestionException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentIngestionException(
                    "Размер файла превышает " + MAX_FILE_SIZE / (1024 * 1024) + " МБ"
            );
        }
    }

    /**
     * Проверяет существование документа.
     *
     * @param fileName имя файла
     * @throws DocumentIngestionException если документ уже существует
     */
    private void checkDocumentExists(String fileName) throws DocumentIngestionException {
        Optional<DocumentEntity> existingDoc = documentRepository.findByFileName(fileName);
        if (existingDoc.isPresent()) {
            log.warn("⚠️ Документ '{}' уже существует в БД", fileName);
            throw new DocumentIngestionException("Документ '" + fileName + "' уже существует в БД");
        }
    }

    /**
     * Проверяет содержимое документа.
     *
     * @param content  содержимое документа
     * @param fileName имя файла
     * @throws DocumentIngestionException если содержимое пустое
     */
    private void validateContent(String content, String fileName) throws DocumentIngestionException {
        if (content == null || content.trim().isEmpty()) {
            throw new DocumentIngestionException("Файл пуст или содержит только бинарные данные: " + fileName);
        }
    }

    // ============================================================
    // ПРИВАТНЫЕ МЕТОДЫ - ИЗВЛЕЧЕНИЕ ТЕКСТА
    // ============================================================

    /**
     * Чтение содержимого файла с очисткой от проблемных символов.
     *
     * <p>Алгоритм выбора метода извлечения текста:</p>
     * <ol>
     *   <li>Если файл - изображение → {@link #extractTextFromImage(MultipartFile, String)} (OCR)</li>
     *   <li>Если файл - офисный или текстовый → {@link #extractTextWithTika(MultipartFile, byte[], String)}</li>
     *   <li>Если файл - бинарный → {@link #extractTextFromBinary(byte[])}</li>
     *   <li>Иначе → {@link #extractTextWithTika(MultipartFile, byte[], String)}</li>
     * </ol>
     *
     * @param file загружаемый файл
     * @return извлеченный текст
     * @throws IOException если ошибка чтения файла
     */
    private String readFileContent(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        if (bytes.length == 0) {
            return "";
        }

        String fileName = file.getOriginalFilename();

        // 1. ИЗОБРАЖЕНИЯ → OCR
        if (ocrService.isImageFile(file)) {
            return extractTextFromImage(file, fileName);
        }

        // 2. ОФИСНЫЕ ДОКУМЕНТЫ И ТЕКСТ → Tika
        if (fileName != null && isOfficeOrTextFile(fileName)) {
            return extractTextWithTika(file, bytes, fileName);
        }

        // 3. БИНАРНЫЕ ФАЙЛЫ → БИНАРНЫЙ МЕТОД
        if (fileName != null && isBinaryFile(fileName)) {
            log.warn("⚠️ Бинарный файл: {}, пытаемся извлечь текст", fileName);
            return extractTextFromBinary(bytes);
        }

        // 4. ДЛЯ ВСЕХ ОСТАЛЬНЫХ → Tika, затем бинарный
        return extractTextWithTika(file, bytes, fileName);
    }

    /**
     * Извлекает текст из изображения через OCR.
     *
     * @param file     загружаемый файл изображения
     * @param fileName имя файла
     * @return извлеченный текст
     * @throws IOException если ошибка OCR распознавания
     */
    private String extractTextFromImage(MultipartFile file, String fileName) throws IOException {
        log.info("🖼️ Извлечение текста из изображения {} через OCR", fileName);
        try {
            String text = ocrService.extractText(file);
            if (text != null && !text.isEmpty()) {
                return text;
            }
            throw new IOException("OCR не распознал текст в изображении: " + fileName);
        } catch (Exception e) {
            log.warn("⚠️ OCR не смог распознать текст из {}, пробуем другие методы", fileName);
            throw new IOException("Не удалось извлечь текст из изображения: " + fileName, e);
        }
    }

    /**
     * Извлекает текст через Tika.
     *
     * @param file     загружаемый файл
     * @param bytes    байты файла
     * @param fileName имя файла
     * @return извлеченный текст
     */
    private String extractTextWithTika(MultipartFile file, byte[] bytes, String fileName) {
        log.info("📄 Извлечение текста из {} через Tika", fileName);
        try {
            return TextExtractor.extractText(file);
        } catch (Exception e) {
            log.warn("⚠️ Tika не смог извлечь текст из {}, пробуем бинарный метод", fileName);
            return extractTextFromBinary(bytes);
        }
    }

    /**
     * Извлечение текста из бинарных файлов.
     *
     * <p>Ищет читаемые текстовые последовательности в байтовом массиве.
     * Используется как fallback, когда Tika не справляется.</p>
     *
     * @param bytes бинарные данные файла
     * @return извлеченный текст или сообщение об ошибке
     */
    private String extractTextFromBinary(byte[] bytes) {
        // Пытаемся найти читаемый текст в бинарном файле
        StringBuilder text = new StringBuilder();
        StringBuilder currentWord = new StringBuilder();

        int maxBytes = Math.min(bytes.length, MAX_BINARY_EXTRACT_SIZE);

        for (int i = 0; i < maxBytes; i++) {
            byte b = bytes[i];
            // Проверяем, является ли байт печатаемым ASCII символом или кириллицей в UTF-8
            if ((b >= 32 && b <= 126) || (b < 0 && b >= -64)) {
                currentWord.append((char) (b & 0xFF));
            } else {
                if (currentWord.length() > MIN_WORD_LENGTH) {
                    text.append(currentWord).append(' ');
                }
                currentWord.setLength(0);
            }
        }

        // Добавляем последнее слово
        if (currentWord.length() > MIN_WORD_LENGTH) {
            text.append(currentWord);
        }

        String result = text.toString().trim();
        if (result.isEmpty()) {
            return "Извлечение текста из бинарного файла не удалось. Пожалуйста, используйте текстовый формат.";
        }
        return result;
    }

    /**
     * Проверяет, является ли файл офисным или текстовым.
     *
     * @param fileName имя файла
     * @return {@code true} если файл является офисным или текстовым
     */
    private boolean isOfficeOrTextFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") ||
               lowerName.endsWith(".doc") || lowerName.endsWith(".xlsx") ||
               lowerName.endsWith(".xls") || lowerName.endsWith(".pptx") ||
               lowerName.endsWith(".ppt") || lowerName.endsWith(".rtf") ||
               lowerName.endsWith(".odt") || lowerName.endsWith(".ods") ||
               lowerName.endsWith(".odp") || lowerName.endsWith(".html") ||
               lowerName.endsWith(".htm") || lowerName.endsWith(".xml") ||
               lowerName.endsWith(".json") || lowerName.endsWith(".csv") ||
               lowerName.endsWith(".md") || lowerName.endsWith(".txt") ||
               lowerName.endsWith(".properties");
    }

    /**
     * Проверяет, является ли файл бинарным.
     *
     * @param fileName имя файла
     * @return {@code true} если файл является бинарным
     */
    private boolean isBinaryFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".zip") || lowerName.endsWith(".jar") ||
               lowerName.endsWith(".exe") || lowerName.endsWith(".dll") ||
               lowerName.endsWith(".so") || lowerName.endsWith(".dylib") ||
               lowerName.endsWith(".bin");
    }

    // ============================================================
    // ПРИВАТНЫЕ МЕТОДЫ - ОБРАБОТКА ТЕКСТА
    // ============================================================

    /**
     * Очистка и декодирование текста из байтов.
     *
     * <p>Удаляет нулевые байты и управляющие символы, декодирует в UTF-8.</p>
     *
     * @param bytes исходные байты файла
     * @return очищенный текст
     */
    private String cleanAndDecodeText(byte[] bytes) {
        // Удаляем нулевые байты и другие проблемные символы
        byte[] cleaned = new byte[bytes.length];
        int j = 0;
        for (byte b : bytes) {
            // Пропускаем нулевые байты и управляющие символы (кроме табуляции, перевода строки, возврата каретки)
            if (b != 0x00 && b != 0x1A && b != 0x1B && b != 0x1C && b != 0x1D && b != 0x1E && b != 0x1F) {
                cleaned[j++] = b;
            }
        }

        // Создаем строку в UTF-8, заменяя некорректные символы
        String text = new String(cleaned, 0, j, StandardCharsets.UTF_8)
                .replace('\uFFFD', ' ')  // REPLACEMENT CHARACTER
                .replace('\u0000', ' '); // нулевой байт

        // Удаляем избыточные пробелы
        return text.replaceAll("\\s+", " ").trim();
    }

    // ============================================================
    // ПРИВАТНЫЕ МЕТОДЫ - СОЗДАНИЕ И СОХРАНЕНИЕ
    // ============================================================

    /**
     * Создает объект {@link Document} с контентом и метаданными.
     *
     * @param content  текст документа
     * @param fileName имя файла
     * @param metadata метаданные документа
     * @return объект {@link Document}
     */
    private Document createDocument(String content, String fileName, String metadata) {
        return Document.builder()
                .text(content)
                .metadata("fileName", fileName)
                .metadata("metadata", metadata != null ? metadata : "")
                .metadata("uploadedAt", LocalDateTime.now(ZoneId.of("UTC")).toString())
                .build();
    }

    /**
     * Разбивает документ на чанки для эффективного векторного поиска.
     *
     * <p>Использует {@link TokenTextSplitter} для разделения текста на фрагменты.</p>
     *
     * @param document исходный документ
     * @return список чанков документа
     */
    private List<Document> splitDocumentIntoChunks(Document document) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(List.of(document));
        log.info("📦 Документ разбит на {} чанков", chunks.size());
        return chunks;
    }

    /**
     * Сохраняет эмбеддинги чанков в векторную базу данных.
     * Для больших документов использует пакетную обработку.
     *
     * @param chunks   список чанков документа
     * @param fileName имя файла (для логирования)
     * @throws DocumentIngestionException если ошибка при сохранении эмбеддингов
     */
    private void saveEmbeddings(List<Document> chunks, String fileName) throws DocumentIngestionException {
        try {
            if (chunks.isEmpty()) {
                log.warn("⚠️ Нет чанков для сохранения в документе: {}", fileName);
                return;
            }

            if (chunks.size() > BATCH_SIZE) {
                // Пакетная обработка для больших документов
                for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, chunks.size());
                    vectorStore.add(chunks.subList(i, end));
                    log.debug("✅ Сохранена партия {}-{} из {}", i, end, chunks.size());
                }
            } else {
                vectorStore.add(chunks);
            }
            log.debug("✅ Эмбеддинги сохранены в векторную БД ({} чанков)", chunks.size());
        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении эмбеддингов: {}", e.getMessage());
            throw new DocumentIngestionException(
                    "Не удалось сохранить эмбеддинги в векторную БД для файла: " + fileName, e
            );
        }
    }

    /**
     * Сохраняет метаданные документа в реляционную базу данных.
     *
     * @param content  текст документа
     * @param fileName имя файла
     * @param metadata метаданные документа
     */
    private void saveDocumentMetadata(String content, String fileName, String metadata) {
        DocumentEntity entity = DocumentEntity.builder()
                .content(content)
                .fileName(fileName)
                .metadata(metadata != null ? metadata : "")
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();
        documentRepository.save(entity);
        log.debug("✅ Метаданные сохранены в БД (ID: {})", entity.getId());
    }
}