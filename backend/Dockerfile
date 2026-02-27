# Build Stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace/app

# 1. Copy only the files needed to download dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 2. Download dependencies (this layer will be cached unless build.gradle changes)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 3. Now copy the source code and build the JAR
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:TieredStopAtLevel=1", \
    "-jar", "app.jar"]
