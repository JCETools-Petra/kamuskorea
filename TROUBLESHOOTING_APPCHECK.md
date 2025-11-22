# 🔧 Troubleshooting Firebase App Check - "Too Many Attempts"

## ❌ ERROR YANG TERJADI

```
Error getting App Check token; using placeholder token instead.
Error: com.google.firebase.FirebaseException: Too many attempts.

Firebase App Check token is invalid
```

## 🔍 PENYEBAB

Error ini terjadi karena:

1. **App Check dalam mode "Enforced"** - memblokir semua request tanpa token yang valid
2. **Debug token belum didaftarkan** di Firebase Console (jika mode Enforced)
3. **Debug token salah** - token yang didaftarkan tidak sesuai dengan yang di-generate aplikasi
4. **Token belum aktif** - baru didaftarkan dan belum propagate
5. **Terlalu banyak request** - aplikasi mencoba request token terlalu banyak kali

## 🎯 QUICK DECISION: Opsi Mana Yang Harus Dipilih?

| Situasi | Pilih Opsi | Alasan |
|---------|-----------|--------|
| **Development/Testing** | **OPSI 1: Permissive Mode** | ✅ Tercepat, tidak perlu debug token |
| **Banyak device/emulator** | **OPSI 1: Permissive Mode** | ✅ Tidak perlu daftar token untuk tiap device |
| **Simulasi production** | OPSI 2: Debug Token | Untuk test dengan security penuh |
| **Production/Release** | ❌ Jangan ubah App Check | Tetap gunakan Enforced mode + Play Integrity |

**REKOMENDASI: Gunakan OPSI 1 (Permissive Mode) untuk development!**

## ✅ SOLUSI LENGKAP

### 🔧 OPSI 1: PERMISSIVE MODE (TERCEPAT & DIREKOMENDASIKAN UNTUK DEVELOPMENT)

**Ini adalah solusi tercepat tanpa perlu register debug token!**

#### Apa itu Permissive Mode?

Permissive Mode memungkinkan semua request ke Firebase (Auth, Firestore, dll) **tanpa memerlukan App Check token yang valid**. Request yang tidak terverifikasi akan tetap diizinkan, tapi akan dicatat (logged) di Firebase Console.

**PENTING:**
- ✅ **Direkomendasikan untuk DEVELOPMENT/TESTING**
- ⚠️ **JANGAN digunakan untuk PRODUCTION** (gunakan Enforced mode untuk production)

#### Langkah-langkah:

1. **Buka Firebase Console:**
   ```
   https://console.firebase.google.com/project/kamus-korea-apps-dcf09/appcheck
   ```

2. **Klik tab "APIs"** (bukan "Apps")

3. **Untuk setiap service yang digunakan:**

   Kamu akan melihat daftar services seperti:
   - Firebase Authentication
   - Cloud Firestore
   - Cloud Storage
   - dll.

4. **Ubah mode dari "Enforced" ke "Permissive":**

   Untuk setiap service:
   - Klik service (misal: Firebase Authentication)
   - Klik icon **Settings (⚙️)** di sebelah kanan
   - Ubah dari **"Enforced"** menjadi **"Permissive"**
   - Klik **"Save"**

   ```
   Firebase Authentication
   ├─ Enforcement: Enforced ❌  →  Permissive ✅
   └─ [Settings ⚙️]
   ```

5. **Ulangi untuk semua services** yang digunakan (minimal Firebase Authentication)

6. **Restart aplikasi** dan test Google Sign-In

7. **SELESAI!** Google Sign-In seharusnya langsung berfungsi tanpa perlu debug token! 🎉

#### Kapan menggunakan Permissive Mode?

✅ **GUNAKAN saat:**
- Development/testing lokal
- Debugging authentication issues
- Testing di emulator atau physical device
- Tidak ingin repot manage debug tokens untuk banyak device

❌ **JANGAN GUNAKAN saat:**
- Aplikasi sudah di production/release
- Ingin security yang maksimal saat testing

---

### 🔑 OPSI 2: DEBUG TOKEN (Jika Tetap Ingin Gunakan Enforced Mode)

Jika kamu tetap ingin menggunakan **Enforced Mode** untuk testing (untuk simulasi production behavior), ikuti langkah berikut:

#### STEP 1: Dapatkan Debug Token dari Logcat

#### A. Jalankan Aplikasi

1. Build dan install aplikasi:
   ```bash
   ./gradlew installDebug
   ```

2. Buka aplikasi di device/emulator

#### B. Cari Debug Token di Logcat

3. Di Android Studio, buka **Logcat** (View → Tool Windows → Logcat)

4. Filter dengan tag `DebugAppCheckProvider`:
   ```
   tag:DebugAppCheckProvider
   ```

5. Cari log yang berisi:
   ```
   DebugAppCheckProvider: Enter this debug secret into the allow list in
   the Firebase Console for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
   ```

