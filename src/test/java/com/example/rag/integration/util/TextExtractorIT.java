package com.example.rag.integration.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.util.TextExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты для TextExtractor
 * Проверяют извлечение текста из реальных файлов
 */
class TextExtractorIT {

    @TempDir
    Path tempDir;

    // ==================== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ====================

    /**
     * Нормализует текст для сравнения - удаляет лишние переносы строк в конце
     */
    private String normalizeText(String text) {
        if (text == null) return null;
        // Удаляем все переносы строк в конце
        return text.replaceAll("\n$", "").replaceAll("\r$", "");
    }

    // ==================== БАЗОВЫЕ ТЕСТЫ ====================

    @Test
    void shouldExtractTextFromSimpleTxtFile() throws IOException {
        // Подготовка
        Path txtFile = tempDir.resolve("test.txt");
        String expectedContent = "Это тестовое содержимое файла.";
        Files.writeString(txtFile, expectedContent, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                Files.readAllBytes(txtFile)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка - нормализуем оба текста
        assertEquals(normalizeText(expectedContent), normalizeText(extractedText));
        assertFalse(extractedText.isEmpty());
    }

    @Test
    void shouldExtractTextFromFileWithSpecialChars() throws IOException {
        // Подготовка
        Path file = tempDir.resolve("special.txt");
        String content = "Специальные символы: é, ñ, ü, 你好, 🚀, 日本語";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "special.txt",
                "text/plain;charset=UTF-8",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("🚀"));
        assertTrue(extractedText.contains("你好"));
        assertTrue(extractedText.contains("日本語"));
        // Нормализуем для точного сравнения
        assertEquals(normalizeText(content), normalizeText(extractedText));
    }

    @Test
    void shouldExtractTextFromFileWithDifferentEncoding() throws IOException {
        // Подготовка - файл с UTF-8 содержимым
        Path file = tempDir.resolve("utf8.txt");
        String content = "UTF-8 содержимое с русскими буквами и символами ©®™";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "utf8.txt",
                "text/plain;charset=UTF-8",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertEquals(normalizeText(content), normalizeText(extractedText));
    }

    // ==================== ТЕСТЫ С РАЗЛИЧНЫМИ ФОРМАТАМИ ====================

