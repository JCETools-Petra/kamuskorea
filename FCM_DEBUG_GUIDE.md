# 🔔 FCM Debugging Guide - Kenapa Notifikasi Tidak Masuk?

## ✅ Perbaikan yang Sudah Dilakukan

### 1. **Topic Mismatch Fixed**
- **Masalah**: Aplikasi subscribe ke topic `"all_users"` tapi server mengirim ke `"all"`
- **Solusi**: Diubah di `send_notification.php` menjadi `"all_users"`

### 2. **Channel ID Fixed**
- **Masalah**: Channel ID tidak match antara server dan aplikasi
- **Solusi**: Diubah di `send_notification.php` menjadi `"announcements"` (sesuai AndroidManifest.xml)

### 3. **Error Handling Added**
- Ditambahkan error logging di `admin_pdfs.php` untuk debug upload cover

---

## 🔍 Checklist Debugging FCM

Ikuti langkah ini **urut dari atas ke bawah** untuk debug FCM yang tidak masuk:

### ✅ Step 1: Cek Permission & Subscription

**Di Aplikasi Android**, tambahkan log di `MainActivity.onCreate()`:

```kotlin
// Log FCM token
notificationManager.getToken { token ->
    Log.d("FCM_DEBUG", "📱 FCM Token: $token")
}

// Subscribe ke topic
notificationManager.subscribeToAllUsersNotifications()
Log.d("FCM_DEBUG", "✅ Subscribed to all_users topic")
```

**Expected Output di Logcat:**
```
FCM_DEBUG: 📱 FCM Token: fxxxxxxxxxxxxxxxxxxx...
FCM_DEBUG: ✅ Subscribed to all_users topic
NotificationManager: ✅ Successfully subscribed to all_users topic
```

**Jika tidak ada output**:
- Firebase SDK tidak ter-initialize dengan benar
- Cek file `google-services.json` ada di `app/` folder

---

### ✅ Step 2: Test Kirim Notifikasi dari Web Admin

1. Buka: **https://webtechsolution.my.id/kamuskorea/send_notification.php**
2. Login sebagai admin
3. Isi form:
   - **Judul**: "Test Notification"
   - **Pesan**: "Ini adalah test"
   - **Topic**: **all_users** (PENTING!)
4. Klik **Kirim Notifikasi**

**Expected Output di Web:**
```
✅ Notifikasi berhasil dikirim!
Message ID: projects/kamus-korea-6542e/messages/...
```

**Jika error 403 Permission Denied**:
- Ikuti panduan di `FCM_TROUBLESHOOTING.md`
- Enable Firebase Cloud Messaging API
- Add role ke service account

---

### ✅ Step 3: Cek Notifikasi Diterima di Service

**Monitor Logcat dengan filter `KamusMessagingService`:**

```bash
adb logcat | grep KamusMessagingService
```

**Expected Output saat notifikasi diterima:**
```
KamusMessagingService: FCM message received from: /topics/all_users
KamusMessagingService: Notification Title: Test Notification
KamusMessagingService: Notification Body: Ini adalah test
KamusMessagingService: Announcement notification
```

**Jika tidak ada output**:
- Notifikasi tidak sampai ke device
- Cek connection internet
- Cek topic subscription
- Cek Firebase Console → Cloud Messaging untuk delivery status

---

### ✅ Step 4: Cek Notification Channel

**Di Aplikasi Android**, cek channel sudah dibuat:

```kotlin
// Di MainActivity atau Application class
val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val channel = notificationManager.getNotificationChannel("announcements")
Log.d("FCM_DEBUG", "Channel exists: ${channel != null}")
if (channel != null) {
    Log.d("FCM_DEBUG", "Channel importance: ${channel.importance}")
}
```

**Expected Output:**
```
FCM_DEBUG: Channel exists: true
FCM_DEBUG: Channel importance: 4 (IMPORTANCE_HIGH)
```

**Jika channel importance = 0 atau null**:
- Channel belum dibuat atau disabled
- User mungkin sudah disable notifikasi di Settings
- Re-install aplikasi untuk reset channel

---

### ✅ Step 5: Cek Device Notification Permission

**Android 13+ (API 33+)** membutuhkan runtime permission untuk notifikasi.

```kotlin
// Cek permission
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val hasPermission = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    Log.d("FCM_DEBUG", "Notification permission granted: $hasPermission")
}
```

**Jika permission = false**:
- Request permission ke user
- Atau minta user enable manual di Settings → Apps → Kamus Korea → Notifications

---

