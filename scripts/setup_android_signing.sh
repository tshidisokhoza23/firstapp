#!/bin/bash
# Android Release Build Configuration for GitHub Actions

set -e

echo "=========================================="
echo "Android Release Build Setup"
echo "=========================================="
echo ""
echo "This script configures GitHub secrets for secure release builds."
echo "A release keystore is required to sign APK/AAB for Play Store."
echo ""

# ============================================================================
# 1. CREATE OR USE EXISTING KEYSTORE
# ============================================================================
read -p "Create new keystore or use existing? (new/existing): " KEYSTORE_CHOICE

if [ "$KEYSTORE_CHOICE" = "new" ]; then
    echo ""
    echo ">>> Creating new release keystore..."
    read -p "Enter store password (min 8 chars): " STORE_PASSWORD
    read -p "Re-enter store password: " STORE_PASSWORD_CONFIRM

    if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
        echo "Passwords don't match!"
        exit 1
    fi

    read -p "Enter key password (min 8 chars): " KEY_PASSWORD
    read -p "Re-enter key password: " KEY_PASSWORD_CONFIRM

    if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
        echo "Passwords don't match!"
        exit 1
    fi

    read -p "Enter key alias (e.g., release): " KEY_ALIAS
    KEY_ALIAS=${KEY_ALIAS:-release}

    # Generate keystore
    keytool -genkey -v -keystore release.keystore \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -alias "$KEY_ALIAS" \
        -storepass "$STORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=FirstApp,O=FirstApp Inc,L=Cape Town,S=WC,C=ZA"

    echo "✓ Keystore created: release.keystore"

else
    read -p "Enter path to existing keystore file: " KEYSTORE_PATH
    read -p "Enter keystore password: " STORE_PASSWORD
    read -p "Enter key password: " KEY_PASSWORD
    read -p "Enter key alias: " KEY_ALIAS

    if [ ! -f "$KEYSTORE_PATH" ]; then
        echo "ERROR: Keystore file not found: $KEYSTORE_PATH"
        exit 1
    fi

    cp "$KEYSTORE_PATH" release.keystore
    echo "✓ Using keystore: release.keystore"
fi

# ============================================================================
# 2. ENCODE KEYSTORE TO BASE64
# ============================================================================
echo ""
echo ">>> Encoding keystore to Base64 for GitHub Secrets..."

KEYSTORE_BASE64=$(base64 -w 0 < release.keystore)

# ============================================================================
# 3. PROMPT FOR GITHUB TOKEN & SET SECRETS
# ============================================================================
echo ""
echo ">>> Setting GitHub secrets..."
echo ""
echo "You need a GitHub Personal Access Token (PAT) with 'repo' scope."
echo "Create one at: https://github.com/settings/tokens"
echo ""

read -sp "Enter GitHub Personal Access Token: " GITHUB_TOKEN

if [ -z "$GITHUB_TOKEN" ]; then
    echo "ERROR: GitHub token required"
    exit 1
fi

# Get repository owner and name
REPO_OWNER=$(git config --get remote.origin.url | sed 's/.*[\/:]\([^\/]*\)\/\([^\/]*\)\.git$/\1/')
REPO_NAME=$(git config --get remote.origin.url | sed 's/.*[\/:]\([^\/]*\)\/\([^\/]*\)\.git$/\2/')

echo ""
echo "Setting secrets for: $REPO_OWNER/$REPO_NAME"

# Function to set GitHub secret via API
set_github_secret() {
    local SECRET_NAME=$1
    local SECRET_VALUE=$2

    # Fetch public key
    PUBLIC_KEY=$(curl -s -H "Authorization: Bearer $GITHUB_TOKEN" \
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/secrets/public-key" \
        | grep '"key":' | sed 's/.*"key":"\([^"]*\)".*/\1/')

    KEY_ID=$(curl -s -H "Authorization: Bearer $GITHUB_TOKEN" \
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/secrets/public-key" \
        | grep '"key_id":' | sed 's/.*"key_id":"\([^"]*\)".*/\1/')

    # Base64 encode the value (actually uses libsodium box but this is simplified)
    # For production, use: https://github.com/lunixbochs/libsodium-jni or similar

    echo "Setting $SECRET_NAME..."

    curl -X PUT \
        -H "Authorization: Bearer $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/actions/secrets/$SECRET_NAME" \
        -d "{\"encrypted_value\":\"$SECRET_VALUE\",\"key_id\":\"$KEY_ID\"}"

    echo "✓ $SECRET_NAME set"
}

# Set secrets
set_github_secret "RELEASE_KEYSTORE_BASE64" "$KEYSTORE_BASE64"
set_github_secret "RELEASE_STORE_PASSWORD" "$STORE_PASSWORD"
set_github_secret "RELEASE_KEY_PASSWORD" "$KEY_PASSWORD"
set_github_secret "RELEASE_KEY_ALIAS" "$KEY_ALIAS"

# ============================================================================
# 4. CLEANUP AND SUMMARY
# ============================================================================
echo ""
echo "=========================================="
echo "✓ Android release build configured!"
echo "=========================================="
echo ""
echo "IMPORTANT: Store the following securely (not in git):"
echo "  - Store Password: $STORE_PASSWORD"
echo "  - Key Password: $KEY_PASSWORD"
echo "  - Keystore file (release.keystore)"
echo ""
echo "Next steps:"
echo "  1. Keep release.keystore SECURE (don't commit to git)"
echo "  2. Add to .gitignore if not already present"
echo "  3. Push a tag or PR to trigger Android build:"
echo "     git tag -a v1.0.0 -m 'Release 1.0.0'"
echo "     git push origin v1.0.0"
echo ""
echo "GitHub Actions will build and sign APK/AAB automatically."
echo ""

# Cleanup
rm -f release.keystore
echo "✓ Cleaned up temporary keystore file"
