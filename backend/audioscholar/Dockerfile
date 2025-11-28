# ===================================================================
# Stage 1: Build using Maven + Java 24
FROM maven:3.9-eclipse-temurin-24 AS builder
WORKDIR /app

# Copy POM and download dependencies (cacheable layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B -Ddependency-check.skip=true

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B -Ddependency-check.skip=true

# Stage 2: Run using lightweight Java 24 JRE (Alpine for optimization)
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Install font libraries (Required for Apache POI/PDFBox) - Alpine equivalent
RUN apk add --no-cache fontconfig ttf-dejavu

# Copy the built application
COPY --from=builder /app/target/audioscholar-*.jar app.jar

# Optimized ENTRYPOINT with Java 24 Compact Object Headers (Single Line)
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders", "-XX:+UseSerialGC", "-Xss256k", "-XX:MaxRAMPercentage=75.0", "-XX:ActiveProcessorCount=2", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]