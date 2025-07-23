# =============================================================================
# Optimized Multi-stage Dockerfile for FHIR Bridge Spring Boot Application
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build Dependencies Cache
# This stage creates a layer with just dependencies for better caching
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS deps

# Install dos2unix for handling line endings
RUN apk add --no-cache dos2unix

WORKDIR /app

# Copy Maven wrapper and POM first for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix line endings and make Maven wrapper executable
RUN dos2unix mvnw && chmod +x ./mvnw

# Download dependencies (this layer will be cached unless POM changes)
RUN ./mvnw dependency:go-offline -B

# -----------------------------------------------------------------------------
# Stage 2: Build Application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy dependencies from previous stage
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /app/mvnw .
COPY --from=deps /app/.mvn .mvn
COPY --from=deps /app/pom.xml .

# Copy source code
COPY src src

# Build the application with optimizations
RUN ./mvnw clean package -DskipTests -B \
    -Dmaven.compile.fork=true \
    -Dmaven.compiler.maxmem=1024m

# Extract JAR layers for better Docker layer caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# -----------------------------------------------------------------------------
# Stage 3: Runtime Image
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install required packages for health checks and debugging
RUN apk add --no-cache \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Create non-root user with specific UID/GID for security
RUN addgroup -g 1001 -S fhirbridge && \
    adduser -u 1001 -S fhirbridge -G fhirbridge

# Create application directory
WORKDIR /app

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/tmp && \
    chown -R fhirbridge:fhirbridge /app

# Copy application layers from builder stage
COPY --from=builder --chown=fhirbridge:fhirbridge /app/dependencies/ ./
COPY --from=builder --chown=fhirbridge:fhirbridge /app/spring-boot-loader/ ./
COPY --from=builder --chown=fhirbridge:fhirbridge /app/snapshot-dependencies/ ./
COPY --from=builder --chown=fhirbridge:fhirbridge /app/application/ ./

# Switch to non-root user
USER fhirbridge:fhirbridge

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -XX:+UseCompressedOops \
               -XX:+UseCompressedClassPointers \
               -Djava.security.egd=file:/dev/./urandom \
               -Djava.awt.headless=true \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=UTC \
               -Djava.io.tmpdir=/app/tmp"

# Application-specific environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
ENV MANAGEMENT_SERVER_PORT=8081

# Expose application and management ports
EXPOSE 8080 8081

# Health check with proper timeout and retries
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=90s \
            --retries=3 \
            CMD curl -f http://localhost:8081/actuator/health/readiness || exit 1

# Use dumb-init to handle signals properly and run as PID 1
ENTRYPOINT ["dumb-init", "--"]

# Run the application using Spring Boot's layered JAR approach
CMD ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# -----------------------------------------------------------------------------
# Metadata
# -----------------------------------------------------------------------------
LABEL maintainer="FHIR Bridge Team"
LABEL version="0.1.0"
LABEL description="Optimized FHIR Bridge - HL7 v2 to FHIR R4 transformation service"
LABEL org.opencontainers.image.source="https://github.com/your-org/fhir-bridge"
LABEL org.opencontainers.image.title="FHIR Bridge"
LABEL org.opencontainers.image.description="Healthcare interoperability service for HL7 v2 to FHIR R4 transformation"
LABEL org.opencontainers.image.version="0.1.0"