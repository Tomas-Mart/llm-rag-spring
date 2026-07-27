package com.example.rag.unit.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
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
 * <h2>Тестируемые сценарии</h2>
 * <ul>
 *   <li>Распознавание различных форматов изображений (PNG, JPG, GIF, BMP, TIFF, WebP)</li>
 *   <li>Обработка null и пустых файлов</li>
 *   <li>Извлечение текста из изображений</li>
 *   <li>Обработка ошибок Tesseract</li>
 *   <li>Проверка доступности Tesseract в системе</li>
 * </ul>
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

    // ============================================================
    // ЗАВИСИМОСТИ (МОКИ)
    // ============================================================

    @Mock
    private ITesseract mockTesseract;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private OcrService ocrService;

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final String TEST_FILE_NAME = "test.png";

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ СОЗДАНИЯ ТЕСТОВОГО ИЗОБРАЖЕНИЯ
    // ============================================================

    /**
     * Создает тестовое изображение с текстом.
     *
     * @param text текст для отображения на изображении
     * @return BufferedImage с текстом
     */
    private BufferedImage createTestImage(String text) {
        BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Белый фон
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 300, 100);

        // Черный текст
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString(text, 50, 60);

        g2d.dispose();
        return image;
    }

    /**
     * Конвертирует BufferedImage в byte[].
     *
     * @param image изображение
     * @return массив байт в формате PNG
     * @throws IOException если ошибка при конвертации
     */
    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    /**
     * Настраивает моки перед каждым тестом.
     * Использует {@code lenient()} для избежания UnnecessaryStubbingException.
     *
     * @throws IOException если ошибка при создании InputStream
     */
    @BeforeEach
    void setUp() throws IOException {
        lenient().when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
        lenient().when(multipartFile.getContentType()).thenReturn("image/png");
        lenient().when(multipartFile.isEmpty()).thenReturn(false);
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isImageFile
    // ============================================================

    @Test
    @DisplayName("Проверка PNG изображения")
    void testIsImageFile_WithPng() {
        // Given
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.png");
        lenient().when(multipartFile.getContentType()).thenReturn("image/png");

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
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.jpg");
        lenient().when(multipartFile.getContentType()).thenReturn("image/jpeg");

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
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.jpeg");
        lenient().when(multipartFile.getContentType()).thenReturn("image/jpeg");

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
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.gif");
        lenient().when(multipartFile.getContentType()).thenReturn("image/gif");

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
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.bmp");
        lenient().when(multipartFile.getContentType()).thenReturn("image/bmp");

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
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.tiff");
        lenient().when(multipartFile.getContentType()).thenReturn("image/tiff");

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
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.webp");
        lenient().when(multipartFile.getContentType()).thenReturn("image/webp");

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ WebP распознан как изображение");
    }

    @Test
    @DisplayName("Текстовый файл не должен распознаваться как изображение")
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
    @DisplayName("null файл не должен распознаваться как изображение")
    void testIsImageFile_WithNullFile() {
        // When
        boolean result = ocrService.isImageFile(null);

        // Then
        assertThat(result).isFalse();
        log.info("✅ null файл обработан корректно");
    }

    @Test
    @DisplayName("null имя файла не должно распознаваться как изображение")
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
    @DisplayName("Пустое имя файла не должно распознаваться как изображение")
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
    @DisplayName("Изображение должно распознаваться по MIME типу без расширения")
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
    @DisplayName("Изображение с null ContentType должно распознаваться по расширению")
    void testIsImageFile_WithNullContentType() {
        // Given
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("image.png");
        lenient().when(multipartFile.getContentType()).thenReturn(null);

        // When
        boolean result = ocrService.isImageFile(multipartFile);

        // Then
        assertThat(result).isTrue();
        log.info("✅ Изображение распознано по расширению");
    }

    @Test
    @DisplayName("Изображение с верхним регистром должно распознаваться")
    void testIsImageFile_WithUpperCaseExtension() {
        // Given
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("IMAGE.PNG");
        lenient().when(multipartFile.getContentType()).thenReturn("image/png");

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
    @DisplayName("Успешное извлечение текста из изображения")
    void testExtractText_Success() throws IOException, TesseractException {
        // Given - создаем реальное изображение с текстом
        BufferedImage testImage = createTestImage("Test OCR");
        byte[] imageBytes = imageToBytes(testImage);

        // Создаем MultipartFile с реальным изображением
        MultipartFile realImageFile = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageBytes
        );

        // Создаем spy для доступа к приватному полю
        OcrService spyService = org.mockito.Mockito.spy(ocrService);

        // Создаем мок для ITesseract
        ITesseract mockTesseractLocal = mock(ITesseract.class);
        String expectedText = "Test OCR";
        when(mockTesseractLocal.doOCR(any(BufferedImage.class)))
                .thenReturn(expectedText);

        // Заменяем tesseract через рефлексию
        try {
            Field field = OcrService.class.getDeclaredField("tesseract");
            field.setAccessible(true);
            field.set(spyService, mockTesseractLocal);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Не удалось заменить tesseract через рефлексию: {}", e.getMessage());
        }

        // When
        String result = spyService.extractText(realImageFile);

        // Then
        assertThat(result).isEqualTo(expectedText.trim().replaceAll("\\s+", " "));
        log.info("✅ Текст успешно извлечен");
    }

    @Test
    @DisplayName("null файл должен выбрасывать исключение")
    void testExtractText_WithNullFile() {
        // When & Then
        assertThatThrownBy(() -> ocrService.extractText(null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Файл пуст или отсутствует");
        log.info("✅ null файл обработан корректно");
    }

    @Test
    @DisplayName("Пустой файл должен выбрасывать исключение")
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
    @DisplayName("TesseractException должна обрабатываться корректно")
    void testExtractText_WithTesseractException() throws IOException, TesseractException {
        // Given - создаем реальное изображение
        BufferedImage testImage = createTestImage("Test OCR");
        byte[] imageBytes = imageToBytes(testImage);

        MultipartFile realImageFile = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageBytes
        );

        // Создаем spy
        OcrService spyService = org.mockito.Mockito.spy(ocrService);

        // Создаем мок для ITesseract
        ITesseract mockTesseractLocal = mock(ITesseract.class);
        when(mockTesseractLocal.doOCR(any(BufferedImage.class)))
                .thenThrow(new TesseractException("OCR error"));

        // Заменяем tesseract через рефлексию
        try {
            Field field = OcrService.class.getDeclaredField("tesseract");
            field.setAccessible(true);
            field.set(spyService, mockTesseractLocal);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Не удалось заменить tesseract через рефлексию: {}", e.getMessage());
        }

        // When & Then
        assertThatThrownBy(() -> spyService.extractText(realImageFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Ошибка распознавания текста");
        log.info("✅ TesseractException обработана корректно");
    }

    // ============================================================
    // ТЕСТЫ ДЛЯ isTesseractAvailable
    // ============================================================

    @Test
    @DisplayName("Проверка доступности Tesseract в системе")
    void testIsTesseractAvailable() {
        // When
        boolean result = ocrService.isTesseractAvailable();

        // Then
        log.info("✅ Tesseract доступен: {}", result);
        // Проверяем, что результат не null (может быть true или false)
        assertThat(result).isTrue();
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
        Method method = OcrService.class.getDeclaredMethod("checkTesseractPath", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ocrService, validPath);

        // Then
        log.info("✅ checkTesseractPath с существующим путем: {}", result);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Проверка checkTesseractPath с несуществующим путем")
    void testCheckTesseractPath_WithInvalidPath() throws Exception {
        // Given
        String invalidPath = "/path/to/nonexistent/tesseract";

        // When - через рефлексию
        Method method = OcrService.class.getDeclaredMethod("checkTesseractPath", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ocrService, invalidPath);

        // Then
        log.info("✅ checkTesseractPath с несуществующим путем: {}", result);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Проверка checkTesseractPath с null путем")
    void testCheckTesseractPath_WithNullPath() throws Exception {
        // When - через рефлексию
        Method method = OcrService.class.getDeclaredMethod("checkTesseractPath", String.class);
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
    @DisplayName("Конструктор OcrService должен успешно инициализироваться")
    void testConstructor() {
        // Given & When
        OcrService service = new OcrService();

        // Then
        assertThat(service).isNotNull();
        log.info("✅ Конструктор OcrService отработал успешно");
    }
}