# Step 1: Build the Spring Boot app
FROM maven:3.9.2-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies first (caching layer)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Package the application (skip tests for faster build)
RUN ./mvnw clean package -DskipTests

# Step 2: Create a lightweight image for running the jar
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy the built jar from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port for Render
EXPOSE 8080

# Set environment variable for dynamic port
ENV PORT=8080

# Run the Spring Boot jar
ENTRYPOINT ["java","-jar","app.jar"]
