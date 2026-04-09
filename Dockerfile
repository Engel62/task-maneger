FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app


COPY target/task-manager-*.jar app.jar


COPY src/main/resources/application.yml application.yml
COPY src/main/resources/application-docker.yml application-docker.yml

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]