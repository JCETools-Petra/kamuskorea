# 📱 Panduan Setup Complete: Learning Korea App
## Package Name: `com.webtech.learningkorea`

---

## 📋 **RINGKASAN PERUBAHAN**

| Item | Sebelum | Sesudah |
|------|---------|---------|
| Package Name | `com.webtech.kamuskorea` | `com.webtech.learningkorea` |
| App Name | Kamus Korea | Learning Korea |
| Bundle ID | com.webtech.kamuskorea | com.webtech.learningkorea |

---

## 🔧 **BAGIAN 1: REFACTORING CODE (Otomatis)**

Semua perubahan code akan dilakukan otomatis oleh sistem:

### ✅ Yang Sudah Dilakukan:
1. ✓ Update `build.gradle.kts` (namespace & applicationId)
2. ✓ Rename struktur direktori package
3. ✓ Update import statements di semua file Kotlin
4. ✓ Update `AndroidManifest.xml`
5. ✓ Update referensi package lainnya
6. ✓ Clean & rebuild project

---

## 🔐 **BAGIAN 2: KEYSTORE & SIGNING**

### **A. Generate Keystore Baru**

Jalankan command berikut untuk membuat keystore baru:

```bash
keytool -genkey -v -keystore learningkorea-release.jks \
  -alias learningkorea \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Simpan informasi berikut:**
- Keystore Password: `[RAHASIA - SIMPAN DI PASSWORD MANAGER]`
- Key Password: `[RAHASIA - SIMPAN DI PASSWORD MANAGER]`
- Keystore Alias: `learningkorea`
- File: `learningkorea-release.jks`

### **B. Update keystore.properties**

Buat/update file `keystore.properties` di root project:

```properties
storeFile=learningkorea-release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=learningkorea
keyPassword=YOUR_KEY_PASSWORD
```

⚠️ **PENTING:** Jangan commit file ini ke Git! Sudah ada di `.gitignore`

### **C. Dapatkan SHA-1 & SHA-256 Fingerprint**

```bash
# Debug SHA-1
./gradlew getDebugSha1

# Release SHA-1 & SHA-256
keytool -list -v -keystore learningkorea-release.jks -alias learningkorea
```

**Simpan fingerprint ini untuk Firebase & Google Cloud Console:**
- SHA-1 Debug: `[akan muncul setelah command dijalankan]`
- SHA-1 Release: `[akan muncul setelah command dijalankan]`
- SHA-256 Release: `[akan muncul setelah command dijalankan]`

---

## 🔥 **BAGIAN 3: FIREBASE SETUP**

### **Step 1: Buat Project Firebase Baru**

1. **Buka Firebase Console**
   - URL: https://console.firebase.google.com/
   - Klik "Add project" atau "Create a project"

2. **Konfigurasi Project**
   - Project name: `Learning Korea` (atau nama pilihan Anda)
   - Project ID: akan auto-generate (contoh: `learning-korea-xxxxx`)
   - Analytics: Enable (recommended)
   - Analytics account: Pilih atau buat baru

### **Step 2: Tambah Android App**

1. **Register App**
   - Klik icon Android atau "Add app" → Android
   - Package name: `com.webtech.learningkorea` ⚠️ **HARUS SAMA PERSIS**
   - App nickname: `Learning Korea Android`
   - Debug signing certificate SHA-1: `[paste SHA-1 debug dari step sebelumnya]`

2. **Download google-services.json**
   - Download file `google-services.json`
   - Letakkan di: `app/google-services.json`
   - Replace file lama jika ada

### **Step 3: Tambahkan SHA Fingerprints**

1. **Buka Project Settings** di Firebase
   - Gear icon → Project settings
   - Tab "General" → Your apps → Learning Korea Android

2. **Add Fingerprints**
   - Klik "Add fingerprint"
   - Tambahkan:
     - SHA-1 Debug
     - SHA-1 Release
     - SHA-256 Release

3. **Download ulang google-services.json** setelah menambah fingerprints

### **Step 4: Enable Firebase Services**

#### **A. Authentication**
1. Build → Authentication → Get started
2. Enable provider:
   - ✅ **Google Sign-In**
     - Enable
     - Project support email: [email Anda]
     - Download updated `google-services.json`
   - ✅ **Email/Password** (jika diperlukan)

#### **B. Firestore Database**
1. Build → Firestore Database → Create database
2. **Start in Production Mode** (kita update rules nanti)
3. Location: Pilih yang terdekat (asia-southeast1 atau asia-southeast2)
4. **Update Security Rules:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Achievements collection
    match /achievements/{achievementId} {
      allow read: if request.auth != null;
      allow write: if false; // Only through Cloud Functions
    }

    // Leaderboard collection
    match /leaderboard/{userId} {
      allow read: if request.auth != null;
      allow write: if false; // Only through Cloud Functions
    }

    // Progress collection
    match /progress/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Public data
    match /public/{document=**} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

#### **C. Cloud Storage**
1. Build → Storage → Get started
2. Start in production mode
3. Location: Same as Firestore
4. **Update Security Rules:**

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // User avatars
    match /avatars/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId
                   && request.resource.size < 5 * 1024 * 1024; // 5MB limit
    }

    // Public media (lessons, audio, etc)
    match /media/{allPaths=**} {
      allow read: if true;
      allow write: if false; // Only through admin/functions
    }
  }
}
```

