# ===================================================================
# Stage 1: Build and Extract Application Layers
# ===================================================================
FROM maven:3.9.9-eclipse-temurin-24 AS builder
WORKDIR /app

# Copy POM and download dependencies (heavily cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B -Ddependency-check.skip=true

# Copy source and build the application JAR
COPY src ./src
RUN mvn package -DskipTests -B -Ddependency-check.skip=true

# Extract the JAR into distinct layers using the modern 'tools' mode
# This replaces the deprecated 'layertools' mode and correctly structures
# the extracted contents for the new JarLauncher.
RUN java -Djarmode=tools -jar target/audioscholar-*.jar extract --layers --destination extracted

# ===================================================================
# Stage 2: Create the Final, Optimized Runtime Image
# ===================================================================
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Install font libraries (Required for Apache POI/PDFBox)
RUN apk add --no-cache fontconfig ttf-dejavu

# Copy the layers from the builder stage in order of least to most frequently changed.
# This maximizes Docker's caching capabilities.
COPY --from=builder /app/extracted/dependencies/ ./dependencies
COPY --from=builder /app/extracted/spring-boot-loader/ ./spring-boot-loader
COPY --from=builder /app/extracted/snapshot-dependencies/ ./snapshot-dependencies
COPY --from=builder /app/extracted/application/ ./application

# The ENTRYPOINT now uses the extracted layers to construct the classpath
# and calls the correct, modern JarLauncher class: org.springframework.boot.loader.launch.JarLauncher
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
  -XX:+UseG1GC \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:ActiveProcessorCount=1 \
  -Djava.awt.headless=true \
  -cp 'dependencies/*:snapshot-dependencies/*:spring-boot-loader/:application/' \
  org.springframework.boot.loader.launch.JarLauncher"]