# syntax=docker/dockerfile:1
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /app
COPY gradlew gradlew
COPY gradle/ gradle/
RUN chmod +x gradlew
COPY build.gradle settings.gradle ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle,id=api-gateway-gradle \
    --mount=type=cache,target=/app/build,id=api-gateway-build \
    ./gradlew bootJar --no-daemon -x test && \
    cp /app/build/libs/*.jar /app/app.jar

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]