#### **D. Cloud Functions** (jika diperlukan)
1. Build → Functions → Get started
2. Upgrade to Blaze plan (Pay as you go) - diperlukan untuk Functions
3. Deploy functions yang ada di folder `functions/`

```bash
cd functions
npm install
firebase deploy --only functions
```

#### **E. Cloud Messaging (FCM)**
1. Build → Cloud Messaging → Get started
2. Sudah enabled by default dengan `google-services.json`
3. No additional setup needed

#### **F. App Check** (PENTING untuk security)
1. Release & Monitor → App Check → Get started
2. Register app
3. **Play Integrity Provider:**
   - Select provider: Play Integrity
   - Register
4. **Debug Provider** (untuk development):
   ```bash
   # Dapatkan debug token
   adb logcat | grep DebugAppCheckProvider

   # Atau check di Logcat saat run app
   # Cari: "AppCheck debug token: ..."
   ```
5. Add debug token di Firebase Console

#### **G. Remote Config** (untuk feature flags)
1. Engage → Remote Config → Create configuration
2. Add parameters sesuai kebutuhan app

#### **H. Analytics**
1. Already enabled saat buat project
2. Klik "Analytics" untuk dashboard

---

## ☁️ **BAGIAN 4: GOOGLE CLOUD CONSOLE SETUP**

### **Step 1: OAuth 2.0 Configuration**

1. **Buka Google Cloud Console**
   - URL: https://console.cloud.google.com/
   - Pilih project Firebase yang baru dibuat
   - Atau buat project baru dengan nama sama

2. **OAuth Consent Screen**
   - Navigation Menu → APIs & Services → OAuth consent screen
   - User Type: **External**
   - App name: `Learning Korea`
   - User support email: [email Anda]
   - Developer contact: [email Anda]
   - Scopes: Add `email`, `profile`, `openid`
   - Test users: Add email Anda untuk testing

3. **Create OAuth 2.0 Client ID**
   - APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: **Android**
   - Name: `Learning Korea Android`
   - Package name: `com.webtech.learningkorea`
   - SHA-1 certificate fingerprint: `[paste SHA-1 release dari keystore]`
   - Create

4. **Web Client ID (untuk Google Sign-In)**
   - Sudah auto-create oleh Firebase
   - Atau buat manual:
     - Application type: **Web application**
     - Name: `Learning Korea Web Client (auto created by Google Service)`

### **Step 2: Enable APIs**

Enable API berikut di Google Cloud Console:

```
✅ Google Sign-In API
✅ Firebase Authentication API
✅ Cloud Firestore API
✅ Cloud Storage API
✅ Cloud Functions API
✅ Cloud Messaging API
✅ Play Integrity API
✅ Firebase App Check API
```

Cara enable:
- APIs & Services → Library
- Search API name → Enable

---

## 📱 **BAGIAN 5: GOOGLE PLAY CONSOLE SETUP**

### **Step 1: Create New App**

1. **Buka Play Console**
   - URL: https://play.google.com/console/
   - Klik "Create app"

2. **App Details**
   - App name: `Learning Korea`
   - Default language: Indonesian (Bahasa Indonesia)
   - App or game: App
   - Free or paid: Free
   - Declarations:
     - ✅ Developer Program Policies
     - ✅ US export laws

