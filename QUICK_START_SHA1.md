# 🔑 Quick Start: Get SHA-1 untuk Firebase

## ✅ **Anda Sudah Punya Release Keystore!**

File: `kamuskorea-release.jks` ← Ini yang penting untuk production!

---

## 📋 **Step-by-Step Instructions**

### **Step 1: Get SHA-1 dari Release Keystore**

Buka **PowerShell** dan jalankan:

```powershell
# Di folder project
cd C:\Users\Administrator\Desktop\KamusKorea2

# Get SHA-1
keytool -list -v -keystore kamuskorea-release.jks -alias kamuskorea
```

**Masukkan password** yang Anda buat tadi.

**Output yang akan muncul:**
```
Alias name: kamuskorea
Creation date: Nov 20, 2025
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Petra Yoshua Marturia, OU=Web Tech Solution, O=Web Tech Solution, L=Tangerang, ST=Banten, C=17
Issuer: CN=Petra Yoshua Marturia, OU=Web Tech Solution, O=Web Tech Solution, L=Tangerang, ST=Banten, C=17
Serial number: xxxxxxxx
Valid from: Wed Nov 20 17:55:00 WIB 2025 until: ...
Certificate fingerprints:
         SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
         SHA256: XX:XX:XX:...
Signature algorithm name: SHA256withRSA
...
```

**COPY nilai SHA1** (format: XX:XX:XX:... dengan 20 pasang angka/huruf)

---

### **Step 2: Daftarkan SHA-1 ke Firebase Console**

1. Buka browser: **https://console.firebase.google.com/**
2. Login dengan akun Google Anda
3. Pilih project: **"kamuskorea"** atau nama project Firebase Anda
4. Klik ⚙️ **"Project Settings"** (pojok kiri bawah)
5. Scroll ke bagian **"Your apps"**
6. Pilih aplikasi Android: **`com.webtech.kamuskorea`**
7. Klik tombol **"Add fingerprint"**
8. **Paste SHA-1** yang sudah di-copy
9. Klik **"Save"**

---

### **Step 3: Download google-services.json**

Masih di halaman yang sama:

1. Scroll ke app `com.webtech.kamuskorea`
2. Klik tombol **"google-services.json"** (ada icon download)
3. **Save file** ke komputer
4. **Copy file** ke folder project:
   ```
   C:\Users\Administrator\Desktop\KamusKorea2\app\google-services.json
   ```
5. **Replace** jika sudah ada file lama

**PENTING:** File harus berada di folder `app\`, BUKAN di root project!

---

### **Step 4: Verify File Structure**

Pastikan struktur folder seperti ini:
```
KamusKorea2/
├── app/
│   ├── google-services.json          ← Harus ada di sini!
│   ├── build.gradle.kts
│   └── src/
├── kamuskorea-release.jks            ← Release keystore
├── build.gradle.kts
└── settings.gradle.kts
```

---

### **Step 5: Install Java 11+ (CRITICAL!)**

Error sebelumnya: "Dependency requires at least JVM runtime version 11"

Anda masih pakai **Java 8**, tapi perlu **Java 11+**.

#### **Download & Install Java 17:**

1. Download dari: **https://adoptium.net/temurin/releases/**
2. Pilih:
   - **Version:** 17 - LTS
   - **Operating System:** Windows
   - **Architecture:** x64
   - **Package Type:** JDK
3. Download & **Install** (ikuti wizard)
4. Install ke: `C:\Program Files\Eclipse Adoptium\jdk-17...`

#### **Update Environment Variables:**

**Cara Manual (Permanent):**
1. Search Windows: **"Environment Variables"**
2. Klik **"Environment Variables"**
3. Di **System variables**, cari `JAVA_HOME`:
   - Jika ada: **Edit** → Set ke `C:\Program Files\Eclipse Adoptium\jdk-17.x.x+x`
   - Jika tidak ada: **New** → Name: `JAVA_HOME`, Value: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x+x`
4. Edit **Path** → Pastikan ada: `%JAVA_HOME%\bin`
5. Klik **OK** semua
6. **Restart PowerShell**

