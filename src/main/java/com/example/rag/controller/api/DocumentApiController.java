package com.example.rag.controller.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API контроллер для работы с документами.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentApiController {

    private final DocumentIngestionService ingestionService;

    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata
    ) {
        String fileName = file.getOriginalFilename();
        log.info("📄 [API] Загрузка документа: {}", fileName);

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Файл не выбран или пустой"));
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Размер файла превышает 10MB"));
            }

            ingestionService.ingestDocument(file, metadata);

            assert fileName != null;
            return ResponseEntity.ok(Map.of(
                    "message", "Document uploaded successfully",
                    "fileName", fileName,
                    "size", file.getSize()
            ));

        } catch (DocumentIngestionException e) {
            log.error("❌ [API] Ошибка обработки документа: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [API] Непредвиденная ошибка: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        log.info("🗑️ [API] Удаление документа: {}", id);

        try {
            boolean deleted = ingestionService.deleteDocument(id);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Документ с ID " + id + " не найден"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Document deleted successfully",
                    "id", id
            ));

        } catch (RuntimeException e) {
            // ✅ Если RuntimeException содержит "not found" - возвращаем 404
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("not found")) {
                log.warn("⚠️ [API] Документ не найден: {}", message);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Документ с ID " + id + " не найден"));
            }
            // Иначе - 500
            log.error("❌ [API] Ошибка удаления документа: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка удаления: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [API] Ошибка удаления документа: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка удаления: " + e.getMessage()));
        }
    }

    @GetMapping("/documents/exists")
    public ResponseEntity<Map<String, Object>> documentExists(
            @RequestParam("fileName") String fileName
    ) {
        log.info("🔍 [API] Проверка существования документа: {}", fileName);

        try {
            boolean exists = ingestionService.documentExists(fileName);

            return ResponseEntity.ok(Map.of(
                    "fileName", fileName,
                    "exists", exists
            ));

        } catch (Exception e) {
            log.error("❌ [API] Ошибка проверки: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Ошибка проверки: " + e.getMessage()));
        }
    }
}