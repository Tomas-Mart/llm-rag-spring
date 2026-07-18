package com.example.rag.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.rag.entity.DocumentEntity;
import com.example.rag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;

    @Transactional
    public void ingestDocument(MultipartFile file, String metadata) throws IOException {
        log.info("📄 Загружаем документ: {}", file.getOriginalFilename());

        // 1. Создаём документ из текста (через билдер)
        String content = new String(file.getBytes());
        Document document = Document.builder()
                .withContent(content)
                .withMetadata("fileName", file.getOriginalFilename())
                .withMetadata("metadata", metadata != null ? metadata : "")
                .withMetadata("uploadedAt", LocalDateTime.now().toString())
                .build();

        // 2. Разбиваем на чанки (TokenTextSplitter)
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(List.of(document));
        log.info("📦 Документ разбит на {} чанков", chunks.size());

        // 3. Сохраняем эмбеддинги в векторную БД
        vectorStore.add(chunks);

        // 4. Сохраняем метаданные в обычную таблицу
        DocumentEntity entity = DocumentEntity.builder()
                .content(content)
                .fileName(file.getOriginalFilename())
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
        documentRepository.save(entity);

        log.info("✅ Документ загружен успешно");
    }
}