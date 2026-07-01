variable "aws_region" {
  description = "AWS region to deploy resources in"
  type        = string
  default     = "eu-central-1"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Name prefix for all resources"
  type        = string
  default     = "notification-orchestrator"
}

variable "db_name" {
  description = "The database name for logging-service"
  type        = string
  default     = "logging_db"
}

variable "db_username" {
  description = "Database administrator username"
  type        = string
  default     = "logging_user"
}

variable "db_password" {
  description = "Database administrator password"
  type        = string
  sensitive   = true
}
