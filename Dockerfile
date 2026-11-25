
# Stage 1: Build Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy project files
COPY . .

# Build using preinstalled Maven
RUN mvn -B clean package -DskipTests

# Stage 2: Run Spring Boot application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the generated JAR
COPY --from=build /app/target/*.jar app.jar

# Render uses PORT=10000 by default
EXPOSE 10000

# Start Spring Boot with your existing memory settings
ENTRYPOINT ["java", "-Xms128m", "-Xmx350m", "-jar", "app.jar"]
