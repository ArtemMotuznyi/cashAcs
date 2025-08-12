package ua.developer.artemmotuznyi.tools

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Utility to hash passwords for API credentials
 * Run with: ./gradlew run --args="hash-password username password"
 */
fun main(args: Array<String>) {
    if (args.size < 3 || args[0] != "hash-password") {
        println("Usage: ./gradlew run --args=\"hash-password <username> <password>\"")
        println("Example: ./gradlew run --args=\"hash-password api_user_1 my_secure_password\"")
        return
    }
    
    val username = args[1]
    val password = args[2]
    
    // Validate username format
    if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
        println("Error: Username must contain only alphanumeric characters and underscores")
        return
    }
    
    if (username.length > 50) {
        println("Error: Username must be 50 characters or less")
        return
    }
    
    val encoder = BCryptPasswordEncoder(12)
    val hash = encoder.encode(password)
    
    println("\nBCrypt hash generated successfully!")
    println("Add this line to your /run/secrets/api_credentials file:")
    println("$username:$hash")
    println("\nSecurity note: Store this file securely and set appropriate permissions (600)")
}