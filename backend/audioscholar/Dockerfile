# ===================================================================
# Stage 1: Build with Java 24 (Early Access)
# ===================================================================
# We use the JDK image because official Maven images for Java 24 don't exist yet
FROM eclipse-temurin:24-ea-jdk-jammy AS builder

WORKDIR /app

# 1. Install Maven manually inside the container
RUN apt-get update && \
    apt-get install -y maven

# 2. Copy dependency definitions
COPY pom.xml .

# 3. Download dependencies (Go offline)
# We add -Ddependency-check.skip=true to prevent OWASP check from slowing down build or failing on missing API key
RUN mvn dependency:go-offline -B -Ddependency-check.skip=true

# 4. Copy source code
COPY src ./src

# 5. Build the application
# We skip tests and the dependency check to ensure a fast build on Render
RUN mvn package -DskipTests -B -Ddependency-check.skip=true

# ===================================================================
# Stage 2: Run with Java 24 JRE
# ===================================================================
FROM eclipse-temurin:24-ea-jre-jammy
WORKDIR /app

# Install font libraries (Required for Apache POI and PDFBox which are in your pom)
RUN apt-get update && apt-get install -y \
    fontconfig \
    libfreetype6 \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# Copy the JAR from the builder stage
COPY --from=builder /app/target/audioscholar-*.jar app.jar

# Run the app
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Xmx256m", "-jar", "app.jar"]