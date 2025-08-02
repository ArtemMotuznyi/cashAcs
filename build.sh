#!/bin/bash
set -e

# Build the project using Gradle Wrapper
./gradlew clean build

echo "Build completed successfully. Artifacts are in the build/libs directory."

# Remove old Docker images and containers for this project
COMPOSE_PROJECT_NAME="cashacs"
docker-compose down --remove-orphans

echo "Old containers stopped and removed."

echo "Building and starting new containers..."
docker-compose up --build

echo "Docker Compose started with the new build."
