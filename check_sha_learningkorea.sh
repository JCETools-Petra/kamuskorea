#!/bin/bash

echo "=================================="
echo "🔍 SHA Fingerprint Checker - Learning Korea"
echo "=================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo -e "${RED}❌ keytool tidak ditemukan!${NC}"
    echo "Pastikan Java JDK sudah terinstall."
    exit 1
fi

echo "📋 Informasi Konfigurasi (UPDATED)"
echo "=================================="
echo -e "${GREEN}Package Name: com.webtech.learningkorea${NC}"
echo "App Name: Learning Korea"
echo "Keystore Alias: learningkorea"
echo ""

# Check Debug Keystore
echo "=================================="
echo "🔑 1. Debug Keystore (Development)"
echo "=================================="
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

if [ -f "$DEBUG_KEYSTORE" ]; then
    echo -e "${GREEN}✅ Debug keystore ditemukan${NC}"
    echo "Location: $DEBUG_KEYSTORE"
    echo ""
    echo "SHA Fingerprints (Debug):"
    echo "-----------------------------------"
    keytool -list -v -alias androiddebugkey \
        -keystore "$DEBUG_KEYSTORE" \
        -storepass android 2>/dev/null | grep -E "SHA1:|SHA256:" | sed 's/^/  /'

    DEBUG_SHA1=$(keytool -list -v -alias androiddebugkey \
        -keystore "$DEBUG_KEYSTORE" \
        -storepass android 2>/dev/null | grep "SHA1:" | cut -d' ' -f3)

    echo ""
    echo -e "${YELLOW}⚠️  Tambahkan SHA-1 DEBUG ke Firebase Console!${NC}"
    echo -e "   ${BLUE}https://console.firebase.google.com/${NC}"
else
    echo -e "${RED}❌ Debug keystore tidak ditemukan${NC}"
    echo "Run app sekali untuk generate debug.keystore"
fi

echo ""

# Check Release Keystore
echo "=================================="
echo "🔑 2. Release Keystore (Production)"
echo "=================================="
RELEASE_KEYSTORE="./learningkorea-release.jks"
RELEASE_ALIAS="learningkorea"

if [ -f "$RELEASE_KEYSTORE" ]; then
    echo -e "${GREEN}✅ Release keystore ditemukan${NC}"
    echo "Location: $RELEASE_KEYSTORE"
    echo "Alias: $RELEASE_ALIAS"
    echo ""
    echo "SHA Fingerprints (Release):"
    echo "-----------------------------------"

    # Try with default password first
    KEYSTORE_PASS="LearningKorea2025!Secure"

    SHA_OUTPUT=$(keytool -list -v -alias "$RELEASE_ALIAS" \
        -keystore "$RELEASE_KEYSTORE" \
        -storepass "$KEYSTORE_PASS" 2>/dev/null | grep -E "SHA1:|SHA256:")

    if [ $? -eq 0 ]; then
        echo "$SHA_OUTPUT" | sed 's/^/  /'

        RELEASE_SHA1=$(echo "$SHA_OUTPUT" | grep "SHA1:" | cut -d' ' -f3)
        RELEASE_SHA256=$(echo "$SHA_OUTPUT" | grep "SHA256:" | cut -d' ' -f3)

        echo ""
        echo -e "${YELLOW}⚠️  Tambahkan SHA-1 & SHA-256 RELEASE ke Firebase!${NC}"
    else
        echo -e "${YELLOW}⚠️  Password default tidak match${NC}"
        echo ""
        echo "Masukkan password keystore secara manual:"
        keytool -list -v -alias "$RELEASE_ALIAS" \
            -keystore "$RELEASE_KEYSTORE" | grep -E "SHA1:|SHA256:" | sed 's/^/  /'

        if [ $? -ne 0 ]; then
            echo -e "${RED}❌ Gagal membaca keystore${NC}"
            echo "Periksa password dan alias (harus: learningkorea)"
        fi
    fi
else
    echo -e "${RED}❌ Release keystore tidak ditemukan!${NC}"
    echo "Expected location: $RELEASE_KEYSTORE"
    echo ""
    echo -e "${YELLOW}💡 Generate keystore baru:${NC}"
    echo "keytool -genkey -v -keystore learningkorea-release.jks \\"
    echo "  -alias learningkorea -keyalg RSA -keysize 2048 -validity 10000"
fi

