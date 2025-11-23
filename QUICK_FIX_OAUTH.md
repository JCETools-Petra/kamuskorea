# 🚀 QUICK FIX: OAuth Consent Screen Setup

## ❌ Masalah Anda:
- Tidak bisa menemukan "External" type
- Tidak bisa add test users
- Google Sign-In tidak berfungsi

## ✅ Solusi Cepat (5 Menit):

### Step 1: Buka Google Cloud Console
🔗 https://console.cloud.google.com/

### Step 2: Pastikan Project yang Benar
```
Top bar → Project dropdown
→ Pilih: "learning-korea" atau nama project Firebase Anda
→ BUKAN project lain!
```

### Step 3: Navigasi ke OAuth Consent Screen
```
Sidebar kiri
→ APIs & Services
→ OAuth consent screen
```

### Step 4: Cek Kondisi Halaman

#### Jika KOSONG atau ada tombol "CONFIGURE CONSENT SCREEN":
1. Klik **"CONFIGURE CONSENT SCREEN"**
2. Anda akan melihat pilihan User Type:
   ```
   ○ Internal
      Only available to users within your organization

   ○ External  ← PILIH INI!
      Available to any test user with a Google Account
   ```
3. Pilih **External**
4. Klik **"CREATE"**
5. Lanjut ke Step 5

#### Jika sudah ada (ada tombol "EDIT APP"):
1. Klik **"EDIT APP"**
2. Lanjut ke Step 5

### Step 5: Isi Form (4 Steps)

**🔹 Step 1/4: App Information**
```
App name: Learning Korea
User support email: [pilih email Anda]
Developer contact: [email Anda]
```
Klik **SAVE AND CONTINUE**

**🔹 Step 2/4: Scopes**
```
Klik "ADD OR REMOVE SCOPES"

Centang 3 scopes ini:
☑ .../auth/userinfo.email
☑ .../auth/userinfo.profile  
☑ openid

Klik "UPDATE"
```
Klik **SAVE AND CONTINUE**

**🔹 Step 3/4: Test Users** ← PENTING!!!
```
Klik "ADD USERS"

Masukkan email testing:
your.email@gmail.com

Klik "ADD"
```
Klik **SAVE AND CONTINUE**

**🔹 Step 4/4: Summary**
```
Review dan klik "BACK TO DASHBOARD"
```

### Step 6: Verifikasi

Anda harus melihat:
```
OAuth consent screen
├─ Publishing status: Testing
├─ User type: External
├─ App name: Learning Korea
└─ Test users: 1 user (atau lebih)

[EDIT APP] [PUBLISH APP]
```

---

## ✅ Setelah Setup OAuth Consent Screen:

### 1. Verify di Firebase
```
Firebase Console → Project Settings
→ Your apps → com.webtech.learningkorea
→ SHA certificate fingerprints

Pastikan sudah ada:
☑ Debug SHA-1: 4D:FD:54:52:5E:37:AF:BC:AD:16:67:EC:79:CA:AB:B9:3D:98:3A:3E
☑ Release SHA-1: 46:8D:C5:20:21:F2:DD:51:63:25:48:9A:E5:18:75:53:F9:93:3F:17
☑ Release SHA-256: 6C:6A:2A:85:09:FE:86:26:64:16:7F:CA:53:56:63:83:C0:F1:AB:CD:E1:58:4B:B6:15:81:C9:C3:63:17:8A:16
```

### 2. Download google-services.json (LAGI!)
```
Firebase Console → Project Settings
→ Your apps → Learning Korea Android
→ Download google-services.json
→ Replace file di: app/google-services.json
```

### 3. Clean & Rebuild App
```bash
# Di Windows (PowerShell):
.\gradlew clean
.\gradlew assembleDebug -x lint

# Di Linux/Mac:
./gradlew clean
./gradlew assembleDebug -x lint
```

### 4. Install & Test
```bash
# Uninstall old app (jika ada)
adb uninstall com.webtech.kamuskorea
adb uninstall com.webtech.learningkorea

# Install new app
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 5. Test Google Sign-In
```
1. Buka app "Learning Korea"
2. Klik tombol "Sign in with Google"
3. Pilih akun Google (yang ada di Test Users!)
4. Klik "Allow"
5. ✅ Harus berhasil login!
```

---

## 🐛 Jika Masih Error:

### Error: "Access blocked: This app's request is invalid"
**Penyebab:** Email yang dipilih tidak ada di Test Users

**Solusi:**
1. Google Cloud Console → OAuth consent screen
2. EDIT APP → Test users
3. ADD USERS → masukkan email yang ingin digunakan
4. Save
5. Test lagi

### Error: "Developer Error"
**Penyebab:** SHA fingerprint tidak match

**Solusi:**
1. Verify SHA di Firebase Console
2. Download ulang google-services.json
3. Rebuild app: `.\gradlew clean assembleDebug -x lint`

### Error: "Error 12500" atau "Sign in cancelled"
**Penyebab:** Web Client ID tidak ada atau salah

**Solusi:**
1. Check google-services.json
2. Cari section `"client_type": 3`
3. Harus ada `client_id`: `191033536798-...apps.googleusercontent.com`
4. Jika tidak ada, re-enable Google Sign-In di Firebase Authentication

---

## 📱 Checklist Lengkap:

```
Firebase Setup:
☑ Project created: learning-korea
☑ Android app: com.webtech.learningkorea
☑ SHA fingerprints added (3 SHA total)
☑ google-services.json downloaded
☑ Google Sign-In enabled
☑ Web Client ID exists in google-services.json

Google Cloud Console:
☑ Same project as Firebase selected
☑ OAuth consent screen configured
☑ User type: External
☑ App name: Learning Korea
☑ Scopes: email, profile, openid
☑ Test users added (minimal 1 email)

App:
☑ google-services.json in app/ folder
☑ Package name: com.webtech.learningkorea
☑ Build successful
☑ Installed on device
☑ Test Google Sign-In → Works! ✅
```

---

## 🆘 Masih Stuck?

Baca dokumentasi lengkap:
- **TROUBLESHOOTING_AUTH.md** - Troubleshooting detail
- **FIREBASE_SETUP_STEPS.md** - Setup lengkap Firebase
- **check_sha_learningkorea.sh** - Script untuk verify setup

---

**Last Updated:** 2025-11-23
**Your Web Client ID:** 191033536798-1lq8npgrbgevqd0ep9rneaoud6blhtnt.apps.googleusercontent.com
