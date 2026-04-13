# Build context: repo root (set in docker-compose.yml)
# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY backend backend

RUN chmod +x gradlew && ./gradlew :backend:bootJar --no-daemon -x test

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/backend/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
