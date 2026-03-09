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

# Render sets PORT; Spring Boot will read SERVER_PORT or we pass it
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --spring.profiles.active=production -Dserver.port=${PORT:-8080}"]