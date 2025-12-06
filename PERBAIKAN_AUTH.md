# 🔧 Perbaikan Masalah Autentikasi Google Sign-In

## 📝 Ringkasan Masalah

**Gejala:**
- Aplikasi sudah di-download dari Google Play
- SHA-1 dan SHA-256 sudah ditambahkan dari PC lokal dan Google Play Console
- Google Sign-In Hub Activity membuka lalu langsung tertutup (~1.5 detik)
- Tidak ada error message yang jelas

**Penyebab:**
Masalah ini biasanya terjadi karena salah satu dari berikut:
1. SHA fingerprints ditambahkan ke Firebase project yang salah
2. `google-services.json` tidak ter-update setelah menambahkan SHA
3. Android OAuth Client belum dibuat di Google Cloud Console
4. Web Client ID tidak cocok
5. OAuth consent screen belum dikonfigurasi

---

## ✅ Solusi yang Sudah Diterapkan

### 1. Logging Detail Ditambahkan

File yang diupdate:
- `app/src/main/java/com/webtech/kamuskorea/ui/screens/auth/AuthViewModel.kt`

**Logging baru yang ditambahkan:**
```kotlin
// Saat membuat Google Sign-In Intent:
Log.d(TAG, "=== GOOGLE SIGN IN CONFIGURATION ===")
Log.d(TAG, "Web Client ID: $WEB_CLIENT_ID")
Log.d(TAG, "Package Name: ${context.packageName}")

// Saat sign in dengan token:
Log.d(TAG, "=== GOOGLE SIGN IN WITH TOKEN ===")
Log.d(TAG, "ID Token length: ${idToken.length}")
Log.d(TAG, "Google credential created successfully")
Log.d(TAG, "Firebase authentication successful")
```

**Cara melihat log:**
```bash
# Hubungkan device via USB
adb logcat -s AuthViewModel:D LoginScreen:D
```

### 2. Script Checker SHA

File baru dibuat: `check_sha.sh`

**Cara menggunakan:**
```bash
./check_sha.sh
```

Script ini akan:
- ✅ Memeriksa SHA-1 dari debug keystore
- ✅ Memeriksa SHA-1 dari release keystore
- ✅ Memberikan instruksi untuk mendapatkan SHA dari Google Play
- ✅ Memeriksa apakah `google-services.json` ada
- ✅ Menampilkan checklist verifikasi

### 3. Panduan Troubleshooting Lengkap

File baru dibuat: `TROUBLESHOOTING_SHA_ISSUES.md`

Panduan ini mencakup:
- ✅ Cara memverifikasi Firebase project yang benar
- ✅ Cara memverifikasi Web Client ID
- ✅ Cara membuat Android OAuth Client
- ✅ Cara download `google-services.json` terbaru
- ✅ Cara konfigurasi OAuth consent screen
- ✅ Cara disable Firebase App Check sementara
- ✅ Checklist debugging lengkap

---

## 🚀 Langkah-Langkah Selanjutnya

### STEP 1: Jalankan Script Checker

```bash
./check_sha.sh
```

Script ini akan menampilkan semua SHA fingerprints yang perlu ditambahkan ke Firebase.

### STEP 2: Verifikasi Konfigurasi

Baca panduan lengkap di: **[TROUBLESHOOTING_SHA_ISSUES.md](./TROUBLESHOOTING_SHA_ISSUES.md)**

**Poin penting yang harus dicek:**

1. **Firebase Project yang Benar:**
   - Harus: `kamus-korea-apps-dcf09`
   - URL: https://console.firebase.google.com/project/kamus-korea-apps-dcf09/

2. **Web Client ID yang Benar:**
   - `237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1.apps.googleusercontent.com`

3. **Android OAuth Client:**
   - Buat di Google Cloud Console
   - Package: `com.webtech.kamuskorea`
   - Tambahkan SEMUA SHA-1 (debug, release, Play Console)

