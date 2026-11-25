
# Stage 1: Build Spring Boot
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven mirror configuration
COPY settings.xml /root/.m2/settings.xml

# Copy project files
COPY . .

# Build using Google Maven Central mirror
RUN mvn -B \
    -s /root/.m2/settings.xml \
    clean package -DskipTests

# Stage 2: Run Spring Boot
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-Xms128m", "-Xmx350m", "-jar", "app.jar"]
