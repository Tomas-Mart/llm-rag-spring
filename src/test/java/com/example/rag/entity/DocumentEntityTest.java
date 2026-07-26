package com.example.rag.entity;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки сущности {@link DocumentEntity}.
 *
 * <h2>Назначение</h2>
 * <p>Проверяет корректность работы сущности DocumentEntity: конструкторы,
 * геттеры/сеттеры, аннотации JPA, валидацию и equals/hashCode.</p>
 *
 * <h2>Тестируемые аспекты</h2>
 * <ul>
 *   <li>Конструкторы (пустой, с параметрами, builder)</li>
 *   <li>Геттеры и сеттеры</li>
 *   <li>JPA аннотации (@Table, @Column, @GeneratedValue)</li>
 *   <li>Валидация (Bean Validation)</li>
 *   <li>equals() и hashCode()</li>
 *   <li>Работа с различными типами данных</li>
 * </ul>
 *
 * @author RAG Application Team
 * @version 5.0
 * @see DocumentEntity
 * @since 1.0
 */
@Slf4j
@Epic("Модульные тесты")
@Feature("Сущность документа")
class DocumentEntityTest {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final Long TEST_ID = 1L;
    private static final String TEST_CONTENT = "Test content";
    private static final String TEST_FILE_NAME = "support.txt";
    private static final String TEST_METADATA = "{\"author\":\"support\"}";
    private static final String LONG_FILE_NAME = "a".repeat(255) + ".txt";

    private static final String JSON_METADATA = """
            {
                "author": "John Doe",
                "category": "technical",
                "tags": ["spring", "ai", "rag"],
                "version": "1.0"
            }
            """;

    // ============================================================
    // ПОЛЯ
    // ============================================================

