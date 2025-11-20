# 🚀 Google Sign In - Production Deployment Guide

## ⚠️ PENTING: SHA-1 untuk Production/Release

Ketika aplikasi di-upload ke Google Play Store, **SHA-1 akan berbeda** dari development! Google Play menggunakan **App Signing** mereka sendiri.

---

## 🔍 Masalah Error yang Anda Alami

### 1. Java Version Error
```
Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM.
```

**Solusi:**
- Install **Java 11 atau lebih tinggi**
- Set JAVA_HOME ke Java 11+

**Download Java:**
- [AdoptOpenJDK](https://adoptium.net/) (Recommended)
- [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

**Set JAVA_HOME (Windows):**
```bash
# Di Git Bash atau PowerShell
export JAVA_HOME="C:/Program Files/Java/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java -version  # Harus Java 11+
```

**Set JAVA_HOME (Permanent - Windows):**
1. Search "Environment Variables"
2. System Properties → Environment Variables
3. Add/Edit `JAVA_HOME`: `C:\Program Files\Java\jdk-17`
4. Edit `Path`: Tambahkan `%JAVA_HOME%\bin`

---

### 2. Keytool Not Found

**Solusi:** Tambahkan Java bin ke PATH

```bash
# Windows - Git Bash
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
keytool -version
```

---

## 🔐 Mendapatkan SHA-1 Fingerprint

### Metode 1: Gradle (Setelah Java 11+ Terinstall)

```bash
cd C:/Users/Administrator/Desktop/KamusKorea2
./gradlew signingReport
```

Cari output:
```
Variant: debug
Config: debug
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
```

---

### Metode 2: Keytool (Debug Build)

```bash
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# Look for SHA1
```

---

### Metode 3: Android Studio (PALING MUDAH)

1. Buka project di **Android Studio**
2. Klik **Gradle** tab (kanan)
3. Expand: `app` → `Tasks` → `android`
4. Double-click **`signingReport`**
5. Lihat output di terminal bawah
6. Copy **SHA1** dan **SHA-256**

---

## 🏭 PRODUCTION SETUP (CRITICAL!)

### Step 1: Buat Release Keystore

**JANGAN gunakan debug keystore untuk production!**

```bash
# Buat keystore baru untuk release
keytool -genkey -v -keystore kamuskorea-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kamuskorea

# Isi informasi:
# - Password: [SIMPAN DENGAN AMAN!]
# - Name: Your Name
# - Organization: Your Company
# - City, State, Country
```

**BACKUP keystore ini! Jika hilang, Anda tidak bisa update app di Play Store!**

---

### Step 2: Configure Release Signing di build.gradle.kts

Edit `app/build.gradle.kts`:

```kotlin
android {
    ...

    signingConfigs {
        create("release") {
            storeFile = file("../kamuskorea-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your-password"
            keyAlias = "kamuskorea"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "your-password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

### Step 3: Dapatkan SHA-1 dari Release Keystore

```bash
keytool -list -v -keystore kamuskorea-release.jks -alias kamuskorea

# Copy SHA1 dan SHA-256
```

---

### Step 4: Google Play App Signing SHA-1

**PENTING:** Ketika upload ke Google Play Console:

#### 4.1 Upload Pertama Kali

1. Build release APK/AAB:
   ```bash
   ./gradlew bundleRelease
   ```

2. Upload ke Google Play Console (Internal Testing)

3. **Aktifkan Google Play App Signing**:
   - Play Console → Setup → App Signing
   - Pilih "**Use Google-generated key**" (Recommended)

4. **Dapatkan SHA-1 dari Google Play**:
   - Setelah aktif, Anda akan lihat **"App signing key certificate"**
   - Copy **SHA-1** dan **SHA-256** dari sini

#### 4.2 Daftarkan SHA-1 ke Firebase (SEMUA!)

Anda perlu mendaftarkan **3 SHA-1**:

1. **Debug SHA-1** - untuk development
2. **Release SHA-1** - dari keystore Anda
3. **Google Play App Signing SHA-1** - dari Play Console

**Cara daftar:**
1. Firebase Console → Project Settings
2. Pilih app `com.webtech.kamuskorea`
3. Klik "Add fingerprint" - **3 kali** untuk 3 SHA-1
4. Download **google-services.json** yang baru
5. Replace di project

---

## 🧪 Testing Sebelum Production

### Test di Internal Testing Track

1. Upload AAB ke **Internal Testing** (bukan Production dulu!)
2. Add tester email addresses
3. Download dari Play Store (internal testing link)
4. **Test Google Sign In** - pastikan berhasil!

Jika gagal:
- Cek SHA-1 Google Play App Signing sudah didaftarkan
- Download google-services.json terbaru
- Rebuild & re-upload

---

## 📋 Production Deployment Checklist

### Pre-Upload:

- [ ] Java 11+ terinstall
- [ ] Release keystore sudah dibuat dan **di-backup**
- [ ] build.gradle.kts sudah dikonfigurasi signing
- [ ] Version code & version name sudah di-update
- [ ] Proguard rules sudah dikonfigurasi (jika ada issue)

### Upload ke Play Console:

- [ ] Build release AAB: `./gradlew bundleRelease`
- [ ] Upload ke Internal Testing dulu
- [ ] Aktifkan Google Play App Signing
- [ ] Copy SHA-1 dari Google Play App Signing

### Firebase Configuration:

- [ ] Daftarkan 3 SHA-1 (debug, release, play signing)
- [ ] Download google-services.json terbaru
- [ ] Replace di project: `app/google-services.json`
- [ ] Commit & rebuild

### Testing:

- [ ] Test di Internal Testing track
- [ ] Test Google Sign In berhasil
- [ ] Test fitur-fitur utama
- [ ] Jika semua OK → promote ke Production

---

## 🔥 Troubleshooting Production

### Masalah: Google Sign In gagal setelah publish

**Penyebab:** SHA-1 Google Play App Signing belum didaftarkan

**Solusi:**
1. Play Console → App Signing
2. Copy SHA-1 dari "App signing key certificate"
3. Daftarkan ke Firebase Console
4. Download google-services.json baru
5. Rebuild & re-upload (version code harus +1)

**TIDAK perlu rollback!** User yang sudah download akan otomatis update.

---

### Masalah: "Error 10" atau "Developer Error"

**Solusi:**
1. Verify **package name** sama: `com.webtech.kamuskorea`
2. Verify **SHA-1** sudah terdaftar SEMUA (3 SHA-1)
3. Wait 5-10 menit untuk propagasi
4. Clear app data & test lagi

---

### Masalah: Different SHA-1 per device

Jika menggunakan **different build variants** atau **multiple keystores**:

1. Get SHA-1 dari setiap keystore
2. Daftarkan SEMUA ke Firebase
3. Download google-services.json baru

---

## 📱 After Production Release

### Monitoring

Check di **Firebase Console → Authentication**:
- Apakah ada user login dengan Google?
- Apakah ada error logs?

Check di **Play Console → Crashes & ANRs**:
- Apakah ada crash terkait Google Sign In?

---

## 🆘 Emergency Rollback Plan

Jika Google Sign In **completely broken** di production:

### Plan A: Hotfix Release
1. Fix SHA-1 configuration
2. Build new version (version code +1)
3. Upload ke production
4. Users akan auto-update

### Plan B: Temporary Workaround
1. Add alternative login methods (email/password masih bisa)
2. Show in-app message: "Google Sign In temporarily unavailable"
3. Fix & release update ASAP

### Plan C: Staged Rollout
Saat upload, gunakan **Staged Rollout**:
- Release to 20% users dulu
- Monitor untuk 24 jam
- Jika OK → increase to 50% → 100%
- Jika error → halt rollout & fix

---

## 📝 Best Practices

### 1. Always Test in Internal Testing First
**NEVER** upload langsung ke production tanpa test!

### 2. Backup Keystore
- Simpan `kamuskorea-release.jks` di:
  - Local backup (external drive)
  - Cloud storage (Google Drive, encrypted)
  - Team password manager (1Password, LastPass)

### 3. Use Environment Variables for Secrets
```bash
# .bashrc atau .zshrc
export KEYSTORE_PASSWORD="your-password"
export KEY_PASSWORD="your-password"
```

### 4. Document Everything
- Simpan SHA-1 values di secure note
- Document keystore location & passwords
- Share dengan team lead/CTO (jika ada)

---

## 🎯 Quick Reference

### SHA-1 yang Perlu Didaftarkan:

| Build Type | Keystore | Purpose | Firebase? |
|------------|----------|---------|-----------|
| Debug | debug.keystore | Development | ✅ YES |
| Release (Local) | kamuskorea-release.jks | Testing | ✅ YES |
| Release (Play Store) | Google Play App Signing | Production | ✅ YES |

### Commands Cheat Sheet:

```bash
# Get SHA-1 (Debug)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Get SHA-1 (Release)
keytool -list -v -keystore kamuskorea-release.jks -alias kamuskorea

# Build Release
./gradlew bundleRelease

# Verify Signing
./gradlew signingReport
```

---

## 📞 Need Help?

1. Check Firebase Console → Authentication logs
2. Check Play Console → Pre-launch reports
3. Check Android Studio → Logcat during testing
4. Google's official docs: [Google Sign-In Setup](https://developers.google.com/identity/sign-in/android/start-integrating)

---

**Last Updated:** 2025-11-20
**Next Review:** Before production upload
