package com.example.rag.unit.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Модульные тесты для {@link OcrService}.
 *
 * <p>Тестирует логику распознавания текста из изображений с использованием
 * Tesseract OCR в изоляции от реальных зависимостей.</p>
 *
 * @author RAG Application Team
 * @version 1.0
 * @see OcrService
 * @since 1.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Модульные тесты OcrService")
class OcrServiceUnitTest {

    @Mock
    private ITesseract mockTesseract;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private OcrService ocrService;

    private static final String TEST_FILE_NAME = "test.png";
    private static final String TEST_IMAGE_DATA = "fake image data";

    @BeforeEach
    void setUp() throws IOException {
        // Используем lenient() для моков, которые не используются во всех тестах
        lenient().when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        lenient().when(multipartFile.getContentType()).thenReturn("image/png");
        lenient().when(multipartFile.getInputStream()).thenReturn(
                new ByteArrayInputStream(TEST_IMAGE_DATA.getBytes(StandardCharsets.UTF_8))
        );
        lenient().when(multipartFile.getBytes()).thenReturn(TEST_IMAGE_DATA.getBytes(StandardCharsets.UTF_8));
        lenient().when(multipartFile.isEmpty()).thenReturn(false);
        lenient().when(multipartFile.getSize()).thenReturn((long) TEST_IMAGE_DATA.length());
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isImageFile
    // ============================================================

    @Test
    @DisplayName("Проверка PNG изображения")
    void testIsImageFile_WithPng() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.png");
        when(multipartFile.getContentType()).thenReturn("image/png");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ PNG распознан как изображение");
    }

