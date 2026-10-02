#!/usr/bin/env bash

set -e

# 1. Push any local changes to current branch
if [[ -n $(git status -s) ]]; then
  echo "==> Pushing changes to remote..."
  CURRENT_BRANCH=$(git branch --show-current)
  git add .
  git commit -m "chore: test compilation"
  git push origin "$CURRENT_BRANCH"
fi

echo "==> Triggering remote macOS build..."
gh workflow run ios-verify.yml || true

sleep 4

# 2. Stream build progress in terminal
gh run watch || true

# 3. Print status/errors
RUN_ID=$(gh run list --workflow=ios-verify.yml --limit 1 --json databaseId -q '.[0].databaseId' 2>/dev/null || echo "")
STATUS=$(gh run list --workflow=ios-verify.yml --limit 1 --json conclusion -q '.[0].conclusion' 2>/dev/null || echo "")

if [ "$STATUS" != "success" ] && [ -n "$RUN_ID" ]; then
  echo ""
  echo "==================== XCODE COMPILER ERRORS ===================="
  gh run view "$RUN_ID" --log-failed || true
  echo "==============================================================="
  exit 1
else
  echo ""
  echo "==> Build verification completed."
fi
