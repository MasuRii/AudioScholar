# Build
FROM maven:3.9.9-eclipse-temurin-24 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Runtime – absolute smallest possible image that still works with POI/PDFBox
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Tiny font set – enough for headless PDF/Excel generation
RUN apk add --no-cache fontconfig ttf-dejavu msttcorefonts-installer && update-ms-fonts && fc-cache -f

COPY --from=builder /app/target/audioscholar-*.jar app.jar

# 512-MB-tuned, ultra-safe JVM flags
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=82.0 -XX:+UseZGC -XX:CompressedClassSpaceSize=12m -XX:ReservedCodeCacheSize=32m -Djava.awt.headless=true $JAVA_OPTS -jar app.jar"]