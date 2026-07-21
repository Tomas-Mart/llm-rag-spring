package com.example.rag.util;

import java.io.IOException;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TextExtractor {

    private static final Tika tika = new Tika();

    private TextExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String extractText(MultipartFile file) throws IOException {
        try {
            String text = tika.parseToString(file.getInputStream());
            log.info("✅ Текст извлечен из {} ({} символов)",
                    file.getOriginalFilename(), text.length());
            return text;
        } catch (Exception e) {
            log.error("❌ Ошибка извлечения текста из {}: {}",
                    file.getOriginalFilename(), e.getMessage());
            throw new IOException("Не удалось извлечь текст из файла", e);
        }
    }
}