#!/usr/bin/env bash
set -euo pipefail

# --- Package the Lambda deployment zip ---
echo "Packaging Lambda function..."
zip -rq lambda.zip src/ node_modules/ package.json -x "node_modules/.cache/*"
echo "Created lambda.zip ($(du -h lambda.zip | cut -f1))"

# --- Run Terraform ---
cd infra

echo "Initializing Terraform..."
terraform init

echo "Planning infrastructure changes..."
terraform plan -out=tfplan

echo ""
echo "Review the plan above. To apply:"
echo "  cd infra && terraform apply tfplan"
