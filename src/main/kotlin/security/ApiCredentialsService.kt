package ua.developer.artemmotuznyi.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.io.File

/**
 * Service to manage API credentials for the 2 hardcoded users
 * Credentials are stored in /run/secrets/api_credentials file
 * Format: username:bcrypt_hash (one per line)
 */
class ApiCredentialsService {
    
    private val passwordEncoder = BCryptPasswordEncoder(12) // Strong work factor for security
    
    private val credentialsFile: File
        get() = File(System.getenv("API_CREDENTIALS_FILE") ?: "/run/secrets/api_credentials")
    
    // Cache for credentials to avoid file I/O on every request
    private var credentialsCache: Map<String, String>? = null
    private var lastModified: Long = 0
    
    /**
     * Load and cache API credentials from file
     * File format: username:bcrypt_hash (one per line)
     */
    private fun loadCredentials(): Map<String, String> {
        if (!credentialsFile.exists()) {
            throw IllegalStateException("API credentials file not found at: ${credentialsFile.path}")
        }
        
        val currentModified = credentialsFile.lastModified()
        
        // Return cached credentials if file hasn't changed
        if (credentialsCache != null && currentModified == lastModified) {
            return credentialsCache!!
        }
        
        val credentials = mutableMapOf<String, String>()
        
        credentialsFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val username = parts[0].trim()
                    val hash = parts[1].trim()
                    
                    // Validate username format (only alphanumeric and underscore)
                    if (username.matches(Regex("^[a-zA-Z0-9_]+$")) && username.length <= 50) {
                        credentials[username] = hash
                    }
                }
            }
        }
        
        if (credentials.isEmpty()) {
            throw IllegalStateException("No valid API credentials found in file")
        }
        
        if (credentials.size > 2) {
            throw IllegalStateException("Too many API users found. Maximum allowed: 2")
        }
        
        credentialsCache = credentials
        lastModified = currentModified
        
        return credentials
    }
    
    /**
     * Validate API user credentials
     * @param username The API username
     * @param password The plaintext password
     * @return true if credentials are valid, false otherwise
     */
    fun validateCredentials(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            return false
        }
        
        // Input validation
        if (username.length > 50 || password.length > 100) {
            return false
        }
        
        return try {
            val credentials = loadCredentials()
            val storedHash = credentials[username] ?: return false
            
            // Check if the stored password is hashed (BCrypt format)
            if (storedHash.startsWith("\$2a\$") || storedHash.startsWith("\$2b\$") || storedHash.startsWith("\$2y\$")) {
                passwordEncoder.matches(password, storedHash)
            } else {
                // Legacy plaintext password - should be hashed in production
                password == storedHash
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all valid API usernames
     */
    fun getValidUsernames(): Set<String> {
        return try {
            loadCredentials().keys
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Utility method to hash a password for setup
     */
    fun hashPassword(password: String): String {
        return passwordEncoder.encode(password)
    }
    
    /**
     * Check if a username is valid (exists in credentials file)
     */
    fun isValidUsername(username: String): Boolean {
        return try {
            loadCredentials().containsKey(username)
        } catch (e: Exception) {
            false
        }
    }
}