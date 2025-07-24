# FHIR Bridge Authentication Setup Guide

## Overview
This guide provides comprehensive instructions for setting up authentication for testing the FHIR Bridge API, including JWT token generation, user management, and security configuration.

## Authentication Architecture

### JWT Token Structure
```json
{
  "sub": "test.user@healthsystem.com",
  "iss": "fhir-bridge-auth-service",
  "aud": "fhir-bridge-api",
  "exp": 1721836800,
  "iat": 1721833200,
  "scope": "fhir:read fhir:write hl7:transform",
  "organization": "TEST_HEALTH_SYSTEM",
  "roles": ["HL7_TRANSFORMER", "FHIR_READER", "AUDIT_VIEWER"]
}
```

## Quick Setup (5 minutes)

### 1. Generate Test JWT Token
```bash
# Using the provided test script
./scripts/generate-test-token.sh

# Or manually with curl
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.user@healthsystem.com",
    "password": "TestPass123!",
    "organization": "TEST_HEALTH_SYSTEM"
  }'
```

### 2. Set Token in Postman
1. Open Postman
2. Go to **Environments** → **FHIR Bridge Testing Environment**
3. Update `jwt_token` variable with generated token
4. Token will auto-refresh every 30 minutes

## Detailed Authentication Setup

### 3.1 User Registration

#### Create Test User
```bash
# Create organization first
curl -X POST http://localhost:8080/api/v1/auth/organizations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "TEST_HEALTH_SYSTEM",
    "displayName": "Test Health System",
    "domain": "test-health-system.com"
  }'

# Create test user
curl -X POST http://localhost:8080/api/v1/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.user@healthsystem.com",
    "email": "test.user@healthsystem.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User",
    "organization": "TEST_HEALTH_SYSTEM",
    "roles": ["HL7_TRANSFORMER", "FHIR_READER"]
  }'
```

### 3.2 Role-Based Access Control