## 🐛 Common Issues & Solutions

### Issue 1: Notifikasi tidak muncul di device tapi log ada

**Penyebab**:
- Notification channel disabled oleh user
- App dalam mode "Do Not Disturb"
- Battery optimization membunuh app

**Solusi**:
```kotlin
// Buka notification settings
val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
}
startActivity(intent)
```

---

### Issue 2: Notifikasi hanya muncul saat app terbuka

**Penyebab**:
- Service tidak running di background
- Payload notification tidak ada, hanya data

**Solusi**:
- Pastikan kirim **notification payload**, bukan hanya data
- Di `send_notification.php`, pastikan ada `withNotification()`

---

### Issue 3: Topic subscription gagal

**Penyebab**:
- Firebase SDK not initialized
- No internet connection
- Google Play Services outdated

**Solusi**:
```kotlin
FirebaseMessaging.getInstance().subscribeToTopic("all_users")
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FCM", "✅ Subscribed")
        } else {
            Log.e("FCM", "❌ Subscribe failed", task.exception)
            // Retry after 10 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                // Retry subscription
            }, 10000)
        }
    }
```

---

### Issue 4: Notifikasi diterima tapi tidak ditampilkan

**Penyebab**:
- `AppNotificationManager` tidak inject dengan benar
- Channel ID tidak match
- Notification builder error

**Solusi**:
1. Cek `@AndroidEntryPoint` ada di `KamusMessagingService`
2. Cek channel ID: `"announcements"`
3. Tambahkan try-catch di `sendNotification()`:

```kotlin
try {
    notificationManager.notify(notificationId, notification)
    Log.d(TAG, "✅ Notification displayed")
} catch (e: Exception) {
    Log.e(TAG, "❌ Failed to display notification", e)
}
```

---

## 📊 Complete Debug Log Example

Berikut adalah log lengkap yang harus muncul saat notifikasi berhasil:

```
=== APP START ===
FCM_DEBUG: 📱 FCM Token: fAbCdEfGhIjKlMnOpQrStUvWxYz...
FCM_DEBUG: ✅ Subscribed to all_users topic
NotificationManager: ✅ Successfully subscribed to all_users topic
FCM_DEBUG: Channel exists: true
FCM_DEBUG: Channel importance: 4
FCM_DEBUG: Notification permission granted: true

=== NOTIFICATION RECEIVED ===
KamusMessagingService: FCM message received from: /topics/all_users
KamusMessagingService: Notification Title: Test Notification
KamusMessagingService: Notification Body: Ini adalah test
KamusMessagingService: Announcement notification
KamusMessagingService: ✅ Notification displayed
```

---

## 🧪 Testing Scenarios

### Test 1: Basic Notification
- Title: "Test Basic"
- Body: "This is a test"
- Topic: all_users
- No image, no custom data

**Expected**: Notifikasi muncul dalam 1-3 detik

---

### Test 2: Notification with Image
- Title: "Test with Image"
- Body: "Testing image notification"
- Topic: all_users
- Image URL: https://example.com/image.jpg

**Expected**: Notifikasi dengan big picture style

---

### Test 3: Notification with Custom Data
- Title: "Test Custom Data"
- Body: "Testing custom action"
- Topic: all_users
- Data: type = "update"

**Expected**: Notifikasi muncul, data di-handle oleh app

---

## 🆘 Still Not Working?

1. **Clear app data**:
   ```bash
   adb shell pm clear com.webtech.learningkorea
   ```

2. **Reinstall app**

3. **Check Firebase Console**:
   - Go to: Cloud Messaging → Send test message
   - Enter FCM token directly
   - See if notification arrives

4. **Check server logs**:
   ```bash
   tail -f /home/user/kamuskorea/kamuskoreaweb/error_log
   ```

5. **Contact developer** dengan info:
   - Full logcat output
   - FCM token
   - Screenshot error dari web admin
   - Android version & device model

---

## 📚 Files to Check

| File | Purpose | Check |
|------|---------|-------|
| `AndroidManifest.xml` | Service registration | Service name = `KamusMessagingService` |
| `NotificationManager.kt` | Topic subscription | Topic = `all_users` |
| `KamusMessagingService.kt` | Handle FCM messages | `@AndroidEntryPoint` present |
| `AppNotificationManager.kt` | Display notifications | Channel ID = `announcements` |
| `send_notification.php` | Send from server | Topic = `all_users` |

---

**Last Updated**: 2024-11-25
**Version**: 2.0 (Fixed topic mismatch issue)
