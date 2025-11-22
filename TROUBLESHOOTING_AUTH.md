# 🔧 Troubleshooting Autentikasi - Play Console Closed Testing

## ❌ MASALAH

Setelah upload AAB ke Play Console (closed testing):
1. ❌ **Login Google tidak berfungsi**
2. ❌ **Registrasi akun baru gagal**

## 🔍 PENYEBAB UTAMA

### 1. SHA-256 Fingerprint Berubah

Ketika aplikasi di-upload ke Play Console, **Google Play App Signing** akan menandatangani aplikasi dengan key mereka sendiri, bukan dengan keystore development Anda.

**Akibatnya:**
- SHA-256 fingerprint berubah
- Google Sign-In configuration tidak cocok lagi
- Error code 10: "Developer Error" muncul

**Bukti di kode** (`LoginScreen.kt:79-81`):
```kotlin
10 -> {
    Log.e("LoginScreen", "Developer Error: Check SHA-1 certificate fingerprint in Firebase Console")
    "Konfigurasi Google Sign-In bermasalah..."
}
```

### 2. Firebase Configuration Tidak Lengkap

File `google-services.json` mungkin tidak up-to-date atau tidak ada di repository.

### 3. OAuth Web Client ID Configuration

Web Client ID yang digunakan di `AuthViewModel.kt:49`:
```kotlin
const val WEB_CLIENT_ID = "214644364883-f0oh0k0lnd3buj07se4rlpmqd2s1lo33.apps.googleusercontent.com"
```

Harus dikonfigurasi dengan SHA-256 yang benar.

---

## ✅ SOLUSI LENGKAP

### STEP 1: Dapatkan SHA-256 dari Play Console

1. Buka **Google Play Console**: https://play.google.com/console/
2. Pilih aplikasi **EPS TOPIK** (Kamus Korea)
3. Di sidebar kiri, pergi ke: **Release** → **Setup** → **App Integrity**

   Atau langsung: **Release** → **Setup** → **App signing**

4. Scroll ke bagian **App signing key certificate**

5. **COPY SHA-256 certificate fingerprint** (bukan SHA-1!)
   ```
   Contoh format:
   AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB
   ```

6. **COPY juga SHA-1** (untuk Firebase configuration)

---

### STEP 2: Tambahkan SHA-256 ke Firebase Console

#### A. Buka Firebase Console

1. Pergi ke: https://console.firebase.google.com/
2. Pilih project: **kamus-korea-6542e**
3. Klik ikon **Settings** (⚙️) di sidebar kiri → **Project settings**

#### B. Tambahkan Fingerprint

4. Scroll ke bagian **Your apps**
5. Cari aplikasi Android: **com.webtech.kamuskorea**
6. Klik nama aplikasi untuk expand detailnya
7. Scroll ke **SHA certificate fingerprints**
8. Klik tombol **Add fingerprint**

#### C. Masukkan SHA-1 dan SHA-256

9. Tambahkan **SHA-1** dari Play Console → Klik **Save**
10. Klik **Add fingerprint** lagi
11. Tambahkan **SHA-256** dari Play Console → Klik **Save**

**Screenshot referensi:**
```
SHA certificate fingerprints
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SHA-1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
SHA-256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY...
                                                           [+ Add fingerprint]
```

---

### STEP 3: Download google-services.json Baru

#### ⚠️ SANGAT PENTING!

Setelah menambahkan fingerprint, Firebase akan generate konfigurasi baru.

1. Masih di **Firebase Console** → **Project settings** → **Your apps**
2. Cari aplikasi Android: **com.webtech.kamuskorea**
3. Klik tombol **Download google-services.json**
4. **Replace** file lama dengan yang baru di path:
   ```
   app/google-services.json
   ```

5. **COMMIT dan PUSH** perubahan ini ke repository!

---

### STEP 4: Verifikasi OAuth 2.0 Client di Google Cloud Console

#### A. Buka Google Cloud Console

1. Pergi ke: https://console.cloud.google.com/
2. Pilih project yang sama: **kamus-korea-6542e**

   (Biasanya Firebase project otomatis terlink dengan Google Cloud project)

#### B. Periksa OAuth Credentials

3. Di sidebar kiri, klik **APIs & Services** → **Credentials**
4. Cari **OAuth 2.0 Client IDs**
5. Cari Web client dengan ID:
   ```
   214644364883-f0oh0k0lnd3buj07se4rlpmqd2s1lo33.apps.googleusercontent.com
   ```

6. Klik untuk edit (atau pastikan sudah dikonfigurasi dengan benar)

#### C. Konfigurasi Yang Diperlukan

**Authorized JavaScript origins:**
```
https://kamus-korea-6542e.firebaseapp.com
https://kamus-korea-6542e.web.app
```

