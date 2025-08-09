# Production stage: Using JDK 17 slim image (matching build toolchain)
FROM openjdk:17-slim

# Set working directory
WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/cashacs-all.jar ./app.jar

# Expose only the application port (no debug port for production)
EXPOSE 8080
ENV PORT=8080

# Production environment settings
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Command to run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]