# 🔄 CARA PULL LATEST CHANGES

## Dari Command Line (Git Bash / Terminal):

```bash
# 1. Pastikan di directory project
cd C:\Users\Joshhh\AndroidStudioProjects\KamusKorea2

# 2. Check branch current
git branch

# 3. Pull latest changes dari branch
git pull origin claude/fix-user-repository-api-01EEno9wKCobPJ2NwZ35YHG5

# 4. Atau jika sudah di branch yang benar:
git pull
```

## Dari Android Studio:

1. **Menu:** VCS → Git → Pull (atau tekan Ctrl+T)
2. **Select branch:** `claude/fix-user-repository-api-01EEno9wKCobPJ2NwZ35YHG5`
3. **Click:** Pull

## Setelah Pull:

### Clean & Rebuild Project:
1. **Menu:** Build → Clean Project
2. **Tunggu selesai**
3. **Menu:** Build → Rebuild Project

### Atau via Gradle:
```bash
# Windows (di directory project)
gradlew.bat clean build
```

## Verify Pull Berhasil:

Check file `MainActivity.kt` line 77-98 harus ada:

```kotlin
import com.webtech.learningkorea.di.AuthInterceptor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authInterceptor: AuthInterceptor  // ← Harus ada ini!

    // ... rest of code
}
```

Check file `MainActivity.kt` line 832 harus ada:

```kotlin
onClick = {
    Log.d("MainActivity", "🚪 Logout initiated")

    // CRITICAL: Clear cached token BEFORE logout
    authInterceptor.clearTokenCache()  // ← Harus ada ini!

    // ... rest of logout code
}
```

## Jika Masih Error Setelah Pull:

1. **Invalidate Caches:**
   - Menu: File → Invalidate Caches
   - Check: Clear file system cache and Local History
   - Click: Invalidate and Restart

2. **Delete Build Folders:**
   ```bash
   # Windows
   rmdir /s /q .gradle
   rmdir /s /q build
   rmdir /s /q app\build

   # Kemudian rebuild
   gradlew.bat clean build
   ```

3. **Sync Gradle:**
   - Menu: File → Sync Project with Gradle Files
