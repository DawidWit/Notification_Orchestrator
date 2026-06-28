resource "aws_iam_role" "lambda_role" {
  name = "${var.project_name}-lambda-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_policy" "dynamodb_access" {
  name = "${var.project_name}-dynamodb-access-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem"
      ]
      Resource = aws_dynamodb_table.notification_preferences.arn
    }]
  })
}

resource "aws_iam_role_policy_attachment" "dynamodb_access" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = aws_iam_policy.dynamodb_access.policy_arn
}

resource "aws_lambda_function" "api" {
  function_name = "${var.project_name}-${var.environment}"
  role          = aws_iam_role.lambda_role.arn

  filename         = "${path.module}/../lambda.zip"
  source_code_hash = filebase64sha256("${path.module}/../lambda.zip")

  handler = "dist/lambda.handler"
  runtime = "nodejs22.x"
  timeout = 30

  environment {
    variables = {
      NODE_ENV          = var.environment
      AWS_REGION_CUSTOM = var.aws_region
    }
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}
