# Production Security Configuration Guide

This document outlines the security measures implemented and required configurations for safe server deployment of the cashAcs application.

## 🔒 Security Improvements Implemented

### 1. Password Security
- **BCrypt password hashing**: Admin credentials are now hashed using BCrypt with work factor 12
- **Legacy support**: Automatically upgrades plaintext passwords to hashed versions on first login
- **Strong master key validation**: Ensures encryption keys are at least 32 characters long

### 2. OAuth Security
- **Configurable redirect URI**: Use `OAUTH_REDIRECT_URI` environment variable for server deployment
- **Input validation**: OAuth codes are validated for format and length

### 3. Network Security
- **Production Docker**: No debug ports exposed (port 5005 removed)
- **Security headers**: HSTS, CSP, X-Frame-Options, X-Content-Type-Options
- **Rate limiting**: Authentication endpoints limited to 5 attempts per minute per IP

### 4. Data Protection
- **Input sanitization**: Username/password length limits and character filtering
- **Secure logging**: Sensitive data removed from logs
- **PostgreSQL encryption**: OAuth tokens encrypted using pgcrypto extension

## 🚀 Production Deployment Requirements

### Environment Variables (Required)
```bash
# Database
DATABASE_URL=jdbc:postgresql://your-db-host:5432/ktor_db
DATABASE_USER=your_secure_user
DATABASE_PASSWORD=your_strong_password

# OAuth Configuration  
OAUTH_REDIRECT_URI=https://your-domain.com/oauth2callback
ALLOWED_HOST=your-domain.com

# Secret Files
ADMIN_CRED_FILE=/run/secrets/admin_credentials
CLIENT_SECRET=/run/secrets/client_secret
MAIL_TOKEN_MASTER_KEY=/run/secrets/master_key
```

### Secret Files Setup

1. **Admin Credentials** (`secrets/admin_credentials.txt`):
   ```
   admin_username
   $2a$12$hashed_password_will_be_generated_here
   ```

2. **Master Key** (`secrets/master_key.txt`):
   - Generate a strong 64+ character random key
   - Example: Use `openssl rand -hex 32` to generate

3. **Google OAuth** (`secrets/client_secret.json`):
   - Download from Google Cloud Console
   - Configure authorized redirect URIs to include your production domain

### Password Hashing Setup
To hash your admin password for initial deployment:

```bash
# Build the application
./gradlew build

# Run a temporary instance to hash password
java -cp build/libs/cashacs-all.jar -Dexec.mainClass="kotlin.script.templates.ScriptTemplateWithArgs" \
-Dexec.args="your_plain_password" \
./scripts/hash-password.kt
```

### Production Deployment Commands
```bash
# Production deployment (no debug ports, no pgAdmin)
docker-compose up -d app postgres-db

# Development with debug tools
docker-compose -f docker-compose.dev.yml up -d
```

## 🛡️ Security Checklist for Deployment

- [ ] Use HTTPS/TLS in production (configure reverse proxy)
- [ ] Generate strong master key (64+ characters)
- [ ] Hash admin password using BCrypt
- [ ] Set proper OAUTH_REDIRECT_URI for your domain
- [ ] Configure strong database passwords
- [ ] Restrict database access to application only
- [ ] Set up proper firewall rules
- [ ] Configure log rotation and secure log storage
- [ ] Monitor for security alerts and update dependencies regularly
- [ ] Remove pgAdmin service in production (use dev profile only)

## 🔧 Security Features

### Rate Limiting
- Authentication endpoints: 5 attempts per minute per IP
- Protects against brute force attacks

### Input Validation
- Username/password length limits (100 characters)
- OAuth code format validation
- Character filtering for log safety

### Encryption
- OAuth tokens encrypted in database using PostgreSQL pgcrypto
- Master key validation ensures adequate key strength
- BCrypt password hashing with work factor 12

### Security Headers
- **HSTS**: Enforces HTTPS connections
- **CSP**: Prevents XSS attacks
- **X-Frame-Options**: Prevents clickjacking
- **X-Content-Type-Options**: Prevents MIME type sniffing

## 📊 Security Monitoring

Monitor these security aspects in production:
- Failed authentication attempts
- Database connection security
- OAuth ua.developer.artemmotuznyi.token refresh patterns
- Rate limiting triggers
- Security header compliance