#!/bin/bash

# Script to hash API user passwords using BCrypt
# Usage: ./hash-api-password.sh username password

if [ $# -ne 2 ]; then
    echo "Usage: $0 <username> <password>"
    echo "Example: $0 api_user_1 'my_secure_password'"
    exit 1
fi

USERNAME="$1"
PASSWORD="$2"

# Validate username format
if [[ ! "$USERNAME" =~ ^[a-zA-Z0-9_]+$ ]]; then
    echo "Error: Username must contain only alphanumeric characters and underscores"
    exit 1
fi

# Check if project is built
if [ ! -f "build/libs/cashacs-0.0.1-all.jar" ]; then
    echo "Building project..."
    ./gradlew build
fi

# Use a temporary Kotlin script to hash the password
cat > /tmp/hash_password.kt << 'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

val encoder = BCryptPasswordEncoder(12)
val args = System.getProperty("exec.args")?.split(" ") ?: emptyList()

if (args.size >= 2) {
    val username = args[0]
    val password = args[1]
    val hash = encoder.encode(password)
    println("$username:$hash")
} else {
    println("Error: Missing arguments")
    System.exit(1)
}
EOF

echo "Hashing password for user '$USERNAME'..."

# Run the script
java -cp "build/libs/cashacs-0.0.1-all.jar" \
     -Dexec.args="$USERNAME $PASSWORD" \
     kotlin.script.templates.standard.ScriptTemplateWithArgs \
     /tmp/hash_password.kt

# Clean up
rm -f /tmp/hash_password.kt

echo ""
echo "Add this line to your /run/secrets/api_credentials file:"