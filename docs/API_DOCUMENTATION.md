# FHIR Bridge API Documentation

## Overview

The FHIR Bridge API is a comprehensive healthcare interoperability solution that transforms HL7 v2 messages to FHIR R4 format while managing patient consent and providing robust security features.

## API Documentation Access

### Swagger UI
Interactive API documentation is available at:
- **Development**: `http://localhost:8080/swagger-ui/index.html`
- **Production**: `https://api.fhirbridge.com/swagger-ui/index.html`

### OpenAPI Specification
Raw OpenAPI 3.0 specification is available at:
- **JSON Format**: `/v3/api-docs`
- **YAML Format**: `/v3/api-docs.yaml`

## Authentication

All API endpoints (except health checks) require authentication using JWT Bearer tokens.

### Obtaining Access Tokens

#### OAuth2 Authorization Code Flow
```http
POST /api/v1/auth/oauth2/token
Content-Type: application/x-www-form-urlencoded

code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI
```

#### Client Credentials Flow
```http
POST /api/v1/auth/client-credentials/token
Content-Type: application/x-www-form-urlencoded

client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET
```

### Using Access Tokens
Include the JWT token in the Authorization header:
```http
Authorization: Bearer YOUR_JWT_TOKEN
```

## API Endpoints

### Transformation Endpoints

#### Transform HL7 v2 to FHIR
Converts HL7 v2 messages to FHIR R4 resources.

```http
POST /api/v1/transform/hl7v2-to-fhir
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "hl7Message": "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||ADT^A01|MSG123|P|2.4",
  "sendingApplication": "SENDING_APP",
  "receivingApplication": "RECEIVING_APP"
}
```

**Response:**
```json
{
  "requestId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "SUCCESS",
  "fhirResources": [
    {
      "resourceId": "patient-123",
      "resourceType": "Patient",
      "fhirVersion": "R4",
      "jsonContent": "{\"resourceType\": \"Patient\", \"id\": \"patient-123\"}",
      "sourceMessageId": "123e4567-e89b-12d3-a456-426614174000",
      "createdAt": "2025-01-15T10:30:00"
    }
  ],
  "transformationTimestamp": "2025-01-15T10:30:00",
  "resourceCount": 1
}
```

### FHIR Resource Endpoints

#### Retrieve FHIR Resource
Gets a specific FHIR resource by type and ID.

```http
GET /api/v1/fhir/Patient/patient-123
Authorization: Bearer YOUR_JWT_TOKEN
```

#### Search FHIR Resources
Searches for FHIR resources with optional parameters.

```http
GET /api/v1/fhir/Patient?name=John&birthdate=1980-01-01
Authorization: Bearer YOUR_JWT_TOKEN
```

### Consent Management Endpoints

#### Check Consent Status
Verifies patient consent for data access.

```http
GET /api/v1/consent/status/patient-123?organizationId=org-456
Authorization: Bearer YOUR_JWT_TOKEN
```

**Response:**
```json
{
  "requestId": "123e4567-e89b-12d3-a456-426614174000",
  "patientId": "patient-123",
  "organizationId": "org-456",
  "consentValid": true,
  "consentStatus": "ACTIVE",
  "allowedCategories": ["DEMOGRAPHICS", "CLINICAL"],
  "deniedCategories": ["FINANCIAL"],
  "effectiveDate": "2025-01-01T00:00:00",
  "expirationDate": "2026-01-01T00:00:00",
  "reason": "Valid consent found",
  "verificationTimestamp": "2025-01-15T10:30:00"
}
```

#### Check Consent for Categories
Verifies consent for specific data categories.

```http
POST /api/v1/consent/check/patient-123
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "organizationId": "org-456",
  "dataCategories": ["DEMOGRAPHICS", "CLINICAL"]
}
```

#### Update Consent
Updates patient consent preferences.

```http
PUT /api/v1/consent/update/patient-123
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "organizationId": "org-456",
  "status": "ACTIVE",
  "allowedCategories": ["DEMOGRAPHICS", "CLINICAL"],
  "expirationDate": "2026-01-01T00:00:00",
  "policyReference": "policy-123"
}
```

### Monitoring Endpoints

#### System Health
Gets overall system health status.

