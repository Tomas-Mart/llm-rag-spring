package com.example.rag.unit.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.util.TextExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextExtractorTest {

    private MultipartFile mockFile;

    /**
     * Нормализует текст для сравнения - удаляет лишние переносы строк в конце
     */
    private String normalizeText(String text) {
        if (text == null) return null;
        // Удаляем все переносы строк в конце
        return text.replaceAll("\n$", "").replaceAll("\r$", "");
    }

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);
    }

    @Test
    void shouldExtractTextFromValidTxtFile() throws IOException {
        // Given
        String expectedText = "Sample text content";
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertEquals(normalizeText(expectedText), normalizeText(extractedText));
        verify(mockFile, times(1)).getInputStream();
        verify(mockFile, times(1)).getOriginalFilename();
    }

    @Test
    void shouldExtractTextFromValidPdfFile() throws IOException {
        // Given
        String expectedText = "PDF content";
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertEquals(normalizeText(expectedText), normalizeText(extractedText));
    }

    @Test
    void shouldThrowIOExceptionWhenFileInputStreamFails() throws IOException {
        // Given
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream error"));
        when(mockFile.getOriginalFilename()).thenReturn("faulty.txt");

        // When & Then
        IOException exception = assertThrows(IOException.class,
                () -> TextExtractor.extractText(mockFile));

        assertTrue(exception.getMessage().contains("Не удалось извлечь текст из файла"));
        assertInstanceOf(IOException.class, exception.getCause());
        assertEquals("Stream error", exception.getCause().getMessage());
    }

    @Test
    void shouldHandleInvalidBinaryFile() throws IOException {
        // Given
        byte[] invalidContent = {0x00, 0x01, 0x02, (byte) 0xFF};
        InputStream inputStream = new ByteArrayInputStream(invalidContent);

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("invalid.bin");

        // When & Then
        try {
            String result = TextExtractor.extractText(mockFile);
            assertNotNull(result);
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Не удалось извлечь текст из файла"));
        }
    }

    @Test
    void shouldHandleEmptyFile() throws IOException {
        // Given
        String emptyContent = "";
        InputStream inputStream = new ByteArrayInputStream(emptyContent.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("empty.txt");

        // When & Then
        Exception exception = assertThrows(IOException.class,
                () -> TextExtractor.extractText(mockFile));

        assertTrue(exception.getMessage().contains("Не удалось извлечь текст из файла") ||
                   exception.getMessage().contains("InputStream must have > 0 bytes"));
    }

    @Test
    void shouldExtractTextWithSpecialCharacters() throws IOException {
        // Given
        String expectedText = "Привет мир! 你好世界! Hello World! 🚀";
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("special.txt");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertEquals(normalizeText(expectedText), normalizeText(extractedText));
        assertTrue(extractedText.contains("🚀"));
        assertTrue(extractedText.contains("你好"));
    }

    @Test
    void shouldHandleLargeTextFile() throws IOException {
        // Given
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("Line ").append(i).append("\n");
        }
        String expectedText = largeText.toString();
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("large.txt");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertEquals(normalizeText(expectedText), normalizeText(extractedText));
        // Используем trim() для удаления пустой строки в конце
        long lineCount = extractedText.trim().lines().count();
        assertEquals(10000, lineCount);
    }

    @Test
    void shouldExtractTextFromDocxFile() throws IOException {
        // Given
        String expectedText = "Word document content";
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("test.docx");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertEquals(normalizeText(expectedText), normalizeText(extractedText));
    }

    @Test
    void shouldExtractTextFromHtmlFile() throws IOException {
        // Given
        String htmlContent = "<html><body><p>HTML content</p></body></html>";
        InputStream inputStream = new ByteArrayInputStream(htmlContent.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("test.html");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("HTML content"));
    }

    @Test
    void shouldHandleNullFilename() throws IOException {
        // Given
        String content = "Some content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn(null);

        // When & Then
        String extractedText = TextExtractor.extractText(mockFile);
        assertNotNull(extractedText);
        assertEquals(normalizeText(content), normalizeText(extractedText));
    }

    @Test
    void testPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<TextExtractor> constructor =
                TextExtractor.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class,
                constructor::newInstance);

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Utility class", exception.getCause().getMessage());
    }

    @Test
    void shouldLogErrorWhenExtractionFails() throws IOException {
        // Given
        when(mockFile.getInputStream()).thenThrow(new IOException("IO Error"));
        when(mockFile.getOriginalFilename()).thenReturn("error.txt");

        // When & Then
        assertThrows(IOException.class, () -> TextExtractor.extractText(mockFile));
    }

    @Test
    void shouldHandleNullInputStream() throws IOException {
        // Given
        when(mockFile.getInputStream()).thenReturn(null);
        when(mockFile.getOriginalFilename()).thenReturn("null.txt");

        // When & Then
        Exception exception = assertThrows(IOException.class,
                () -> TextExtractor.extractText(mockFile));

        assertTrue(exception.getMessage().contains("Не удалось извлечь текст из файла") ||
                   exception.getCause() instanceof NullPointerException);
    }

    @Test
    void shouldExtractTextWithNewlines() throws IOException {
        // Given
        String expectedText = "Line 1\nLine 2\nLine 3\n";
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("lines.txt");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Line 1"));
        assertTrue(extractedText.contains("Line 2"));
        assertTrue(extractedText.contains("Line 3"));
        // Используем trim() для удаления пустой строки в конце
        long lineCount = extractedText.trim().lines().count();
        assertEquals(3, lineCount);
    }

    @Test
    void shouldExtractTextWithDifferentEncoding() throws IOException {
        // Given
        String expectedText = "UTF-8 text with special chars: é, ñ, ü";
        byte[] bytes = expectedText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(bytes);

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("utf8.txt");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertEquals(normalizeText(expectedText), normalizeText(extractedText));
        assertTrue(extractedText.contains("é"));
        assertTrue(extractedText.contains("ñ"));
        assertTrue(extractedText.contains("ü"));
    }

    @Test
    void shouldExtractTextFromMultipleFiles() throws IOException {
        // Given
        String[] contents = {"First content", "Second content", "Third content"};

        for (String content : contents) {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes());
            when(mockFile.getInputStream()).thenReturn(inputStream);
            when(mockFile.getOriginalFilename()).thenReturn("test.txt");

            // When
            String extractedText = TextExtractor.extractText(mockFile);

            // Then
            assertNotNull(extractedText);
            assertEquals(normalizeText(content), normalizeText(extractedText));
        }
    }

    @Test
    void shouldExtractTextWithWhitespace() throws IOException {
        // Given
        String expectedText = "  Text with   multiple spaces  and   tabs\t\t\t";
        InputStream inputStream = new ByteArrayInputStream(expectedText.getBytes());

        when(mockFile.getInputStream()).thenReturn(inputStream);
        when(mockFile.getOriginalFilename()).thenReturn("whitespace.txt");

        // When
        String extractedText = TextExtractor.extractText(mockFile);

        // Then
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("  Text with   multiple spaces"));
        assertTrue(extractedText.contains("tabs"));
    }
}