# ===================================================================
# Stage 1: Build using Maven + Java 24 (Stable)
# ===================================================================
# Use the official Maven image with Eclipse Temurin Java 24
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
# Stage 2: Run using Java 24 JRE (Stable)
# ===================================================================
# REMOVED "-ea" from the tag because Java 24 is now GA
FROM eclipse-temurin:24-jre-jammy
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
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Xmx256m", "-jar", "app.jar"]