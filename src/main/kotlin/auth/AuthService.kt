package ua.developer.artemmotuznyi.auth

import java.io.File

open class AuthService {
    open val credentialsFile = File("secrets/admin_credentials")
    
    fun validateCredentials(username: String, password: String): Boolean {
        if (!credentialsFile.exists()) {
            return false
        }
        
        val credentials = credentialsFile.readText().trim()
        val expectedCredentials = "$username:$password"
        
        return credentials == expectedCredentials
    }
}