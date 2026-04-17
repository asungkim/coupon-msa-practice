# ===== Build stage =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper + 루트 빌드 파일
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY coupon-api/build.gradle coupon-api/build.gradle

# 의존성 캐시
RUN ./gradlew :coupon-api:dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY coupon-api/src coupon-api/src
RUN ./gradlew :coupon-api:bootJar --no-daemon -x test

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/coupon-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
