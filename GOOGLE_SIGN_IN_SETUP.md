# 🔐 Cara Memperbaiki Google Sign In

## ❌ Masalah yang Terjadi

Google Sign In **langsung dibatalkan** (RESULT_CANCELED) karena:
1. **SHA-1 fingerprint** tidak terdaftar di Firebase Console
2. **google-services.json** tidak ada atau tidak up-to-date
3. OAuth consent screen belum dikonfigurasi

---

## ✅ Solusi Lengkap

### Step 1: Dapatkan SHA-1 Fingerprint

#### Untuk Debug Build (Development):

**Opsi A - Menggunakan Gradle (RECOMMENDED):**
```bash
cd /path/to/kamuskorea
./gradlew signingReport
```

Cari output yang mirip seperti ini:
```
Variant: debug
Config: debug
Store: ~/.android/debug.keystore
Alias: androiddebugkey
MD5: XX:XX:XX:...
SHA1: A1:B2:C3:D4:E5:F6:... ← COPY INI
SHA-256: XX:XX:XX:...
```

**Opsi B - Menggunakan Keytool:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy **SHA1 fingerprint** yang muncul.

#### Untuk Release Build (Production):

```bash
keytool -list -v -keystore /path/to/your-release-key.jks -alias your-key-alias
```

---

### Step 2: Daftarkan SHA-1 ke Firebase Console

1. Buka **[Firebase Console](https://console.firebase.google.com/)**
2. Pilih project: **kamuskorea**
3. Klik **⚙️ Project Settings**
4. Scroll ke bagian **"Your apps"**
5. Pilih app Android Anda: `com.webtech.kamuskorea`
6. Klik **"Add fingerprint"**
7. **Paste SHA-1 fingerprint** yang sudah di-copy
8. Klik **"Save"**

#### Tambahkan kedua fingerprint (Debug + Release):
- **Debug SHA-1** - untuk development/testing
- **Release SHA-1** - untuk production build

---

### Step 3: Download google-services.json Terbaru

1. Masih di Firebase Console → **Project Settings**
2. Scroll ke app `com.webtech.kamuskorea`
3. Klik **"google-services.json"** untuk download
4. **Replace** file lama di:
   ```
   kamuskorea/app/google-services.json
   ```

---

### Step 4: Verifikasi Web Client ID

1. Di Firebase Console → **Project Settings**
2. Tab **"Service Accounts"** atau **"Google Sign-In"**
3. Copy **Web Client ID**, contoh:
   ```
   214644364883-f0oh0k0lnd3buj07se4rlpmqd2s1lo33.apps.googleusercontent.com
   ```

4. Pastikan sama dengan yang ada di `AuthViewModel.kt:47`:
   ```kotlin
   const val WEB_CLIENT_ID = "214644364883-f0oh0k0lnd3buj07se4rlpmqd2s1lo33.apps.googleusercontent.com"
   ```

---

### Step 5: Konfigurasi OAuth Consent Screen

1. Buka **[Google Cloud Console](https://console.cloud.google.com/)**
2. Pilih project yang sama dengan Firebase
3. Menu: **APIs & Services** → **OAuth consent screen**
4. Isi informasi:
   - **App name**: Kamus Korea
   - **User support email**: your-email@example.com
   - **Developer contact**: your-email@example.com
5. **Scopes**: Minimal pilih `email` dan `profile`
6. **Test users** (jika app status "Testing"): Tambahkan email yang akan digunakan untuk testing
7. Klik **Save**

---

### Step 6: Clean & Rebuild Project

```bash
cd /path/to/kamuskorea
./gradlew clean
./gradlew assembleDebug
```

atau di Android Studio:
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

---

### Step 7: Install & Test

```bash
./gradlew installDebug
```

atau:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Test Google Sign In di aplikasi!

---

## 🔍 Troubleshooting

### Masalah: "Developer Error" atau "Error 10"

**Penyebab:** SHA-1 fingerprint tidak cocok atau belum terdaftar

**Solusi:**
1. Pastikan SHA-1 sudah terdaftar di Firebase Console
2. Download google-services.json baru
3. Clean & rebuild project
4. Tunggu 5-10 menit untuk propagasi konfigurasi

---

### Masalah: Sign In Screen Langsung Hilang (Cancelled)

**Penyebab:** google-services.json tidak ada atau Web Client ID salah

**Solusi:**
1. Pastikan file `app/google-services.json` ada dan up-to-date
2. Verifikasi Web Client ID di AuthViewModel.kt
3. Pastikan OAuth consent screen sudah dikonfigurasi

---

### Masalah: "This app is not verified"

**Penyebab:** OAuth consent screen dalam status "Testing"

**Solusi Sementara:**
- Tambahkan email testing ke **Test users** di OAuth consent screen

**Solusi Permanen:**
- Submit app untuk **verification** di Google Cloud Console
- Proses ini bisa memakan waktu beberapa hari

---

### Masalah: Multiple SHA-1 untuk Device yang Berbeda

Jika testing di multiple devices/emulators:

1. Get SHA-1 untuk setiap device:
   ```bash
   adb shell pm list packages -f com.webtech.kamuskorea
   adb pull /data/app/.../base.apk
   keytool -printcert -jarfile base.apk
   ```

2. Daftarkan semua SHA-1 ke Firebase Console

---

## 📱 Testing Google Sign In

Setelah setup selesai:

1. ✅ Buka aplikasi
2. ✅ Klik "Masuk dengan Google"
3. ✅ Pilih akun Google
4. ✅ Login berhasil!

**Expected Log Output:**
```
D/LoginScreen: === STARTING GOOGLE SIGN IN ===
D/LoginScreen: Sign in intent created successfully
D/LoginScreen: === GOOGLE SIGN IN RESULT ===
D/LoginScreen: Result code: -1  ← RESULT_OK
D/LoginScreen: Sign in successful
D/LoginScreen: ID Token: Present
D/AuthViewModel: ✅ User synced to MySQL successfully
```

---

## 📚 Resources

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start-integrating)
- [OAuth Consent Screen Setup](https://support.google.com/cloud/answer/10311615)

---

## 🆘 Masih Ada Masalah?

Jika masih error, cek log dengan:
```bash
adb logcat | grep -E "LoginScreen|AuthViewModel|GoogleSignIn"
```

Cari error message dan konsultasikan dengan dokumentasi di atas.
