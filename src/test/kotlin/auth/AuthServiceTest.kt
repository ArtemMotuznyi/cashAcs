package ua.developer.artemmotuznyi.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.File

class AuthServiceTest {
    
    @Test
    fun testValidCredentials() {
        // Create a temporary credentials file for testing
        val tempCredsFile = File.createTempFile("test_creds", ".txt")
        tempCredsFile.writeText("admin:password123")
        tempCredsFile.deleteOnExit()
        
        // Mock the AuthService to use our temp file
        val authService = object : AuthService() {
            override val credentialsFile = tempCredsFile
        }
        
        assertTrue(authService.validateCredentials("admin", "password123"))
        assertFalse(authService.validateCredentials("admin", "wrongpassword"))
        assertFalse(authService.validateCredentials("wronguser", "password123"))
    }
    
    @Test
    fun testNonExistentCredentialsFile() {
        val authService = object : AuthService() {
            override val credentialsFile = File("nonexistent_file.txt")
        }
        
        assertFalse(authService.validateCredentials("admin", "password123"))
    }
}