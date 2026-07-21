package com.example.rag.entity;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEntityTest {

    private Validator validator;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        now = LocalDateTime.now();
    }

    @Test
    void testDocumentEntityBuilder() {
        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .id(1L)
                .content("Test content")
                .fileName("support.txt")
                .metadata("{\"author\":\"support\"}")
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getContent()).isEqualTo("Test content");
        assertThat(entity.getFileName()).isEqualTo("support.txt");
        assertThat(entity.getMetadata()).isEqualTo("{\"author\":\"support\"}");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testDocumentEntityNoArgsConstructor() {
        // Act
        DocumentEntity entity = new DocumentEntity();

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getContent()).isNull();
        assertThat(entity.getFileName()).isNull();
        assertThat(entity.getMetadata()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void testDocumentEntityAllArgsConstructor() {
        // Act
        DocumentEntity entity = new DocumentEntity(
                1L,
                "Test content",
                "support.txt",
                "{\"author\":\"support\"}",
                now
        );

        // Assert
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getContent()).isEqualTo("Test content");
        assertThat(entity.getFileName()).isEqualTo("support.txt");
        assertThat(entity.getMetadata()).isEqualTo("{\"author\":\"support\"}");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testDocumentEntitySettersAndGetters() {
        // Arrange
        DocumentEntity entity = new DocumentEntity();

        // Act
        entity.setId(1L);
        entity.setContent("Test content");
        entity.setFileName("support.txt");
        entity.setMetadata("{\"author\":\"support\"}");
        entity.setCreatedAt(now);

        // Assert
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getContent()).isEqualTo("Test content");
        assertThat(entity.getFileName()).isEqualTo("support.txt");
        assertThat(entity.getMetadata()).isEqualTo("{\"author\":\"support\"}");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testDocumentEntityWithLongContent() {
        // Arrange
        String longContent = "A".repeat(10000);

        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content(longContent)
                .fileName("large.txt")
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity.getContent())
                .isEqualTo(longContent)
                .hasSize(10000);
    }

    @Test
    void testDocumentEntityWithSpecialCharacters() {
        // Arrange
        String specialContent = """
                Special chars: !@#$%^&*()_+
                Unicode: 中文, 日本語, 한국어
                Emoji: 🚀🎉💻
                """;

        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content(specialContent)
                .fileName("special.txt")
                .metadata("{\"chars\":\"unicode\"}")
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity.getContent()).contains("中文");
        assertThat(entity.getContent()).contains("🚀");
        assertThat(entity.getMetadata()).contains("unicode");
    }

    @Test
    void testDocumentEntityWithNullFields() {
        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .id(null)
                .content(null)
                .fileName(null)
                .metadata(null)
                .createdAt(null)
                .build();

        // Assert
        assertThat(entity.getId()).isNull();
        assertThat(entity.getContent()).isNull();
        assertThat(entity.getFileName()).isNull();
        assertThat(entity.getMetadata()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void testDocumentEntityWithEmptyMetadata() {
        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content("Test")
                .fileName("support.txt")
                .metadata("")
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity.getMetadata()).isEmpty();
    }

    @Test
    void testDocumentEntityWithJsonMetadata() {
        // Arrange
        String jsonMetadata = """
                {
                    "author": "John Doe",
                    "category": "technical",
                    "tags": ["spring", "ai", "rag"],
                    "version": "1.0"
                }
                """;

        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content("Test content")
                .fileName("support.txt")
                .metadata(jsonMetadata)
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity.getMetadata()).contains("author")
                .contains("spring")
                .contains("\"version\": \"1.0\"");
    }

    @Test
    void testDocumentEntityEqualsAndHashCode() {
        // Arrange
        DocumentEntity entity1 = DocumentEntity.builder()
                .id(1L)
                .content("Test content")
                .fileName("support.txt")
                .metadata("metadata")
                .createdAt(now)
                .build();

        DocumentEntity entity2 = DocumentEntity.builder()
                .id(1L)
                .content("Test content")
                .fileName("support.txt")
                .metadata("metadata")
                .createdAt(now)
                .build();

        DocumentEntity entity3 = DocumentEntity.builder()
                .id(2L)
                .content("Different content")
                .fileName("different.txt")
                .metadata("different")
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity1)
                .isEqualTo(entity2)
                .isNotEqualTo(entity3);

        assertThat(entity1.hashCode())
                .isEqualTo(entity2.hashCode())
                .isNotEqualTo(entity3.hashCode());
    }

    @Test
    void testDocumentEntityToString() {
        // Arrange
        DocumentEntity entity = DocumentEntity.builder()
                .id(1L)
                .content("Test content")
                .fileName("support.txt")
                .createdAt(now)
                .build();

        // Act
        String toString = entity.toString();

        // Assert
        assertThat(toString).contains("1")
                .contains("Test content")
                .contains("support.txt");
    }

    @Test
    void testDocumentEntityTableNameAnnotation() {
        // Arrange
        DocumentEntity entity = new DocumentEntity();
        jakarta.persistence.Table table = entity.getClass().getAnnotation(jakarta.persistence.Table.class);

        // Assert
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("documents");
    }

    @Test
    void testDocumentEntityIdGenerationStrategy() throws NoSuchFieldException {
        // Arrange
        DocumentEntity entity = new DocumentEntity();
        java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        // Assert
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void testDocumentEntityColumnAnnotations() throws NoSuchFieldException {
        // Test content column
        var contentField = DocumentEntity.class.getDeclaredField("content");
        var column = contentField.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.columnDefinition()).isEqualTo("TEXT");

        // Test createdAt column
        var createdAtField = DocumentEntity.class.getDeclaredField("createdAt");
        var createdAtColumn = createdAtField.getAnnotation(Column.class);
        assertThat(createdAtColumn).isNotNull();
        assertThat(createdAtColumn.name()).isEqualTo("created_at");
    }

    @Test
    void testDocumentEntityTimestamp() {
        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content("Test")
                .fileName("support.txt")
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void testDocumentEntityWithFutureTimestamp() {
        // Arrange
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content("Test")
                .fileName("support.txt")
                .createdAt(future)
                .build();

        // Assert
        assertThat(entity.getCreatedAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void testDocumentEntityValidation() {
        // Arrange
        DocumentEntity entity = DocumentEntity.builder()
                .content("Valid content")
                .fileName("valid.txt")
                .metadata("{\"valid\":\"json\"}")
                .createdAt(now)
                .build();

        // Act
        Set<ConstraintViolation<DocumentEntity>> violations = validator.validate(entity);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void testDocumentEntityWithVeryLongFileName() {
        // Arrange
        String longFileName = "a".repeat(255) + ".txt";

        // Act
        DocumentEntity entity = DocumentEntity.builder()
                .content("Test")
                .fileName(longFileName)
                .createdAt(now)
                .build();

        // Assert
        assertThat(entity.getFileName())
                .isEqualTo(longFileName)
                .hasSize(259); // 255 + ".txt"
    }
}