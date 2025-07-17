# Project Structure

## Root Directory
```
fhir-bridge/
├── .kiro/                  # Kiro AI assistant configuration
├── infra/                  # Terraform infrastructure code
├── src/                    # Source code
├── target/                 # Maven build output
├── pom.xml                 # Maven project configuration
├── mvnw, mvnw.cmd         # Maven wrapper scripts
└── README.md              # Project documentation
```

## Source Code Organization
```
src/
├── main/
│   ├── java/com/bridge/
│   │   ├── config/         # Configuration classes
│   │   ├── controller/     # REST API controllers
│   │   ├── model/          # JPA entities and data models
│   │   ├── repository/     # Data access layer
│   │   ├── service/        # Business logic layer
│   │   └── FhirBridgeApplication.java  # Main application class
│   └── resources/
│       └── application.yml # Application configuration
└── test/
    └── resources/
        └── application-test.yml # Test configuration
```

## Package Structure Conventions
- **com.bridge.config**: Spring configuration classes (Security, Database, FHIR, Cache)
- **com.bridge.controller**: REST endpoints for API access
- **com.bridge.model**: JPA entities representing domain objects
- **com.bridge.repository**: Spring Data JPA repositories
- **com.bridge.service**: Business logic and transformation services

## Key Components
- **FhirBridgeApplication.java**: Spring Boot main class
- **SecurityConfig.java**: OAuth2/JWT security configuration
- **DatabaseConfig.java**: PostgreSQL and JPA setup
- **FhirConfig.java**: HAPI FHIR configuration
- **CacheConfig.java**: Redis caching setup

## Naming Conventions
- Classes use PascalCase
- Packages use lowercase
- Configuration classes end with "Config"
- Service classes end with "Service" or descriptive names
- Repository interfaces end with "Repository"
- Controllers end with "Controller"