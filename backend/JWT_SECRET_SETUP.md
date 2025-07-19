# JWT Secret Key Setup Guide

## Overview
This application requires a JWT secret key to be configured as an environment variable. The secret key is used to sign and verify JWT tokens for authentication.

## Security Requirements
- The JWT secret **MUST** be at least 32 characters long
- The secret should be randomly generated and unique for each environment
- Never commit the secret key to version control
- Use different secrets for development, staging, and production environments

## Generating a Secure JWT Secret

### Option 1: Using OpenSSL (Recommended)
```bash
# Generate a 64-character random secret
openssl rand -hex 32

# Example output: a1b2c3d4e5f6789012345678901234567890abcdef123456789012345678901234
```

### Option 2: Using Python
```python
import secrets
import string

# Generate a 64-character random secret
secret = ''.join(secrets.choice(string.ascii_letters + string.digits) for _ in range(64))
print(secret)
```

### Option 3: Using Node.js
```javascript
// Generate a 64-character random secret
const crypto = require('crypto');
const secret = crypto.randomBytes(32).toString('hex');
console.log(secret);
```

## Configuration

### Environment Variable
Set the `JWT_SECRET` environment variable with your generated secret:

```bash
export JWT_SECRET=your-generated-secret-key-here
```

### Docker Environment
For Docker deployments, add to your docker-compose.yml:

```yaml
services:
  backend:
    environment:
      - JWT_SECRET=your-generated-secret-key-here
```

### Development Setup
For local development, create a `.env` file in the backend directory:

```bash
JWT_SECRET=your-development-secret-key-here
```

## Testing
The test environment has a pre-configured secret key for unit tests. You don't need to set JWT_SECRET for running tests.

## Troubleshooting

### Application Won't Start
If you see an error like:
```
JWT secret is not configured. Please set the 'jwt.secret' property...
```

This means the `JWT_SECRET` environment variable is not set. Follow the configuration steps above.

### Secret Too Short
If you see an error like:
```
JWT secret is too short. The secret must be at least 32 characters long...
```

Generate a longer secret using one of the methods above.

### Security Warnings
If you see a warning about the secret containing common words, generate a new random secret that doesn't contain words like "secret", "password", "key", etc.

## Best Practices
1. **Rotate secrets regularly** - Change the JWT secret periodically
2. **Use different secrets per environment** - Never use the same secret in development and production
3. **Store secrets securely** - Use a secrets management system in production
4. **Monitor for leaks** - Never log or expose the JWT secret in application logs