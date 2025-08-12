# cashacs

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                               | Description                                                 |
| ----------------------------------------------------|------------------------------------------------------------- |
| [Routing](https://start.ktor.io/p/routing-default) | Allows to define structured routes and associated handlers. |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

## Security & Production Deployment

⚠️ **IMPORTANT**: This application handles sensitive financial data and OAuth tokens. Please review the security documentation before deployment.

### Quick Security Setup
1. **Review security documentation**: See [SECURITY.md](SECURITY.md) for detailed security guide
2. **Run security check**: `./scripts/security-check.sh`
3. **Hash admin password**: `./scripts/hash-password.sh username password`
4. **Production deployment**: `docker-compose up -d app postgres-db`
5. **Development with debug**: `docker-compose -f docker-compose.dev.yml up -d`

### Security Features Implemented
- 🔐 BCrypt password hashing for admin credentials
- 🛡️ Rate limiting on authentication endpoints (5 attempts/minute)
- 🔒 PostgreSQL pgcrypto encryption for OAuth tokens
- 🌐 Configurable OAuth redirect URIs for server deployment
- 📡 Security headers (HSTS, CSP, X-Frame-Options, etc.)
- 🚫 Input validation and sanitization
- 🏗️ Production Docker configuration without debug ports

### Critical Environment Variables
```bash
OAUTH_REDIRECT_URI=https://your-domain.com/oauth2callback
ALLOWED_HOST=your-domain.com
DATABASE_PASSWORD=your_strong_password
```

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

