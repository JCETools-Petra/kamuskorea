# 🔧 Fix Gradle Build Error - dexBuilderDebug

## ❌ Error yang Terjadi

```
Execution failed for task ':app:dexBuilderDebug'.
Failed to create MD5 hash for file ... as it does not exist.
```

Ini adalah **Gradle cache corruption** setelah upgrade Java atau clean build.

---

## ✅ **Solusi Cepat (3 Langkah)**

### **Step 1: Clean Build Folder**

```powershell
# PowerShell - di folder project
cd C:\Users\Administrator\Desktop\KamusKorea2

# Hapus build folder
Remove-Item -Recurse -Force .\app\build
Remove-Item -Recurse -Force .\build

# Atau manual: Delete folder 'build' di root dan di 'app/'
```

### **Step 2: Clean Gradle Cache**

```powershell
# Clean Gradle cache
./gradlew clean --no-daemon

# Atau dengan daemon
./gradlew clean
```

### **Step 3: Rebuild Project**

```powershell
# Build tanpa daemon (lebih reliable)
./gradlew assembleDebug --no-daemon

# Atau build normal
./gradlew assembleDebug
```

---

## 🚀 **Alternatif: All-in-One Command**

Jalankan command ini untuk fix semua sekaligus:

```powershell
# Stop all Gradle daemons
./gradlew --stop

# Delete build folders
Remove-Item -Recurse -Force .\app\build, .\build -ErrorAction SilentlyContinue

# Clean & rebuild
./gradlew clean assembleDebug --no-daemon
```

---

## 📱 **Install ke Device**

Setelah build berhasil:

```powershell
# Install debug APK
./gradlew installDebug

# Atau manual
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 🔥 **Troubleshooting**

### **Problem: Masih error setelah clean**

**Solusi 1: Stop all Gradle daemons**
```powershell
./gradlew --stop
```

**Solusi 2: Clear Gradle cache (global)**
```powershell
# WARNING: Ini akan delete semua cache Gradle
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches
```

**Solusi 3: Restart PowerShell & try again**
```powershell
# Close PowerShell
# Open new PowerShell
cd C:\Users\Administrator\Desktop\KamusKorea2
./gradlew clean assembleDebug --no-daemon
```

---

### **Problem: "Detected multiple Kotlin daemon sessions"**

**Solusi:**
```powershell
# Stop all Gradle daemons
./gradlew --stop

# Rebuild
./gradlew assembleDebug
```

---

### **Problem: Out of memory error**

**Solusi:** Increase heap size

Edit atau buat file `gradle.properties`:
```properties
# gradle.properties (root project)
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

---

## 🎯 **Expected Success Output**

Setelah build berhasil, Anda akan lihat:

```
BUILD SUCCESSFUL in 45s
42 actionable tasks: 42 executed
```

Lalu install:
```powershell
./gradlew installDebug
```

Output:
```
> Task :app:installDebug
Installing APK 'app-debug.apk' on 'Device Name' ...
Installed app on 'Device Name'

BUILD SUCCESSFUL in 5s
```

---

## ✅ **Quick Reference**

**Command untuk fix build error:**
```powershell
# 1. Stop daemons
./gradlew --stop

# 2. Delete build folders
Remove-Item -Recurse -Force .\app\build, .\build -ErrorAction SilentlyContinue

# 3. Clean
./gradlew clean --no-daemon

# 4. Build
./gradlew assembleDebug --no-daemon

# 5. Install
./gradlew installDebug
```

---

## 📝 **Next Steps After Build Success**

1. ✅ Build berhasil
2. ✅ App ter-install di device
3. ✅ Test Google Sign In (masih perlu SHA-1 setup!)
4. ⏳ Follow **QUICK_START_SHA1.md** untuk setup Firebase

---

**Common Issue:** Error ini **TIDAK berhubungan** dengan Google Sign In. Ini hanya masalah Gradle cache!