```http
GET /api/v1/monitoring/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "database": {"status": "UP"},
    "cache": {"status": "UP"},
    "fhir": {"status": "UP"}
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

#### Performance Metrics
Gets system performance metrics (requires ADMIN or MONITOR role).

```http
GET /api/v1/monitoring/metrics/performance
Authorization: Bearer YOUR_JWT_TOKEN
```

## Error Handling

All API errors follow a consistent format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": "Additional error details or validation errors",
    "timestamp": "2025-01-15T10:30:00Z",
    "requestId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `AUTHENTICATION_FAILED` | 401 | Invalid or missing authentication |
| `INSUFFICIENT_PERMISSIONS` | 403 | User lacks required permissions |
| `RESOURCE_NOT_FOUND` | 404 | Requested resource not found |
| `CONSENT_DENIED` | 403 | Patient consent not granted |
| `TRANSFORMATION_ERROR` | 500 | HL7 to FHIR transformation failed |
| `SYSTEM_ERROR` | 500 | Internal system error |

## Rate Limiting

API requests are rate limited to ensure system stability:

- **Authenticated users**: 1000 requests per hour
- **Unauthenticated endpoints**: 100 requests per hour

Rate limit headers are included in responses:
- `X-RateLimit-Limit`: Maximum requests per hour
- `X-RateLimit-Remaining`: Remaining requests in current window
- `X-RateLimit-Reset`: Time when rate limit resets

## Data Categories

The following data categories are supported for consent management:

| Category | Description |
|----------|-------------|
| `DEMOGRAPHICS` | Patient demographic information |
| `CLINICAL` | Clinical data including diagnoses, procedures |
| `LABORATORY` | Laboratory results and test data |
| `IMAGING` | Medical imaging and radiology data |
| `MEDICATIONS` | Medication history and prescriptions |
| `FINANCIAL` | Billing and insurance information |
| `BEHAVIORAL` | Mental health and behavioral data |
| `SUBSTANCE_ABUSE` | Substance abuse treatment records |

## FHIR Resource Types

The API supports transformation to the following FHIR R4 resource types:

- **Patient**: Patient demographic and administrative information
- **Encounter**: Healthcare encounters and visits
- **Observation**: Clinical observations and measurements
- **DiagnosticReport**: Diagnostic test results
- **Condition**: Patient conditions and diagnoses
- **Procedure**: Medical procedures performed
- **MedicationRequest**: Medication prescriptions
- **AllergyIntolerance**: Patient allergies and intolerances
- **Organization**: Healthcare organizations
- **Practitioner**: Healthcare providers

## Security Considerations

### Transport Security
- All communications must use HTTPS/TLS 1.3 or higher
- Certificate pinning is recommended for production environments

### Authentication Security
- JWT tokens expire after 15 minutes
- Refresh tokens expire after 24 hours
- Tokens should be stored securely and never logged

### Authorization
Role-based access control (RBAC) with the following roles:

| Role | Permissions |
|------|-------------|
| `ADMIN` | Full system access |
| `TRANSFORMER` | HL7 to FHIR transformation |
| `READER` | Read FHIR resources |
| `CONSENT_READER` | Read consent information |
| `CONSENT_MANAGER` | Manage consent records |
| `MONITOR` | Access monitoring endpoints |

### Audit Logging
All API operations are logged for compliance and security monitoring:
- Authentication attempts
- Data transformations
- Consent verifications
- Resource access
- System errors

## Testing the API

### Using curl

#### Get Access Token
```bash
curl -X POST "http://localhost:8080/api/v1/auth/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "code=AUTH_CODE&redirect_uri=http://localhost:3000/callback"
```

#### Transform HL7 Message
```bash
curl -X POST "http://localhost:8080/api/v1/transform/hl7v2-to-fhir" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hl7Message": "MSH|^~\\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20250115103000||ADT^A01|MSG123|P|2.4",
    "sendingApplication": "SENDING_APP",
    "receivingApplication": "RECEIVING_APP"
  }'
```

### Using Postman

1. Import the OpenAPI specification from `/v3/api-docs`
2. Set up environment variables for base URL and tokens
3. Configure OAuth2 authentication in Postman
4. Test endpoints using the generated collection

## Support and Contact

- **Documentation**: Available at `/swagger-ui/index.html`
- **Support Email**: support@fhirbridge.com
- **Support URL**: https://fhirbridge.com/support
- **API Status**: https://status.fhirbridge.com

## Changelog

### Version 1.0.0
- Initial API release
- HL7 v2 to FHIR R4 transformation
- OAuth2 authentication
- Consent management
- Comprehensive monitoring
- OpenAPI 3.0 documentation