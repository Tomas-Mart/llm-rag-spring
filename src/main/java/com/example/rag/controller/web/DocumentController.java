package com.example.rag.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Контроллер для загрузки документов через веб-интерфейс.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Загрузку документов через форму</li>
 *   <li>Валидацию файлов (размер, тип, содержимое)</li>
 *   <li>Перенаправление с flash-сообщениями</li>
 * </ul>
 * <p>
 * Вся бизнес-логика делегируется {@link DocumentIngestionService}.
 *
 * @author RAG Application Team
 * @version 1.0
 * @see DocumentIngestionService
 * @since 1.0
 */
@Slf4j
@Controller
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final String MESSAGE_ATTR = "message";
    private static final String REDIRECT_HOME = "redirect:/";
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private final DocumentIngestionService ingestionService;

    /**
     * Загружает документ через веб-форму.
     * <p>
     * Выполняет валидацию:
     * <ul>
     *   <li>Файл не должен быть пустым</li>
     *   <li>Размер не должен превышать 10MB</li>
     * </ul>
     *
     * @param file               загружаемый файл
     * @param metadata           метаданные (опционально)
     * @param force              флаг принудительной перезагрузки
     * @param redirectAttributes атрибуты для flash-сообщений
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
        log.info("📄 Загрузка документа: {}, размер: {} bytes, force: {}",
                fileName, file.getSize(), force);

        // 1. Валидация: пустой файл
        if (file.isEmpty()) {
            String error = "❌ Файл не выбран или пустой";
            log.warn(error);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTR, error);
            return REDIRECT_HOME;
        }

        // 2. Валидация: размер файла
        if (file.getSize() > MAX_FILE_SIZE) {
            String error = String.format("❌ Размер файла превышает 10MB: %s (%.2f MB)",
                    fileName, file.getSize() / (1024.0 * 1024.0));
            log.warn(error);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTR, error);
            return REDIRECT_HOME;
        }

        try {
            // 3. Проверка существования документа
            boolean exists = ingestionService.documentExists(fileName);

            if (exists && !force) {
                String warning = String.format(
                        "⚠️ Документ '%s' уже загружен. Для перезагрузки добавьте параметр ?force=true",
                        fileName
                );
                log.warn(warning);
                redirectAttributes.addFlashAttribute(MESSAGE_ATTR, warning);
                return REDIRECT_HOME;
            }

            // 4. Загрузка или перезагрузка документа
            if (exists && force) {
                ingestionService.reIngestDocument(file, metadata);
                String success = String.format("🔄 Документ '%s' успешно перезагружен", fileName);
                log.info(success);
                redirectAttributes.addFlashAttribute(MESSAGE_ATTR, success);
            } else {
                ingestionService.ingestDocument(file, metadata);
                String success = String.format("✅ Документ '%s' успешно загружен", fileName);
                log.info(success);
                redirectAttributes.addFlashAttribute(MESSAGE_ATTR, success);
            }

        } catch (DocumentIngestionException e) {
            String error = String.format("❌ Ошибка обработки документа: %s", e.getMessage());
            log.error(error, e);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTR, error);

        } catch (IllegalArgumentException e) {
            String error = String.format("❌ Некорректные параметры: %s", e.getMessage());
            log.error(error, e);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTR, error);

        } catch (Exception e) {
            String error = "❌ Непредвиденная ошибка при загрузке документа";
            log.error(error, e);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTR, error);
        }

        return REDIRECT_HOME;
    }
}