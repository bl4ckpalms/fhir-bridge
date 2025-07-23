# AWS Secrets Manager for database credentials
resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "fhir-bridge/database/credentials"
  description             = "Database credentials for FHIR Bridge"
  recovery_window_in_days = 7

  tags = {
    Name        = "fhir-bridge-db-credentials"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = "fhirbridge_admin"
    password = random_password.db_password.result
  })
}

resource "random_password" "db_password" {
  length  = 32
  special = true
}