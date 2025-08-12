package ua.developer.artemmotuznyi.test

import ua.developer.artemmotuznyi.security.ApiCredentialsService
import ua.developer.artemmotuznyi.security.JwtService

/**
 * Simple manual test for API security components
 * Run with: ./gradlew run --args="test-api"
 */
fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] != "test-api") {
        println("Usage: ./gradlew run --args=\"test-api\"")
        return
    }
    
    println("🔒 Testing API Security Components")
    println("=" * 50)
    
    // Set environment variables for testing
    System.setProperty("API_CREDENTIALS_FILE", "/tmp/test-secrets/api_credentials")
    System.setProperty("JWT_SECRET_FILE", "/tmp/test-secrets/jwt_secret")
    
    try {
        // Test API Credentials Service
        println("\n1. Testing API Credentials Service...")
        val credentialsService = ApiCredentialsService()
        
        // Test password hashing
        val testPassword = "test123"
        val hashedPassword = credentialsService.hashPassword(testPassword)
        println("✅ Password hashing: Generated hash $hashedPassword")
        
        // Test valid usernames
        val validUsernames = credentialsService.getValidUsernames()
        println("✅ Valid usernames loaded: $validUsernames")
        
        // Test credential validation (these will fail because we used dummy hashes)
        val isValid1 = credentialsService.validateCredentials("test_user_1", "test123")
        val isValid2 = credentialsService.validateCredentials("invalid_user", "test123")
        println("✅ Credential validation test: valid user = $isValid1, invalid user = $isValid2")
        
        // Test JWT Service
        println("\n2. Testing JWT Service...")
        val jwtService = JwtService()
        
        // Generate tokens
        val accessToken = jwtService.generateToken("test_user_1", 1) // 1 hour
        val refreshToken = jwtService.generateRefreshToken("test_user_1")
        println("✅ Token generation: access token length = ${accessToken.length}")
        println("✅ Token generation: refresh token length = ${refreshToken.length}")
        
        // Validate tokens
        val accessValidation = jwtService.validateToken(accessToken)
        val refreshValidation = jwtService.validateToken(refreshToken)
        
        if (accessValidation != null) {
            println("✅ Access token validation: user = ${accessValidation.userId}, type = ${accessValidation.tokenType}")
        } else {
            println("❌ Access token validation failed")
        }
        
        if (refreshValidation != null) {
            println("✅ Refresh token validation: user = ${refreshValidation.userId}, type = ${refreshValidation.tokenType}")
        } else {
            println("❌ Refresh token validation failed")
        }
        
        // Test invalid token
        val invalidValidation = jwtService.validateToken("invalid.token.here")
        println("✅ Invalid token validation: result = $invalidValidation")
        
        println("\n✅ All security components initialized successfully!")
        println("\n📋 Next steps for production:")
        println("1. Generate real BCrypt hashes for your API users")
        println("2. Create strong JWT secret (32+ characters)")  
        println("3. Set up proper /run/secrets directory")
        println("4. Configure environment variables")
        println("5. Test with actual API calls")
        
    } catch (e: Exception) {
        println("❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
}

private operator fun String.times(count: Int) = repeat(count)