resource "aws_db_instance" "logging_db" {
  identifier           = "${var.project_name}-logging-db-${var.environment}"
  allocated_storage    = 20
  max_allocated_storage = 100
  engine               = "postgres"
  engine_version       = "16.3"
  instance_class       = "db.t4g.micro"
  db_name              = var.db_name
  username             = var.db_username
  password             = var.db_password
  parameter_group_name = "default.postgres16"
  skip_final_snapshot  = true
  publicly_accessible  = true # Set to true to allow connections without VPC peering/tunnels for simple setups

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}