echo ""

# Check google-services.json
echo "=================================="
echo "📄 3. google-services.json"
echo "=================================="
GOOGLE_SERVICES="./app/google-services.json"

if [ -f "$GOOGLE_SERVICES" ]; then
    echo -e "${GREEN}✅ google-services.json ditemukan${NC}"
    echo ""

    # Check package name
    PACKAGE_NAME=$(cat "$GOOGLE_SERVICES" | grep -o '"package_name": *"[^"]*"' | head -1 | cut -d'"' -f4)
    PROJECT_ID=$(cat "$GOOGLE_SERVICES" | grep -o '"project_id": *"[^"]*"' | head -1 | cut -d'"' -f4)

    echo "Package Name: $PACKAGE_NAME"
    echo "Project ID: $PROJECT_ID"
    echo ""

    if [ "$PACKAGE_NAME" == "com.webtech.learningkorea" ]; then
        echo -e "${GREEN}✅ Package name CORRECT!${NC}"
    else
        echo -e "${RED}❌ Package name SALAH!${NC}"
        echo -e "   Expected: ${GREEN}com.webtech.learningkorea${NC}"
        echo -e "   Found: ${RED}$PACKAGE_NAME${NC}"
        echo ""
        echo -e "${YELLOW}⚠️  DOWNLOAD google-services.json yang BENAR!${NC}"
    fi

    # Check Web Client ID
    WEB_CLIENT=$(cat "$GOOGLE_SERVICES" | grep -A 3 '"client_type": 3' | grep '"client_id"' | cut -d'"' -f4 | head -1)
    if [ ! -z "$WEB_CLIENT" ]; then
        echo ""
        echo "Web Client ID: $WEB_CLIENT"
        echo -e "${GREEN}✅ Web Client ID found (needed for Google Sign-In)${NC}"
    else
        echo -e "${YELLOW}⚠️  Web Client ID tidak ditemukan${NC}"
    fi
else
    echo -e "${RED}❌ google-services.json TIDAK DITEMUKAN!${NC}"
    echo ""
    echo -e "${YELLOW}📥 DOWNLOAD dari Firebase Console:${NC}"
    echo "1. Buka: https://console.firebase.google.com/"
    echo "2. Pilih project 'Learning Korea'"
    echo "3. Project Settings → Your apps"
    echo "4. Download google-services.json"
    echo "5. Letakkan di: app/google-services.json"
    echo ""
    echo -e "${RED}⚠️  Google Sign-In TIDAK AKAN BERFUNGSI tanpa file ini!${NC}"
fi

echo ""

# Google Play Console
echo "=================================="
echo "🎮 4. Google Play Console Setup"
echo "=================================="
echo "SHA fingerprints dari Play Console diperlukan untuk:"
echo "  • Google Sign-In (production)"
echo "  • Firebase App Check"
echo "  • In-app purchases"
echo ""
echo "Cara mendapatkan SHA dari Play Console:"
echo ""
echo "1. Buka: ${BLUE}https://play.google.com/console/${NC}"
echo "2. Pilih app: ${GREEN}Learning Korea${NC}"
echo "3. Release → Setup → App integrity"
echo "4. Tab 'App signing key certificate'"
echo "5. Copy SHA-1 dan SHA-256"
echo ""
echo -e "${YELLOW}⚠️  Tambahkan SHA-1 & SHA-256 dari Play Console ke Firebase!${NC}"

echo ""

# Firebase Checklist
echo "=================================="
echo "🔥 5. Firebase Console Checklist"
echo "=================================="
echo ""
echo "Pastikan di Firebase Console sudah setup:"
echo ""
echo "□ SHA-1 Debug (dari step 1 di atas)"
echo "□ SHA-1 Release (dari step 2 di atas)"
echo "□ SHA-256 Release (dari step 2 di atas)"
echo "□ SHA-1 dari Play Console (dari step 4 di atas)"
echo "□ SHA-256 dari Play Console (dari step 4 di atas)"
echo ""
echo "Lokasi di Firebase:"
echo "  → Project Settings → Your apps"
echo "  → ${GREEN}com.webtech.learningkorea${NC}"
echo "  → SHA certificate fingerprints"
echo ""
echo -e "${BLUE}https://console.firebase.google.com/${NC}"

echo ""

