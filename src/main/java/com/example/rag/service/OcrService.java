package com.example.rag.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Сервис для распознавания текста с изображений с помощью Tesseract OCR.
 *
 * <p>Поддерживаемые форматы:
 * <ul>
 *   <li>PNG (.png)</li>
 *   <li>JPEG (.jpg, .jpeg)</li>
 *   <li>GIF (.gif)</li>
 *   <li>BMP (.bmp)</li>
 *   <li>TIFF (.tiff)</li>
 *   <li>WebP (.webp)</li>
 * </ul>
 * </p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class OcrService {

    /**
     * Экземпляр Tesseract OCR для распознавания текста.
     */
    private final ITesseract tesseract;

    /**
     * Конструктор, инициализирующий Tesseract OCR с настройками.
     */
    public OcrService() {
        this.tesseract = new Tesseract();

        // Пробуем разные пути к tessdata
        String[] possiblePaths = {
                "/usr/share/tesseract-ocr/5/tessdata/",
                "/usr/share/tesseract-ocr/4.00/tessdata/",
                "/usr/share/tesseract/tessdata/",
                System.getenv("TESSDATA_PREFIX")
        };

        for (String path : possiblePaths) {
            if (path != null && new File(path).exists()) {
                this.tesseract.setDatapath(path);
                log.info("✅ Tesseract использует путь: {}", path);
                break;
            }
        }

        this.tesseract.setLanguage("rus+eng");
        this.tesseract.setPageSegMode(1);
        this.tesseract.setOcrEngineMode(1);
        log.info("✅ Tesseract OCR инициализирован");
    }

    /**
     * Извлекает текст из изображения с помощью OCR.
     *
     * @param file загружаемый файл изображения
     * @return извлеченный и очищенный текст
     * @throws IOException если ошибка при обработке файла или распознавании
     */
    public String extractText(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            log.info("🔍 Распознавание текста из изображения: {}", file.getOriginalFilename());

            // ⭐ Используем var (Java 21)
            var image = ImageIO.read(inputStream);

            if (image == null) {
                log.warn("⚠️ Не удалось прочитать изображение: {}", file.getOriginalFilename());
                return "";
            }

            String text = tesseract.doOCR(image);
            String cleanedText = text.trim().replaceAll("\\s+", " ");

            if (cleanedText.isEmpty()) {
                log.warn("⚠️ Текст не распознан на изображении: {}", file.getOriginalFilename());
                return "";
            }

            log.info("✅ Текст распознан из {} ({} символов)",
                    file.getOriginalFilename(), cleanedText.length());
            return cleanedText;

        } catch (TesseractException e) {
            log.error("❌ Ошибка OCR распознавания для {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new IOException("Ошибка распознавания текста: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("❌ Ошибка чтения изображения: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Проверяет, является ли файл изображением для OCR.
     *
     * @param file загружаемый файл
     * @return true если файл является поддерживаемым изображением
     */
    public boolean isImageFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();

        // Проверка по расширению
        if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
            lowerName.endsWith(".bmp") || lowerName.endsWith(".tiff") ||
            lowerName.endsWith(".webp")) {
            return true;
        }

        // Проверка по MIME типу
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Проверяет, доступен ли Tesseract в системе.
     *
     * @return true если Tesseract доступен
     */
    public boolean isTesseractAvailable() {
        try {
            // ✅ ИСПРАВЛЕНО: Используем фиксированные пути вместо переменной PATH
            String[] possiblePaths = {
                    "/usr/bin/tesseract",
                    "/usr/local/bin/tesseract",
                    "/opt/homebrew/bin/tesseract",
                    "/usr/share/tesseract-ocr/tesseract"
            };

            for (String tesseractPath : possiblePaths) {
                File tesseractFile = new File(tesseractPath);
                if (tesseractFile.exists() && tesseractFile.canExecute()) {
                    Process process = Runtime.getRuntime().exec(new String[]{tesseractPath, "--version"});
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        log.info("✅ Tesseract найден по пути: {}", tesseractPath);
                        return true;
                    }
                }
            }

            log.warn("⚠️ Tesseract не найден в стандартных путях");
            return false;

        } catch (Exception e) {
            log.warn("⚠️ Ошибка проверки Tesseract: {}", e.getMessage());
            return false;
        }
    }
}