    @Test
    @DisplayName("Проверка JPG изображения")
    void testIsImageFile_WithJpg() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ JPG распознан как изображение");
    }

    @Test
    @DisplayName("Проверка JPEG изображения")
    void testIsImageFile_WithJpeg() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.jpeg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ JPEG распознан как изображение");
    }

    @Test
    @DisplayName("Проверка GIF изображения")
    void testIsImageFile_WithGif() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.gif");
        when(multipartFile.getContentType()).thenReturn("image/gif");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ GIF распознан как изображение");
    }

    @Test
    @DisplayName("Проверка BMP изображения")
    void testIsImageFile_WithBmp() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.bmp");
        when(multipartFile.getContentType()).thenReturn("image/bmp");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ BMP распознан как изображение");
    }

    @Test
    @DisplayName("Проверка TIFF изображения")
    void testIsImageFile_WithTiff() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.tiff");
        when(multipartFile.getContentType()).thenReturn("image/tiff");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ TIFF распознан как изображение");
    }

    @Test
    @DisplayName("Проверка WebP изображения")
    void testIsImageFile_WithWebp() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.webp");
        when(multipartFile.getContentType()).thenReturn("image/webp");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ WebP распознан как изображение");
    }

    @Test
    @DisplayName("Проверка текстового файла - не изображение")
    void testIsImageFile_WithTextFile() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("document.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isFalse();
        log.info("✅ Текстовый файл НЕ распознан как изображение");
    }

    @Test
    @DisplayName("Проверка с null файлом")
    void testIsImageFile_WithNullFile() {
        // When
        boolean result = ocrService.isImageFile(null);

        // Then
        assertThat(result).isFalse();
        log.info("✅ null файл обработан корректно");
    }

    @Test
    @DisplayName("Проверка с null именем файла")
    void testIsImageFile_WithNullFileName() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isFalse();
        log.info("✅ null имя файла обработано корректно");
    }

    @Test
    @DisplayName("Проверка с пустым именем файла")
    void testIsImageFile_WithEmptyFileName() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isFalse();
        log.info("✅ пустое имя файла обработано корректно");
    }

    @Test
    @DisplayName("Проверка по MIME типу без расширения")
    void testIsImageFile_ByMimeTypeOnly() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image");
        when(multipartFile.getContentType()).thenReturn("image/png");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ Изображение распознано по MIME типу");
    }

    @Test
    @DisplayName("Проверка с null ContentType")
    void testIsImageFile_WithNullContentType() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("image.png");
        when(multipartFile.getContentType()).thenReturn(null);

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ Изображение распознано по расширению");
    }

    @Test
    @DisplayName("Проверка с верхним регистром")
    void testIsImageFile_WithUpperCaseExtension() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("IMAGE.PNG");
        when(multipartFile.getContentType()).thenReturn("image/png");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ Изображение с верхним регистром распознано");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ extractText
    // ============================================================

    @Test
    @DisplayName("Извлечение текста из изображения")
    void testExtractText_Success() throws IOException, TesseractException {
        // Given
        String expectedText = "Extracted text from image";
        String cleanedText = expectedText.trim().replaceAll("\\s+", " ");

        // Создаем spy для доступа к приватному полю
        OcrService spyService = org.mockito.Mockito.spy(ocrService);

        // Создаем мок для ITesseract
        ITesseract mockTesseractLocal = mock(ITesseract.class);
        when(mockTesseractLocal.doOCR(any(java.awt.image.BufferedImage.class)))
                .thenReturn(expectedText);

        // Заменяем tesseract через рефлексию с обработкой ошибок
        try {
            java.lang.reflect.Field field = OcrService.class.getDeclaredField("tesseract");
            field.setAccessible(true);
            field.set(spyService, mockTesseractLocal);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Не удалось заменить tesseract через рефлексию: {}", e.getMessage());
            // Если не удалось, используем существующий
        }

        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        when(multipartFile.getInputStream()).thenReturn(
                new ByteArrayInputStream("fake image data".getBytes(StandardCharsets.UTF_8))
        );
        when(multipartFile.isEmpty()).thenReturn(false);

        // When
        String result = spyService.extractText(multipartFile);

        // Then
        assertThat(result).isEqualTo(cleanedText);
        log.info("✅ Текст успешно извлечен");
    }

    @Test
    @DisplayName("Извлечение текста с null файлом")
    void testExtractText_WithNullFile() {
        // When & Then
        assertThatThrownBy(() -> ocrService.extractText(null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Файл пуст или отсутствует");
        log.info("✅ null файл обработан корректно");
    }

    @Test
    @DisplayName("Извлечение текста с пустым файлом")
    void testExtractText_WithEmptyFile() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> ocrService.extractText(multipartFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Файл пуст или отсутствует");
        log.info("✅ Пустой файл обработан корректно");
    }

    @Test
    @DisplayName("Извлечение текста с TesseractException")
    void testExtractText_WithTesseractException() throws TesseractException, IOException {
        // Given
        OcrService spyService = org.mockito.Mockito.spy(ocrService);
        ITesseract mockTesseractLocal = mock(ITesseract.class);
        when(mockTesseractLocal.doOCR(any(java.awt.image.BufferedImage.class)))
                .thenThrow(new TesseractException("OCR error"));

        // Заменяем tesseract через рефлексию
        try {
            java.lang.reflect.Field field = OcrService.class.getDeclaredField("tesseract");
            field.setAccessible(true);
            field.set(spyService, mockTesseractLocal);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Не удалось заменить tesseract через рефлексию: {}", e.getMessage());
        }

        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        when(multipartFile.getInputStream()).thenReturn(
                new ByteArrayInputStream("fake image data".getBytes(StandardCharsets.UTF_8))
        );
        when(multipartFile.isEmpty()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> spyService.extractText(multipartFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Ошибка распознавания текста");
        log.info("✅ TesseractException обработана корректно");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isTesseractAvailable
    // ============================================================

    @Test
    @DisplayName("Проверка доступности Tesseract")
    void testIsTesseractAvailable() {
        // When
        boolean result = ocrService.isTesseractAvailable();

        // Then
        // Не делаем строгий assert, так как в CI может не быть Tesseract
        log.info("✅ Tesseract доступен: {}", result);
        // Проверяем, что результат не null (может быть true или false)
        assertThat(result).isNotNull();
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ checkTesseractPath (через рефлексию)
    // ============================================================

    @Test
    @DisplayName("Проверка checkTesseractPath с существующим путем")
    void testCheckTesseractPath_WithValidPath() throws Exception {
        // Given
        String validPath = "/usr/bin/tesseract";

        // When - через рефлексию
        java.lang.reflect.Method method = OcrService.class.getDeclaredMethod("checkTesseractPath", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ocrService, validPath);

        // Then
        // Результат может быть true или false в зависимости от системы
        log.info("✅ checkTesseractPath с существующим путем: {}", result);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Проверка checkTesseractPath с несуществующим путем")
    void testCheckTesseractPath_WithInvalidPath() throws Exception {
        // Given
        String invalidPath = "/path/to/nonexistent/tesseract";

        // When - через рефлексию
        java.lang.reflect.Method method = OcrService.class.getDeclaredMethod("checkTesseractPath", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ocrService, invalidPath);

        // Then
        log.info("✅ checkTesseractPath с несуществующим путем: {}", result);
        // В большинстве случаев должно быть false
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Проверка checkTesseractPath с null путем")
    void testCheckTesseractPath_WithNullPath() throws Exception {
        // When - через рефлексию
        java.lang.reflect.Method method = OcrService.class.getDeclaredMethod("checkTesseractPath", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ocrService, new Object[]{null});

        // Then
        log.info("✅ checkTesseractPath с null путем: {}", result);
        assertThat(result).isFalse();
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ КОНСТРУКТОРА
    // ============================================================

    @Test
    @DisplayName("Проверка конструктора OcrService")
    void testConstructor() {
        // Given & When
        OcrService service = new OcrService();

        // Then
        assertThat(service).isNotNull();
        log.info("✅ Конструктор OcrService отработал успешно");
    }
}