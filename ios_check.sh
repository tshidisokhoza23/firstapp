#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

# Target directory: default to current directory or use first argument
PROJECT_DIR="${1:-$(pwd)}"

echo "==> Scanning Swift files for syntax and linting in: $PROJECT_DIR"

# Ensure docker daemon is active
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH."
    exit 1
fi

# Run SwiftLint inside official Docker container
docker run --rm \
    -v "$PROJECT_DIR":/repo \
    -w /repo \
    ghcr.io/realm/swiftlint:latest \
    lint --reporter emoji

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo "==> No critical linting or syntax errors found!"
else
    echo "==> Swift lint check completed."
fi

exit $EXIT_CODE
