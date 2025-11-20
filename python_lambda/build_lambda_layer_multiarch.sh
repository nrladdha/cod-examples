#!/bin/bash

##############################################################################
# AWS Lambda Layer Builder for Phoenix DB (Multi-Architecture Support)
# 
# This script builds a Lambda layer with all dependencies compiled for
# Amazon Linux 2 (Lambda runtime environment) using Docker.
#
# Usage:
#   ./build_lambda_layer_multiarch.sh [x86_64|arm64]
#
# Default: x86_64
##############################################################################

set -e  # Exit on error

# Architecture selection
ARCH="${1:-x86_64}"  # Default to arm64 if not specified

if [[ "$ARCH" != "x86_64" && "$ARCH" != "arm64" ]]; then
    echo "ERROR: Invalid architecture. Use 'x86_64' or 'arm64'"
    echo "Usage: $0 [x86_64|arm64]"
    exit 1
fi

LAYER_NAME="phoenix-db-layer-${ARCH}"
PYTHON_VERSION="3.12"  # Adjust to match your Lambda runtime
LAYER_DIR="lambda_layer_${ARCH}"
LAYER_ZIP="phoenix_layer_${ARCH}.zip"

# Docker platform mapping
if [[ "$ARCH" == "arm64" ]]; then
    DOCKER_PLATFORM="linux/arm64"
    DOCKER_TAG="python:${PYTHON_VERSION}"
else
    DOCKER_PLATFORM="linux/amd64"
    DOCKER_TAG="python:${PYTHON_VERSION}"
fi

echo "=========================================="
echo "AWS Lambda Layer Builder"
echo "Layer: $LAYER_NAME"
echo "Python: $PYTHON_VERSION"
echo "Architecture: $ARCH"
echo "Docker Platform: $DOCKER_PLATFORM"
echo "=========================================="

# Clean up previous builds
echo "Cleaning up previous builds..."
rm -rf $LAYER_DIR
rm -f $LAYER_ZIP

# Create layer directory structure
# Lambda expects packages in: python/lib/pythonX.Y/site-packages/
echo "Creating layer directory structure..."
mkdir -p $LAYER_DIR/python

echo "Building dependencies using Docker (Amazon Linux 2)..."
echo "This ensures binary compatibility with AWS Lambda..."

# Use Docker to build dependencies in Amazon Linux 2 environment
# Override the entrypoint to use bash instead of Lambda runtime
docker run --rm \
  --platform $DOCKER_PLATFORM \
  --entrypoint /bin/bash \
  -v "$PWD":/workspace \
  -w /workspace \
  public.ecr.aws/lambda/$DOCKER_TAG \
  -c "
    echo 'Installing dependencies...'
    pip install --upgrade pip
    
    # Install Kerberos development libraries and build tools (required for gssapi)
    echo 'Installing Kerberos development libraries...'
    microdnf install -y krb5-devel gcc python3-devel findutils

    # Install all dependencies including gssapi
    echo 'Installing Python packages with gssapi support...'
    pip install -r requirements.txt -t $LAYER_DIR/python/ --no-cache-dir

    # Copy Kerberos shared libraries to the layer
    echo 'Copying Kerberos shared libraries...'
    mkdir -p $LAYER_DIR/lib
    cp -L /usr/lib64/libgssapi_krb5.so* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libkrb5.so* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libk5crypto.so* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libcom_err.so* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libkrb5support.so* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libkeyutils.so* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libselinux.so.* $LAYER_DIR/lib64/ || true
    cp -L /usr/lib64/libpcre2-8.so.* $LAYER_DIR/lib64/ || true

    # Clean up unnecessary files to reduce layer size
    echo 'Cleaning up unnecessary files...'
    cd $LAYER_DIR/python
    
    # Remove test files and documentation (use /usr/bin/find for reliability)
    /usr/bin/find . -type d -name 'tests' -exec rm -rf {} + 2>/dev/null || true
    /usr/bin/find . -type d -name 'test' -exec rm -rf {} + 2>/dev/null || true
    /usr/bin/find . -type d -name '__pycache__' -exec rm -rf {} + 2>/dev/null || true
    /usr/bin/find . -name '*.pyc' -delete 2>/dev/null || true
    /usr/bin/find . -name '*.pyo' -delete 2>/dev/null || true
    
    echo 'Dependencies installed successfully'
  "

echo "Creating layer ZIP file..."
cd $LAYER_DIR
zip -r9 ../$LAYER_ZIP . -x '*.pyc' -x '*/__pycache__/*'
cd ..

LAYER_SIZE=$(ls -lh $LAYER_ZIP | awk '{print $5}')
echo "=========================================="
echo "✓ Layer package created successfully!"
echo "Architecture: $ARCH"
echo "File: $LAYER_ZIP"
echo "Size: $LAYER_SIZE"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Upload this layer to AWS Lambda:"
echo "2. Make sure your Lambda function uses the same architecture:"
echo "3. Attach the layer to your Lambda function"
echo "=========================================="

