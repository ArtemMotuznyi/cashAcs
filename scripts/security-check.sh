#!/bin/bash

# Security deployment checklist script
# Run this before deploying to production

echo "🔒 Security Deployment Checklist for cashAcs"
echo "=============================================="
echo

# Check if secrets directory exists
if [ ! -d "secrets" ]; then
    echo "❌ CRITICAL: secrets/ directory not found"
    echo "   Create secrets/ directory and add required files"
    exit 1
else
    echo "✅ Secrets directory exists"
fi

# Check admin credentials
if [ ! -f "secrets/admin_credentials.txt" ]; then
    echo "❌ CRITICAL: secrets/admin_credentials.txt not found"
    echo "   Use ./scripts/hash-password.sh to create hashed credentials"
    exit 1
else
    echo "✅ Admin credentials file exists"
    
    # Check if password is hashed
    if grep -q '^\$2[ab]\$' secrets/admin_credentials.txt; then
        echo "✅ Admin password is properly hashed"
    else
        echo "⚠️  WARNING: Admin password appears to be plaintext"
        echo "   Use ./scripts/hash-password.sh to hash the password"
    fi
fi

# Check master key
if [ ! -f "secrets/master_key.txt" ]; then
    echo "❌ CRITICAL: secrets/master_key.txt not found"
    echo "   Generate with: openssl rand -hex 32 > secrets/master_key.txt"
    exit 1
else
    echo "✅ Master key file exists"
    
    # Check key length
    KEY_LENGTH=$(wc -c < secrets/master_key.txt)
    if [ $KEY_LENGTH -lt 32 ]; then
        echo "❌ CRITICAL: Master key is too short ($KEY_LENGTH chars)"
        echo "   Generate a longer key: openssl rand -hex 32 > secrets/master_key.txt"
        exit 1
    else
        echo "✅ Master key has adequate length ($KEY_LENGTH chars)"
    fi
fi

# Check Google OAuth credentials
if [ ! -f "secrets/client_secret.json" ]; then
    echo "❌ CRITICAL: secrets/client_secret.json not found"
    echo "   Download from Google Cloud Console"
    exit 1
else
    echo "✅ Google OAuth credentials file exists"
fi

# Check environment variables
echo
echo "🌍 Environment Variable Checklist:"

required_vars=(
    "DATABASE_URL"
    "DATABASE_USER"
    "DATABASE_PASSWORD"
    "OAUTH_REDIRECT_URI"
    "ALLOWED_HOST"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ $var not set"
    else
        echo "✅ $var is set"
    fi
done

# Check if HTTPS is configured
if [[ "${OAUTH_REDIRECT_URI:-}" == https://* ]]; then
    echo "✅ HTTPS redirect URI configured"
else
    echo "⚠️  WARNING: OAUTH_REDIRECT_URI should use HTTPS in production"
fi

echo
echo "📋 Additional Production Checklist:"
echo "- [ ] Reverse proxy configured with SSL/TLS termination"
echo "- [ ] Firewall rules restrict database access"
echo "- [ ] Log rotation configured"
echo "- [ ] Monitoring and alerting set up"
echo "- [ ] Regular security updates scheduled"
echo "- [ ] Backup strategy implemented"
echo
echo "🚀 If all checks pass, you're ready for production deployment!"