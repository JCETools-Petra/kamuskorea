# 🔧 Panduan Perbaikan Error 401 - Token Tidak Valid

## ❌ **Error yang Terjadi:**
```
API error: 401 - Token tidak valid atau sudah kadaluarsa
- The token was not issued by the given issuers
- The token is not allowed to be used by this audience
```

## 🔍 **Penyebab:**
Backend API masih menggunakan Firebase Service Account dari project **lama** (`kamus-korea-apps-dcf09`), sedangkan aplikasi Android sudah menggunakan project **baru** (`Learning Korea`).

---

## ✅ **Solusi: Update Firebase Service Account di Backend**

### **Langkah 1: Download Service Account JSON dari Firebase Console**

1. Buka **Firebase Console** untuk project **Learning Korea**:
   ```
   https://console.firebase.google.com/project/learning-korea/settings/serviceaccounts/adminsdk
   ```

2. Klik tab **"Service accounts"**

3. Klik tombol **"Generate new private key"**

4. Konfirmasi dan download file JSON
   - File akan bernama seperti: `learning-korea-firebase-adminsdk-xxxxx-xxxxxxxxxx.json`

5. **⚠️ PENTING:** Simpan file ini dengan aman! Jangan bagikan ke siapapun!

---

### **Langkah 2: Upload ke Server**

Upload file service account ke server dengan nama yang tepat:

**Lokasi file di server:**
```
/path/to/kamuskorea/firebase-service-account.json
```

**Cara upload (pilih salah satu):**

#### **Option A: Via FTP/SFTP**
```bash
# Gunakan FileZilla atau WinSCP
# Upload file ke: /path/to/kamuskorea/firebase-service-account.json
```

#### **Option B: Via SCP (dari terminal lokal)**
```bash
scp learning-korea-firebase-adminsdk-xxxxx-xxxxxxxxxx.json \
    user@webtechsolution.my.id:/path/to/kamuskorea/firebase-service-account.json
```

#### **Option C: Via SSH (jika sudah login ke server)**
```bash
# 1. Login ke server
ssh user@webtechsolution.my.id

# 2. Navigate ke folder project
cd /path/to/kamuskorea/

# 3. Upload file (atau copy paste content)
nano firebase-service-account.json
# Paste isi file JSON, lalu save (Ctrl+X, Y, Enter)

# 4. Set permission yang benar
chmod 600 firebase-service-account.json
chown www-data:www-data firebase-service-account.json
```

---

### **Langkah 3: Verifikasi**

#### **A. Cek apakah file ada dan readable:**
```bash
# SSH ke server
ls -la /path/to/kamuskorea/firebase-service-account.json

# Output seharusnya:
# -rw------- 1 www-data www-data 2400 Nov 23 20:00 firebase-service-account.json
```

#### **B. Validasi format JSON:**
```bash
# Cek apakah JSON valid
cat /path/to/kamuskorea/firebase-service-account.json | python3 -m json.tool

# Atau
cat /path/to/kamuskorea/firebase-service-account.json | jq .
```

#### **C. Cek isi file (HATI-HATI, jangan share output ini!):**
```bash
cat /path/to/kamuskorea/firebase-service-account.json | grep -E '(project_id|client_email)'

# Output seharusnya mengandung:
# "project_id": "learning-korea"
# "client_email": "firebase-adminsdk-xxxxx@learning-korea.iam.gserviceaccount.com"
```

---

### **Langkah 4: Restart PHP Backend (Opsional)**

Jika menggunakan PHP-FPM:
```bash
sudo systemctl restart php8.1-fpm
# atau
sudo systemctl restart php-fpm
```

Jika menggunakan Apache:
```bash
sudo systemctl restart apache2
```

Jika menggunakan Nginx:
```bash
sudo systemctl restart nginx
```

---

### **Langkah 5: Test dari Aplikasi Android**

1. **Force stop** aplikasi Learning Korea
2. **Clear cache** aplikasi (opsional)
3. **Login ulang** dengan akun Google
4. Test fitur premium atau API lain

**Cek log di Android Studio:**
```
✅ Token verified successfully for UID: ...
✅ Premium status from API: true/false
```

---

## 🔒 **Security Checklist:**

- [ ] File `firebase-service-account.json` sudah di .gitignore
- [ ] File permission: `chmod 600` (hanya owner bisa read/write)
- [ ] Owner: `www-data` atau user yang menjalankan PHP
- [ ] Jangan commit file ini ke Git
- [ ] Jangan share private key ke siapapun
- [ ] Backup file ini di tempat yang aman

---

## 🧪 **Testing API Manually (Opsional):**

Untuk test apakah backend sudah bisa verify token baru:

```bash
# 1. Login di aplikasi Android dan copy ID Token dari logcat
# Cari log: "Token preview: eyJhbGc..."

# 2. Test API dengan curl
curl -X GET "https://webtechsolution.my.id/kamuskorea/api.php?action=check_user_status" \
     -H "Authorization: Bearer YOUR_ID_TOKEN_HERE" \
     -v

# Response sukses:
# {
#   "success": true,
#   "isPremium": true/false,
#   "expiryDate": "..."
# }
```

---

## ❓ **Troubleshooting:**

### **Problem: Masih error 401 setelah update**
**Solution:**
1. Pastikan file `firebase-service-account.json` benar-benar dari project **"Learning Korea"**
2. Cek `project_id` di dalam file harus: `"learning-korea"`
3. Restart PHP backend
4. Clear cache browser/app

### **Problem: Error "Failed to load service account"**
**Solution:**
1. Cek path file: `/path/to/kamuskorea/firebase-service-account.json`
2. Cek permission: `chmod 600 firebase-service-account.json`
3. Cek ownership: `chown www-data:www-data firebase-service-account.json`
4. Validasi JSON format

### **Problem: Masih dapat error "issuer/audience mismatch"**
**Solution:**
1. **Regenerate service account** dari Firebase Console
2. Pastikan download dari project yang **benar** (Learning Korea, bukan kamus-korea-apps)
3. Delete file lama, upload file baru
4. Restart backend

---

## 📝 **Catatan Penting:**

1. **Jangan hapus** file service account lama sebelum backup
2. **Backup** file lama: `mv firebase-service-account.json firebase-service-account.json.old`
3. **Test** dulu di development sebelum ke production
4. **Koordinasi** dengan team backend jika ada

---

## ✅ **Checklist Lengkap:**

- [ ] Download service account dari Firebase Console (Learning Korea)
- [ ] Upload ke server: `/path/to/kamuskorea/firebase-service-account.json`
- [ ] Set permission: `chmod 600`
- [ ] Set owner: `chown www-data:www-data`
- [ ] Restart PHP backend
- [ ] Test dari aplikasi Android
- [ ] Verify log: "Token verified successfully"
- [ ] Update .gitignore (sudah dilakukan)

---

## 🆘 **Butuh Bantuan?**

Jika masih error setelah ikuti semua langkah:
1. Check PHP error log: `tail -f /path/to/kamuskorea/kamuskoreaweb/php_error.log`
2. Check Nginx/Apache error log
3. Cek logcat Android untuk detail error
4. Hubungi tim backend/DevOps

---

**Last Updated:** 2025-11-23
**Status:** Ready to deploy
