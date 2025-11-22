# 🔧 Troubleshooting: SHA Sudah Ditambahkan Tapi Masih Error

## 📋 Situasi Anda Saat Ini

✅ **Sudah dilakukan:**
- SHA-1 dari PC lokal sudah ditambahkan ke Firebase
- SHA-256 dari PC lokal sudah ditambahkan ke Firebase
- SHA-1 dari Google Play Console App signing key sudah ditambahkan ke Firebase
- SHA-256 dari Google Play Console App signing key sudah ditambahkan ke Firebase
- Aplikasi sudah di-download dari Google Play

❌ **Masalah yang terjadi:**
- Google Sign-In Hub Activity membuka lalu langsung tertutup
- Tidak ada error message yang jelas
- Login Google tidak berhasil

---

## 🔍 Analisis Log

Dari log yang Anda berikan:
```
2025-11-22 14:50:39.722  WindowManager  setClientVisible SignInHubActivity true
2025-11-22 14:50:41.203  WindowManager  setClientVisible SignInHubActivity false
```

**Artinya:** SignInHubActivity hanya terbuka selama ~1.5 detik lalu langsung ditutup. Ini adalah tanda khas:
1. **Web Client ID tidak cocok** dengan konfigurasi Firebase
2. **google-services.json tidak ter-update** setelah menambahkan SHA fingerprints
3. **OAuth consent screen** belum dikonfigurasi dengan benar
4. **Firebase App Check** memblokir request

---

## ✅ SOLUSI LENGKAP

### STEP 1: Verifikasi Firebase Project yang Benar

Aplikasi ini menggunakan Firebase project: **kamus-korea-apps-dcf09**

**PENTING:** Pastikan Anda menambahkan SHA fingerprints ke project yang **BENAR**!

1. Buka **Firebase Console**: https://console.firebase.google.com/
2. **PASTIKAN** Anda membuka project: **kamus-korea-apps-dcf09**
   - ⚠️ BUKAN project lain seperti `kamus-korea-6542e`
3. URL yang benar harus seperti:
   ```
   https://console.firebase.google.com/project/kamus-korea-apps-dcf09/...
   ```

---

### STEP 2: Verifikasi Web Client ID di Google Cloud Console

Web Client ID yang digunakan aplikasi:
```
237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1.apps.googleusercontent.com
```

**Verifikasi:**

1. Buka **Google Cloud Console**: https://console.cloud.google.com/
2. Pilih project: **kamus-korea-apps-dcf09**
3. Sidebar kiri → **APIs & Services** → **Credentials**
4. Cari **OAuth 2.0 Client IDs**
5. Cari Web client dengan ID: `237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1`

**Jika TIDAK DITEMUKAN:**
- ⚠️ Berarti Web Client ID salah!
- Anda perlu membuat OAuth 2.0 Client ID baru atau update kode dengan Client ID yang benar

**Jika DITEMUKAN:**
6. Klik untuk edit
7. Pastikan konfigurasi berikut:

**Authorized JavaScript origins:**
```
https://kamus-korea-apps-dcf09.firebaseapp.com
https://kamus-korea-apps-dcf09.web.app
```

**Authorized redirect URIs:**
```
https://kamus-korea-apps-dcf09.firebaseapp.com/__/auth/handler
https://kamus-korea-apps-dcf09.web.app/__/auth/handler
```

8. Klik **Save**

---

### STEP 3: Verifikasi Android OAuth Client

Selain Web Client ID, Google Sign-In juga membutuhkan **Android OAuth Client**.

1. Masih di **Google Cloud Console** → **Credentials**
2. Cari **Android client** dengan:
   - Package name: `com.webtech.kamuskorea`
   - SHA-1 fingerprints

**Jika TIDAK ADA Android OAuth Client:**
3. Klik **Create Credentials** → **OAuth client ID**
4. Application type: **Android**
5. Name: `Kamus Korea Android`
6. Package name: `com.webtech.kamuskorea`
7. **Tambahkan SEMUA SHA-1 fingerprints:**
   - SHA-1 dari debug keystore (PC lokal)
   - SHA-1 dari release keystore (PC lokal)
   - SHA-1 dari Google Play App Signing
