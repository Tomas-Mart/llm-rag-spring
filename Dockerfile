# Dockerfile (оптимизированный мультистейдж)

# ============================================
# STAGE 1: BUILD
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Кэширование зависимостей
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Загрузка зависимостей (кэшируется)
RUN ./mvnw dependency:go-offline -B

# Копирование исходников и сборка
COPY src src
RUN ./mvnw package -DskipTests -Dmaven.javadoc.skip=true -B

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Установка необходимых пакетов
RUN apk add --no-cache \
    tini \
    curl \
    tzdata \
    bash \
    && rm -rf /var/cache/apk/*

# Создание пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Копирование JAR из builder
COPY --from=builder /app/target/*.jar app.jar

# Оптимизация JAR (удаление ненужных файлов)
RUN jar --update --file app.jar --delete BOOT-INF/lib/*-sources.jar 2>/dev/null || true

# Права
RUN chown appuser:appgroup app.jar

# Настройки JVM для Java 21
ENV JAVA_OPTS="\
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -XX:MaxRAMPercentage=75.0 \
    -XX:MinRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=UTC"

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Tini для graceful shutdown
ENTRYPOINT ["tini", "--", "sh", "-c"]
CMD ["java $JAVA_OPTS -jar app.jar"]