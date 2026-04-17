# ===== Build stage =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY notification-mock-api/build.gradle notification-mock-api/build.gradle

RUN ./gradlew :notification-mock-api:dependencies --no-daemon || true

COPY notification-mock-api/src notification-mock-api/src
RUN ./gradlew :notification-mock-api:bootJar --no-daemon -x test

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/notification-mock-api/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