8. Klik **Create**

**Jika SUDAH ADA Android OAuth Client:**
3. Klik untuk edit
4. Pastikan **SEMUA SHA-1** sudah ditambahkan:
   - SHA-1 debug
   - SHA-1 release
   - SHA-1 Google Play
5. Klik **Save**

---

### STEP 4: Download google-services.json Terbaru

**⚠️ INI SANGAT PENTING!**

Setelah menambahkan SHA fingerprints, Firebase akan generate konfigurasi baru. Anda **HARUS** download `google-services.json` yang baru!

1. Buka **Firebase Console**: https://console.firebase.google.com/project/kamus-korea-apps-dcf09/
2. Klik ⚙️ **Settings** → **Project settings**
3. Scroll ke **Your apps**
4. Cari aplikasi Android: `com.webtech.kamuskorea`
5. Klik **Download google-services.json**
6. **Simpan** file ke lokasi:
   ```
   app/google-services.json
   ```
7. **⚠️ PENTING:** File ini WAJIB ada di project, jika tidak ada maka Google Sign-In akan gagal!

---

### STEP 5: Verifikasi SHA Fingerprints di Firebase

Pastikan **SEMUA** SHA fingerprints sudah ditambahkan ke Firebase:

1. Buka **Firebase Console** → Project: `kamus-korea-apps-dcf09`
2. **Settings** → **Project settings** → **Your apps**
3. Pilih aplikasi Android: `com.webtech.kamuskorea`
4. Scroll ke **SHA certificate fingerprints**
5. Harus ada **MINIMAL 3 fingerprints**:

```
✅ SHA-1 (Debug - dari PC lokal)
✅ SHA-1 (Release - dari keystore lokal)
✅ SHA-1 (Google Play App Signing)
✅ SHA-256 (Google Play App Signing) - OPSIONAL tapi disarankan
```

**Cara mendapatkan SHA dari berbagai sumber:**

#### A. SHA-1 Debug (PC Lokal)
```bash
keytool -list -v -alias androiddebugkey \
  -keystore ~/.android/debug.keystore \
  -storepass android
```

#### B. SHA-1 Release (Keystore Lokal)
```bash
keytool -list -v -alias kamuskorea \
  -keystore kamuskorea-release.jks
```

#### C. SHA-1 & SHA-256 Google Play
1. **Google Play Console**: https://play.google.com/console/
2. Pilih aplikasi **Kamus Korea**
3. **Release** → **Setup** → **App integrity**
4. Bagian **App signing key certificate**
5. Copy **SHA-1** dan **SHA-256**

---

### STEP 6: Konfigurasi OAuth Consent Screen

Jika OAuth consent screen belum dikonfigurasi, Google Sign-In akan gagal.

1. **Google Cloud Console**: https://console.cloud.google.com/
2. Pilih project: **kamus-korea-apps-dcf09**
3. Sidebar → **APIs & Services** → **OAuth consent screen**
4. Pastikan:
   - User Type: **External** atau **Internal**
   - App name: Diisi
   - User support email: Diisi
   - Developer contact information: Diisi
5. **Scopes**: Minimal ada:
   ```
   .../auth/userinfo.email
   .../auth/userinfo.profile
   openid
   ```
6. **Test users** (jika masih Testing):
   - Tambahkan email tester Anda
7. Klik **Save and Continue**

---

### STEP 7: Disable Firebase App Check Sementara

Firebase App Check mungkin memblokir request dari aplikasi debug.

1. **Firebase Console** → Project: `kamus-korea-apps-dcf09`
2. Sidebar → **Build** → **App Check**
3. Pilih aplikasi Android: `com.webtech.kamuskorea`
4. **Sementara waktu**, set enforcement:
   - **Authentication**: Off atau Metrics only
5. Klik **Save**

**⚠️ Jangan lupa enable kembali setelah testing berhasil!**

---

### STEP 8: Clean Build dan Test Ulang

