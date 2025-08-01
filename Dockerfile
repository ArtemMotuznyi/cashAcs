# Runtime stage: Using JDK 21 slim image
FROM openjdk:22-slim

# Set working directory
WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/cashacs-all.jar ./app.jar

# Expose the port that Ktor will use
EXPOSE 8080

# Define the port environment variable
ENV PORT=8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]