# ===== Build stage =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY gateway/build.gradle gateway/build.gradle

RUN ./gradlew :gateway:dependencies --no-daemon || true

COPY gateway/src gateway/src
RUN ./gradlew :gateway:bootJar --no-daemon -x test

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/gateway/build/libs/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
