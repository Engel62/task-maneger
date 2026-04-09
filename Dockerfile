FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Копируем JAR файл
COPY target/task-manager-*.jar app.jar

# Копируем конфигурационные файлы
COPY src/main/resources/application.yml application.yml
COPY src/main/resources/application-docker.yml application-docker.yml

# Создаем пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]