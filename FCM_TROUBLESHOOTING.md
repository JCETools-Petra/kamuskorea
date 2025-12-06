# 🔥 FCM Push Notification - Troubleshooting Guide

## ❌ Error: Permission Denied (403)

```
{
  "error": {
    "code": 403,
    "message": "Permission 'cloudmessaging.messages.create' denied on resource '//cloudresourcemanager.googleapis.com/projects/kamus-korea-6542e' (or it may not exist).",
    "status": "PERMISSION_DENIED"
  }
}
```

## ✅ Solusi - Langkah demi Langkah

### 1. Aktifkan Firebase Cloud Messaging API

1. Buka **[Google Cloud Console](https://console.cloud.google.com/)**
2. Pilih project: **kamus-korea-6542e**
3. Pergi ke **APIs & Services** → **Enable APIs and Services**
4. Cari: **"Firebase Cloud Messaging API"**
5. Klik **Enable** (jika belum aktif)

**Direct Link:**
```
https://console.cloud.google.com/apis/library/fcm.googleapis.com?project=kamus-korea-6542e
```

### 2. Periksa Service Account Permissions

#### Option A: Tambahkan Role ke Service Account yang Ada

1. Buka **[IAM & Admin](https://console.cloud.google.com/iam-admin/iam?project=kamus-korea-6542e)**
2. Cari service account yang digunakan (lihat di `firebase-service-account.json` → field `client_email`)
3. Klik **Edit** (ikon pensil)
4. Klik **Add Another Role**
5. Tambahkan role berikut:
   - **Firebase Admin SDK Administrator Service Agent**
   - ATAU **Firebase Cloud Messaging Admin**
6. Klik **Save**

#### Option B: Generate Service Account Baru dengan Permission yang Benar

1. Buka **[Firebase Console](https://console.firebase.google.com/project/kamus-korea-6542e/settings/serviceaccounts/adminsdk)**
2. Pilih tab **Service accounts**
3. Klik **Generate new private key**
4. Simpan file JSON yang di-download
5. Replace file `firebase-service-account.json` dengan file baru ini
6. Pastikan permission file: `chmod 600 firebase-service-account.json`

**Direct Link:**
```
https://console.firebase.google.com/project/kamus-korea-6542e/settings/serviceaccounts/adminsdk
```

### 3. Verifikasi Service Account

Cek file `firebase-service-account.json` harus memiliki struktur:
```json
{
  "type": "service_account",
  "project_id": "kamus-korea-6542e",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...",
  "client_email": "firebase-adminsdk-xxxxx@kamus-korea-6542e.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

### 4. Test Notifikasi

1. Buka: **https://webtechsolution.my.id/kamuskorea/send_notification.php**
2. Login sebagai admin
3. Isi form notifikasi:
   - **Judul**: "Test Notification"
   - **Pesan**: "Ini adalah test notifikasi"
   - **Topic**: "all" (untuk semua user)
4. Klik **Kirim Notifikasi**
5. Jika sukses, akan muncul "Message ID: ..."

---

## 📱 Setup Topic Subscription di Aplikasi Android

Pastikan aplikasi Android sudah subscribe ke topic FCM:

```kotlin
// Di MainActivity atau Application class
FirebaseMessaging.getInstance().subscribeToTopic("all")
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FCM", "Subscribed to topic: all")
        }
    }
```

Tambahkan juga topic lain sesuai kebutuhan:
```kotlin
FirebaseMessaging.getInstance().subscribeToTopic("premium")
FirebaseMessaging.getInstance().subscribeToTopic("android")
```

---

## 🔐 Role yang Diperlukan

Service account harus memiliki minimal salah satu dari role berikut:

1. **Firebase Admin SDK Administrator Service Agent** (Recommended)
   - `roles/firebase.admin`

2. **Firebase Cloud Messaging Admin**
   - `roles/cloudmessaging.admin`

3. **Editor** (Too permissive, not recommended for production)
   - `roles/editor`

---

## ⚠️ Common Issues

### Issue 1: "Service account not found"
**Solusi**: Generate service account baru dari Firebase Console

### Issue 2: "Invalid credential"
**Solusi**:
- Pastikan file JSON tidak corrupt
- Re-download service account JSON
- Cek permission file (harus readable oleh PHP)

### Issue 3: "Topic not found"
**Solusi**:
- Device harus subscribe ke topic terlebih dahulu
- Coba kirim ke topic "all" terlebih dahulu
- Verifikasi di aplikasi sudah ada `subscribeToTopic()`

### Issue 4: Notifikasi terkirim tapi tidak muncul di device
**Solusi**:
- Cek FCM token di device masih valid
- Pastikan aplikasi memiliki permission notifikasi
- Cek notification channel (Android 8+)
- Test dengan device yang berbeda

---

## 📚 Referensi

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [FCM Server Reference](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
- [Firebase Admin SDK Setup](https://firebase.google.com/docs/admin/setup)

---

## 🆘 Masih Bermasalah?

Cek log error di:
```bash
tail -f /var/www/html/kamuskorea/kamuskoreaweb/error_log
```

Atau hubungi developer dengan menyertakan:
1. Screenshot error
2. Service account email (`client_email` dari JSON)
3. Project ID
4. Role yang sudah di-assign ke service account
