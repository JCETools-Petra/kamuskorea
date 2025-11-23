# 🔧 Troubleshooting: Google Sign-In Setup - Learning Korea

## ❌ Masalah: Tidak Ada "External" Type & "Add Test Users"

### Penyebab Umum

1. **OAuth Consent Screen belum dibuat sama sekali**
2. **Melihat halaman yang salah** (Data access vs OAuth consent screen)
3. **Organization account** dengan workspace restrictions
4. **Sudah publish app** (tidak bisa ubah ke External lagi)

---

## 🔍 Langkah Troubleshooting

### Step 1: Verifikasi Anda di Halaman yang Benar

1. Buka Google Cloud Console: https://console.cloud.google.com/
2. **PENTING**: Pastikan project yang dipilih adalah yang SAMA dengan Firebase
   - Lihat di top bar, should say project name yang sama dengan Firebase
3. Di sidebar kiri, cari: **APIs & Services** → **OAuth consent screen**

### Step 2: Cek Status OAuth Consent Screen

Anda akan melihat salah satu dari 3 kondisi:

#### Kondisi A: Belum Ada Consent Screen (Blank Page)
**Tampilan:**
- Kosong atau ada tombol "CREATE" / "CONFIGURE CONSENT SCREEN"

**Solusi:**
1. Klik tombol **"CONFIGURE CONSENT SCREEN"** atau **"CREATE"**
2. Anda akan diminta pilih User Type:
   - ⚫ Internal (hanya untuk Google Workspace)
   - 🔵 **External** ← PILIH INI!
3. Klik **"CREATE"**
4. Lanjut ke Step 3

#### Kondisi B: Sudah Ada tapi Status "Testing"
**Tampilan:**
- Publishing status: Testing
- Ada tombol "EDIT APP"

**Solusi:**
1. Klik **"EDIT APP"**
2. Scroll ke bawah sampai section **"Test users"**
3. Klik **"ADD USERS"**
4. Masukkan email untuk testing
5. Save

#### Kondisi C: Sudah Published (Status: In Production/Published)
**Tampilan:**
- Publishing status: In production atau Published
- User type: Internal/External

**Catatan:**
- Jika sudah published dan type "Internal", TIDAK BISA ubah ke "External"
- Harus buat project baru atau request ke admin (jika workspace account)

---

## 📋 Step-by-Step: Setup OAuth Consent Screen (dari Awal)

### 1. Akses OAuth Consent Screen

```
Google Cloud Console
→ Select Project: "learning-korea" (atau nama project Firebase Anda)
→ Sidebar: APIs & Services
→ OAuth consent screen
```

### 2. Configure Consent Screen (First Time)

Jika halaman kosong, klik **"CONFIGURE CONSENT SCREEN"**:

**User Type Selection:**
```
○ Internal
   Hanya untuk Google Workspace users

● External  ← PILIH INI
   Siapa saja dengan Google Account
```

Klik **"CREATE"**

### 3. App Information (Step 1 of 4)

Fill in the form:

```
App name: Learning Korea
User support email: [pilih email Anda dari dropdown]

App logo: (optional, bisa skip dulu)

App domain:
  - Application home page: (optional)
  - Application privacy policy link: (optional)
  - Application terms of service link: (optional)

Authorized domains: (optional untuk testing)

Developer contact information:
  Email addresses: [email Anda]
```

Klik **"SAVE AND CONTINUE"**

### 4. Scopes (Step 2 of 4)

Klik **"ADD OR REMOVE SCOPES"**

Centang scopes berikut:
```
☑ .../auth/userinfo.email
   View your email address

☑ .../auth/userinfo.profile
   See your personal info

☑ openid
   Associate you with your personal info on Google
```

Klik **"UPDATE"**

Klik **"SAVE AND CONTINUE"**

### 5. Test Users (Step 3 of 4) ← INI YANG PENTING!

**Ini adalah section untuk add test users!**

