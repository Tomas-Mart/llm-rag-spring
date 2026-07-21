# Dockerfile
FROM eclipse-temurin:21-jre-alpine

# Установка tini для graceful shutdown
RUN apk add --no-cache tini

# Создание пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Копирование JAR
COPY target/*.jar app.jar

# Права
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["tini", "--", "java"]
CMD ["-jar", "app.jar"]