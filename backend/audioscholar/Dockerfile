FROM maven:3.9.9-eclipse-temurin-24 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/audioscholar-*.jar app.jar

ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseZGC -Djava.awt.headless=true $JAVA_OPTS -jar app.jar"]