#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SONAR_HOST_URL="${SONAR_HOST_URL:-https://sonarcloud.io}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-firstapp}"

if [ -z "${SONAR_TOKEN:-}" ]; then
  echo "Set SONAR_TOKEN (SonarCloud or local SonarQube token)."
  exit 1
fi

if [[ "$SONAR_HOST_URL" == *"sonarcloud.io"* ]] && [ -z "${SONAR_ORGANIZATION:-}" ]; then
  echo "Set SONAR_ORGANIZATION for SonarCloud."
  exit 1
fi

chmod +x ./gradlew

echo "Running betaDebug unit tests and lint..."
./gradlew testBetaDebugUnitTest lintBetaDebug --no-daemon

echo "Uploading to Sonar..."
./gradlew sonar --no-daemon \
  -Dsonar.gradle.skipCompile=true \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
  ${SONAR_ORGANIZATION:+-Dsonar.organization="$SONAR_ORGANIZATION"}
