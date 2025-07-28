#!/bin/bash

# Build the application fat JAR
echo "Building the application..."
./gradlew buildFatJar

# Start the services using Docker Compose
echo "Starting services with Docker Compose..."
docker compose up --build