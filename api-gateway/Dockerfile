FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /app
# Copy wrapper first — cached layer until gradle version changes
COPY gradlew gradlew
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew --version --no-daemon
# Build
COPY build.gradle settings.gradle ./
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]