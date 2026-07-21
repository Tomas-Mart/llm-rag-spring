package com.example.rag.controller;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.rag.exception.DocumentIngestionException;
import com.example.rag.service.DocumentIngestionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentIngestionService ingestionService;

    @InjectMocks
    private DocumentController documentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    void testUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("metadata", "support-metadata"))
                .andExpect(status().is3xxRedirection())  // ← Исправлено: редирект
                .andExpect(redirectedUrl("/"));  // ← Проверяем редирект

        verify(ingestionService).ingestDocument(any(), anyString());

        System.out.println("✅ Тест загрузки документа пройден");
    }

    @Test
    void testUploadDocumentWithoutMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().is3xxRedirection())  // ← Исправлено: редирект
                .andExpect(redirectedUrl("/"));

        verify(ingestionService).ingestDocument(any(), any());

        System.out.println("✅ Тест загрузки без метаданных пройден");
    }

    @Test
    void testUploadDocumentWhenServiceThrowsDocumentIngestionException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );
        doThrow(new DocumentIngestionException("Document error")).when(ingestionService)
                .ingestDocument(any(), anyString());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("metadata", "support"))
                .andExpect(status().is3xxRedirection())  // ← Исправлено: редирект
                .andExpect(redirectedUrl("/"));

        verify(ingestionService).ingestDocument(any(), anyString());

        System.out.println("✅ Тест DocumentIngestionException пройден");
    }

    @Test
    void testUploadDocumentWhenServiceThrowsException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );
        doThrow(new RuntimeException("Processing error")).when(ingestionService)
                .ingestDocument(any(), anyString());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("metadata", "support"))
                .andExpect(status().is3xxRedirection())  // ← Исправлено: редирект
                .andExpect(redirectedUrl("/"));

        verify(ingestionService).ingestDocument(any(), anyString());

        System.out.println("✅ Тест ошибки обработки пройден");
    }

    @Test
    void testUploadDocumentWithMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "support.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("metadata", "support-metadata"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));  // ← Добавить проверку

        verify(ingestionService).ingestDocument(any(), anyString());
    }
}