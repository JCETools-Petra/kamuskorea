# 🔥 Firebase Setup - Learning Korea App

## ✅ Langkah-langkah yang BENAR untuk Google Sign-In

### 1️⃣ Firebase Console Setup

#### A. Register Android App di Firebase (✅ SUDAH SELESAI)
- ✅ Project: Learning Korea
- ✅ Package name: `com.webtech.learningkorea`
- ✅ SHA-1: `46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17`

#### B. Download google-services.json
1. Buka Firebase Console: https://console.firebase.google.com/
2. Pilih project "Learning Korea"
3. Project Settings → Your apps → Learning Korea Android
4. Scroll down → Download `google-services.json`
5. **PENTING:** Replace file di project:
   ```bash
   # Copy ke:
   /home/user/kamuskorea/app/google-services.json
   ```

#### C. Enable Google Sign-In di Firebase Authentication
1. Firebase Console → Build → Authentication
2. Klik "Get started" (jika belum)
3. Tab "Sign-in method"
4. Klik "Google" → Enable
5. **Project support email:** pilih email Anda
6. Klik "Save"

#### D. Dapatkan Web Client ID
Setelah enable Google Sign-In:
1. Masih di halaman Google Sign-in method
2. Expand section "Web SDK configuration"
3. Copy **Web Client ID** (format: `xxxxx.apps.googleusercontent.com`)
4. Simpan untuk digunakan di code

---

### 2️⃣ Google Cloud Console Setup

#### ⚠️ PENTING: JANGAN Buat Manual Android OAuth Client!

**Kesalahan Umum:**
- ❌ JANGAN buat OAuth 2.0 Client ID tipe "Android" secara manual
- ❌ JANGAN klik "Verify ownership" untuk Android client
- ✅ Firebase sudah otomatis handle ini!

**Yang Perlu Dilakukan:**

#### A. OAuth Consent Screen (Mandatory)

**⚠️ TROUBLESHOOTING:** Jika Anda tidak bisa menemukan "External" type atau "Add test users", baca file **TROUBLESHOOTING_AUTH.md** untuk panduan lengkap!

**Step-by-step setup OAuth Consent Screen:**

1. **Buka Google Cloud Console:** https://console.cloud.google.com/

2. **Pilih project yang BENAR:**
   - Klik dropdown project di top bar
   - Pilih project "Learning Korea" atau nama yang SAMA dengan Firebase
   - ⚠️ JANGAN buat project baru!

3. **Navigasi ke OAuth consent screen:**
   - Sidebar kiri: **APIs & Services** → **OAuth consent screen**

4. **Jika halaman kosong (belum ada consent screen):**
   - Klik tombol **"CONFIGURE CONSENT SCREEN"** atau **"CREATE"**
   - Pilih **User Type: External** (bukan Internal!)
   - Klik **"CREATE"**

5. **Edit App Information (atau lanjut dari step 4):**

   **Step 1 of 4 - App Information:**
   ```
   App name: Learning Korea
   User support email: [pilih dari dropdown]
   App logo: (skip untuk testing)

   Developer contact information:
   Email addresses: [email Anda]
   ```
   Klik **"SAVE AND CONTINUE"**

   **Step 2 of 4 - Scopes:**
   - Klik **"ADD OR REMOVE SCOPES"**
   - Centang:
     ☑ `.../auth/userinfo.email`
     ☑ `.../auth/userinfo.profile`
     ☑ `openid`
   - Klik **"UPDATE"**
   - Klik **"SAVE AND CONTINUE"**

   **Step 3 of 4 - Test users:** ← **INI YANG PALING PENTING!**
   - Klik **"ADD USERS"**
   - Masukkan email yang akan Anda gunakan untuk testing
   - Klik **"ADD"**
   - ⚠️ **Hanya email yang ditambahkan di sini yang bisa login!**
   - Klik **"SAVE AND CONTINUE"**

   **Step 4 of 4 - Summary:**
   - Review semua settings
   - Klik **"BACK TO DASHBOARD"**

6. **Verifikasi setup berhasil:**
   - Harus melihat "Publishing status: Testing"
   - "User type: External"
   - "Test users: [jumlah] users"