6. **COPY** token tersebut (format UUID):
   ```
   XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
   ```

   **Contoh token asli:**
   ```
   A1B2C3D4-E5F6-7890-ABCD-EF1234567890
   ```

#### C. Alternatif - Gunakan Tag AppCheck

Jika tidak menemukan dengan tag `DebugAppCheckProvider`, coba:

7. Filter Logcat dengan tag: `AppCheck`

8. Lihat log instruksi lengkap yang sudah disediakan aplikasi

---

### STEP 2: Daftarkan Debug Token di Firebase Console

#### A. Buka Firebase Console

1. Buka browser dan pergi ke: https://console.firebase.google.com/

2. Login dengan akun Google yang memiliki akses ke project

3. Pilih project: **kamus-korea-apps-dcf09**

   (atau project Firebase yang sedang digunakan)

#### B. Navigasi ke App Check

4. Di sidebar kiri, klik **App Check**

5. Di tab **Apps**, cari aplikasi: **com.webtech.kamuskorea**

6. Jika aplikasi belum terdaftar di App Check:
   - Klik **Register app** atau **Add app**
   - Pilih platform: **Android**
   - Masukkan package name: `com.webtech.kamuskorea`
   - Klik **Register**

#### C. Tambahkan Debug Token

7. Di halaman App Check, scroll ke bagian **Debug tokens**

8. Klik tombol **Add debug token** atau **Manage debug tokens**

9. Klik **Add token**

10. **Paste** debug token yang sudah di-copy dari Logcat:
    ```
    XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
    ```

11. (Opsional) Beri nama token, misalnya:
    ```
    My Development Device
    ```
    atau
    ```
    Android Emulator
    ```

12. Klik **Add** atau **Save**

**Screenshot referensi:**
```
Firebase App Check
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Apps
  com.webtech.kamuskorea (Android)

Debug tokens
  ┌─────────────────────────────────────────┐
  │ A1B2C3D4-E5F6-7890-ABCD-EF1234567890    │ My Development Device
  └─────────────────────────────────────────┘
                              [+ Add debug token]
```

---

### STEP 3: Tunggu Token Aktif

⏰ **PENTING:** Setelah menambahkan debug token:

1. **Tunggu 1-2 menit** untuk token aktif
   - Firebase perlu waktu untuk propagate token ke semua server
   - Jangan langsung test, tunggu dulu!

2. **Tutup aplikasi** sepenuhnya:
   - Swipe aplikasi dari recent apps
   - Atau: Settings → Apps → Kamus Korea → Force Stop

3. **Bersihkan cache** (opsional tapi direkomendasikan):
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```

---

### STEP 4: Test Aplikasi

#### A. Restart Aplikasi

1. Buka aplikasi lagi setelah tunggu 1-2 menit

2. Perhatikan Logcat dengan filter tag: `AppCheck`

3. Seharusnya muncul log:
   ```
   AppCheck: ✅ App Check Debug Provider initialized
   AppCheck: ✅ App Check token berhasil di-generate
   ```

#### B. Test Google Sign In

4. Klik tombol **Masuk dengan Google**

5. Pilih akun Google

6. **Seharusnya berhasil login tanpa error!** ✅

#### C. Cek Log Success

7. Di Logcat, filter dengan tag: `LoginScreen` dan `AuthViewModel`

8. Seharusnya TIDAK ada error:
   ```
   ❌ Error getting App Check token (SEHARUSNYA TIDAK ADA LAGI)
   ❌ Firebase App Check token is invalid (SEHARUSNYA TIDAK ADA LAGI)
   ```

9. Seharusnya muncul:
   ```
   ✅ Sign in successful
   ✅ Account email: your-email@gmail.com
   ```

---

## 🐛 TROUBLESHOOTING - Jika Masih Gagal

### Problem 1: Token Tidak Muncul di Logcat

**Solusi:**

1. Pastikan aplikasi dalam **DEBUG mode**, bukan RELEASE mode

2. Check `BuildConfig.DEBUG`:
   ```bash
   # Pastikan build variant adalah debug
   # Di Android Studio: Build → Select Build Variant → debug
   ```

3. Rebuild aplikasi:
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```

4. Restart aplikasi dan cek Logcat lagi

---

### Problem 2: Error "Too Many Attempts" Masih Muncul

**Kemungkinan:**

1. **Token belum aktif** - Tunggu lebih lama (5 menit)

2. **Token salah** - Token yang didaftarkan berbeda dengan yang di-generate:
   - **Hapus token lama** di Firebase Console
   - **Cari token baru** di Logcat lagi
   - **Daftarkan token baru**
   - **Tunggu 2 menit**
   - **Restart aplikasi**

3. **Multiple devices/emulators** - Setiap device/emulator punya token berbeda:
   - Daftarkan token untuk **masing-masing device**
   - Atau gunakan satu device saja untuk testing

