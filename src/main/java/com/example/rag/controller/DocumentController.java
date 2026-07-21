package com.example.rag.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private static final String MESSAGE_KEY = "message";

    private final DocumentIngestionService ingestionService;

    /**
     * Отображает страницу загрузки документов.
     * Перенаправляет на главную страницу.
     *
     * @return перенаправление на главную страницу
     */
    @GetMapping("/upload")
    public String uploadPage() {
        log.debug("Отображение страницы загрузки документов");
        return "redirect:/";
    }

    /**
     * Обрабатывает загрузку документа.
     * При успешной загрузке перенаправляет на главную страницу с сообщением.
     *
     * @param file               загружаемый файл
     * @param metadata           метаданные документа (опционально)
     * @param force              флаг принудительной перезагрузки
     * @param redirectAttributes атрибуты для перенаправления (сохраняют сообщения)
     * @return перенаправление на главную страницу
     */
    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
            RedirectAttributes redirectAttributes
    ) {
        String fileName = file.getOriginalFilename();

        // Проверка на пустой файл
        if (file.isEmpty()) {
            String errorMessage = "❌ Файл не выбран или пустой";
            redirectAttributes.addFlashAttribute(MESSAGE_KEY, errorMessage);
            log.warn(errorMessage);
            return "redirect:/";
        }

        // Проверка размера файла (максимум 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            String errorMessage = "❌ Размер файла превышает 10MB: " + fileName;
            redirectAttributes.addFlashAttribute(MESSAGE_KEY, errorMessage);
            log.warn(errorMessage);
            return "redirect:/";
        }

        try {
            // Проверяем, существует ли документ
            if (ingestionService.documentExists(fileName)) {
                if (force) {
                    // Принудительная перезагрузка
                    ingestionService.reIngestDocument(file, metadata);
                    String successMessage = "🔄 Документ '" + fileName + "' перезагружен";
                    redirectAttributes.addFlashAttribute(MESSAGE_KEY, successMessage);
                    log.info(successMessage);
                } else {
                    // Документ уже существует
                    String warningMessage = "⚠️ Документ '" + fileName + "' уже загружен. " +
                                            "Для перезагрузки добавьте параметр ?force=true";
                    redirectAttributes.addFlashAttribute(MESSAGE_KEY, warningMessage);
                    log.warn(warningMessage);
                }
            } else {
                // Обычная загрузка
                ingestionService.ingestDocument(file, metadata);
                String successMessage = "✅ Документ успешно загружен: " + fileName;
                redirectAttributes.addFlashAttribute(MESSAGE_KEY, successMessage);
                log.info(successMessage);
            }

        } catch (DocumentIngestionException documentException) {
            String errorMessage = "❌ Ошибка обработки документа: " + documentException.getMessage();
            redirectAttributes.addFlashAttribute(MESSAGE_KEY, errorMessage);
            log.error(errorMessage, documentException);

        } catch (IllegalArgumentException illegalArgumentException) {
            String errorMessage = "❌ Некорректные параметры: " + illegalArgumentException.getMessage();
            redirectAttributes.addFlashAttribute(MESSAGE_KEY, errorMessage);
            log.error(errorMessage, illegalArgumentException);

        } catch (Exception unexpectedException) {
            String errorMessage = "❌ Непредвиденная ошибка при загрузке документа";
            redirectAttributes.addFlashAttribute(MESSAGE_KEY, errorMessage);
            log.error(errorMessage, unexpectedException);
        }

        return "redirect:/";
    }
}