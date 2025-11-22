#!/bin/bash

echo "=================================="
echo "🔍 SHA Fingerprint Checker"
echo "=================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo -e "${RED}❌ keytool tidak ditemukan!${NC}"
    echo "Pastikan Java JDK sudah terinstall."
    exit 1
fi

echo "📋 Informasi Konfigurasi"
echo "=================================="
echo "Package Name: com.webtech.kamuskorea"
echo "Firebase Project: kamus-korea-apps-dcf09"
echo "Web Client ID: 237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1"
echo ""

# Check Debug Keystore
echo "=================================="
echo "🔑 1. Debug Keystore (PC Lokal)"
echo "=================================="
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

if [ -f "$DEBUG_KEYSTORE" ]; then
    echo -e "${GREEN}✅ Debug keystore ditemukan${NC}"
    echo ""
    echo "SHA-1 dan SHA-256 (Debug):"
    keytool -list -v -alias androiddebugkey \
        -keystore "$DEBUG_KEYSTORE" \
        -storepass android 2>/dev/null | grep -E "SHA1:|SHA256:"
    echo ""
    echo -e "${YELLOW}⚠️  Tambahkan SHA-1 di atas ke Firebase Console!${NC}"
else
    echo -e "${RED}❌ Debug keystore tidak ditemukan di: $DEBUG_KEYSTORE${NC}"
fi

echo ""

# Check Release Keystore
echo "=================================="
echo "🔑 2. Release Keystore"
echo "=================================="
RELEASE_KEYSTORE="./kamuskorea-release.jks"

if [ -f "$RELEASE_KEYSTORE" ]; then
    echo -e "${GREEN}✅ Release keystore ditemukan${NC}"
    echo ""
    echo "Masukkan password untuk release keystore:"
    echo "SHA-1 dan SHA-256 (Release):"
    keytool -list -v -alias kamuskorea \
        -keystore "$RELEASE_KEYSTORE" 2>/dev/null | grep -E "SHA1:|SHA256:"

    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Gagal membaca keystore. Periksa password dan alias.${NC}"
        echo "Alias yang digunakan: kamuskorea"
    else
        echo ""
        echo -e "${YELLOW}⚠️  Tambahkan SHA-1 di atas ke Firebase Console!${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Release keystore tidak ditemukan di: $RELEASE_KEYSTORE${NC}"
    echo "Jika keystore ada di lokasi lain, jalankan manual:"
    echo "keytool -list -v -alias kamuskorea -keystore /path/to/your.jks"
fi

echo ""

# Google Play Reminder
echo "=================================="
echo "🎮 3. Google Play App Signing"
echo "=================================="
echo "Untuk mendapatkan SHA dari Google Play:"
echo ""
echo "1. Buka: https://play.google.com/console/"
echo "2. Pilih aplikasi Kamus Korea"
echo "3. Release → Setup → App integrity"
echo "4. Bagian 'App signing key certificate'"
echo "5. Copy SHA-1 dan SHA-256"
echo ""
echo -e "${YELLOW}⚠️  Tambahkan SHA-1 dan SHA-256 dari Play Console ke Firebase!${NC}"

echo ""

# Check google-services.json
echo "=================================="
echo "📄 4. google-services.json"
echo "=================================="
GOOGLE_SERVICES="./app/google-services.json"

if [ -f "$GOOGLE_SERVICES" ]; then
    echo -e "${GREEN}✅ google-services.json ditemukan${NC}"
    echo ""
    echo "Project info:"
    cat "$GOOGLE_SERVICES" | grep -A 5 "project_info" | head -10
    echo ""
    echo -e "${YELLOW}⚠️  Pastikan file ini adalah versi TERBARU setelah menambahkan SHA!${NC}"
else
    echo -e "${RED}❌ google-services.json TIDAK DITEMUKAN!${NC}"
    echo ""
    echo "DOWNLOAD google-services.json dari:"
    echo "https://console.firebase.google.com/project/kamus-korea-apps-dcf09/settings/general"
    echo ""
    echo -e "${RED}⚠️  Google Sign-In TIDAK AKAN BERFUNGSI tanpa file ini!${NC}"
fi

echo ""

# Summary
echo "=================================="
echo "📋 CHECKLIST"
echo "=================================="
echo ""
echo "Pastikan semua SHA fingerprints sudah ditambahkan ke:"
echo ""
echo "1️⃣  Firebase Console:"
echo "   https://console.firebase.google.com/project/kamus-korea-apps-dcf09/settings/general"
echo "   → Your apps → com.webtech.kamuskorea → SHA certificate fingerprints"
echo ""
echo "2️⃣  Google Cloud Console (Android OAuth Client):"
echo "   https://console.cloud.google.com/apis/credentials?project=kamus-korea-apps-dcf09"
echo "   → OAuth 2.0 Client IDs → Android client"
echo ""
echo "3️⃣  Download google-services.json terbaru dan simpan di:"
echo "   app/google-services.json"
echo ""
echo "4️⃣  Clean dan rebuild project:"
echo "   ./gradlew clean && ./gradlew bundleRelease"
echo ""
echo "=================================="
echo "✅ Selesai!"
echo "=================================="
echo ""
echo "Untuk troubleshooting lebih lanjut, baca:"
echo "📖 TROUBLESHOOTING_SHA_ISSUES.md"
echo ""
