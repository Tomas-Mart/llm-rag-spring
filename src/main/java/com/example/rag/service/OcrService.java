package com.example.rag.service;

import java.io.IOException;
import java.io.InputStream;
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
        // Путь к папке с языковыми данными Tesseract в контейнере
        this.tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata/");
        // Языки: русский + английский (можно добавить другие через пробел)
        this.tesseract.setLanguage("rus+eng");
        // Режим сегментации страницы: 1 = Automatic page segmentation with OSD
        this.tesseract.setPageSegMode(1);
        // Режим OCR движка: 1 = LSTM only (более точный)
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

            // 🔧 ИСПРАВЛЕНО: используем BufferedImage вместо InputStream
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(inputStream);

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
        if (contentType != null && contentType.startsWith("image/")) {
            return true;
        }

        return false;
    }

    /**
     * Проверяет, доступен ли Tesseract в системе.
     *
     * @return true если Tesseract доступен
     */
    public boolean isTesseractAvailable() {
        try {
            // Проверяем, что tesseract может выполнить простую команду
            Process process = Runtime.getRuntime().exec(new String[]{"tesseract", "--version"});
            return process.waitFor() == 0;
        } catch (Exception e) {
            log.warn("⚠️ Tesseract не доступен: {}", e.getMessage());
            return false;
        }
    }
}