1. Klik **"ADD USERS"**
2. Masukkan email yang akan Anda gunakan untuk testing:
   ```
   Contoh:
   yourname@gmail.com
   developer@example.com
   ```
3. Klik **"ADD"**
4. Ulangi untuk email lain jika perlu

**⚠️ PENTING:**
- Hanya email yang ditambahkan di sini yang bisa login
- Jika email tidak ada di list, akan muncul error "Access blocked: This app's request is invalid"

Klik **"SAVE AND CONTINUE"**

### 6. Summary (Step 4 of 4)

Review semua settings:
```
User type: External
App name: Learning Korea
Support email: [your-email]
Scopes: email, profile, openid
Test users: [your-email]
```

Klik **"BACK TO DASHBOARD"**

---

## ✅ Verifikasi Setup Berhasil

Setelah setup, Anda harus melihat:

```
OAuth consent screen
├─ Publishing status: Testing
├─ User type: External
├─ App name: Learning Korea
└─ Test users: [jumlah] users

[EDIT APP] [PUBLISH APP]
```

**Test users section:**
```
Test users                                    [ADD USERS]
──────────────────────────────────────────────────────
Email
yourname@gmail.com                             [Remove]
──────────────────────────────────────────────────────
```

---

## 🐛 Masalah Khusus & Solusi

### Masalah 1: "User type cannot be changed"
**Penyebab:** App sudah pernah published
**Solusi:**
- Jika masih testing, reset app ke unpublished
- Atau buat Firebase project baru dengan Google Cloud project baru

### Masalah 2: "Internal user type only"
**Penyebab:** Google Workspace account dengan restrictions
**Solusi:**
1. Gunakan personal Google account untuk buat project
2. Atau minta admin workspace untuk allow External apps

### Masalah 3: Tidak bisa add test users
**Penyebab:**
- Belum save consent screen settings
- User type adalah "Internal"
**Solusi:**
1. Pastikan user type adalah "External"
2. Complete semua steps (1-4) dulu
3. Baru bisa add test users

### Masalah 4: "This app isn't verified"
**Penyebab:** Normal untuk app yang masih Testing
**Solusi:**
- Klik "Advanced" → "Go to Learning Korea (unsafe)"
- Ini normal untuk development
- Untuk production, submit untuk verification

---

## 🔥 Langkah Lengkap: Firebase + Google Cloud

### Checklist Setup (Urutan Penting!)

#### A. Firebase Console

1. **Create Project**
   - [ ] Project name: "Learning Korea"
   - [ ] Enable Google Analytics (optional)

2. **Add Android App**
   - [ ] Package name: `com.webtech.learningkorea`
   - [ ] App nickname: Learning Korea
   - [ ] Download google-services.json

3. **Add SHA Fingerprints**
   - [ ] Debug SHA-1: `4D:FD:54:52:5E:37:AF:BC:AD:16:67:EC:79:CA:AB:B9:3D:98:3A:3E`
   - [ ] Release SHA-1: `46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17`
   - [ ] Release SHA-256: `6C:6A:2A:85:09:FE:86:26:64:16:7F:CA:53:56:63:83:C0:F1:AB:CD:E1:58:4B:B6:15:81:C9:C3:63:17:8A:16`

4. **Enable Authentication**
   - [ ] Build → Authentication → Get Started
   - [ ] Sign-in method → Google → Enable
   - [ ] Project support email: [your-email]
   - [ ] Save

5. **Verify Web Client ID**
   - [ ] Check google-services.json has `client_type: 3`
   - [ ] Web Client ID: `191033536798-1lq8npgrbgevqd0ep9rneaoud6blhtnt.apps.googleusercontent.com`

#### B. Google Cloud Console (AFTER Firebase Setup!)

**⚠️ PENTING: Gunakan PROJECT YANG SAMA dengan Firebase!**

1. **Select Correct Project**
   - [ ] Top bar → Project dropdown
   - [ ] Pilih project "learning-korea" atau nama yang sama dengan Firebase
   - [ ] JANGAN buat project baru!

