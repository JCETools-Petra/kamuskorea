# 🔐 Setup Release Signing Configuration

## ✅ **Signing Config Sudah Ditambahkan!**

File `app/build.gradle.kts` sudah dikonfigurasi untuk menggunakan release keystore.

---

## 📝 **Cara Set Password**

Ada **2 opsi** untuk set password keystore Anda:

### **Opsi 1: Environment Variables (RECOMMENDED - Lebih Aman)**

**Windows PowerShell:**
```powershell
# Temporary (untuk session ini saja)
$env:KEYSTORE_PASSWORD = "password-anda"
$env:KEY_PASSWORD = "password-anda"

# Verify
echo $env:KEYSTORE_PASSWORD
```

**Windows CMD:**
```cmd
set KEYSTORE_PASSWORD=password-anda
set KEY_PASSWORD=password-anda
```

**Windows Permanent (System Environment Variables):**
1. Search: **"Environment Variables"**
2. **Environment Variables** → **User variables**
3. Click **New**:
   - Variable name: `KEYSTORE_PASSWORD`
   - Variable value: `[password-anda]`
4. Click **New** lagi:
   - Variable name: `KEY_PASSWORD`
   - Variable value: `[password-anda]`
5. Click **OK**
6. **Restart PowerShell/CMD**

---

### **Opsi 2: Hardcode di build.gradle.kts (Tidak Direkomendasikan)**

**⚠️ WARNING:** Jangan commit password ke Git!

Edit `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../kamuskorea-release.jks")
        storePassword = "password-anda-disini"  // ← Ganti ini
        keyAlias = "kamuskorea"
        keyPassword = "password-anda-disini"    // ← Ganti ini
    }
}
```

**Tambahkan ke `.gitignore`** untuk keamanan:
```
# Di .gitignore, tambahkan:
local.properties
*.jks
*.keystore
```

---

## 🔨 **Build Release APK/AAB**

Setelah set password:

### **Build APK (untuk testing):**
```powershell
# Build release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### **Build AAB (untuk Play Store):**
```powershell
# Build release AAB (Android App Bundle)
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

### **Install Release APK ke Device:**
```powershell
# Install
./gradlew installRelease

# Atau manual
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 🔍 **Verify Signing**

Check apakah APK sudah ter-sign dengan benar:

```powershell
# Verify APK signature
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Expected output:
# jar verified.
```

Get SHA-1 dari APK:
```powershell
# Extract APK signature
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk

# Look for SHA1 fingerprint
```

---

## 📋 **Checklist Build Release**

Before building for production:

- [ ] Password keystore sudah set (env variables atau hardcode)
- [ ] Keystore file ada: `kamuskorea-release.jks`
- [ ] Version code di-increment: `versionCode = 2` (untuk update)
- [ ] Version name updated: `versionName = "1.1"`
- [ ] Proguard rules tested (no crashes)
- [ ] Google Sign In tested dengan release build
- [ ] All features tested

---

## 🎯 **For Google Sign In Testing**

**IMPORTANT:** Anda perlu test Google Sign In dengan **release build**!

### **Step 1: Build & Install Release**
```powershell
./gradlew installRelease
```

### **Step 2: Test Google Sign In**
- Buka app (release build)
- Click "Masuk dengan Google"
- **Harus berhasil!**

**Jika gagal:**
1. Get SHA-1 dari release keystore (sudah dilakukan)
2. Verify SHA-1 sudah didaftarkan di Firebase
3. Download google-services.json terbaru
4. Rebuild: `./gradlew clean && ./gradlew assembleRelease`

---

## 🚀 **Production Upload Checklist**

Before uploading to Play Store:

### **1. Get SHA-1 dari Release Keystore**
```powershell
keytool -list -v -keystore kamuskorea-release.jks -alias kamuskorea
```
Copy SHA-1 → Daftarkan ke Firebase

### **2. Build AAB**
```powershell
./gradlew bundleRelease
```

### **3. Upload ke Play Console**
- Login: https://play.google.com/console/
- Select app
- Release → Internal Testing (recommended first)
- Upload AAB: `app/build/outputs/bundle/release/app-release.aab`

### **4. Activate Google Play App Signing**
- Play Console → Setup → App Signing
- Choose "Use Google-generated key"

### **5. Get SHA-1 dari Google Play**
- Copy SHA-1 from "App signing key certificate"
- Daftarkan ke Firebase (SHA-1 ke-3!)

### **6. Download google-services.json Baru**
- Firebase Console → Project Settings
- Download google-services.json
- Replace di project

### **7. Rebuild & Re-upload**
```powershell
./gradlew clean
./gradlew bundleRelease
```
Upload version baru (version code +1)

### **8. Test di Internal Testing**
- Download dari Play Console internal testing link
- Test Google Sign In
- Test all features

### **9. Promote to Production**
If all OK → Release → Production → Publish

---

## 🔥 **Troubleshooting**

### **Error: "keystore not found"**
**Solusi:** Verify path di `build.gradle.kts`:
```kotlin
storeFile = file("../kamuskorea-release.jks")
// Should be in project root, not in app/ folder
```

### **Error: "incorrect password"**
**Solusi:**
- Check environment variables: `echo $env:KEYSTORE_PASSWORD`
- Verify password yang Anda masukkan saat create keystore

### **Error: "signingConfig null"**
**Solusi:** Make sure signingConfigs block ada SEBELUM buildTypes

---

## 📚 **Related Files**

- `kamuskorea-release.jks` - Release keystore (BACKUP!)
- `app/build.gradle.kts` - Signing configuration
- `QUICK_START_SHA1.md` - SHA-1 setup guide
- `PRODUCTION_GOOGLE_SIGNIN_GUIDE.md` - Production deployment guide

---

## 🆘 **Need Help?**

Check logs:
```powershell
# Build with stacktrace
./gradlew assembleRelease --stacktrace

# Build with info
./gradlew assembleRelease --info
```

**Common Issues:**
- Password incorrect → Check env variables
- Keystore not found → Check file path
- SHA-1 mismatch → Get SHA-1 from actual APK/AAB
