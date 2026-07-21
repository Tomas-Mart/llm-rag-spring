package com.example.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
 * Сервис для загрузки и обработки документов.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Извлечение текста из различных форматов файлов (PDF, DOCX, TXT и др.)</li>
 *   <li>Разбивка текста на чанки для векторного поиска</li>
 *   <li>Сохранение эмбеддингов в векторную базу данных</li>
 *   <li>Сохранение метаданных в реляционную базу данных</li>
 *   <li>Проверка на дубликаты документов</li>
 * </ul>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    /**
     * Хранилище векторов для эмбеддингов документов.
     */
    private final VectorStore vectorStore;

    /**
     * Репозиторий для работы с метаданными документов.
     */
    private final DocumentRepository documentRepository;

    /**
     * Сервис для OCR распознавания текста из изображений.
     */
    private final OcrService ocrService;  // ← ДОБАВИТЬ!

    /**
     * Загружает и обрабатывает документ.
     *
     * <p>Процесс загрузки включает:
     * <ol>
     *   <li>Проверку на дубликаты (если документ уже существует, загрузка пропускается)</li>
     *   <li>Извлечение текста из файла</li>
     *   <li>Создание документа с метаданными</li>
     *   <li>Разбивку на чанки</li>
     *   <li>Сохранение эмбеддингов в векторную БД</li>
     *   <li>Сохранение метаданных в реляционную БД</li>
     * </ol>
     * </p>
     *
     * @param file     загружаемый файл {@link MultipartFile}
     * @param metadata метаданные документа (произвольная строка)
     * @throws DocumentIngestionException если ошибка при загрузке или обработке документа
     */
    @Transactional
    public void ingestDocument(MultipartFile file, String metadata) throws DocumentIngestionException {
        String fileName = file.getOriginalFilename();
        log.info("📄 Загружаем документ: {}", fileName);
        log.info("📄 Тип файла: {}, Размер: {} байт", file.getContentType(), file.getSize());

        try {
            // 1. Проверяем, существует ли уже такой документ
            Optional<DocumentEntity> existingDoc = documentRepository.findByFileName(fileName);

            if (existingDoc.isPresent()) {
                log.warn("⚠️ Документ '{}' уже существует в БД. Пропускаем загрузку.", fileName);
                throw new DocumentIngestionException("Документ '" + fileName + "' уже существует в БД");
            }

            // 2. Читаем содержимое файла (с очисткой)
            String content = readFileContent(file);

            // 3. Проверяем, что содержимое не пустое
            if (content == null || content.trim().isEmpty()) {
                throw new DocumentIngestionException("Файл пуст или содержит только бинарные данные: " + fileName);
            }

            // 4. Создаём документ
            Document document = createDocument(content, fileName, metadata);

            // 5. Разбиваем на чанки
            List<Document> chunks = splitDocumentIntoChunks(document);

            // 6. Сохраняем эмбеддинги
            saveEmbeddings(chunks, fileName);

            // 7. Сохраняем метаданные
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
     * Чтение содержимого файла с очисткой от проблемных символов.
     *
     * <p>Поддерживаемые форматы:
     * <ul>
     *   <li>Текстовые: TXT, MD, CSV, JSON, XML, HTML, PROPERTIES</li>
     *   <li>Офисные: PDF, DOC, DOCX, XLSX, PPTX, RTF, ODT, ODS, ODP</li>
     *   <li>Изображения: PNG, JPG, JPEG, GIF, BMP, TIFF, WebP (через OCR)</li>
     *   <li>Бинарные: ZIP, JAR, EXE, DLL, SO, DYLIB, BIN (ограниченное извлечение)</li>
     * </ul>
     * </p>
     *
     * <p>Для изображений используется {@link OcrService} (Tesseract OCR).
     * Для офисных и текстовых форматов используется {@link TextExtractor} (Apache Tika).
     * Для бинарных файлов используется упрощенный метод {@link #extractTextFromBinary(byte[])}.</p>
     *
     * @param file загружаемый файл
     * @return извлеченный текст
     * @throws IOException если ошибка чтения файла
     */
    private String readFileContent(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        // Если файл пустой
        if (bytes.length == 0) {
            return "";
        }

        String fileName = file.getOriginalFilename();

        // =====================================================
        // 1. ИЗОБРАЖЕНИЯ → OCR
        // =====================================================
        if (ocrService.isImageFile(file)) {
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

        // Проверяем по расширению
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();

            // =====================================================
            // 2. ОФИСНЫЕ ДОКУМЕНТЫ И ТЕКСТ → Tika
            // =====================================================
            if (lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") ||
                lowerName.endsWith(".doc") || lowerName.endsWith(".xlsx") ||
                lowerName.endsWith(".xls") || lowerName.endsWith(".pptx") ||
                lowerName.endsWith(".ppt") || lowerName.endsWith(".rtf") ||
                lowerName.endsWith(".odt") || lowerName.endsWith(".ods") ||
                lowerName.endsWith(".odp") || lowerName.endsWith(".html") ||
                lowerName.endsWith(".htm") || lowerName.endsWith(".xml") ||
                lowerName.endsWith(".json") || lowerName.endsWith(".csv") ||
                lowerName.endsWith(".md") || lowerName.endsWith(".txt") ||
                lowerName.endsWith(".properties")) {

                log.info("📄 Извлечение текста из {} через Tika", fileName);
                try {
                    return TextExtractor.extractText(file);
                } catch (Exception e) {
                    log.warn("⚠️ Tika не смог извлечь текст из {}, пробуем бинарный метод", fileName);
                    return extractTextFromBinary(bytes);
                }
            }

            // =====================================================
            // 3. БИНАРНЫЕ ФАЙЛЫ → БИНАРНЫЙ МЕТОД
            // =====================================================
            if (lowerName.endsWith(".zip") || lowerName.endsWith(".jar") ||
                lowerName.endsWith(".exe") || lowerName.endsWith(".dll") ||
                lowerName.endsWith(".so") || lowerName.endsWith(".dylib") ||
                lowerName.endsWith(".bin")) {
                log.warn("⚠️ Бинарный файл: {}, пытаемся извлечь текст", fileName);
                return extractTextFromBinary(bytes);
            }
        }

        // =====================================================
        // 4. ДЛЯ ВСЕХ ОСТАЛЬНЫХ → Tika, затем бинарный
        // =====================================================
        log.info("📄 Пробуем извлечь текст из {} через Tika", fileName);
        try {
            return TextExtractor.extractText(file);
        } catch (Exception e) {
            log.warn("⚠️ Tika не смог извлечь текст из {}, пробуем бинарный метод", fileName);
            return extractTextFromBinary(bytes);
        }
    }

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

        for (int i = 0; i < bytes.length && i < 100000; i++) { // Ограничиваем для производительности
            byte b = bytes[i];
            // Проверяем, является ли байт печатаемым ASCII символом или кириллицей в UTF-8
            if ((b >= 32 && b <= 126) || (b < 0 && b >= -64)) {
                currentWord.append((char) (b & 0xFF));
            } else {
                if (currentWord.length() > 3) { // Минимальная длина слова
                    text.append(currentWord).append(' ');
                }
                currentWord.setLength(0);
            }
        }

        // Добавляем последнее слово
        if (currentWord.length() > 3) {
            text.append(currentWord);
        }

        String result = text.toString().trim();
        if (result.isEmpty()) {
            return "Извлечение текста из бинарного файла не удалось. Пожалуйста, используйте текстовый формат.";
        }
        return result;
    }

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
                .text(content)                           // ← ИСПРАВЛЕНО: withContent → text
                .metadata("fileName", fileName)
                .metadata("metadata", metadata != null ? metadata : "")
                .metadata("uploadedAt", LocalDateTime.now().toString())
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
     *
     * @param chunks   список чанков документа
     * @param fileName имя файла (для логирования)
     * @throws DocumentIngestionException если ошибка при сохранении эмбеддингов
     */
    private void saveEmbeddings(List<Document> chunks, String fileName) throws DocumentIngestionException {
        try {
            vectorStore.add(chunks);
            log.debug("✅ Эмбеддинги сохранены в векторную БД");
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
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
        documentRepository.save(entity);
        log.debug("✅ Метаданные сохранены в БД");
    }

    /**
     * Принудительная перезагрузка документа.
     *
     * <p>Удаляет существующий документ по имени файла и загружает новый.</p>
     *
     * @param file     загружаемый файл
     * @param metadata метаданные документа
     * @throws DocumentIngestionException если ошибка при загрузке
     */
    @Transactional
    public void reIngestDocument(MultipartFile file, String metadata) throws DocumentIngestionException {
        String fileName = file.getOriginalFilename();

        // Удаляем старый документ
        documentRepository.deleteByFileName(fileName);
        log.info("🗑️ Старый документ '{}' удален", fileName);

        // Загружаем новый
        ingestDocument(file, metadata);
        log.info("🔄 Документ '{}' перезагружен", fileName);
    }

    /**
     * Проверяет, существует ли документ с указанным именем файла.
     *
     * @param fileName имя файла
     * @return {@code true} если документ существует, {@code false} в противном случае
     */
    public boolean documentExists(String fileName) {
        return documentRepository.findByFileName(fileName).isPresent();
    }
}