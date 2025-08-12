#!/bin/bash

# Quick Production Deployment Guide for cashAcs
echo "🚀 CashAcs Production Deployment Guide"
echo "======================================="
echo

echo "📋 Pre-deployment Steps:"
echo "1. Run security check: ./scripts/security-check.sh"
echo "2. Create secrets directory: mkdir -p secrets"
echo "3. Generate master key: openssl rand -hex 32 > secrets/master_key.txt" 
echo "4. Hash admin password: ./scripts/hash-password.sh admin your_password"
echo "5. Download Google OAuth credentials to secrets/client_secret.json"
echo "6. Set environment variables (see .env.production.template)"
echo

echo "🔧 Environment Setup:"
echo "export OAUTH_REDIRECT_URI=https://your-domain.com/oauth2callback"
echo "export ALLOWED_HOST=your-domain.com"
echo "export DATABASE_PASSWORD=your_strong_db_password"
echo

echo "🐳 Deployment Commands:"
echo "# Build the application"
echo "./gradlew build"
echo
echo "# Production deployment (secure)"
echo "docker-compose up -d app postgres-db"
echo
echo "# Development with debug tools"
echo "docker-compose -f docker-compose.dev.yml up -d"
echo

echo "✅ Security Features Active:"
echo "- BCrypt password hashing (work factor 12)"
echo "- Rate limiting (5 auth attempts/minute per IP)"
echo "- PostgreSQL token encryption with pgcrypto"
echo "- Security headers (HSTS, CSP, X-Frame-Options)"
echo "- Input validation and sanitization"
echo "- Production Docker without debug ports"
echo

echo "📖 For detailed information, see:"
echo "- SECURITY.md - Complete security documentation"
echo "- SECURITY-SUMMARY.md - Security analysis summary"
echo

echo "🎯 Your application is now SECURE and ready for production deployment!"