# Authentication Setup
echo "=================================="
echo "🔐 6. Firebase Authentication"
echo "=================================="
echo ""
echo "Checklist Authentication:"
echo ""
echo "□ Google Sign-In ENABLED di Firebase Authentication"
echo "□ Support email sudah di-set"
echo "□ Web Client ID tersedia"
echo ""
echo "Setup Authentication:"
echo "  1. Firebase Console → Build → Authentication"
echo "  2. Sign-in method → Google → Enable"
echo "  3. Pilih support email"
echo "  4. Save"

echo ""

# Google Cloud Console
echo "=================================="
echo "☁️  7. Google Cloud Console"
echo "=================================="
echo ""
echo "OAuth Consent Screen checklist:"
echo ""
echo "□ User Type: External"
echo "□ App name: Learning Korea"
echo "□ Support email: [your-email]"
echo "□ Scopes: email, profile, openid"
echo "□ Test users: [your-email] ← PENTING untuk testing!"
echo ""
echo "Setup OAuth Consent:"
echo "  1. ${BLUE}https://console.cloud.google.com/${NC}"
echo "  2. APIs & Services → OAuth consent screen"
echo "  3. Pilih project yang SAMA dengan Firebase"
echo "  4. Setup sesuai checklist di atas"
echo ""
echo -e "${YELLOW}⚠️  Tanpa test user, akan muncul 'Access blocked' error!${NC}"

echo ""

# Build Instructions
echo "=================================="
echo "🏗️  8. Build & Test"
echo "=================================="
echo ""
echo "Setelah semua setup selesai:"
echo ""
echo "1. Clean project:"
echo "   ${BLUE}./gradlew clean${NC}"
echo ""
echo "2. Build debug APK:"
echo "   ${BLUE}./gradlew assembleDebug -x lint${NC}"
echo ""
echo "3. Install & test:"
echo "   ${BLUE}adb install app/build/outputs/apk/debug/app-debug.apk${NC}"
echo ""
echo "4. Test Google Sign-In:"
echo "   - Buka app"
echo "   - Klik tombol Google Sign-In"
echo "   - Pilih akun Google (harus yang ada di test users)"
echo "   - Seharusnya berhasil login"

echo ""

# Troubleshooting
echo "=================================="
echo "🐛 Troubleshooting"
echo "=================================="
echo ""
echo "Jika Google Sign-In gagal:"
echo ""
echo "❌ Error 'Developer Error':"
echo "   → SHA-1 tidak match atau google-services.json lama"
echo "   → Download ulang google-services.json"
echo "   → Rebuild app"
echo ""
echo "❌ Error 'Access blocked':"
echo "   → Email belum ditambahkan sebagai test user"
echo "   → Tambahkan di OAuth consent screen"
echo ""
echo "❌ Error '12500' atau 'Sign in cancelled':"
echo "   → Web Client ID salah"
echo "   → Check google-services.json"
echo "   → Setup OAuth Consent Screen"
echo ""
echo "📖 Baca dokumentasi lengkap:"
echo "   - FIREBASE_SETUP_STEPS.md"
echo "   - TROUBLESHOOTING_AUTH.md"

echo ""

# Summary
echo "=================================="
echo "📋 SUMMARY - What to Copy to Firebase"
echo "=================================="
echo ""

if [ ! -z "$DEBUG_SHA1" ]; then
    echo "Debug SHA-1:"
    echo -e "  ${GREEN}$DEBUG_SHA1${NC}"
fi

if [ ! -z "$RELEASE_SHA1" ]; then
    echo ""
    echo "Release SHA-1:"
    echo -e "  ${GREEN}$RELEASE_SHA1${NC}"
fi

if [ ! -z "$RELEASE_SHA256" ]; then
    echo ""
    echo "Release SHA-256:"
    echo -e "  ${GREEN}$RELEASE_SHA256${NC}"
fi

echo ""
echo "Tambahkan semua SHA di atas ke Firebase Console:"
echo -e "${BLUE}https://console.firebase.google.com/${NC}"
echo "→ Project Settings → Your apps → SHA certificate fingerprints"

echo ""
echo "=================================="
echo "✅ Check Complete!"
echo "=================================="
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Add all SHA fingerprints to Firebase"
echo "2. Download google-services.json (AFTER adding SHA)"
echo "3. Setup OAuth Consent Screen di Google Cloud Console"
echo "4. Add test users"
echo "5. Rebuild & test app"
echo ""
