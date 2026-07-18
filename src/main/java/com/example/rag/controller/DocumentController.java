package com.example.rag.controller;

import com.example.rag.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata
    ) throws IOException {
        ingestionService.ingestDocument(file, metadata);
        return ResponseEntity.ok("Документ загружен: " + file.getOriginalFilename());
    }
}