**Authorized redirect URIs:**
```
https://kamus-korea-6542e.firebaseapp.com/__/auth/handler
https://kamus-korea-6542e.web.app/__/auth/handler
```

7. Klik **Save** jika ada perubahan

---

### STEP 5: Build dan Upload AAB Baru

#### A. Clean dan Rebuild Project

```bash
# Clean project
./gradlew clean

# Build release AAB
./gradlew bundleRelease
```

#### B. Upload ke Play Console

1. Buka **Google Play Console**
2. Pergi ke **Release** → **Testing** → **Closed testing**
3. Klik **Create new release**
4. Upload AAB baru dari:
   ```
   app/build/outputs/bundle/release/app-release.aab
   ```
5. Isi **Release notes**:
   ```
   - Fix Google Sign-In authentication
   - Fix user registration
   - Update Firebase configuration
   ```
6. Klik **Review release** → **Start rollout to Closed testing**

---

### STEP 6: Testing

#### A. Tunggu Processing

Tunggu ~15-30 menit hingga Google Play memproses AAB baru.

#### B. Update Aplikasi di Device Testing

1. Buka Play Store di device tester
2. Cari aplikasi **EPS TOPIK** (Kamus Korea)
3. Klik **Update**
4. Setelah update, buka aplikasi

#### C. Test Google Sign-In

1. Klik **Masuk dengan Google**
2. Pilih akun Google
3. **Seharusnya berhasil login** ✅

#### D. Test Registrasi

1. Logout dari aplikasi
2. Klik **Daftar Sekarang**
3. Isi form:
   - Nama lengkap
   - Email
   - Password
   - Konfirmasi password
   - Jawab CAPTCHA matematika
4. Klik **Daftar**
5. **Seharusnya berhasil register** ✅

---

## 🐛 DEBUGGING - Jika Masih Gagal

### 1. Check Logs di Android Studio Logcat

Jika masih gagal, hubungkan device via USB dan cek logcat:

```
# Filter untuk Google Sign-In
TAG: LoginScreen
TAG: AuthViewModel

# Cari error messages:
"Developer Error: Check SHA-1 certificate fingerprint"
"Google sign in failed"
```

### 2. Check API Backend

Jika registrasi gagal, periksa response dari backend:

```
TAG: AuthViewModel
TAG: AuthInterceptor

# Cari log:
"⚠️ MySQL sync failed"
"❌ Failed to get Firebase token"
```

### 3. Verifikasi Firebase Auth di Firebase Console

1. Buka **Firebase Console** → **Authentication**
2. Tab **Users** - Cek apakah user baru terdaftar setelah registrasi
3. Tab **Sign-in method** - Pastikan:
   - ✅ Email/Password: **Enabled**
   - ✅ Google: **Enabled** dengan Web SDK configuration

### 4. Test dengan Debug Build Dulu

Sebelum upload ke Play Console, test dulu dengan debug build:

```bash
# Install debug APK
./gradlew installDebug

# Atau build APK
./gradlew assembleDebug
```

Jika debug build berhasil tapi release AAB di Play Console gagal, berarti masalahnya pasti di SHA-256 fingerprint.

---

## 📋 CHECKLIST VERIFIKASI

Sebelum upload AAB baru, pastikan:

- [ ] SHA-1 dari Play Console sudah ditambahkan ke Firebase Console
- [ ] SHA-256 dari Play Console sudah ditambahkan ke Firebase Console
- [ ] File `google-services.json` sudah di-download ulang dan di-replace
- [ ] OAuth Web Client ID sudah dikonfigurasi dengan benar di Google Cloud Console
- [ ] Firebase Authentication sudah enable Email/Password dan Google
- [ ] Backend API `https://webtechsolution.my.id/kamuskorea/` dapat diakses
- [ ] Project sudah di-clean dan di-rebuild
- [ ] AAB baru sudah di-upload ke Play Console

---

## 🆘 MASIH BELUM BERHASIL?

Jika setelah mengikuti semua langkah di atas masih gagal:

1. **Kirim screenshot error message** dari aplikasi
2. **Kirim logcat** dari Android Studio saat error terjadi
3. **Verifikasi SHA-256** - pastikan yang di-copy dari Play Console sama dengan yang di-paste di Firebase Console
4. **Cek Firebase Authentication** - pastikan Email/Password dan Google sign-in method sudah enabled

---

## 📚 REFERENSI

- [Firebase Authentication Setup](https://firebase.google.com/docs/auth/android/start)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [SHA certificate fingerprints](https://developers.google.com/android/guides/client-auth)

---

**Good luck! 🚀**

Jika ada pertanyaan, silakan hubungi developer atau buat issue di repository.