#### Available Roles
| Role | Permissions | Description |
|------|-------------|-------------|
| `HL7_TRANSFORMER` | POST /api/v1/transform/* | Transform HL7 to FHIR |
| `FHIR_READER` | GET /fhir/* | Read FHIR resources |
| `FHIR_WRITER` | POST/PUT /fhir/* | Write FHIR resources |
| `AUDIT_VIEWER` | GET /api/v1/audit/* | View audit logs |
| `ADMIN` | All endpoints | Full system access |

#### Assign Multiple Roles
```bash
curl -X PUT http://localhost:8080/api/v1/auth/users/test.user@healthsystem.com/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "roles": ["HL7_TRANSFORMER", "FHIR_READER", "AUDIT_VIEWER"]
  }'
```

### 3.3 Token Management

#### Token Expiration
- **Access Token**: 30 minutes
- **Refresh Token**: 7 days
- **Auto-refresh**: Enabled in Postman collection

#### Refresh Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

#### Revoke Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Security Configuration

### 4.1 Environment Variables
```bash
# Required for authentication
export JWT_SECRET=your-secret-key-here
export JWT_EXPIRATION=1800
export REFRESH_TOKEN_EXPIRATION=604800

# Database connection
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=fhir_bridge
export DB_USER=fhir_bridge_user
export DB_PASSWORD=secure-password
```

### 4.2 SSL/TLS Configuration
```bash
# For production environments
export SSL_ENABLED=true
export SSL_KEYSTORE_PATH=/path/to/keystore.jks
export SSL_KEYSTORE_PASSWORD=keystore-password
export SSL_TRUSTSTORE_PATH=/path/to/truststore.jks
export SSL_TRUSTSTORE_PASSWORD=truststore-password
```

## Testing Authentication

### 5.1 Valid Token Test
```bash
# Test with valid token
curl -H "Authorization: Bearer YOUR_VALID_TOKEN" \
     http://localhost:8080/api/v1/transform/hl7v2-to-fhir
```

### 5.2 Invalid Token Test
```bash
# Test with invalid token
curl -H "Authorization: Bearer invalid_token" \
     http://localhost:8080/api/v1/transform/hl7v2-to-fhir
# Expected: 401 Unauthorized
```

### 5.3 Expired Token Test
```bash
# Test with expired token
curl -H "Authorization: Bearer expired_token" \
     http://localhost:8080/api/v1/transform/hl7v2-to-fhir
# Expected: 401 Unauthorized with "Token expired" message
```

## Postman Configuration

### 6.1 Environment Variables
```json
{
  "base_url": "http://localhost:8080",
  "jwt_token": "your-jwt-token-here",
  "refresh_token": "your-refresh-token-here",
  "test_username": "test.user@healthsystem.com",
  "test_password": "TestPass123!",
  "organization": "TEST_HEALTH_SYSTEM"
}
```

### 6.2 Pre-request Script
```javascript
// Auto-refresh token if expired
const token = pm.environment.get('jwt_token');
if (!token || isTokenExpired(token)) {
    pm.sendRequest({
        url: pm.environment.get('base_url') + '/api/v1/auth/refresh',
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                refreshToken: pm.environment.get('refresh_token')
            })
        }
    }, function (err, res) {
        if (res.code === 200) {
            const jsonData = res.json();
            pm.environment.set('jwt_token', jsonData.accessToken);
            pm.environment.set('refresh_token', jsonData.refreshToken);
        }
    });
}
```

### 6.3 Test Scripts
```javascript
// Verify authentication
pm.test("Status code is 200 or 401", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 401]);
});

// Check for authentication errors
pm.test("Handle authentication errors", function () {
    if (pm.response.code === 401) {
        const jsonData = pm.response.json();
        pm.expect(jsonData.error).to.eql('Unauthorized');
    }
});
```

## Troubleshooting

### 7.1 Common Issues

#### "Invalid credentials" error
**Solution**:
1. Verify username/password
2. Check organization matches
3. Ensure user is active

#### "Token expired" error
**Solution**:
1. Re-authenticate to get new token
2. Check token expiration time
3. Verify system clock is synchronized

#### "Insufficient permissions" error
**Solution**:
1. Check user roles
2. Verify role assignments
3. Contact administrator for role updates

### 7.2 Debug Mode
```bash
# Enable debug logging
export LOGGING_LEVEL_ROOT=DEBUG
export LOGGING_LEVEL_COM_BRIDGE=DEBUG

# Check authentication logs
tail -f logs/fhir-bridge.log | grep "Authentication"
```

## Production Setup

### 8.1 OAuth2 Integration
```bash
# For OAuth2 providers (Keycloak, Auth0, etc.)
export OAUTH2_ENABLED=true
export OAUTH2_ISSUER=https://your-auth-provider.com
export OAUTH2_CLIENT_ID=your-client-id
export OAUTH2_CLIENT_SECRET=your-client-secret
```

### 8.2 LDAP Integration
```bash
# For LDAP authentication
export LDAP_ENABLED=true
export LDAP_URL=ldap://your-ldap-server:389
export LDAP_BASE_DN=dc=example,dc=com
export LDAP_USER_DN_PATTERN=uid={0},ou=users
```

### 8.3 Multi-Factor Authentication
```bash
# Enable MFA
export MFA_ENABLED=true
export MFA_PROVIDER=google-authenticator
```

## Quick Reference

### 9.1 Authentication Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/login` | POST | User authentication |
| `/api/v1/auth/refresh` | POST | Token refresh |
| `/api/v1/auth/logout` | POST | User logout |
| `/api/v1/auth/users` | POST | Create user |
| `/api/v1/auth/users/{id}` | GET | Get user info |
| `/api/v1/auth/users/{id}/roles` | PUT | Update roles |

### 9.2 Token Validation
```bash
# Decode JWT token
echo "YOUR_TOKEN" | cut -d. -f2 | base64 -d | jq

# Check token expiration
echo "YOUR_TOKEN" | cut -d. -f2 | base64 -d | jq '.exp'
```

### 9.3 Security Best Practices
1. **Never commit tokens to version control**
2. **Use environment variables for secrets**
3. **Rotate tokens regularly**
4. **Enable audit logging**
5. **Use HTTPS in production**
6. **Implement rate limiting**
7. **Monitor failed authentication attempts**