# Build Stage
FROM ghcr.io/graalvm/native-image-community:17-ol9 AS builder
WORKDIR /workspace/app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Grant execution rights on the gradlew script
RUN chmod +x ./gradlew

# Install xargs (findutils) which is required by gradlew
RUN microdnf install -y findutils binutils

# Copy source code
COPY src src

# Build Native Image
# This will produce a native executable in build/native/nativeCompile/
RUN ./gradlew nativeCompile -x test --no-daemon

# Runtime Stage: Use a tiny base image (even smaller than JRE)
FROM debian:bookworm-slim
WORKDIR /app

# Install necessary libraries for native executable (like glibc/zlib)
RUN apt-get update && apt-get install -y libstdc++6 zlib1g && rm -rf /var/lib/apt/lists/*

# Copy the native binary from the builder stage
COPY --from=builder /workspace/app/build/native/nativeCompile/tech-market-backend tech-market-backend

# Run the native binary
ENTRYPOINT ["./tech-market-backend"]
