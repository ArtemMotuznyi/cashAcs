package ua.developer.artemmotuznyi.test

import kotlinx.coroutines.runBlocking
import ua.developer.artemmotuznyi.gmail.GmailService
import ua.developer.artemmotuznyi.gmail.SimpleTokenRepository

fun main() = runBlocking {
    println("Testing Gmail service in demo mode...")
    
    val tokenRepository = SimpleTokenRepository("secrets/master_key")
    val gmailService = GmailService(tokenRepository)
    
    // Test authentication
    println("Testing authentication...")
    val authResult = gmailService.authenticate()
    println("Authentication result: $authResult")
    
    // Test email retrieval
    println("Testing email retrieval...")
    val emails = gmailService.getEmails()
    println("Retrieved ${emails.size} emails:")
    emails.forEachIndexed { index, email ->
        println("${index + 1}. $email")
    }
}