# Build stage: Using Gradle with JDK 21
FROM gradle:8.6-jdk21 AS build

# Set working directory
WORKDIR /app

# Copy Gradle configuration files
COPY gradle ./gradle

# This is done before copying the source code to leverage Docker's layer caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy the source code
COPY src ./src

# Build the application
# The --no-daemon flag ensures Gradle doesn't start a background process
RUN gradle installDist --no-daemon

# Runtime stage: Using a lightweight JDK 21 image
FROM openjdk:21-slim

# Set working directory
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/build/install/ ./

# Copy the correct jar file into the container
COPY target/cashacs.jar app.jar

# Expose the port that Ktor will use (adjust according to your configuration)
EXPOSE 8080

# Define the port environment variable (can be used in your Ktor configuration)
ENV PORT=8080

# Command to run the application
CMD ["app/bin/app"]
