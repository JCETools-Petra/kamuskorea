# 🔐 Keystore Information - Learning Korea App

## ⚠️ IMPORTANT - SIMPAN INFORMASI INI DENGAN AMAN!

Jangan pernah share atau commit file ini ke Git!

---

## 📋 Keystore Details

**File:** `learningkorea-release.jks`
**Location:** `/home/user/kamuskorea/learningkorea-release.jks`

**Alias:** `learningkorea`
**Algorithm:** RSA 2048-bit
**Validity:** 10,000 days (sampai 2053)

**Distinguished Name:**
```
CN=Learning Korea
OU=Development
O=WebTech
L=Jakarta
ST=Jakarta
C=ID
```

---

## 🔑 Passwords

**Keystore Password:** `LearningKorea2025!Secure`
**Key Password:** `LearningKorea2025!Secure`

⚠️ **PENTING:** Simpan passwords ini di password manager yang aman!
- LastPass
- 1Password
- Bitwarden
- atau password manager lainnya

**JANGAN PERNAH:**
- Share ke orang lain
- Commit ke Git
- Upload ke cloud storage tanpa enkripsi
- Simpan di plain text file

---

## 🔒 SHA Fingerprints

### SHA-1 (untuk Firebase & Google Cloud Console)
```
46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17
```

### SHA-256 (untuk Firebase & Google Cloud Console)
```
6C:6A:2A:85:09:FE:86:26:64:16:7F:CA:53:56:63:83:C0:F1:AB:CD:E1:58:4B:B6:15:81:C9:C3:63:17:8A:16
```

---

## 📝 Setup Checklist

Gunakan SHA fingerprints di atas untuk:

### ✅ Firebase Console
1. Buka: https://console.firebase.google.com/
2. Pilih project "Learning Korea"
3. Project Settings → Your apps → Android app
4. Scroll ke "SHA certificate fingerprints"
5. Klik "Add fingerprint"
6. Tambahkan SHA-1: `46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17`
7. Klik "Add fingerprint" lagi
8. Tambahkan SHA-256: `6C:6A:2A:85:09:FE:86:26:64:16:7F:CA:53:56:63:83:C0:F1:AB:CD:E1:58:4B:B6:15:81:C9:C3:63:17:8A:16`
9. Download ulang `google-services.json`

### ✅ Google Cloud Console
1. Buka: https://console.cloud.google.com/
2. Pilih project yang sama dengan Firebase
3. APIs & Services → Credentials
4. OAuth 2.0 Client IDs → Android Client
5. Edit dan pastikan SHA-1 sama: `46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17`

### ✅ Google Play Console
1. Buka: https://play.google.com/console/
2. Create new app "Learning Korea"
3. Setup → App integrity
4. App signing → Upload keystore atau let Google manage
5. Jika upload manual, gunakan `learningkorea-release.jks`
6. Copy SHA-1 & SHA-256 dari Play Console
7. Tambahkan juga ke Firebase Console

---

## 🛡️ Backup Strategy

### Backup Keystore File
1. **Primary Backup:** Cloud storage terenkripsi (Google Drive, Dropbox, dll)
2. **Secondary Backup:** External hard drive
3. **Tertiary Backup:** USB drive di tempat aman

### Backup Passwords
1. Password Manager (recommended)
2. Encrypted note di safe place
3. Printed & stored di safety deposit box (untuk production)

---

## 🔧 Usage Commands

### Verify Keystore
```bash
keytool -list -v -keystore learningkorea-release.jks -alias learningkorea
# Password: LearningKorea2025!Secure
```

### Get SHA Fingerprints
```bash
keytool -list -v -keystore learningkorea-release.jks -alias learningkorea -storepass "LearningKorea2025!Secure" | grep -A 2 "Certificate fingerprints"
```

### Sign APK Manually
```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore learningkorea-release.jks \
  app-release-unsigned.apk learningkorea
# Password: LearningKorea2025!Secure
```

### Sign App Bundle
```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore learningkorea-release.jks \
  app-release.aab learningkorea
# Password: LearningKorea2025!Secure
```

---

## 📱 Debug Keystore (untuk Development)

Untuk mendapatkan SHA-1 debug (development):

```bash
./gradlew getDebugSha1
```

Atau manual:
```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

---

## ⚠️ Security Notes

1. **NEVER commit** keystore file ke Git
2. **NEVER commit** keystore.properties ke Git (sudah ada di .gitignore)
3. **ALWAYS backup** keystore file - jika hilang, tidak bisa update app di Play Store!
4. **ROTATE passwords** secara berkala untuk production
5. **USE different keystore** untuk development dan production (optional)

---

## 🚨 What If Keystore Lost?

Jika keystore hilang:

### For Apps Already Published:
- **TIDAK BISA** update app yang sudah publish
- Harus publish sebagai app baru dengan package name berbeda
- Users harus uninstall app lama dan install app baru
- **SANGAT BURUK** untuk user experience

### Prevention:
- Backup keystore ke multiple locations
- Gunakan Play App Signing (Google manages keystore)
- Document everything

---

## 📞 Support

Jika ada masalah dengan keystore:
1. Check backup locations
2. Verify file integrity
3. Test keystore dengan command di atas
4. Contact Android Developer support

---

**Created:** 2025-11-23
**App:** Learning Korea
**Package:** com.webtech.learningkorea
**Keystore:** learningkorea-release.jks
**Alias:** learningkorea