    private Validator validator;
    private LocalDateTime now;

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        now = LocalDateTime.now();
        log.info("✅ DocumentEntityTest initialized");
    }

    // ============================================================
    // ТЕСТЫ КОНСТРУКТОРОВ
    // ============================================================

    @Test
    @Description("Проверка создания сущности через builder")
    @Story("Конструкторы")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntityBuilder() {
        logTestStart("Testing builder");

        DocumentEntity entity = createTestEntity();

        assertThat(entity)
                .as("Entity should be created with builder")
                .satisfies(e -> {
                    assertThat(e.getId()).isEqualTo(TEST_ID);
                    assertThat(e.getContent()).isEqualTo(TEST_CONTENT);
                    assertThat(e.getFileName()).isEqualTo(TEST_FILE_NAME);
                    assertThat(e.getMetadata()).isEqualTo(TEST_METADATA);
                    assertThat(e.getCreatedAt()).isEqualTo(now);
                });

        logTestSuccess("Builder works correctly");
    }

    @Test
    @Description("Проверка пустого конструктора")
    @Story("Конструкторы")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityNoArgsConstructor() {
        logTestStart("Testing no-args constructor");

        DocumentEntity entity = new DocumentEntity();

        assertThat(entity)
                .as("Entity should be created with no-args constructor")
                .satisfies(e -> {
                    assertThat(e.getId()).isNull();
                    assertThat(e.getContent()).isNull();
                    assertThat(e.getFileName()).isNull();
                    assertThat(e.getMetadata()).isNull();
                    assertThat(e.getCreatedAt()).isNull();
                });

        logTestSuccess("No-args constructor works correctly");
    }

    @Test
    @Description("Проверка конструктора со всеми параметрами")
    @Story("Конструкторы")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityAllArgsConstructor() {
        logTestStart("Testing all-args constructor");

        DocumentEntity entity = new DocumentEntity(
                TEST_ID,
                TEST_FILE_NAME,
                TEST_CONTENT,
                TEST_METADATA,
                now
        );

        assertThat(entity)
                .as("Entity should be created with all-args constructor")
                .satisfies(e -> {
                    assertThat(e.getId()).isEqualTo(TEST_ID);
                    assertThat(e.getFileName()).isEqualTo(TEST_FILE_NAME);
                    assertThat(e.getContent()).isEqualTo(TEST_CONTENT);
                    assertThat(e.getMetadata()).isEqualTo(TEST_METADATA);
                    assertThat(e.getCreatedAt()).isEqualTo(now);
                });

        logTestSuccess("All-args constructor works correctly");
    }

    // ============================================================
    // ТЕСТЫ ГЕТТЕРОВ И СЕТТЕРОВ
    // ============================================================

    @Test
    @Description("Проверка геттеров и сеттеров")
    @Story("Геттеры и сеттеры")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntitySettersAndGetters() {
        logTestStart("Testing getters and setters");

        DocumentEntity entity = new DocumentEntity();

        entity.setId(TEST_ID);
        entity.setContent(TEST_CONTENT);
        entity.setFileName(TEST_FILE_NAME);
        entity.setMetadata(TEST_METADATA);
        entity.setCreatedAt(now);

        assertThat(entity)
                .as("Entity should have correct values after setters")
                .satisfies(e -> {
                    assertThat(e.getId()).isEqualTo(TEST_ID);
                    assertThat(e.getContent()).isEqualTo(TEST_CONTENT);
                    assertThat(e.getFileName()).isEqualTo(TEST_FILE_NAME);
                    assertThat(e.getMetadata()).isEqualTo(TEST_METADATA);
                    assertThat(e.getCreatedAt()).isEqualTo(now);
                });

        logTestSuccess("Getters and setters work correctly");
    }

    // ============================================================
    // ТЕСТЫ СОДЕРЖИМОГО
    // ============================================================

    @Test
    @Description("Проверка длинного содержимого")
    @Story("Содержимое")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityWithLongContent() {
        logTestStart("Testing long content");

        String longContent = "A".repeat(10000);
        DocumentEntity entity = DocumentEntity.builder()
                .content(longContent)
                .fileName("large.txt")
                .createdAt(now)
                .build();

        assertThat(entity.getContent())
                .as("Content should be 10000 characters")
                .isEqualTo(longContent)
                .hasSize(10000);

        logTestSuccess("Long content handled correctly");
    }

    @Test
    @Description("Проверка специальных символов и Unicode")
    @Story("Содержимое")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityWithSpecialCharacters() {
        logTestStart("Testing special characters");

        String specialContent = """
                Special chars: !@#$%^&*()_+
                Unicode: 中文, 日本語, 한국어
                Emoji: 🚀🎉💻
                """;

        DocumentEntity entity = DocumentEntity.builder()
                .content(specialContent)
                .fileName("special.txt")
                .metadata("{\"chars\":\"unicode\"}")
                .createdAt(now)
                .build();

        assertThat(entity.getContent())
                .as("Content should contain special characters")
                .contains("中文", "🚀");

        assertThat(entity.getMetadata())
                .as("Metadata should contain unicode")
                .contains("unicode");

        logTestSuccess("Special characters handled correctly");
    }

    @Test
    @Description("Проверка JSON метаданных")
    @Story("Метаданные")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityWithJsonMetadata() {
        logTestStart("Testing JSON metadata");

        DocumentEntity entity = DocumentEntity.builder()
                .content(TEST_CONTENT)
                .fileName(TEST_FILE_NAME)
                .metadata(JSON_METADATA)
                .createdAt(now)
                .build();

        assertThat(entity.getMetadata())
                .as("Metadata should be valid JSON")
                .contains("author", "spring", "\"version\": \"1.0\"");

        logTestSuccess("JSON metadata handled correctly");
    }

    // ============================================================
    // ТЕСТЫ NULL И ПУСТЫХ ЗНАЧЕНИЙ
    // ============================================================

    @Test
    @Description("Проверка null полей")
    @Story("Null значения")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityWithNullFields() {
        logTestStart("Testing null fields");

        DocumentEntity entity = DocumentEntity.builder()
                .id(null)
                .content(null)
                .fileName(null)
                .metadata(null)
                .createdAt(null)
                .build();

        assertThat(entity)
                .as("All fields should be null")
                .satisfies(e -> {
                    assertThat(e.getId()).isNull();
                    assertThat(e.getContent()).isNull();
                    assertThat(e.getFileName()).isNull();
                    assertThat(e.getMetadata()).isNull();
                    assertThat(e.getCreatedAt()).isNull();
                });

        logTestSuccess("Null fields handled correctly");
    }

    @Test
    @Description("Проверка пустых метаданных")
    @Story("Метаданные")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityWithEmptyMetadata() {
        logTestStart("Testing empty metadata");

        DocumentEntity entity = DocumentEntity.builder()
                .content(TEST_CONTENT)
                .fileName(TEST_FILE_NAME)
                .metadata("")
                .createdAt(now)
                .build();

        assertThat(entity.getMetadata())
                .as("Metadata should be empty")
                .isEmpty();

        logTestSuccess("Empty metadata handled correctly");
    }

    // ============================================================
    // ТЕСТЫ ДЛИННЫХ ИМЕН ФАЙЛОВ
    // ============================================================

    @Test
    @Description("Проверка очень длинного имени файла")
    @Story("Имя файла")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityWithVeryLongFileName() {
        logTestStart("Testing very long file name");

        DocumentEntity entity = DocumentEntity.builder()
                .content(TEST_CONTENT)
                .fileName(LONG_FILE_NAME)
                .createdAt(now)
                .build();

        assertThat(entity.getFileName())
                .as("File name should be preserved")
                .isEqualTo(LONG_FILE_NAME)
                .hasSize(259);

        logTestSuccess("Long file name handled correctly");
    }

    // ============================================================
    // ТЕСТЫ ВРЕМЕНИ
    // ============================================================

    @Test
    @Description("Проверка временной метки")
    @Story("Временные метки")
    @Severity(SeverityLevel.NORMAL)
    void testDocumentEntityTimestamp() {
        logTestStart("Testing timestamp");

        DocumentEntity entity = DocumentEntity.builder()
                .content(TEST_CONTENT)
                .fileName(TEST_FILE_NAME)
                .createdAt(now)
                .build();

        assertThat(entity.getCreatedAt())
                .as("CreatedAt should not be null and not in future")
                .isNotNull()
                .isBeforeOrEqualTo(LocalDateTime.now());

        logTestSuccess("Timestamp handled correctly");
    }

    // ============================================================
    // ТЕСТЫ JPA АННОТАЦИЙ
    // ============================================================

    @Test
    @Description("Проверка аннотации @Table")
    @Story("JPA аннотации")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntityTableNameAnnotation() {
        logTestStart("Testing @Table annotation");

        Table table = DocumentEntity.class.getAnnotation(Table.class);

        assertThat(table)
                .as("@Table annotation should exist")
                .isNotNull();

        assertThat(table.name())
                .as("Table name should be 'documents'")
                .isEqualTo("documents");

        logTestSuccess("@Table annotation verified");
    }

    @Test
    @Description("Проверка аннотации @GeneratedValue")
    @Story("JPA аннотации")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntityIdGenerationStrategy() throws NoSuchFieldException {
        logTestStart("Testing @GeneratedValue annotation");

        Field idField = DocumentEntity.class.getDeclaredField("id");
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        assertThat(generatedValue)
                .as("@GeneratedValue annotation should exist")
                .isNotNull();

        assertThat(generatedValue.strategy())
                .as("Generation strategy should be IDENTITY")
                .isEqualTo(GenerationType.IDENTITY);

        logTestSuccess("@GeneratedValue annotation verified");
    }

    @Test
    @Description("Проверка аннотаций @Column")
    @Story("JPA аннотации")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntityColumnAnnotations() throws NoSuchFieldException {
        logTestStart("Testing @Column annotations");

        // Content column
        Field contentField = DocumentEntity.class.getDeclaredField("content");
        Column contentColumn = contentField.getAnnotation(Column.class);
        assertThat(contentColumn)
                .as("@Column for content should exist")
                .isNotNull();
        assertThat(contentColumn.columnDefinition())
                .as("Content column should be TEXT")
                .isEqualTo("TEXT");

        // CreatedAt column
        Field createdAtField = DocumentEntity.class.getDeclaredField("createdAt");
        Column createdAtColumn = createdAtField.getAnnotation(Column.class);
        assertThat(createdAtColumn)
                .as("@Column for createdAt should exist")
                .isNotNull();
        assertThat(createdAtColumn.name())
                .as("CreatedAt column should be 'created_at'")
                .isEqualTo("created_at");

        logTestSuccess("@Column annotations verified");
    }

    // ============================================================
    // ТЕСТЫ EQUALS И HASHCODE
    // ============================================================

    @Test
    @Description("Проверка equals() и hashCode()")
    @Story("Equals и HashCode")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntityEqualsAndHashCode() {
        logTestStart("Testing equals and hashCode");

        DocumentEntity entity1 = createTestEntity();
        DocumentEntity entity2 = createTestEntity();
        DocumentEntity entity3 = DocumentEntity.builder()
                .id(2L)
                .content("Different")
                .fileName("different.txt")
                .metadata("different")
                .createdAt(now)
                .build();

        assertThat(entity1)
                .as("Same data should be equal")
                .isEqualTo(entity2)
                .isNotEqualTo(entity3);

        assertThat(entity1.hashCode())
                .as("Same data should have same hashCode")
                .isEqualTo(entity2.hashCode())
                .isNotEqualTo(entity3.hashCode());

        logTestSuccess("equals and hashCode work correctly");
    }

    // ============================================================
    // ТЕСТЫ ВАЛИДАЦИИ
    // ============================================================

    @Test
    @Description("Проверка валидации сущности")
    @Story("Валидация")
    @Severity(SeverityLevel.CRITICAL)
    void testDocumentEntityValidation() {
        logTestStart("Testing validation");

        DocumentEntity entity = createTestEntity();
        Set<ConstraintViolation<DocumentEntity>> violations = validator.validate(entity);

        assertThat(violations)
                .as("No validation violations should occur")
                .isEmpty();

        logTestSuccess("Validation passed");
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    /**
     * Создает тестовую сущность DocumentEntity.
     *
     * @return тестовая сущность
     */
    private DocumentEntity createTestEntity() {
        return DocumentEntity.builder()
                .id(TEST_ID)
                .content(TEST_CONTENT)
                .fileName(TEST_FILE_NAME)
                .metadata(TEST_METADATA)
                .createdAt(now)
                .build();
    }

    /**
     * Логирует начало теста.
     *
     * @param message сообщение для логирования
     */
    private void logTestStart(String message) {
        log.info("🚀 [{}] {}", getTestName(), message);
    }

    /**
     * Логирует успешное завершение теста.
     *
     * @param message сообщение для логирования
     */
    private void logTestSuccess(String message) {
        log.info("✅ [{}] {}", getTestName(), message);
    }

    /**
     * Возвращает имя теста.
     *
     * @return имя класса теста
     */
    private String getTestName() {
        return getClass().getSimpleName();
    }
}