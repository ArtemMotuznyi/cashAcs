# Use the official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the fat JAR file into the container
COPY build/libs/cashacs-all.jar app.jar

# Expose the default Ktor port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]