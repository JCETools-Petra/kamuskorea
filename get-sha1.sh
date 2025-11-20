#!/bin/bash
# Script untuk mendapatkan SHA-1 fingerprint - Mac/Linux/Git Bash

echo "========================================"
echo " Getting SHA-1 Fingerprint for Debug"
echo "========================================"
echo ""

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo "ERROR: keytool not found!"
    echo ""
    echo "Please install Java JDK 11+ and add to PATH:"
    echo "1. Download from: https://adoptium.net/"
    echo "2. Install Java"
    echo "3. Add to PATH: export PATH=\"/path/to/java/bin:\$PATH\""
    echo "4. Restart terminal and run this script again"
    echo ""
    exit 1
fi

echo "Java version:"
java -version
echo ""

echo "Getting SHA-1 from Debug Keystore..."
echo ""

# Try different keystore locations
KEYSTORE_PATHS=(
    "$HOME/.android/debug.keystore"
    "$USERPROFILE/.android/debug.keystore"
    "/Users/$USER/.android/debug.keystore"
)

FOUND=0
for KEYSTORE in "${KEYSTORE_PATHS[@]}"; do
    if [ -f "$KEYSTORE" ]; then
        echo "Found keystore at: $KEYSTORE"
        keytool -list -v -keystore "$KEYSTORE" -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:"
        FOUND=1
        break
    fi
done

if [ $FOUND -eq 0 ]; then
    echo "ERROR: Debug keystore not found!"
    echo "Please build the app at least once to generate debug.keystore"
fi

echo ""
echo "========================================"
echo " INSTRUCTIONS:"
echo "========================================"
echo "1. Copy the SHA1 value above"
echo "2. Go to Firebase Console: https://console.firebase.google.com/"
echo "3. Select your project"
echo "4. Go to Project Settings"
echo "5. Select Android app"
echo "6. Click 'Add fingerprint'"
echo "7. Paste SHA1 and Save"
echo "8. Download new google-services.json"
echo "9. Replace app/google-services.json"
echo ""
