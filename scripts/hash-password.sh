#!/bin/bash

# Password hashing utility for cashAcs application
# Usage: ./scripts/hash-password.sh <username> <password>

if [ $# -ne 2 ]; then
    echo "Usage: $0 <username> <password>"
    echo "Example: $0 admin mySecurePassword123"
    exit 1
fi

USERNAME="$1"
PASSWORD="$2"

# Validate inputs
if [ ${#USERNAME} -gt 100 ] || [ ${#PASSWORD} -gt 100 ]; then
    echo "Error: Username and password must be 100 characters or less"
    exit 1
fi

if [[ ! "$USERNAME" =~ ^[a-zA-Z0-9_.-]+$ ]]; then
    echo "Error: Username can only contain letters, numbers, dots, hyphens, and underscores"
    exit 1
fi

# Build the application if not already built
if [ ! -f "build/libs/cashacs-all.jar" ]; then
    echo "Building application..."
    ./gradlew build
fi

# Create temporary Kotlin script for password hashing
cat > /tmp/hash-password.kt << 'EOF'
@file:DependsOn("org.springframework.security:spring-security-crypto:6.2.1")

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: <username> <password>")
        return
    }
    
    val username = args[0]
    val password = args[1]
    val encoder = BCryptPasswordEncoder(12)
    val hashedPassword = encoder.encode(password)
    
    println("Hashed credentials for admin_credentials.txt:")
    println("$username")
    println("$hashedPassword")
    println()
    println("Copy the above two lines to your secrets/admin_credentials.txt file")
}
EOF

echo "Hashing password for user: $USERNAME"
kotlin /tmp/hash-password.kt "$USERNAME" "$PASSWORD"

# Clean up
rm -f /tmp/hash-password.kt