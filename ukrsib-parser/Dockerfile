FROM openjdk:22-slim

WORKDIR /app

# Копіюємо зібраний JAR парсера (fat JAR з усіма залежностями)
COPY build/libs/ukrsib-parser-all.jar ./ukrsib-parser.jar

EXPOSE 8081
ENV PORT=8081

CMD ["java", "-jar", "ukrsib-parser.jar"]