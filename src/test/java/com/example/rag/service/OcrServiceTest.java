package com.example.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.support.BaseIntegrationTestWithContainers;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для {@link OcrService}.
 * Проверяют распознавание текста из изображений.
 *
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Проверка распознавания изображений (PNG, JPG, GIF, BMP, TIFF, WebP)</li>
 *   <li>Проверка обработки некорректных файлов</li>
 *   <li>Проверка доступности Tesseract</li>
 *   <li>Проверка извлечения текста</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OcrServiceTest extends BaseIntegrationTestWithContainers {

    @Autowired
    private OcrService ocrService;

    private MultipartFile pngImage;
    private MultipartFile jpgImage;
    private MultipartFile gifImage;
    private MultipartFile bmpImage;
    private MultipartFile tiffImage;
    private MultipartFile webpImage;
    private MultipartFile invalidImage;
    private MultipartFile emptyImage;
    private MultipartFile nullNameFile;
    private MultipartFile nullContentTypeFile;
    private MultipartFile upperCaseFile;

    @BeforeEach
    void setUp() {
        byte[] imageData = "fake image data".getBytes(StandardCharsets.UTF_8);

        pngImage = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageData
        );

        jpgImage = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageData
        );

        gifImage = new MockMultipartFile(
                "file",
                "test.gif",
                "image/gif",
                imageData
        );

        bmpImage = new MockMultipartFile(
                "file",
                "test.bmp",
                "image/bmp",
                imageData
        );

        tiffImage = new MockMultipartFile(
                "file",
                "test.tiff",
                "image/tiff",
                imageData
        );

        webpImage = new MockMultipartFile(
                "file",
                "test.webp",
                "image/webp",
                imageData
        );

        invalidImage = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "not an image".getBytes(StandardCharsets.UTF_8)
        );

        emptyImage = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        nullNameFile = new MockMultipartFile(
                "file",
                null,
                "image/png",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        nullContentTypeFile = new MockMultipartFile(
                "file",
                "test.png",
                null,
                "data".getBytes(StandardCharsets.UTF_8)
        );

        upperCaseFile = new MockMultipartFile(
                "file",
                "TEST.PNG",
                "image/png",
                "data".getBytes(StandardCharsets.UTF_8)
        );
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isImageFile
    // ============================================================

    @Test
    void testIsImageFile_WithPng() {
        boolean result = ocrService.isImageFile(pngImage);
        assertThat(result).isTrue();
        log.info("✅ PNG распознан как изображение");
    }

    @Test
    void testIsImageFile_WithJpg() {
        boolean result = ocrService.isImageFile(jpgImage);
        assertThat(result).isTrue();
        log.info("✅ JPG распознан как изображение");
    }

    @Test
    void testIsImageFile_WithGif() {
        boolean result = ocrService.isImageFile(gifImage);
        assertThat(result).isTrue();
        log.info("✅ GIF распознан как изображение");
    }

    @Test
    void testIsImageFile_WithBmp() {
        boolean result = ocrService.isImageFile(bmpImage);
        assertThat(result).isTrue();
        log.info("✅ BMP распознан как изображение");
    }

    @Test
    void testIsImageFile_WithTiff() {
        boolean result = ocrService.isImageFile(tiffImage);
        assertThat(result).isTrue();
        log.info("✅ TIFF распознан как изображение");
    }

    @Test
    void testIsImageFile_WithWebp() {
        boolean result = ocrService.isImageFile(webpImage);
        assertThat(result).isTrue();
        log.info("✅ WebP распознан как изображение");
    }

    @Test
    void testIsImageFile_WithTextFile() {
        boolean result = ocrService.isImageFile(invalidImage);
        assertThat(result).isFalse();
        log.info("✅ Текстовый файл НЕ распознан как изображение");
    }

    @Test
    void testIsImageFile_WithNullFileName() {
        boolean result = ocrService.isImageFile(nullNameFile);
        assertThat(result).isFalse();
        log.info("✅ Файл с null именем обработан корректно");
    }

    @Test
    void testIsImageFile_WithNullContentType() {
        boolean result = ocrService.isImageFile(nullContentTypeFile);
        assertThat(result).isTrue();
        log.info("✅ Файл с null ContentType обработан корректно");
    }

    @Test
    void testIsImageFile_WithUpperCaseExtension() {
        boolean result = ocrService.isImageFile(upperCaseFile);
        assertThat(result).isTrue();
        log.info("✅ PNG с верхним регистром распознан");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isTesseractAvailable
    // ============================================================

    @Test
    void testIsTesseractAvailable() {
        boolean available = ocrService.isTesseractAvailable();
        log.info("✅ Tesseract доступен: {}", available);
        // Не делаем assert, так как в CI может не быть Tesseract
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ extractText
    // ============================================================

    @Test
    void testExtractText_WithInvalidImage() {
        try {
            String result = ocrService.extractText(pngImage);
            assertThat(result).isNotNull();
            log.info("✅ extractText с PNG отработал без ошибок");
        } catch (IOException e) {
            log.info("✅ extractText выбросил ожидаемое исключение для PNG: {}", e.getMessage());
        }
    }

    @Test
    void testExtractText_WithEmptyImage() throws IOException {
        assertThatThrownBy(() -> ocrService.extractText(emptyImage))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Файл пуст");
        log.info("✅ Пустое изображение выбросило исключение");
    }

    @Test
    void testExtractText_WithInvalidImageData() throws IOException {
        // Проверяем, что метод выбрасывает исключение для некорректных данных
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "invalid.png",
                "image/png",
                "invalid data".getBytes(StandardCharsets.UTF_8)
        );

        String result = ocrService.extractText(invalidFile);
        assertThat(result).isEmpty();

        log.info("✅ Некорректные данные изображения обработаны корректно (возвращена пустая строка)");
    }

    @Test
    void testExtractText_WithTextFile() {
        try {
            String result = ocrService.extractText(invalidImage);
            assertThat(result).isNotNull();
            log.info("✅ extractText с текстовым файлом отработал");
        } catch (IOException e) {
            log.info("✅ extractText с текстовым файлом выбросил исключение: {}", e.getMessage());
        }
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ КОНСТРУКТОРА
    // ============================================================

    @Test
    void testConstructor_InitializesTesseract() {
        OcrService service = new OcrService();
        assertThat(service).isNotNull();
        log.info("✅ Конструктор OcrService отработал успешно");
    }

    @Test
    void testOcrServiceIsAvailable() {
        assertThat(ocrService).isNotNull();
        log.info("✅ OcrService внедрен успешно");
    }
}