Setelah semua konfigurasi di atas:

```bash
# Clean project
./gradlew clean

# Build debug APK untuk testing
./gradlew assembleDebug

# Install ke device
./gradlew installDebug

# ATAU build release AAB untuk Play Console
./gradlew bundleRelease
```

---

### STEP 9: Testing dengan Logging Detail

Aplikasi sudah ditambahkan logging detail. Saat testing:

1. Hubungkan device via USB
2. Buka **Android Studio** → **Logcat**
3. Filter dengan tag: `AuthViewModel` dan `LoginScreen`
4. Klik tombol **Masuk dengan Google**
5. **Perhatikan log yang muncul:**

```
AuthViewModel: === GOOGLE SIGN IN CONFIGURATION ===
AuthViewModel: Web Client ID: 237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1
AuthViewModel: Package Name: com.webtech.kamuskorea
AuthViewModel: Google Sign-In Client created successfully

LoginScreen: === GOOGLE SIGN IN RESULT ===
LoginScreen: Result code: -1
LoginScreen: User completed sign-in flow
LoginScreen: Sign in successful
LoginScreen: Account email: [email]
LoginScreen: ID Token: Present

AuthViewModel: === GOOGLE SIGN IN WITH TOKEN ===
AuthViewModel: ID Token length: 850
AuthViewModel: Google credential created successfully
AuthViewModel: Firebase authentication successful
```

**Jika muncul error:**
```
LoginScreen: Status code: 10
LoginScreen: Developer Error: Check SHA-1 certificate fingerprint
```
→ **SHA-1 belum benar di Firebase atau Google Cloud**

```
LoginScreen: Status code: 12500
LoginScreen: Sign in currently unavailable
```
→ **google-services.json belum ter-update atau OAuth consent screen belum dikonfigurasi**

---

## 🐛 DEBUGGING CHECKLIST

Jika masih gagal, cek satu per satu:

- [ ] **Firebase project benar**: `kamus-korea-apps-dcf09` (BUKAN `kamus-korea-6542e`)
- [ ] **Web Client ID benar**: `237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1`
- [ ] **Android OAuth Client** sudah dibuat di Google Cloud Console
- [ ] **SHA-1 Debug** sudah ditambahkan ke Firebase dan Google Cloud
- [ ] **SHA-1 Release** sudah ditambahkan ke Firebase dan Google Cloud
- [ ] **SHA-1 Google Play** sudah ditambahkan ke Firebase dan Google Cloud
- [ ] **google-services.json** sudah di-download ulang dan disimpan di `app/google-services.json`
- [ ] **OAuth consent screen** sudah dikonfigurasi
- [ ] **Test user** sudah ditambahkan (jika OAuth status: Testing)
- [ ] **Firebase App Check** sudah di-disable sementara untuk testing
- [ ] **Clean build** sudah dilakukan
- [ ] **Logcat** sudah dicek untuk melihat error detail

---

## 📸 Kirim Informasi Berikut Jika Masih Gagal

1. **Screenshot Firebase Console:**
   - SHA certificate fingerprints (sembunyikan nilai lengkapnya)
   - Your apps section

2. **Screenshot Google Cloud Console:**
   - OAuth 2.0 Client IDs list
   - Android client configuration (jika ada)

3. **Logcat output:**
```bash
# Jalankan command ini saat testing:
adb logcat -s AuthViewModel:D LoginScreen:D
```

4. **File google-services.json** (baris pertama saja):
```bash
cat app/google-services.json | head -5
```

---

## 🔗 Referensi

- [Google Sign-In Configuration](https://firebase.google.com/docs/auth/android/google-signin)
- [OAuth Client ID](https://developers.google.com/identity/sign-in/android/start-integrating)
- [SHA Certificate Fingerprints](https://developers.google.com/android/guides/client-auth)
- [Firebase App Check](https://firebase.google.com/docs/app-check)

---

**Good luck! 🚀**

Jika semua langkah sudah diikuti dengan benar, Google Sign-In seharusnya berhasil.
