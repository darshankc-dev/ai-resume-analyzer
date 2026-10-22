# ===================================================================
# Stage 1: Build Stage (Maven + OpenJDK 17)
# ===================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy Maven descriptor to cache dependencies
COPY pom.xml .
RUN mvn dependency:resolve -B

# Copy application source code
COPY src ./src

# Package executable Spring Boot JAR (tests are run separately in CI)
RUN mvn clean package -DskipTests && \
    cp target/ai-resume-analyzer-*.jar target/app.jar

# ===================================================================
# Stage 2: Runtime Stage (Lightweight Eclipse Temurin 17 JRE Alpine)
# ===================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install fontconfig and fonts for Apache PDFBox text extraction
RUN apk add --no-cache fontconfig ttf-dejavu

# Create a non-privileged user and group for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy executable JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/app.jar app.jar

# Default PORT environment variable (Render sets this dynamically at runtime)
ENV PORT=8080
EXPOSE 8080

# Execute Spring Boot application, dynamically binding to $PORT provided by Render
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]
