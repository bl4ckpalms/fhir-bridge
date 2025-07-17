# Technology Stack

## Core Technologies
- **Java 17**: Primary programming language
- **Spring Boot 3.3.0**: Application framework
- **Maven**: Build system and dependency management
- **PostgreSQL**: Primary database for persistent storage
- **Redis**: Caching layer for performance optimization

## Key Dependencies
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Database access layer
- **HAPI FHIR 7.0.2**: FHIR R4 implementation library
- **HAPI HL7 2.3**: HL7 v2 message processing
- **Spring Boot Actuator**: Health checks and monitoring

## Infrastructure
- **AWS**: Cloud deployment platform
- **Terraform**: Infrastructure as Code
- **Application Load Balancer**: Traffic distribution

## Common Commands

### Build and Test
```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package application
./mvnw clean package

# Run application locally
./mvnw spring-boot:run
```

### Infrastructure
```bash
# Deploy infrastructure
cd infra
terraform init
terraform plan
terraform apply
```

## Configuration
- Environment-specific configs in `application.yml`
- Test configuration in `application-test.yml`
- Database migrations handled via JPA/Hibernate
- External configuration via environment variables