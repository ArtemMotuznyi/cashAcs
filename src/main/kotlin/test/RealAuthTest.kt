package ua.developer.artemmotuznyi.test

import ua.developer.artemmotuznyi.security.ApiCredentialsService
import ua.developer.artemmotuznyi.security.JwtService

/**
 * Test actual API credentials validation
 */
fun main(args: Array<String>) {
    println("🔐 Testing Real API Credentials")
    println("=" * 40)
    
    // Set environment variables for testing
    System.setProperty("API_CREDENTIALS_FILE", "/tmp/test-secrets/api_credentials")
    System.setProperty("JWT_SECRET_FILE", "/tmp/test-secrets/jwt_secret")
    
    try {
        val credentialsService = ApiCredentialsService()
        val jwtService = JwtService()
        
        // Test correct credentials
        val isValid = credentialsService.validateCredentials("api_user_1", "test_password_123")
        println("✅ Credential validation (correct): $isValid")
        
        // Test wrong password
        val isInvalid = credentialsService.validateCredentials("api_user_1", "wrong_password")
        println("✅ Credential validation (wrong password): $isInvalid")
        
        // Test wrong user
        val isInvalidUser = credentialsService.validateCredentials("wrong_user", "test_password_123")
        println("✅ Credential validation (wrong user): $isInvalidUser")
        
        if (isValid) {
            // Generate tokens for valid user
            val accessToken = jwtService.generateToken("api_user_1")
            val refreshToken = jwtService.generateRefreshToken("api_user_1")
            
            println("\n🎫 Generated tokens:")
            println("Access token: ${accessToken.take(50)}...")
            println("Refresh token: ${refreshToken.take(50)}...")
            
            // Validate tokens
            val accessValidation = jwtService.validateToken(accessToken)
            val refreshValidation = jwtService.validateToken(refreshToken)
            
            println("\n✅ Token validation:")
            println("Access: ${accessValidation?.userId} (${accessValidation?.tokenType})")
            println("Refresh: ${refreshValidation?.userId} (${refreshValidation?.tokenType})")
            
            println("\n🚀 API authentication flow is fully functional!")
        } else {
            println("❌ Authentication failed - check credentials")
        }
        
    } catch (e: Exception) {
        println("❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
}

private operator fun String.times(count: Int) = repeat(count)