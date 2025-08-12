# Secure API Documentation

This document describes the secure API implementation for mobile and Telegram bot access to the cashAcs application.

## Overview

The API provides JWT-based authentication for 2 hardcoded users with secure endpoints for accessing financial data. All secrets are managed through files stored in `/run/secrets` for maximum security.

## Security Features

- **JWT Authentication**: Secure token-based authentication with configurable expiration
- **Hardcoded Credentials**: Maximum 2 API users with BCrypt-hashed passwords 
- **Rate Limiting**: Protection against brute force attacks (10 attempts/minute)
- **Secure Secret Management**: All secrets stored in `/run/secrets` directory
- **Input Validation**: Comprehensive validation and sanitization
- **Secure Logging**: No sensitive data exposure in logs

## API Endpoints

### Authentication Endpoints

#### POST /api/v1/login
Authenticate and receive JWT tokens.

**Request:**
```json
{
    "username": "api_user_1",
    "password": "your_password"
}
```

**Response (Success):**
```json
{
    "success": true,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "tokenType": "Bearer",
    "message": "Login successful"
}
```

**Response (Error):**
```json
{
    "error": "invalid_credentials",
    "message": "Invalid username or password"
}
```

#### POST /api/v1/refresh
Refresh access token using refresh token.

**Request:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (Success):**
```json
{
    "success": true,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
}
```

### Protected Endpoints

All protected endpoints require the `Authorization: Bearer <token>` header.

#### GET /api/v1/status
Check authentication status and token information.

**Response:**
```json
{
    "status": "authenticated",
    "user": "api_user_1",
    "tokenExpiresAt": "2024-12-05T14:30:00Z"
}
```

#### GET /api/v1/cash
Get current exchange rates and financial data.

**Response:**
```json
{
    "cashValues": [
        {
            "provider": "urksib",
            "currencyTitle": "USD",
            "value": 41.50
        },
        {
            "provider": "urksib", 
            "currencyTitle": "EUR",
            "value": 45.20
        }
    ]
}
```

## Setup Instructions

### 1. Create Secret Files

Create the required secret files in your deployment:

```bash
# Create secrets directory
mkdir -p /run/secrets

# API credentials (max 2 users)
echo "api_user_1:$2a$12$hashed_password_here" > /run/secrets/api_credentials
echo "api_user_2:$2a$12$another_hashed_password" >> /run/secrets/api_credentials

# JWT secret (32+ characters)
openssl rand -hex 32 > /run/secrets/jwt_secret

# Set secure permissions
chmod 600 /run/secrets/api_credentials
chmod 600 /run/secrets/jwt_secret
```

### 2. Hash Passwords

Use the built-in password hasher utility:

```bash
# Build the project
./gradlew build

# Hash a password for API user
./gradlew hashPassword --args="api_user_1 my_secure_password"
```

### 3. Environment Configuration

Update your environment file with the new API settings:

```bash
# API Security Configuration
API_CREDENTIALS_FILE=/run/secrets/api_credentials
JWT_SECRET_FILE=/run/secrets/jwt_secret
```

### 4. Client Implementation Examples

#### Curl Example
```bash
# Login
curl -X POST http://localhost:8080/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username": "api_user_1", "password": "your_password"}'

# Use access token
curl -X GET http://localhost:8080/api/v1/cash \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### Python Example
```python
import requests
import json

# Login and get token
login_response = requests.post('http://localhost:8080/api/v1/login', 
    json={'username': 'api_user_1', 'password': 'your_password'})
    
if login_response.status_code == 200:
    tokens = login_response.json()
    access_token = tokens['accessToken']
    
    # Use token for API calls
    headers = {'Authorization': f'Bearer {access_token}'}
    cash_response = requests.get('http://localhost:8080/api/v1/cash', headers=headers)
    
    if cash_response.status_code == 200:
        cash_data = cash_response.json()
        print(json.dumps(cash_data, indent=2))
```

#### Node.js/Telegram Bot Example
```javascript
const axios = require('axios');

class CashAcsClient {
    constructor(baseUrl, username, password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.accessToken = null;
        this.refreshToken = null;
    }
    
    async login() {
        try {
            const response = await axios.post(`${this.baseUrl}/api/v1/login`, {
                username: this.username,
                password: this.password
            });
            
            this.accessToken = response.data.accessToken;
            this.refreshToken = response.data.refreshToken;
            
            return true;
        } catch (error) {
            console.error('Login failed:', error.response?.data);
            return false;
        }
    }
    
    async getCashData() {
        if (!this.accessToken) {
            await this.login();
        }
        
        try {
            const response = await axios.get(`${this.baseUrl}/api/v1/cash`, {
                headers: { Authorization: `Bearer ${this.accessToken}` }
            });
            
            return response.data;
        } catch (error) {
            if (error.response?.status === 401) {
                // Token expired, try to refresh
                await this.refreshAccessToken();
                return this.getCashData(); // Retry
            }
            throw error;
        }
    }
    
    async refreshAccessToken() {
        try {
            const response = await axios.post(`${this.baseUrl}/api/v1/refresh`, {
                refreshToken: this.refreshToken
            });
            
            this.accessToken = response.data.accessToken;
        } catch (error) {
            // Refresh failed, need to login again
            await this.login();
        }
    }
}

// Usage in Telegram bot
const client = new CashAcsClient('http://localhost:8080', 'api_user_1', 'your_password');

bot.on('message', async (msg) => {
    if (msg.text === '/rates') {
        try {
            const cashData = await client.getCashData();
            const rates = cashData.cashValues
                .map(rate => `${rate.currencyTitle}: ${rate.value}`)
                .join('\n');
            bot.sendMessage(msg.chat.id, `Current rates:\n${rates}`);
        } catch (error) {
            bot.sendMessage(msg.chat.id, 'Sorry, could not fetch rates');
        }
    }
});
```

## Security Considerations

### Token Management
- **Access tokens** expire in 24 hours
- **Refresh tokens** expire in 7 days  
- Store tokens securely on client side
- Implement automatic token refresh
- Handle token expiration gracefully

### Rate Limiting
- Login endpoint: 10 attempts per minute per IP
- Implement exponential backoff on client side
- Monitor for repeated failures

### Best Practices
- Use HTTPS in production
- Rotate JWT secret regularly
- Monitor authentication logs
- Set secure file permissions on secret files
- Never log passwords or tokens
- Implement proper error handling

### Mobile App Considerations
- Store tokens in secure keychain/keystore
- Implement certificate pinning
- Use app-specific authentication if needed
- Handle network connectivity issues

### Telegram Bot Considerations
- Protect bot token
- Validate user permissions
- Implement rate limiting per user
- Log bot interactions for monitoring

## Monitoring and Maintenance

### Security Monitoring
- Failed authentication attempts
- Token refresh patterns
- Rate limiting triggers
- Unusual access patterns

### Maintenance Tasks
- Regular secret rotation
- Monitor token expiration patterns
- Update dependencies for security patches
- Review and audit API access logs

## Error Codes

| Code | Error | Description |
|------|-------|-------------|
| 400 | invalid_request | Missing or malformed request |
| 401 | invalid_credentials | Invalid username/password |
| 401 | invalid_token | Invalid or expired token |
| 401 | invalid_user | User no longer valid |
| 429 | rate_limit_exceeded | Too many requests |
| 500 | internal_error | Server error |
| 503 | service_unavailable | Gmail service unavailable |