2. **OAuth Consent Screen** (Ikuti Step 1-6 di atas)
   - [ ] User type: External
   - [ ] App name: Learning Korea
   - [ ] Support email: [your-email]
   - [ ] Scopes: email, profile, openid
   - [ ] Test users: [your-email] ← WAJIB!
   - [ ] Save and continue through all 4 steps

3. **Verify Credentials (Auto-Generated)**
   - [ ] APIs & Services → Credentials
   - [ ] Check ada 2 OAuth clients:
     - Web client (auto created by Google Service)
     - Android client (auto created by Google Service)
   - [ ] JANGAN hapus atau edit yang auto-generated!

#### C. Android App

1. **Update google-services.json**
   - [ ] Copy google-services.json ke `app/` folder
   - [ ] Verify package name: `com.webtech.learningkorea`
   - [ ] Verify Web Client ID exists

2. **Clean & Build**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug -x lint
   ```

3. **Install & Test**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🧪 Testing Google Sign-In

### Test Flow:

1. **Buka app Learning Korea**
2. **Klik Google Sign-In button**
3. **Pilih Google account**
   - ⚠️ HARUS email yang ada di Test Users list!
   - Jika email lain → error "Access blocked"

### Expected Results:

✅ **Success:**
```
1. Google account picker muncul
2. Pilih account (yang ada di test users)
3. App asks permission untuk email & profile
4. Klik "Allow"
5. Login berhasil, redirect ke home screen
```

❌ **Error: "Access blocked: This app's request is invalid"**
```
Penyebab: Email yang dipilih tidak ada di Test Users
Solusi:
1. Tambahkan email ke Test Users di OAuth consent screen
2. Atau gunakan email yang sudah terdaftar
```

❌ **Error: "Developer Error"**
```
Penyebab: SHA fingerprint tidak match
Solusi:
1. Verify SHA di Firebase Console
2. Download ulang google-services.json
3. Rebuild app
```

❌ **Error: "Sign in cancelled" atau "Error 12500"**
```
Penyebab: Web Client ID tidak valid atau salah
Solusi:
1. Check google-services.json punya client_type: 3
2. Verify OAuth consent screen sudah setup
3. Rebuild app
```

---

## 📱 Quick Test Command

Gunakan script untuk verify setup:

```bash
# Di Linux/Mac
chmod +x check_sha_learningkorea.sh
./check_sha_learningkorea.sh

# Di Windows (Git Bash atau WSL)
bash check_sha_learningkorea.sh
```

Script akan check:
- ✅ SHA fingerprints
- ✅ google-services.json validity
- ✅ Package name match
- ✅ Web Client ID presence

---

## 🆘 Still Not Working?

### Debug Checklist:

1. **Verify Firebase Project**
   ```
   Firebase Console → Project Settings
   → Package name: com.webtech.learningkorea ✓
   → SHA fingerprints added ✓
   → google-services.json downloaded ✓
   ```

2. **Verify Google Cloud Project**
   ```
   Google Cloud Console → Project dropdown
   → Same project as Firebase ✓
   → OAuth consent screen configured ✓
   → Test users added ✓
   ```

3. **Verify App Code**
   ```
   app/build.gradle.kts → applicationId: com.webtech.learningkorea ✓
   app/google-services.json → package_name matches ✓
   AndroidManifest.xml → correct ✓
   ```

4. **Check Logcat**
   ```bash
   adb logcat | grep -i "auth\|google\|sign"
   ```
   Look for error messages about:
   - SHA mismatch
   - Invalid client ID
   - Permission errors

---

## 📞 Get Help

Jika masih error setelah mengikuti semua steps:

1. **Screenshot** error message saat sign-in
2. **Check** Logcat output
3. **Verify** semua checklist di atas
4. **Document** exact steps yang sudah dilakukan

---

**Updated:** 2025-11-23
**App:** Learning Korea
**Package:** com.webtech.learningkorea
**Web Client ID:** 191033536798-1lq8npgrbgevqd0ep9rneaoud6blhtnt.apps.googleusercontent.com
