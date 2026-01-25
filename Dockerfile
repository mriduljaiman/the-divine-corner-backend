# Multi-stage build for smaller image size

# Stage 1: Build
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app

# Copy Gradle files and download dependencies (layer caching)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and build
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory and set ownership (before switching to non-root user)
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy JAR from build stage
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", \
  "app.jar"]