4. **Cache issue:**
   ```bash
   # Uninstall aplikasi dari device
   adb uninstall com.webtech.kamuskorea

   # Clean dan rebuild
   ./gradlew clean
   ./gradlew installDebug

   # Cari token baru di Logcat
   # Daftarkan token baru di Firebase Console
   ```

---

### Problem 3: Tidak Bisa Akses Firebase Console

**Solusi:**

1. Pastikan akses dengan akun Google yang memiliki permission ke project

2. Minta **Owner** atau **Editor** role dari admin project

3. Hubungi developer/admin untuk mendaftarkan token Anda:
   - Kirim debug token ke admin
   - Admin akan mendaftarkan token di Firebase Console

---

### Problem 4: Error di Production (Release Build)

**CATATAN:** Debug token **HANYA untuk development**!

Untuk **production/release build**, App Check menggunakan **Play Integrity API**.

**Setup Play Integrity:**

1. Upload APK/AAB ke Play Console

2. Google Play akan sign aplikasi dengan key mereka

3. Di Firebase Console → App Check:
   - Pilih app: `com.webtech.kamuskorea`
   - Enable **Play Integrity** provider
   - Klik **Save**

4. Tidak perlu debug token untuk release build

---

## 📋 CHECKLIST VERIFIKASI

Pastikan semua langkah sudah dilakukan:

- [ ] Aplikasi dalam **DEBUG mode**
- [ ] Debug token sudah di-**COPY** dari Logcat (tag: `DebugAppCheckProvider`)
- [ ] Token sudah **DIDAFTARKAN** di Firebase Console → App Check → Debug tokens
- [ ] Sudah **TUNGGU 1-2 menit** setelah daftarkan token
- [ ] Aplikasi sudah di-**RESTART** (force stop dulu)
- [ ] Test **Google Sign In** berhasil tanpa error ✅

---

## 🔄 QUICK FIX - Langkah Cepat

### Solusi Tercepat (RECOMMENDED):

**Gunakan Permissive Mode:**

1. Buka: https://console.firebase.google.com/project/kamus-korea-apps-dcf09/appcheck
2. Klik tab **"APIs"**
3. Untuk **Firebase Authentication**:
   - Klik **Settings (⚙️)**
   - Ubah **"Enforced"** → **"Permissive"**
   - Klik **"Save"**
4. Restart aplikasi
5. Test Google Sign-In
6. **DONE!** ✅

---

### Solusi Alternatif (Jika Ingin Gunakan Debug Token):

```bash
# 1. Clean project
./gradlew clean

# 2. Uninstall aplikasi dari device
adb uninstall com.webtech.kamuskorea

# 3. Install ulang
./gradlew installDebug

# 4. Buka aplikasi dan cek Logcat
# Filter: tag:DebugAppCheckProvider

# 5. Copy token yang muncul

# 6. Buka Firebase Console
# https://console.firebase.google.com/project/kamus-korea-apps-dcf09/appcheck
# App Check → Apps → com.webtech.kamuskorea → Manage debug tokens

# 7. Paste token dan Add

# 8. Tunggu 2 menit

# 9. Force stop aplikasi di device

# 10. Buka aplikasi lagi dan test Google Sign In
```

---

## 📝 CATATAN PENTING

### Debug Token vs Production

| Mode | Provider | Token Needed |
|------|----------|--------------|
| **Debug Build** | Debug Provider | ✅ Debug token (UUID format) |
| **Release Build** | Play Integrity | ❌ Tidak perlu debug token |

### Token Berbeda Untuk Setiap Device

- **Emulator 1** → Token A
- **Emulator 2** → Token B
- **Physical Device** → Token C

Semua token harus didaftarkan di Firebase Console jika ingin test di multiple devices.

### Token Tidak Expire

Debug token tidak expire, tapi bisa dihapus/revoke dari Firebase Console kapan saja.

---

## 🆘 MASIH BELUM BERHASIL?

Jika setelah mengikuti semua langkah masih gagal:

1. **Kirim screenshot** dari:
   - Error message di aplikasi
   - Logcat dengan filter tag: `AppCheck`
   - Firebase Console → App Check → Debug tokens

2. **Kirim informasi**:
   - Device/emulator yang digunakan
   - Build variant (debug/release)
   - Versi aplikasi
   - Firebase project yang digunakan

3. **Check logs lengkap**:
   ```bash
   # Export all logs
   adb logcat > logcat_full.txt
   ```

---

## 📚 REFERENSI

- [Firebase App Check Documentation](https://firebase.google.com/docs/app-check)
- [Debug App Check Tokens](https://firebase.google.com/docs/app-check/android/debug-provider)
- [Play Integrity API](https://developer.android.com/google/play/integrity)

---

**Good luck! 🚀**

Pastikan follow semua langkah dengan teliti. Masalah ini 99% disebabkan oleh debug token yang belum terdaftar atau belum aktif.
