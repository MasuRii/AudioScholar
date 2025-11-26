# Build (unchanged – still fast and cached)
FROM maven:3.9.9-eclipse-temurin-24 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Runtime – stripped to the bone (no fonts, no AWT, no native libs)
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Literally nothing else to install – no fonts, no freetype, no msttcorefonts
# (You removed POI/PDFBox → zero need)

COPY --from=builder /app/target/audioscholar-*.jar app.jar

# Ultra-tuned for 512 MB Render + pure API workload
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=85.0 -XX:+UseZGC -XX:CompressedClassSpaceSize=10m -XX:ReservedCodeCacheSize=24m -Djava.awt.headless=true $JAVA_OPTS -jar app.jar"]