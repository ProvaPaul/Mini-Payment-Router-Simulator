# ---- Build stage: compile and package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Download dependencies first. This layer is cached and reused while pom.xml does not change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests need a running PostgreSQL, so they are run outside the image build (./mvnw test).
RUN mvn -B -q -DskipTests package

# ---- Runtime stage: only a Java runtime and the jar ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/target/payment-router-0.0.1-SNAPSHOT.jar app.jar

# logging.file.name=logs/payment-router.log resolves to /app/logs/payment-router.log
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
