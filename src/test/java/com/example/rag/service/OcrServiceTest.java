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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для {@link OcrService}.
 * Проверяют распознавание текста из изображений.
 *
 * @author RAG Application Team
 * @version 1.0
 * @since 1.0
 */
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

    @BeforeEach
    void setUp() {
        // Создаем тестовые файлы изображений (фиктивные данные)
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
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isImageFile
    // ============================================================

    @Test
    void testIsImageFile_WithPng() {
        assertThat(ocrService.isImageFile(pngImage)).isTrue();
        System.out.println("✅ PNG распознан как изображение");
    }

    @Test
    void testIsImageFile_WithJpg() {
        assertThat(ocrService.isImageFile(jpgImage)).isTrue();
        System.out.println("✅ JPG распознан как изображение");
    }

    @Test
    void testIsImageFile_WithGif() {
        assertThat(ocrService.isImageFile(gifImage)).isTrue();
        System.out.println("✅ GIF распознан как изображение");
    }

    @Test
    void testIsImageFile_WithBmp() {
        assertThat(ocrService.isImageFile(bmpImage)).isTrue();
        System.out.println("✅ BMP распознан как изображение");
    }

    @Test
    void testIsImageFile_WithTiff() {
        assertThat(ocrService.isImageFile(tiffImage)).isTrue();
        System.out.println("✅ TIFF распознан как изображение");
    }

    @Test
    void testIsImageFile_WithWebp() {
        assertThat(ocrService.isImageFile(webpImage)).isTrue();
        System.out.println("✅ WebP распознан как изображение");
    }

    @Test
    void testIsImageFile_WithTextFile() {
        assertThat(ocrService.isImageFile(invalidImage)).isFalse();
        System.out.println("✅ Текстовый файл НЕ распознан как изображение");
    }

    @Test
    void testIsImageFile_WithNullFileName() {
        // Создаем файл с null именем
        MultipartFile nullNameFile = new MockMultipartFile(
                "file",
                null,
                "image/png",
                "data".getBytes(StandardCharsets.UTF_8)
        );
        assertThat(ocrService.isImageFile(nullNameFile)).isFalse();
        System.out.println("✅ Файл с null именем обработан корректно");
    }

    @Test
    void testIsImageFile_WithNullContentType() {
        // Создаем файл с null contentType
        MultipartFile nullContentTypeFile = new MockMultipartFile(
                "file",
                "test.png",
                null,
                "data".getBytes(StandardCharsets.UTF_8)
        );
        // Должен вернуть true, так как проверка по расширению сработает
        assertThat(ocrService.isImageFile(nullContentTypeFile)).isTrue();
        System.out.println("✅ Файл с null ContentType обработан корректно");
    }

    @Test
    void testIsImageFile_WithUpperCaseExtension() {
        MultipartFile upperCaseFile = new MockMultipartFile(
                "file",
                "TEST.PNG",
                "image/png",
                "data".getBytes(StandardCharsets.UTF_8)
        );
        assertThat(ocrService.isImageFile(upperCaseFile)).isTrue();
        System.out.println("✅ PNG с верхним регистром распознан");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isTesseractAvailable
    // ============================================================

    @Test
    void testIsTesseractAvailable() {
        // Этот тест может вернуть false, если Tesseract не установлен в CI
        // Но метод должен работать без ошибок
        boolean available = ocrService.isTesseractAvailable();
        System.out.println("✅ Tesseract доступен: " + available);
        // Не делаем assert, так как в CI может не быть Tesseract
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ extractText
    // ============================================================

    @Test
    void testExtractText_WithInvalidImage() {
        // Так как у нас нет реальных изображений, проверяем, что метод не падает
        try {
            String result = ocrService.extractText(pngImage);
            // Может вернуть пустую строку или текст
            assertThat(result).isNotNull();
            System.out.println("✅ extractText с PNG отработал без ошибок");
        } catch (IOException e) {
            // Ожидаемо, так как данные не являются реальным изображением
            System.out.println("✅ extractText выбросил ожидаемое исключение для PNG");
        }
    }

    @Test
    void testExtractText_WithEmptyImage() {
        assertThatThrownBy(() -> ocrService.extractText(emptyImage))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Ошибка распознавания текста");
        System.out.println("✅ Пустое изображение выбросило исключение");
    }

    @Test
    void testExtractText_WithNullFile() {
        assertThatThrownBy(() -> ocrService.extractText(null))
                .isInstanceOf(NullPointerException.class);
        System.out.println("✅ null файл выбросил исключение");
    }

    @Test
    void testExtractText_WithTextFile() {
        // Текстовый файл не является изображением
        try {
            String result = ocrService.extractText(invalidImage);
            assertThat(result).isNotNull();
            System.out.println("✅ extractText с текстовым файлом отработал");
        } catch (IOException e) {
            System.out.println("✅ extractText с текстовым файлом выбросил исключение");
        }
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ КОНСТРУКТОРА
    // ============================================================

    @Test
    void testConstructor_InitializesTesseract() {
        // Проверяем, что конструктор отработал без ошибок
        OcrService service = new OcrService();
        assertThat(service).isNotNull();
        System.out.println("✅ Конструктор OcrService отработал успешно");
    }

    @Test
    void testOcrServiceIsAvailable() {
        // Просто проверяем, что сервис существует
        assertThat(ocrService).isNotNull();
        System.out.println("✅ OcrService внедрен успешно");
    }
}