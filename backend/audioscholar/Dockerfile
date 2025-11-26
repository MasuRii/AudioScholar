# ===================================================================
# Stage 1: Build using Maven + Java 24
# ===================================================================
# The build stage worked in your logs, so we keep this exact tag
FROM maven:3.9.9-eclipse-temurin-24 AS builder
WORKDIR /app

# 1. Copy POM and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -Ddependency-check.skip=true

# 2. Copy source and build
COPY src ./src
# Build the application
RUN mvn package -DskipTests -B -Ddependency-check.skip=true

# ===================================================================
# Stage 2: Run using Java 24 JRE
# ===================================================================
# FIXED: Removed "-jammy". We rely on the default tag for Java 24 JRE.
FROM eclipse-temurin:24-jre
WORKDIR /app

# Install font libraries (Required for Apache POI/PDFBox)
RUN apt-get update && apt-get install -y \
    fontconfig \
    libfreetype6 \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# Copy the built application
COPY --from=builder /app/target/audioscholar-*.jar app.jar

# Run the application
ENTRYPOINT ["sh", "-c", "java -XX:+UseSerialGC -XX:ActiveProcessorCount=1 $JAVA_OPTS -Djava.awt.headless=true -jar app.jar"]