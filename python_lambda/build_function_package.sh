#!/bin/bash

##############################################################################
# AWS Lambda Function Package Builder
# 
# This script creates a deployment package with ONLY the function code.
# Dependencies should be provided via a Lambda layer (use build_lambda_layer.sh)
##############################################################################

set -e  # Exit on error

FUNCTION_NAME="phoenix-db-function"
PACKAGE_DIR="function_package"
PACKAGE_ZIP="function_deployment.zip"

echo "=========================================="
echo "AWS Lambda Function Package Builder"
echo "Function: $FUNCTION_NAME"
echo "=========================================="

# Clean up previous builds
echo "Cleaning up previous builds..."
rm -rf $PACKAGE_DIR
rm -f $PACKAGE_ZIP

# Create package directory
echo "Creating function package..."
mkdir -p $PACKAGE_DIR

# Copy function files
echo "Copying function files..."
cp test_function.py $PACKAGE_DIR/
cp database.py $PACKAGE_DIR/
cp config.ini $PACKAGE_DIR/

# Create ZIP (no dependencies - they're in the layer)
echo "Creating deployment package..."
cd $PACKAGE_DIR
zip -r9 ../$PACKAGE_ZIP .
cd ..

PACKAGE_SIZE=$(ls -lh $PACKAGE_ZIP | awk '{print $5}')
echo "=========================================="
echo "✓ Function package created successfully!"
echo "File: $PACKAGE_ZIP"
echo "Size: $PACKAGE_SIZE"
echo "=========================================="
echo ""
echo "This package contains only your function code."
echo "Make sure to attach the Phoenix DB layer to your Lambda function!"
echo "=========================================="

