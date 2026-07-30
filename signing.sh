#!/bin/bash
set -e

# Repository configuration
REPO="RA341/RvxMobile"
KEYSTORE_FILE="rvxmobile-release.jks"
ALIAS="rvxkey"

echo "=== RvxMobile Keystore & GitHub Secrets Generator ==="

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "[-] Error: 'gh' (GitHub CLI) is not installed."
    echo "    Please install it and log in using 'gh auth login' before running this script."
    exit 1
fi

# Check if keytool is installed
if ! command -v keytool &> /dev/null; then
    echo "[-] Error: 'keytool' is not installed or not in PATH."
    echo "    Please install Java JDK to get keytool."
    exit 1
fi

# Prompt/Generate passwords
echo -n "Enter Keystore Password (press Enter to generate a random secure password): "
read -s PASSWORD
echo ""

if [ -z "$PASSWORD" ]; then
    PASSWORD=$(openssl rand -base64 18 | tr -dc 'a-zA-Z0-9' | head -c 16)
    echo "[+] Generated random secure password: $PASSWORD"
fi

# Generate Keystore
echo "[+] Generating keystore '$KEYSTORE_FILE'..."
if [ -f "$KEYSTORE_FILE" ]; then
    echo "[-] Keystore file '$KEYSTORE_FILE' already exists. Skipping generation."
else
    keytool -genkeypair -v \
      -keystore "$KEYSTORE_FILE" \
      -alias "$ALIAS" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storepass "$PASSWORD" \
      -keypass "$PASSWORD" \
      -dname "CN=RvxMobile, OU=Dev, O=RvxMobile, C=US"
    echo "[+] Keystore generated successfully."
fi

# Base64 Encode Keystore
echo "[+] Base64 encoding keystore..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    KEYSTORE_BASE64=$(base64 -i "$KEYSTORE_FILE")
else
    KEYSTORE_BASE64=$(base64 -w 0 "$KEYSTORE_FILE")
fi

# Set Secrets using gh CLI
echo "[+] Uploading secrets to GitHub repo '$REPO'..."

echo -n "$KEYSTORE_BASE64" | gh secret set RELEASE_KEYSTORE --repo "$REPO"
echo -n "$PASSWORD" | gh secret set RELEASE_KEYSTORE_PASSWORD --repo "$REPO"
echo -n "$ALIAS" | gh secret set RELEASE_KEY_ALIAS --repo "$REPO"
echo -n "$PASSWORD" | gh secret set RELEASE_KEY_PASSWORD --repo "$REPO"

echo "=== Success ==="
echo "[+] Keystore and key generated: '$KEYSTORE_FILE'"
echo "[+] Keystore credentials and binary uploaded as GitHub Repository Secrets!"
echo "[!] IMPORTANT: Back up '$KEYSTORE_FILE' securely. If you lose it, you won't be able to update your app on existing installations."