**Cara PowerShell (Temporary):**
```powershell
# Set JAVA_HOME untuk session ini saja
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x+x"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Verify
java -version
# Output harus: openjdk version "17.x.x"
```

---

### **Step 6: Build Project**

Setelah Java 11+ terinstall:

```powershell
# Clean project
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install ke device/emulator
./gradlew installDebug
```

---

### **Step 7: Test Google Sign In**

1. Buka aplikasi di device/emulator
2. Klik **"Masuk dengan Google"**
3. Pilih akun Google
4. **Login harus berhasil!** ✅

**Expected log output:**
```
D/LoginScreen: === STARTING GOOGLE SIGN IN ===
D/LoginScreen: Sign in intent created successfully
D/LoginScreen: === GOOGLE SIGN IN RESULT ===
D/LoginScreen: Result code: -1  ← RESULT_OK (bukan 0!)
D/LoginScreen: Sign in successful
D/LoginScreen: Account email: user@gmail.com
D/LoginScreen: ID Token: Present
```

---

## 🔥 **Troubleshooting**

### **Problem: "Invalid keystore format" untuk debug.keystore**

**Solusi:** Tidak perlu debug keystore untuk production! Pakai release keystore yang sudah dibuat.

Jika tetap mau debug keystore:
```powershell
# Regenerate debug keystore
keytool -genkey -v -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000
```

---

### **Problem: Gradle error "Java 8"**

**Solusi:** Install Java 17 (lihat Step 5)

---

### **Problem: google-services.json not found**

**Solusi:** Pastikan file di folder `app\`, bukan di root!

---

### **Problem: Google Sign In masih gagal (RESULT_CANCELED)**

**Checklist:**
- [ ] SHA-1 sudah didaftarkan ke Firebase?
- [ ] google-services.json sudah di-download ulang setelah add SHA-1?
- [ ] File google-services.json ada di `app/` folder?
- [ ] Sudah rebuild project? (`./gradlew clean && ./gradlew assembleDebug`)
- [ ] Tunggu 5-10 menit untuk propagasi konfigurasi Firebase

---

## 📱 **Untuk Development (Optional)**

Jika mau test di development mode dengan debug keystore:

1. Build project sekali (akan generate debug keystore):
   ```powershell
   ./gradlew assembleDebug
   ```

2. Get SHA-1 debug:
   ```powershell
   keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```

3. Daftarkan SHA-1 debug ke Firebase (tambahan, bukan replace)

---

## 🎯 **Summary Checklist**

**For Now (Development Testing):**
- [x] Release keystore sudah dibuat: `kamuskorea-release.jks`
- [ ] Get SHA-1 dari release keystore
- [ ] Daftarkan SHA-1 ke Firebase Console
- [ ] Download google-services.json
- [ ] Place file di `app/google-services.json`
- [ ] Install Java 17
- [ ] Set JAVA_HOME
- [ ] Build: `./gradlew clean && ./gradlew assembleDebug`
- [ ] Test Google Sign In

**For Production (Later):**
- [ ] Get SHA-1 dari Google Play App Signing (setelah upload)
- [ ] Daftarkan SHA-1 ke-3 ke Firebase
- [ ] Download google-services.json baru
- [ ] Rebuild & re-upload

---

## 🆘 **Need Help?**

Jika masih ada masalah:
1. Check log: `adb logcat | grep -E "LoginScreen|AuthViewModel"`
2. Verify SHA-1 di Firebase Console
3. Verify google-services.json location
4. Restart Android Studio
5. Clean & rebuild: `./gradlew clean build`

---

**Files:**
- ✅ `kamuskorea-release.jks` - Release keystore (BACKUP INI!)
- ⏳ `app/google-services.json` - Download dari Firebase
- 📖 `PRODUCTION_GOOGLE_SIGNIN_GUIDE.md` - Panduan lengkap
- 📖 `GOOGLE_SIGN_IN_SETUP.md` - Setup guide

**Next:** Setelah semua langkah selesai, Google Sign In akan bekerja! 🎉
