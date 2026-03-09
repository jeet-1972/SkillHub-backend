# Build stage: use image that includes both Java 17 and Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/skillhub-lms-backend-1.0.0-SNAPSHOT.jar app.jar

# Render sets PORT. We map it to Spring Boot's server.port so Render can detect the bound port.
ENV SERVER_PORT=8080
EXPOSE 8080

# Note: -Dserver.port must be before -jar, otherwise it's treated as an app argument.
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar --spring.profiles.active=production"]