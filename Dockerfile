# Runtime stage: Using JDK 21 slim image
FROM openjdk:22-slim

# Set working directory
WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/cashacs-all.jar ./app.jar

# Expose the port that Ktor will use
# Відкриваємо HTTP-порт і порт для дебагу
EXPOSE 8080 5005
ENV PORT=8080

# Додаємо JDWP-агент для remote debug
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Command to run the application
CMD ["java", "-jar", "app.jar"]