### **Step 2: App Setup**

#### **A. Store Listing**
- App name: `Learning Korea`
- Short description: (max 80 characters)
  ```
  Belajar Bahasa Korea dengan mudah - Kosakata, Grammar, Listening, dan Quiz
  ```
- Full description: (max 4000 characters)
  ```
  Learning Korea adalah aplikasi pembelajaran bahasa Korea yang komprehensif dan interaktif.

  FITUR UTAMA:
  ✨ Pelajaran terstruktur dari dasar hingga advanced
  📚 Ribuan kosakata dengan audio native speaker
  🎧 Latihan listening dengan berbagai level
  ✍️ Quiz interaktif untuk uji kemampuan
  🏆 Sistem gamifikasi dengan achievement dan leaderboard
  📊 Tracking progress pembelajaran
  💾 Mode offline untuk belajar di mana saja

  COCOK UNTUK:
  - Pemula yang ingin belajar bahasa Korea dari nol
  - Siswa yang mempersiapkan ujian TOPIK
  - K-Drama/K-Pop fans yang ingin memahami bahasa Korea
  - Siapapun yang tertarik dengan bahasa dan budaya Korea

  Download sekarang dan mulai perjalanan belajar bahasa Korea Anda!
  ```
- App icon: 512 x 512 px (format PNG)
- Feature graphic: 1024 x 500 px
- Phone screenshots: Minimal 2, maksimal 8 (16:9 atau 9:16)
- 7-inch tablet screenshots: Optional
- 10-inch tablet screenshots: Optional
- App category: Education
- Contact details: Email developer
- Privacy policy: URL ke privacy policy

#### **B. Setup → App Integrity**

1. **App Signing by Google Play**
   - Enroll in Play App Signing
   - Upload release keystore (`learningkorea-release.jks`)
   - Or let Google generate

2. **Download Certificate**
   - Download SHA-1 & SHA-256 dari Play Console
   - Add ke Firebase Console (lihat Bagian 3, Step 3)

#### **C. Release → Production**

1. **Create New Release**
   - Countries/regions: Indonesia (atau sesuai target)
   - Create release

2. **Upload App Bundle/APK**

   **Build Release APK:**
   ```bash
   cd /home/user/kamuskorea
   ./gradlew clean
   ./gradlew assembleRelease

   # Output: app/build/outputs/apk/release/app-release.apk
   ```

   **Build App Bundle (AAB - Recommended):**
   ```bash
   ./gradlew bundleRelease

   # Output: app/build/outputs/bundle/release/app-release.aab
   ```

3. **Release Details**
   - Release name: `1.0.0 - Initial Release`
   - Release notes:
     ```
     🎉 Learning Korea v1.0.0 - Rilis Perdana!

     ✨ Fitur:
     - Pelajaran bahasa Korea terstruktur
     - Ribuan kosakata dengan audio
     - Quiz interaktif
     - Sistem achievement dan leaderboard
     - Mode offline

     Selamat belajar bahasa Korea!
     ```

#### **D. Policy → App Content**

Complete questionnaire:
- Target audience
- Content rating
- Privacy policy
- Data safety
- Government apps
- Financial features
- Ads (jika ada AdMob)
- Data collection

---

## 📢 **BAGIAN 6: GOOGLE ADMOB SETUP** (Jika menggunakan iklan)

### **Step 1: Create AdMob Account**

1. **Sign Up**
   - URL: https://admob.google.com/
   - Sign in dengan Google account
   - Accept terms

### **Step 2: Add App**

1. **Apps → Add App**
   - Platform: Android
   - App name: `Learning Korea`
   - Is the app listed on a supported app store? → Yes (setelah publish)
   - Or No (jika belum publish)
   - Package name: `com.webtech.learningkorea`

### **Step 3: Create Ad Units**

Buat ad units sesuai kebutuhan:

**A. Banner Ad**
- Format: Banner
- Ad unit name: `Learning Korea Banner`
- Copy Ad Unit ID → Simpan

**B. Interstitial Ad**
- Format: Interstitial
- Ad unit name: `Learning Korea Interstitial`
- Copy Ad Unit ID → Simpan