#### B. Cek Auto-Generated OAuth Clients (Optional)
1. APIs & Services → Credentials
2. Lihat OAuth 2.0 Client IDs
3. Seharusnya ada 2 client yang auto-generated oleh Firebase:
   - **Web client (auto created by Google Service)**
   - **Android client (auto created by Google Service)** ← Firebase buat ini otomatis!

4. **JANGAN hapus atau edit yang auto-generated!**

#### C. Jika Ada Konflik OAuth Client
Jika masih ada error "already in use":

1. **Cari OAuth Client yang konflik:**
   - APIs & Services → Credentials
   - Cek semua OAuth 2.0 Client IDs
   - Cari yang punya SHA-1: `46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17`

2. **Hapus yang bukan auto-generated:**
   - Jika ada Android client yang Anda buat manual → Delete
   - Biarkan yang "auto created by Google Service"

3. **Force refresh Firebase:**
   - Kembali ke Firebase Console
   - Project Settings → Your apps
   - Hapus dan tambah ulang SHA-1 fingerprint (untuk force refresh)

---

### 3️⃣ Code Configuration

#### A. Update google-services.json
Pastikan file sudah di:
```
/home/user/kamuskorea/app/google-services.json
```

#### B. Cek Web Client ID di Code (Jika Ada)
Jika code Anda ada yang pakai Web Client ID, pastikan sudah benar.

**Cara dapat Web Client ID:**
1. Buka `app/google-services.json`
2. Cari section `oauth_client`:
   ```json
   "oauth_client": [
     {
       "client_id": "XXXXX.apps.googleusercontent.com",
       "client_type": 3  // <- ini Web Client
     }
   ]
   ```
3. Copy `client_id` tersebut

**ATAU** dari Firebase Console:
1. Authentication → Sign-in method → Google
2. Web SDK configuration → Web Client ID

---

### 4️⃣ Testing

#### A. Build & Install
```bash
# Clean build
./gradlew clean

# Build debug
./gradlew assembleDebug

# Install
adb uninstall com.webtech.kamuskorea  # uninstall old app if exists
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### B. Test Google Sign-In
1. Buka app
2. Klik Google Sign-In
3. Pilih akun Google
4. Seharusnya berhasil login

**Jika Gagal:**
- Cek Logcat untuk error messages
- Pastikan `google-services.json` sudah benar
- Pastikan SHA-1 sudah terdaftar di Firebase
- Pastikan test email sudah ditambahkan di OAuth consent screen

---

## 🔍 Troubleshooting

### Error: "Developer Error" saat Google Sign-In
**Penyebab:**
- SHA-1 fingerprint tidak cocok
- `google-services.json` belum update

**Solusi:**
1. Verify SHA-1:
   ```bash
   keytool -list -v -keystore learningkorea-release.jks \
     -alias learningkorea -storepass "LearningKorea2025!Secure"
   ```
2. Pastikan SHA-1 sama dengan yang di Firebase
3. Download ulang `google-services.json`
4. Clean & rebuild

### Error: "Sign-in failed" atau "Network error"
**Solusi:**
1. Cek Firebase Authentication sudah enable Google provider
2. Cek internet connection
3. Cek App Check settings (set ke Permissive mode untuk development)

### Error: "The app you're trying to install isn't from Google Play"
**Solusi:**
1. Normal untuk development
2. Allow "Install from unknown sources" di Android settings
3. Atau test dengan debug signing

---

## ✅ Checklist Final

Sebelum testing, pastikan:

- [ ] Firebase project "Learning Korea" created
- [ ] Android app registered dengan package `com.webtech.learningkorea`
- [ ] SHA-1 fingerprint added di Firebase
- [ ] `google-services.json` downloaded & di folder `app/`
- [ ] Google Sign-In enabled di Firebase Authentication
- [ ] OAuth Consent Screen configured di Google Cloud Console
- [ ] Test user email added di OAuth consent screen
- [ ] Build success tanpa error
- [ ] App installed di device/emulator
- [ ] Google Sign-In tested & working

---

## 📞 Support

Jika masih ada masalah:
1. Screenshot error message
2. Check Logcat saat sign-in
3. Verify semua setup di checklist

---

**Created:** 2025-11-23
**App:** Learning Korea
**Package:** com.webtech.learningkorea
**Firebase Project:** Learning Korea
**SHA-1:** 46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17
