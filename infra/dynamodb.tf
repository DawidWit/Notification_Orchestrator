# The DynamoDB table that stores user notification preferences.
# This replaces the manual "aws dynamodb create-table" CLI command.

resource "aws_dynamodb_table" "notification_preferences" {
  name         = "NotificationPreferences"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}