**C. Rewarded Ad**
- Format: Rewarded
- Ad unit name: `Learning Korea Rewarded`
- Copy Ad Unit ID → Simpan

### **Step 4: Update AndroidManifest.xml**

Ganti App ID di `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
```

Replace dengan AdMob App ID Anda.

### **Step 5: Update Ad Unit IDs di Code**

Update ad unit IDs di file yang relevan.

---

## 🧪 **BAGIAN 7: TESTING & VERIFICATION**

### **Checklist Testing:**

#### **A. Firebase Testing**
```bash
# 1. Run app di emulator/device
./gradlew installDebug

# 2. Test di app:
□ Google Sign-In works
□ Firestore read/write works
□ Storage upload/download works
□ FCM notifications received
□ App Check enabled (check Logcat)
□ Analytics events tracked
```

#### **B. Build Testing**
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Check output
ls -la app/build/outputs/apk/release/

# Install & test
adb install app/build/outputs/apk/release/app-release.apk
```

#### **C. Verify SHA Fingerprints**
```bash
# Verify APK signature
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# Should match keystore SHA-1
keytool -list -v -keystore learningkorea-release.jks -alias learningkorea
```

---

## 📝 **BAGIAN 8: UPDATE DOCUMENTATION**

Update file-file berikut dengan informasi baru:

1. **README.md**
   - App name
   - Package name
   - Setup instructions

2. **Build Instructions**
   - Keystore name
   - Alias name

3. **.firebaserc**
   - Firebase project ID

4. **Environment Variables** (jika ada CI/CD)

---

## 🚀 **BAGIAN 9: DEPLOYMENT CHECKLIST**

### **Pre-Launch Checklist:**

```
□ Code refactoring complete
□ New keystore generated dan tersimpan aman
□ SHA fingerprints added to Firebase
□ google-services.json updated
□ Firebase services configured
□ Google Cloud OAuth configured
□ Play Console app created
□ AdMob configured (if applicable)
□ Privacy Policy published
□ All testing passed
□ Version number updated
□ Release notes written
```

### **Launch Steps:**

1. **Build Release:**
   ```bash
   ./gradlew clean
   ./gradlew bundleRelease
   ```

2. **Upload ke Play Console:**
   - Upload `app/build/outputs/bundle/release/app-release.aab`
   - Submit for review

3. **Monitor:**
   - Firebase Console → Analytics
   - Play Console → Statistics
   - Check crash reports
   - Monitor reviews

---

## 📞 **SUPPORT & TROUBLESHOOTING**

### **Common Issues:**

#### **1. Google Sign-In Failed**
- ✓ Check SHA-1 di Firebase Console
- ✓ Check OAuth 2.0 Client ID
- ✓ Download ulang google-services.json

#### **2. App Check Failed**
- ✓ Add debug token untuk development
- ✓ Enable Play Integrity untuk production
- ✓ Check package name match

#### **3. Build Failed**
- ✓ Clean project: `./gradlew clean`
- ✓ Check keystore path
- ✓ Check dependencies

#### **4. Firebase Connection Failed**
- ✓ Check internet connection
- ✓ Verify google-services.json
- ✓ Check Firebase project settings

### **Useful Commands:**

```bash
# Check app signature
./gradlew signingReport

# Get SHA-1
./gradlew getDebugSha1

# Clean build
./gradlew clean build

# Check dependencies
./gradlew dependencies

# Uninstall old app
adb uninstall com.webtech.kamuskorea

# Install new app
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 📚 **RESOURCES**

- Firebase Docs: https://firebase.google.com/docs
- Play Console: https://support.google.com/googleplay/android-developer
- AdMob: https://support.google.com/admob
- Android Developer: https://developer.android.com

---

## ✅ **COMPLETION CHECKLIST**

Setelah semua selesai, verify:

```
□ App berjalan dengan package name baru
□ Firebase fully integrated
□ Google Sign-In working
□ Released to Play Store (Internal/Closed/Open Testing)
□ Analytics tracking
□ No crashes
□ Performance optimized
□ Documentation updated
```

---

**🎉 Good luck dengan Learning Korea app!**

Jika ada pertanyaan, refer ke dokumentasi official atau contact support.

---

_Document created: 2025-11-23_
_Package: com.webtech.learningkorea_
_Version: 1.0.0_