    @Test
    void shouldExtractTextFromHtmlFile() throws IOException {
        // Подготовка
        Path htmlFile = tempDir.resolve("test.html");
        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head><title>Тестовая страница</title></head>
                <body>
                    <h1>Заголовок</h1>
                    <p>Это параграф с текстом.</p>
                    <div>Еще немного текста</div>
                </body>
                </html>
                """;
        Files.writeString(htmlFile, htmlContent, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.html",
                "text/html",
                Files.readAllBytes(htmlFile)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка - Tika извлекает текст из HTML, удаляя теги
        assertNotNull(extractedText);
        assertFalse(extractedText.isEmpty());
        // Проверяем наличие ключевых слов
        assertTrue(extractedText.contains("Заголовок") ||
                   extractedText.contains("заголовок"),
                "Text should contain 'Заголовок'");
        assertTrue(extractedText.contains("параграф") ||
                   extractedText.contains("Параграф"),
                "Text should contain 'параграф'");
        assertTrue(extractedText.contains("текста"),
                "Text should contain 'текста'");
    }

    @Test
    void shouldExtractTextFromCsvFile() throws IOException {
        // Подготовка
        Path csvFile = tempDir.resolve("test.csv");
        String csvContent = """
                Имя,Возраст,Город
                Иван,30,Москва
                Петр,25,СПБ
                Анна,28,Казань
                """;
        Files.writeString(csvFile, csvContent, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                Files.readAllBytes(csvFile)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Иван"));
        assertTrue(extractedText.contains("Москва"));
        assertTrue(extractedText.contains("Петр"));
        assertTrue(extractedText.contains("СПБ"));
        assertEquals(normalizeText(csvContent), normalizeText(extractedText));
    }

    @Test
    void shouldExtractTextFromJsonFile() throws IOException {
        // Подготовка
        Path jsonFile = tempDir.resolve("test.json");
        String jsonContent = """
                {
                    "name": "Тест",
                    "age": 25,
                    "city": "Москва"
                }
                """;
        Files.writeString(jsonFile, jsonContent, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                Files.readAllBytes(jsonFile)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Тест"));
        assertTrue(extractedText.contains("Москва"));
    }

    // ==================== ТЕСТЫ С ПУСТЫМИ И НЕКОРРЕКТНЫМИ ФАЙЛАМИ ====================

    @Test
    void shouldHandleEmptyFile() throws IOException {
        // Подготовка
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "", StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                Files.readAllBytes(emptyFile)
        );

        // Действие и проверка - Tika выбрасывает ZeroByteFileException
        Exception exception = assertThrows(IOException.class,
                () -> TextExtractor.extractText(multipartFile));

        assertTrue(exception.getMessage().contains("Не удалось извлечь текст из файла") ||
                   exception.getMessage().contains("InputStream must have > 0 bytes"));
    }

    @Test
    void shouldHandleFileWithOnlyWhitespace() throws IOException {
        // Подготовка
        Path whitespaceFile = tempDir.resolve("whitespace.txt");
        String content = "   \n\t   \n   ";
        Files.writeString(whitespaceFile, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "whitespace.txt",
                "text/plain",
                Files.readAllBytes(whitespaceFile)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка - Tika может нормализовать пробелы, проверяем что текст не null
        assertNotNull(extractedText);
        // Проверяем, что извлеченный текст содержит только пробельные символы
        assertTrue(extractedText.trim().isEmpty(),
                "Extracted text should be empty or only whitespace");
    }

    @Test
    void shouldThrowExceptionForInvalidPdfFile() throws IOException {
        // Подготовка - поврежденный PDF файл
        Path invalidFile = tempDir.resolve("invalid.pdf");
        String invalidContent = "%PDF-1.4\ninvalid content\n%%EOF";
        Files.writeString(invalidFile, invalidContent, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "invalid.pdf",
                "application/pdf",
                Files.readAllBytes(invalidFile)
        );

        // Действие и проверка
        Exception exception = assertThrows(IOException.class,
                () -> TextExtractor.extractText(multipartFile));

        assertTrue(exception.getMessage().contains("Не удалось извлечь текст из файла") ||
                   exception.getMessage().contains("TIKA-198"));
    }

    @Test
    void shouldHandleInvalidImageFile() throws IOException {
        // Подготовка - бинарный файл, который не является изображением
        Path binaryFile = tempDir.resolve("invalid.jpg");
        byte[] invalidContent = {0x00, 0x01, 0x02, (byte) 0xFF, 0x00, 0x00};
        Files.write(binaryFile, invalidContent);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "invalid.jpg",
                "image/jpeg",
                Files.readAllBytes(binaryFile)
        );

        // Действие - Tika может обработать или не обработать
        try {
            String extractedText = TextExtractor.extractText(multipartFile);
            // Если обработал, проверяем что результат не null
            assertNotNull(extractedText);
        } catch (IOException e) {
            // Если выбросил исключение, проверяем сообщение
            assertTrue(e.getMessage().contains("Не удалось извлечь текст из файла"));
        }
    }

    // ==================== ТЕСТЫ С ИМЕНАМИ ФАЙЛОВ ====================

    @Test
    void shouldHandleFilenameWithSpacesAndSpecialChars() throws IOException {
        // Подготовка
        Path file = tempDir.resolve("test.txt");
        String content = "Тестовое содержимое";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "файл с пробелами и символами!@#.txt",
                "text/plain",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertNotNull(extractedText);
        assertEquals(normalizeText(content), normalizeText(extractedText));
    }

    @Test
    void shouldHandleFilenameWithNull() throws IOException {
        // Подготовка
        Path file = tempDir.resolve("test.txt");
        String content = "Тестовое содержимое";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                null,  // null filename
                "text/plain",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertNotNull(extractedText);
        assertEquals(normalizeText(content), normalizeText(extractedText));
    }

    // ==================== ТЕСТЫ С BOM ====================

    @Test
    void shouldHandleFileWithUtf8BOM() throws IOException {
        // Подготовка - файл с UTF-8 BOM (EF BB BF)
        Path file = tempDir.resolve("bom.txt");
        String content = "Файл с UTF-8 BOM маркером";
        // Добавляем BOM
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + contentBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(contentBytes, 0, combined, bom.length, contentBytes.length);
        Files.write(file, combined);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "bom.txt",
                "text/plain;charset=UTF-8",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка - Tika должен корректно обработать BOM
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("BOM"), "Text should contain 'BOM'");
    }

    // ==================== ТЕСТЫ С БОЛЬШИМИ ФАЙЛАМИ ====================

    @Test
    void shouldExtractTextFromLargeFile() throws IOException {
        // Подготовка
        Path largeFile = tempDir.resolve("large.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("Строка номер ").append(i).append("\n");
        }
        String expectedContent = content.toString();
        Files.writeString(largeFile, expectedContent, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                Files.readAllBytes(largeFile)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertEquals(normalizeText(expectedContent), normalizeText(extractedText));
        // Используем trim() для удаления пустой строки в конце
        long lineCount = extractedText.trim().lines().count();
        assertEquals(1000, lineCount);
    }

    @Test
    @Tag("performance")
    void shouldHandleVeryLargeFileWithinTimeLimit() throws IOException {
        // Подготовка - файл размером ~3-5 MB
        Path largeFile = tempDir.resolve("very_large.txt");
        StringBuilder content = new StringBuilder();
        String line = "Это длинная строка для тестирования производительности. " +
                      "Она содержит различные слова и символы. ";
        // Генерируем ~3MB текста (примерно 50,000 строк)
        for (int i = 0; i < 50000; i++) {
            content.append(i).append(": ").append(line).append("\n");
        }
        Files.writeString(largeFile, content.toString(), StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "very_large.txt",
                "text/plain",
                Files.readAllBytes(largeFile)
        );

        // Действие - проверяем, что не вылетает по времени
        long startTime = System.currentTimeMillis();
        String extractedText = TextExtractor.extractText(multipartFile);
        long duration = System.currentTimeMillis() - startTime;

        // Проверка
        assertNotNull(extractedText);
        assertFalse(extractedText.isEmpty());
        // Должно отработать за разумное время (например, < 5 секунд)
        assertTrue(duration < 5000, "Processing took too long: " + duration + "ms");
    }

    // ==================== ДОПОЛНИТЕЛЬНЫЕ ТЕСТЫ ====================

    @Test
    void shouldExtractTextFromMultipleFilesSequentially() throws IOException {
        // Подготовка - несколько файлов
        String[] fileNames = {"file1.txt", "file2.txt", "file3.txt"};
        String[] contents = {"First file content", "Second file content", "Third file content"};

        for (int i = 0; i < fileNames.length; i++) {
            Path file = tempDir.resolve(fileNames[i]);
            Files.writeString(file, contents[i], StandardCharsets.UTF_8);

            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileNames[i],
                    "text/plain",
                    Files.readAllBytes(file)
            );

            // Действие
            String extractedText = TextExtractor.extractText(multipartFile);

            // Проверка
            assertEquals(normalizeText(contents[i]), normalizeText(extractedText));
        }
    }

    @Test
    void shouldPreserveLineBreaks() throws IOException {
        // Подготовка
        Path file = tempDir.resolve("lines.txt");
        String content = "Строка 1\nСтрока 2\nСтрока 3\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "lines.txt",
                "text/plain",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Строка 1"));
        assertTrue(extractedText.contains("Строка 2"));
        assertTrue(extractedText.contains("Строка 3"));
        // Используем trim() для удаления пустой строки в конце
        long lineCount = extractedText.trim().lines().count();
        assertEquals(3, lineCount);
    }

    @Test
    void shouldHandleFileWithMixedNewlines() throws IOException {
        // Подготовка - смешанные окончания строк (CRLF и LF)
        Path file = tempDir.resolve("mixed.txt");
        String content = "Строка1\r\nСтрока2\nСтрока3\r\nСтрока4\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "mixed.txt",
                "text/plain",
                Files.readAllBytes(file)
        );

        // Действие
        String extractedText = TextExtractor.extractText(multipartFile);

        // Проверка - Tika должен сохранить структуру
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Строка1"));
        assertTrue(extractedText.contains("Строка2"));
        assertTrue(extractedText.contains("Строка3"));
        assertTrue(extractedText.contains("Строка4"));
    }

    @Test
    void shouldThrowExceptionWhenInputStreamFails() throws IOException {
        // Подготовка - создаем MultipartFile, который выбрасывает исключение
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content", StandardCharsets.UTF_8);

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                Files.readAllBytes(file)
        ) {
            @Override
            public java.io.@NotNull InputStream getInputStream() throws IOException {
                throw new IOException("Simulated stream error");
            }
        };

        // Действие и проверка
        IOException exception = assertThrows(IOException.class,
                () -> TextExtractor.extractText(mockFile));

        assertTrue(exception.getMessage().contains("Не удалось извлечь текст из файла"));
        assertNotNull(exception.getCause());
        assertEquals("Simulated stream error", exception.getCause().getMessage());
    }

    // ==================== ТЕСТЫ С РЕАЛЬНЫМИ ФАЙЛАМИ (если есть) ====================

    @Test
    @Tag("requires-resources")
    void shouldExtractTextFromRealPdfFile() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("/sample.pdf")) {
            if (inputStream == null) {
                System.out.println("⚠️  sample.pdf not found in resources, skipping test");
                return;
            }

            byte[] content = inputStream.readAllBytes();
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    "sample.pdf",
                    "application/pdf",
                    content
            );

            String extractedText = TextExtractor.extractText(multipartFile);
            assertNotNull(extractedText);
            assertFalse(extractedText.isEmpty(), "PDF should contain text");
        }
    }

    @Test
    @Tag("requires-resources")
    void shouldExtractTextFromRealDocxFile() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("/sample.docx")) {
            if (inputStream == null) {
                System.out.println("⚠️  sample.docx not found in resources, skipping test");
                return;
            }

            byte[] content = inputStream.readAllBytes();
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    "sample.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    content
            );

            String extractedText = TextExtractor.extractText(multipartFile);
            assertNotNull(extractedText);
            assertFalse(extractedText.isEmpty(), "DOCX should contain text");
        }
    }
}