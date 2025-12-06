# 🚀 DEPLOYMENT INSTRUCTIONS - Kamus Korea V2.0

## 📁 Struktur Folder Production Server

```
/public_html/webtechsolution.my.id/
├── .env                                    ← File sudah ada, tambahkan BASE_URL
├── firebase-service-account.json           ← Upload dari Firebase Console
└── kamuskorea/
    ├── vendor/                             ← Sudah ada (composer dependencies)
    └── kamuskoreaweb/
        └── api.php                         ← UPLOAD FILE INI (api.php.PRODUCTION_READY)
```

---

## ✅ CHECKLIST DEPLOYMENT

### 1️⃣ Update File .env

**Lokasi**: `/public_html/webtechsolution.my.id/.env`

**Tambahkan baris ini** di bagian bawah file:

```bash
# Base URL & Environment
BASE_URL=https://webtechsolution.my.id/kamuskorea
APP_ENV=production
```

File lengkap jadi:
```bash
DB_HOST="localhost"
DB_USERNAME="apsx2353_webtech_api"
DB_PASSWORD="Petra1830!@#"
DB_DATABASE="apsx2353_webtech_api"

# Konfigurasi Email (SMTP)
SMTP_HOST="mail.webtechsolution.my.id"
SMTP_PORT=587
SMTP_SECURE="tls"
SMTP_USER="cs@webtechsolution.my.id"
SMTP_PASS="UoZ$Fq_FA1}toOk("

# Informasi Pengirim Email
MAIL_FROM_ADDRESS="cs@webtechsolution.my.id"
MAIL_FROM_NAME="Kamus Korea Apps"

# Base URL & Environment
BASE_URL=https://webtechsolution.my.id/kamuskorea
APP_ENV=production
```

---

### 2️⃣ Upload api.php Terbaru

**File source**: `kamuskoreaweb/api.php.PRODUCTION_READY`
**Upload ke**: `/public_html/webtechsolution.my.id/kamuskorea/kamuskoreaweb/api.php`

**Via cPanel:**
1. Login cPanel → File Manager
2. Navigate: `/public_html/webtechsolution.my.id/kamuskorea/kamuskoreaweb/`
3. **BACKUP DULU**: Klik kanan `api.php` → Copy → Rename jadi `api.php.backup-old`
4. Delete/rename file `api.php` lama
5. Upload file `api.php.PRODUCTION_READY` → Rename jadi `api.php`

**Via FTP:**
- Upload file ke: `/public_html/webtechsolution.my.id/kamuskorea/kamuskoreaweb/api.php`

---

### 3️⃣ Verifikasi firebase-service-account.json

**Lokasi**: `/public_html/webtechsolution.my.id/firebase-service-account.json`

**Cek apakah file sudah ada:**
- Login cPanel → File Manager
- Check folder `/public_html/webtechsolution.my.id/`
- Cari file `firebase-service-account.json`

**Jika belum ada:**
1. Buka https://console.firebase.google.com/
2. Pilih project "learning-korea"
3. Settings (⚙️) → Service Accounts
4. Klik **"Generate New Private Key"**
5. Download file JSON
6. Upload ke `/public_html/webtechsolution.my.id/firebase-service-account.json`
7. Set permission: `chmod 600 firebase-service-account.json`

---

### 4️⃣ Verifikasi Composer Dependencies

**Lokasi**: `/public_html/webtechsolution.my.id/kamuskorea/vendor/`

**Cek folder vendor:**
- Pastikan folder `vendor/` ada dan berisi dependencies
- Cek file: `vendor/autoload.php` harus ada

**Jika folder vendor kosong atau tidak ada:**

Via SSH:
```bash
cd /home/apsx2353/public_html/webtechsolution.my.id/kamuskorea
composer install --no-dev --optimize-autoloader
```

Via cPanel Terminal:
```bash
cd kamuskorea
/usr/local/bin/php /usr/local/bin/composer install --no-dev
```

---

## 🧪 TESTING

### Test 1: Endpoint Assessments (Tanpa Login)

```bash
curl -X GET "https://webtechsolution.my.id/kamuskorea/api.php/assessments?type=quiz"
```

**Expected**: ✅ Return JSON dengan list quiz (bukan error 401)

### Test 2: Endpoint Ebooks (Tanpa Login)

```bash
curl -X GET "https://webtechsolution.my.id/kamuskorea/api.php/ebooks"
```

**Expected**: ✅ Return JSON dengan list ebooks

### Test 3: Dari Aplikasi Android

1. Buka aplikasi
2. Navigate ke menu "Latihan" atau "Ujian"
3. Lihat apakah list quiz muncul (bukan error "401")

---

## ❌ TROUBLESHOOTING

### Error: "Konfigurasi server error (Firebase Admin SDK)"
**Penyebab**: File `firebase-service-account.json` tidak ditemukan
**Solusi**: Upload file Firebase Service Account ke folder yang benar

### Error: "Database connection failed"
**Penyebab**: File `.env` tidak ditemukan atau konfigurasi salah
**Solusi**: Pastikan file `.env` ada di `/public_html/webtechsolution.my.id/.env`

### Error: "Class 'Dotenv\Dotenv' not found"
**Penyebab**: Composer dependencies belum terinstall
**Solusi**: Run `composer install` di folder `/kamuskorea/`

### Masih error 401 setelah deploy
**Penyebab**: File api.php masih versi lama (belum terupdate)
**Solusi**: Cek tanggal modified file api.php, pastikan yang terbaru

---

## 📝 VERIFIKASI SETELAH DEPLOY

✅ File `.env` sudah ada dan berisi `BASE_URL` dan `APP_ENV`
✅ File `firebase-service-account.json` sudah di-upload
✅ File `api.php` sudah diganti dengan versi baru
✅ Folder `vendor/` ada dan berisi dependencies
✅ Test endpoint dari browser/curl berhasil
✅ Test dari aplikasi Android berhasil

---

## 🎯 PERUBAHAN UTAMA di api.php

1. **Optional Authentication**: Endpoint assessments & ebooks bisa diakses tanpa login
2. **App Check Whitelist**: Endpoint tertentu tidak memerlukan App Check
3. **Path Adjustment**: Path disesuaikan dengan struktur folder production

**File Version**: v3.4 (Fixed 401 Error)
**Last Updated**: 2025-12-06
**Deployed By**: Claude AI Assistant

---

**Jika ada masalah, cek error log di**: `/public_html/webtechsolution.my.id/kamuskorea/kamuskoreaweb/php_error.log`
