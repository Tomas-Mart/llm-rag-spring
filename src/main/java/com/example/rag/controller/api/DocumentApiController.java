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

    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String FILE_NAME_KEY = "fileName";
    private static final String SIZE_KEY = "size";
    private static final String ID_KEY = "id";
    private static final String EXISTS_KEY = "exists";

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
                        .body(Map.of(ERROR_KEY, "Файл не выбран или пустой"));
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of(ERROR_KEY, "Размер файла превышает 10MB"));
            }

            ingestionService.ingestDocument(file, metadata);

            assert fileName != null;
            return ResponseEntity.ok(Map.of(
                    MESSAGE_KEY, "Document uploaded successfully",
                    FILE_NAME_KEY, fileName,
                    SIZE_KEY, file.getSize()
            ));

        } catch (DocumentIngestionException e) {
            log.error("❌ [API] Ошибка обработки документа: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [API] Непредвиденная ошибка: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        log.info("🗑️ [API] Удаление документа: {}", id);

        try {
            boolean deleted = ingestionService.deleteDocument(id);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(ERROR_KEY, "Документ с ID " + id + " не найден"));
            }

            return ResponseEntity.ok(Map.of(
                    MESSAGE_KEY, "Document deleted successfully",
                    ID_KEY , id
            ));

        } catch (RuntimeException e) {
            // ✅ Если RuntimeException содержит "not found" - возвращаем 404
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("not found")) {
                log.warn("⚠️ [API] Документ не найден: {}", message);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(ERROR_KEY, "Документ с ID " + id + " не найден"));
            }
            // Иначе - 500
            log.error("❌ [API] Ошибка удаления документа: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Ошибка удаления: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [API] Ошибка удаления документа: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Ошибка удаления: " + e.getMessage()));
        }
    }

    @GetMapping("/documents/exists")
    public ResponseEntity<Map<String, Object>> documentExists(
            @RequestParam(FILE_NAME_KEY) String fileName
    ) {
        log.info("🔍 [API] Проверка существования документа: {}", fileName);

        try {
            boolean exists = ingestionService.documentExists(fileName);

            return ResponseEntity.ok(Map.of(
                    FILE_NAME_KEY, fileName,
                    EXISTS_KEY, exists
            ));

        } catch (Exception e) {
            log.error("❌ [API] Ошибка проверки: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Ошибка проверки: " + e.getMessage()));
        }
    }
}