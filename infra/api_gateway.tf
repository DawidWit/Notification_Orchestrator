# --- HTTP API ---
# API Gateway v2 (HTTP API) — a lightweight, low-cost API gateway.
# It receives HTTP requests and forwards them to Lambda.

resource "aws_apigatewayv2_api" "api" {
  name          = "${var.project_name}-api-${var.environment}"
  protocol_type = "HTTP"
}

# --- Integration ---
# Connects the API Gateway to the Lambda function.
# "AWS_PROXY" means the full HTTP request (headers, body, path)
# is passed through to Lambda as-is.

resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id             = aws_apigatewayv2_api.api.id
  integration_type   = "AWS_PROXY"
  integration_uri    = aws_lambda_function.api.invoke_arn
  integration_method = "POST"
}

# --- Route ---
# A catch-all route: ANY method, ANY path → forward to Lambda.
# Express handles the actual routing internally.

resource "aws_apigatewayv2_route" "catch_all" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

# --- Stage ---
# A deployment stage. "$default" means requests go directly to
# the API URL without a path prefix like /dev or /prod.

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = "$default"
  auto_deploy = true
}

# --- Permission ---
# Allows API Gateway to invoke the Lambda function.
# Without this, API Gateway would get "Access Denied".

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.api.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.api.execution_arn}/*/*"
}
