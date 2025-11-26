# ===================================================================
# Stage 1: Build using Maven + Java 24
FROM maven:3.9.9-eclipse-temurin-24 AS builder
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

# Optimized ENTRYPOINT with container-aware JVM flags
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:ActiveProcessorCount=1 $JAVA_OPTS -Djava.awt.headless=true -jar app.jar"]