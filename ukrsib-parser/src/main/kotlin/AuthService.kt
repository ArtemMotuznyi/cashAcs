package ua.developer.artemmotuznyi.ukrsibparser

import java.io.File

class AuthService {

    private val credentialsFile: File
        get() = File("secrets/admin_credentials")

    fun validateCredentials(username: String, password: String): Boolean {
        if (!credentialsFile.exists()) {
            return false
        }

        val credentials = credentialsFile.readText().lines()
        if (credentials.size != 2) {
            return false
        }

        val storedUsername = credentials[0].trim()
        val storedPassword = credentials[1].trim()

        return username == storedUsername && password == storedPassword
    }

}