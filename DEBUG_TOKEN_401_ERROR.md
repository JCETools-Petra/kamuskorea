# 🔴 DEBUG: Token 401 Unauthorized Error

## Symptom
```
❌ API error: 401 - {
  "success": false,
  "message": "Token tidak valid atau sudah kadaluarsa. Silakan login ulang.",
  "error_detail": "The value 'eyJhbGciOiJSUzI...' is not a verified ID token:
    - The token was not issued by the given issuers
    - The token is not allowed to be used by this audience"
}
```

## Root Cause
**Firebase Project Mismatch** antara Android App dan Backend API.

### Current Configuration:
- **Android App**: Menggunakan project `learning-korea`
  - Web Client ID: `191033536798-1lq8npgrbgevqd0ep9rneaoud6blhtnt.apps.googleusercontent.com`
  - Location: `AuthViewModel.kt:51`

- **Backend API**: Service account dari project yang berbeda (?)
  - Location: `kamuskoreaweb/firebase-service-account.json`

## Verification Steps

### 1. Cek Firebase Project ID di Android App

```bash
# Option A: Jika google-services.json tersedia
cat app/google-services.json | grep "project_id"

# Option B: Download dari Firebase Console
# https://console.firebase.google.com/project/learning-korea/settings/general
```

Expected output:
```json
{
  "project_id": "learning-korea"  // Atau project ID lainnya
}
```

### 2. Cek Firebase Service Account di Backend

```bash
cat kamuskoreaweb/firebase-service-account.json | grep "project_id"
```

Expected output:
```json
{
  "project_id": "HARUS_SAMA_DENGAN_ANDROID_APP"
}
```

### 3. Verifikasi Token Issuer & Audience

Decode token JWT untuk melihat issuer dan audience:
- Go to https://jwt.io
- Paste token dari log Android (eyJhbGciOiJSUzI...)
- Check fields:
  - `iss` (issuer): harus `https://securetoken.google.com/<project_id>`
  - `aud` (audience): harus sama dengan project_id backend

## Solutions

### ✅ Solution 1: Update Backend Service Account (RECOMMENDED)

Gunakan service account dari project **"learning-korea"** yang sama dengan Android app.

**Steps:**
1. Buka Firebase Console: https://console.firebase.google.com/project/learning-korea
2. Go to **Project Settings** → **Service Accounts**
3. Click **Generate New Private Key**
4. Download file JSON
5. Replace `kamuskoreaweb/firebase-service-account.json` dengan file yang baru
6. Restart backend server

**Verify:**
```bash
cat kamuskoreaweb/firebase-service-account.json | grep "project_id"
# Output harus: "project_id": "learning-korea"
```

### ✅ Solution 2: Update Android App Configuration

Jika ingin menggunakan project "kamus-korea-apps-dcf09":

**Steps:**
1. Download `google-services.json` dari project "kamus-korea-apps-dcf09"
2. Replace `app/google-services.json`
3. Update Web Client ID di `AuthViewModel.kt`:

```kotlin
// AuthViewModel.kt line 51
const val WEB_CLIENT_ID = "YOUR_NEW_WEB_CLIENT_ID_FROM_KAMUS_KOREA_APPS_PROJECT"
```

4. Rebuild app

**Get Web Client ID:**
- Open: https://console.firebase.google.com/project/kamus-korea-apps-dcf09/authentication/providers
- Enable Google Sign-In
- Copy Web Client ID (client_type: 3)

## Quick Fix for Development

Jika hanya untuk testing, aktifkan mode development di backend:

```php
// kamuskoreaweb/api.php line 139-145
// Backend akan menggunakan X-User-ID header tanpa verifikasi token
```

**⚠️ WARNING:** Jangan gunakan ini di production!

## Verify Fix

After implementing solution, test:

```bash
# Check Android logs
adb logcat | grep "UserRepository"

# Expected successful logs:
# ✅ Token verified successfully for UID: xxx
# ✅ Premium status from API: true/false
```

## Related Files

- Android App: `app/src/main/java/com/webtech/learningkorea/ui/screens/auth/AuthViewModel.kt`
- Android Interceptor: `app/src/main/java/com/webtech/learningkorea/di/NetworkModule.kt`
- Backend API: `kamuskoreaweb/api.php` (line 135-216)
- Firebase Config: `.firebaserc`, `app/google-services.json`, `kamuskoreaweb/firebase-service-account.json`

## Additional Resources

- Firebase Console: https://console.firebase.google.com
- JWT Decoder: https://jwt.io
- Firebase Auth Docs: https://firebase.google.com/docs/auth/admin/verify-id-tokens