4. **google-services.json:**
   - Download ulang dari Firebase setelah menambahkan SHA
   - Simpan di: `app/google-services.json`
   - ⚠️ File ini WAJIB ada!

### STEP 3: Build dan Test

Setelah semua konfigurasi benar:

```bash
# Clean project
./gradlew clean

# Build debug untuk testing lokal
./gradlew assembleDebug
./gradlew installDebug

# Build release untuk Play Console
./gradlew bundleRelease
```

### STEP 4: Lihat Log Detail

Saat testing, hubungkan device via USB dan jalankan:

```bash
adb logcat -s AuthViewModel:D LoginScreen:D
```

**Log yang menandakan SUCCESS:**
```
AuthViewModel: Google Sign-In Client created successfully
LoginScreen: Sign in successful
LoginScreen: ID Token: Present
AuthViewModel: Firebase authentication successful
```

**Log yang menandakan ERROR:**
```
LoginScreen: Status code: 10
LoginScreen: Developer Error: Check SHA-1 certificate fingerprint
```
→ SHA-1 belum benar

```
LoginScreen: Status code: 12500
LoginScreen: Sign in currently unavailable
```
→ google-services.json atau OAuth consent screen belum dikonfigurasi

---

## 📋 Checklist Verifikasi

Centang semua item berikut sebelum testing ulang:

- [ ] **Script checker sudah dijalankan:** `./check_sha.sh`
- [ ] **Firebase project benar:** `kamus-korea-apps-dcf09`
- [ ] **SHA-1 Debug** sudah ditambahkan ke Firebase
- [ ] **SHA-1 Release** sudah ditambahkan ke Firebase
- [ ] **SHA-1 Google Play** sudah ditambahkan ke Firebase
- [ ] **Android OAuth Client** sudah dibuat di Google Cloud Console
- [ ] **Web Client ID** sudah diverifikasi: `237948772817-e1hv2gvso08nbajnpfdmbm73i1etqar1`
- [ ] **google-services.json** sudah di-download ulang dan disimpan di `app/google-services.json`
- [ ] **OAuth consent screen** sudah dikonfigurasi
- [ ] **Firebase App Check** sudah di-disable sementara (untuk testing)
- [ ] **Clean build** sudah dilakukan
- [ ] **Logcat** sudah diperiksa untuk error detail

---

## 🐛 Jika Masih Gagal

Kirim informasi berikut:

1. **Output dari script checker:**
   ```bash
   ./check_sha.sh > sha_output.txt
   ```

2. **Logcat saat error:**
   ```bash
   adb logcat -s AuthViewModel:D LoginScreen:D > logcat_error.txt
   ```

3. **Screenshot Firebase Console:**
   - SHA certificate fingerprints (sembunyikan nilai lengkap)
   - Your apps section

4. **Screenshot Google Cloud Console:**
   - OAuth 2.0 Client IDs list

5. **Konfirmasi google-services.json:**
   ```bash
   ls -lh app/google-services.json
   cat app/google-services.json | head -10
   ```

---

## 📚 File Dokumentasi

1. **TROUBLESHOOTING_SHA_ISSUES.md** - Panduan lengkap troubleshooting SHA
2. **TROUBLESHOOTING_AUTH.md** - Panduan umum autentikasi
3. **TROUBLESHOOTING_APPCHECK.md** - Panduan Firebase App Check
4. **check_sha.sh** - Script untuk cek SHA fingerprints

---

## 🔗 Link Penting

- **Firebase Console:** https://console.firebase.google.com/project/kamus-korea-apps-dcf09/
- **Google Cloud Console:** https://console.cloud.google.com/apis/credentials?project=kamus-korea-apps-dcf09
- **Google Play Console:** https://play.google.com/console/

---

**Good luck! 🚀**

Jika ada pertanyaan, silakan buat issue atau hubungi developer.
