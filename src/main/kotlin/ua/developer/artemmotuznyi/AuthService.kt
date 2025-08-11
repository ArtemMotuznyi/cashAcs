package ua.developer.artemmotuznyi

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.io.File

class AuthService {
    private val passwordEncoder = BCryptPasswordEncoder(12) // Strong work factor for security

    private val credentialsFile: File
        get() = File(System.getenv("ADMIN_CRED_FILE") ?: "credentials.txt")

    fun validateCredentials(username: String, password: String): Boolean {
        if (!credentialsFile.exists()) {
            return false
        }

        val credentials = credentialsFile.readText().lines()
        if (credentials.size != 2) {
            return false
        }

        val storedUsername = credentials[0].trim()
        val storedPasswordHash = credentials[1].trim()

        // Check if stored password is already hashed (starts with $2a$ or $2b$)
        return if (storedPasswordHash.startsWith("\$2")) {
            // Stored password is hashed, compare with BCrypt
            username == storedUsername && passwordEncoder.matches(password, storedPasswordHash)
        } else {
            // Legacy plaintext password, validate and then hash it
            if (username == storedUsername && password == storedPasswordHash) {
                // Hash the plaintext password and update the file
                val hashedPassword = passwordEncoder.encode(password)
                credentialsFile.writeText("$storedUsername\n$hashedPassword")
                true
            } else {
                false
            }
        }
    }

    /**
     * Utility method to hash a password for initial setup
     * Call this during deployment to hash admin password
     */
    fun hashPassword(password: String): String {
        return passwordEncoder.encode(password)
    }
}