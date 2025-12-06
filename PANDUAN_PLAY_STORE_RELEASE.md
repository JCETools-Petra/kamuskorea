# 🚀 Panduan Lengkap: Internal Testing → Play Store Official

## 📋 **Table of Contents**
1. [Pre-Release Checklist](#1-pre-release-checklist)
2. [Setup Keystore & Signing](#2-setup-keystore--signing)
3. [Build Release AAB](#3-build-release-aab)
4. [Internal Testing](#4-internal-testing)
5. [Closed Testing](#5-closed-testing)
6. [Open Testing (Beta)](#6-open-testing-beta)
7. [Production Release](#7-production-release)
8. [Post-Release Monitoring](#8-post-release-monitoring)

---

## 1. Pre-Release Checklist

### ✅ **A. Code Quality**

- [ ] **Remove debug code:**
  ```bash
  # Search for TODO, FIXME, DEBUG comments
  grep -r "TODO\|FIXME\|DEBUG" app/src/main/java/
  ```

- [ ] **Verify logging is protected:**
  ```bash
  # All logs should be removed by ProGuard in release
  # Check proguard-rules.pro lines 174-180
  ```

- [ ] **Test release build locally:**
  ```bash
  ./gradlew clean
  ./gradlew assembleRelease
  ```

- [ ] **No hardcoded credentials:**
  ```bash
  # Check for passwords, API keys
  grep -r "password\|apiKey\|secret" app/src/main/java/
  ```

### ✅ **B. App Configuration**

**File:** `app/build.gradle.kts`

- [ ] **Update versionCode & versionName:**
  ```kotlin
  defaultConfig {
      applicationId = "com.webtech.learningkorea"
      versionCode = 1  // Increment for each release: 1, 2, 3...
      versionName = "1.0.0"  // User-facing: 1.0.0, 1.0.1, 1.1.0, etc.
  }
  ```

- [ ] **Verify targetSdk:**
  ```kotlin
  targetSdk = 35  // Must meet Play Store requirements
  ```

### ✅ **C. Firebase Configuration**

- [ ] **Switch to Production Firebase (if different):**
  ```bash
  # Download google-services.json from Firebase Console
  # Replace app/google-services.json
  ```

- [ ] **Disable App Check Debug Mode:**

  **File:** `LearningKoreaApp.kt`
  ```kotlin
  if (BuildConfig.DEBUG) {
      // Debug provider - only for development
      firebaseAppCheck.installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance()
      )
  } else {
      // Production - Play Integrity
      firebaseAppCheck.installAppCheckProviderFactory(
          PlayIntegrityAppCheckProviderFactory.getInstance()
      )
  }
  ```

- [ ] **Firebase App Check: Set to ENFORCED mode:**
  ```
  https://console.firebase.google.com/project/learning-korea/appcheck
  - Change all services from "Permissive" to "Enforced"
  ```

### ✅ **D. Backend API**

- [ ] **Update backend `firebase-service-account.json`:**
  - See: `PERBAIKAN_ERROR_401.md`
  - Use service account from Learning Korea project

- [ ] **Test API in production mode:**
  ```bash
  # Test with release build, not debug
  ```

### ✅ **E. Play Store Assets**

**Required assets:**

1. **App Icon:**
   - 512x512 PNG
   - Location: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

2. **Feature Graphic:**
   - 1024x500 PNG or JPEG
   - Promotional banner

3. **Screenshots:**
   - Minimum 2, recommended 8
   - Phone: 16:9 or 9:16 aspect ratio
   - Tablet: 10" screenshots (optional)
   - Capture from real device or emulator

4. **Privacy Policy URL:**
   - Required for apps with user data
   - Host on GitHub Pages or your website

5. **App Description:**
   - Short description (80 chars)
   - Full description (4000 chars)
   - Korean and English versions

---

## 2. Setup Keystore & Signing

### **A. Check if Keystore Exists:**

```bash
ls -la learningkorea-release.jks
```

### **B. If Keystore DOESN'T Exist - Create New:**

```bash
keytool -genkey -v -keystore learningkorea-release.jks \
  -alias learningkorea \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

**IMPORTANT:**
- ⚠️ **BACKUP keystore file** - if you lose it, you can't update the app!
- 📝 **Save passwords** in password manager
- 🔒 **NEVER commit** keystore to Git

### **C. Create `keystore.properties`:**

**File:** `keystore.properties` (in root project)
```properties
storeFile=learningkorea-release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=learningkorea
keyPassword=YOUR_KEY_PASSWORD
```

**Add to .gitignore:**
```bash
echo "keystore.properties" >> .gitignore
echo "*.jks" >> .gitignore
git add .gitignore
git commit -m "Ignore keystore files"
```

### **D. Verify Signing Configuration:**

**File:** `app/build.gradle.kts` (lines 23-38)
```kotlin
signingConfigs {
    create("release") {
        if (keystorePropertiesFile.exists()) {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
}
```

---

## 3. Build Release AAB

### **A. Clean Build:**

```bash
# Clean previous builds
./gradlew clean

# Optional: Clear cache
rm -rf ~/.gradle/caches/
./gradlew --stop
```

### **B. Build Release AAB:**

```bash
# Build Android App Bundle (AAB) for Play Store
./gradlew bundleRelease

# Location of output:
# app/build/outputs/bundle/release/app-release.aab
```

**AAB vs APK:**
- ✅ **AAB (recommended)** - Smaller download, Play Store optimized
- ❌ **APK** - Larger, for direct distribution only

### **C. Build Release APK (Optional):**

```bash
# For direct distribution or testing
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### **D. Verify Build:**

```bash
# Check AAB file size
ls -lh app/build/outputs/bundle/release/app-release.aab

# Verify signing
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab

# Should see: "jar verified"
```

### **E. Test Release Build:**

**IMPORTANT:** Test release build before uploading!

```bash
# Install release APK on device
adb install app/build/outputs/apk/release/app-release.apk

# Test ALL features:
# - Google Sign-In
# - Premium subscription
# - API calls
# - Ads loading
# - Background sync
# - Notifications
```

**Common issues in release build:**
- ProGuard removes too much → Fix in `proguard-rules.pro`
- Missing resources → Check `isShrinkResources`
- API errors → Verify Firebase config

---

## 4. Internal Testing

### **A. Setup Play Console:**

1. **Go to Google Play Console:**
   ```
   https://play.google.com/console
   ```

2. **Create App:**
   - Click "Create app"
   - App name: "Learning Korea"
   - Language: Korean + English
   - App type: App
   - Category: Education
   - Free/Paid: Free

3. **Complete App Content:**
   - Privacy Policy URL
   - Target audience (age ratings)
   - Content ratings questionnaire
   - Data safety form

### **B. Upload AAB:**

1. **Navigate to:**
   ```
   Testing → Internal testing → Create new release
   ```

2. **Upload AAB:**
   - Click "Upload"
   - Select `app/build/outputs/bundle/release/app-release.aab`
   - Wait for upload to complete

3. **Release Notes:**
   ```
   Version 1.0.0 (1)
   - Initial release
   - Korean dictionary with 10,000+ words
   - Google Sign-In
   - Premium subscription
   - Gamification (XP, levels, achievements)
   - Daily quests
   - Leaderboard
   ```

4. **Review and Rollout:**
   - Click "Review release"
   - Click "Start rollout to Internal testing"

### **C. Add Testers:**

1. **Create Email List:**
   ```
   Testing → Internal testing → Testers tab
   ```

2. **Add testers by email:**
   - Add up to 100 testers
   - Can use any Gmail address

3. **Share Test Link:**
   - Copy opt-in URL
   - Send to testers via email/WhatsApp

   Example:
   ```
   Join Internal Testing:
   https://play.google.com/apps/internaltest/4699999999999999999

   Instructions:
   1. Click link on Android device
   2. Accept invitation
   3. Download app
   4. Test and report bugs
   ```

### **D. Internal Testing Timeline:**

- **Duration:** 1-2 weeks
- **Minimum testers:** 5-10 people
- **Focus:** Find critical bugs

**Test checklist for testers:**
- [ ] Install/uninstall
- [ ] Google Sign-In
- [ ] Search words
- [ ] Bookmark words
- [ ] Take quiz
- [ ] Buy premium (test mode)
- [ ] Check leaderboard
- [ ] Receive notifications
- [ ] Check on different devices

---

## 5. Closed Testing

### **A. When to Move to Closed Testing:**

✅ After Internal Testing:
- No critical bugs
- All features working
- Performance acceptable
- App stable for 1+ week

### **B. Setup Closed Testing:**

1. **Navigate to:**
   ```
   Testing → Closed testing → Create new release
   ```

2. **Create Track:**
   - Track name: "Beta"
   - Description: "Beta testing for early adopters"

3. **Upload AAB:**
   - Can reuse same AAB from internal
   - Or upload new version with fixes

4. **Add More Testers:**
   - Up to 1000 testers
   - Can use email lists or Google Groups

### **C. Closed Testing Timeline:**

- **Duration:** 2-4 weeks
- **Testers:** 20-100 people
- **Focus:**
  - Real-world usage
  - Performance on various devices
  - Edge cases
  - Stability

---

## 6. Open Testing (Beta)

### **A. When to Move to Open Testing:**

✅ After Closed Testing:
- Very stable (crash rate < 1%)
- Good ratings from beta testers
- All major features complete
- Ready for public feedback

### **B. Setup Open Testing:**

1. **Navigate to:**
   ```
   Testing → Open testing → Create new release
   ```

2. **Upload AAB:**
   - Same process as closed testing

3. **Open to Everyone:**
   - No need to add testers
   - Anyone can join via opt-in URL
   - App appears in Play Store with "Beta" badge

4. **Set Country Availability:**
   - Choose countries: Indonesia, South Korea, etc.
   - Or "All countries"

### **C. Open Testing Timeline:**

- **Duration:** 4-8 weeks (optional)
- **Testers:** Unlimited
- **Benefits:**
  - Real user feedback
  - Ratings & reviews visible
  - SEO ranking starts
  - Build user base before launch

**Note:** Open testing is OPTIONAL - you can skip to Production

---

## 7. Production Release

### **A. Final Checks:**

- [ ] **Store Listing Complete:**
  - App icon (512x512)
  - Feature graphic
  - Screenshots (phone + tablet)
  - Short description
  - Full description
  - Privacy policy URL
  - Contact email
  - Category

- [ ] **App Content Complete:**
  - Privacy Policy
  - Target audience
  - Content ratings
  - Data safety
  - Ad policy (if using ads)

- [ ] **Release Approved:**
  - No pending reviews
  - All warnings resolved
  - Green checkmarks everywhere

### **B. Create Production Release:**

1. **Navigate to:**
   ```
   Production → Create new release
   ```

2. **Upload AAB:**
   - Upload final version
   - Increment versionCode if needed

3. **Release Notes:**
   ```
   🎉 Learning Korea v1.0.0 - Official Launch!

   ✨ Features:
   - Korean-Indonesian dictionary (10,000+ words)
   - Audio pronunciations
   - Example sentences
   - Bookmark your favorite words
   - Interactive quizzes
   - Memorization tools
   - Premium features (ad-free, unlimited access)

   📚 Gamification:
   - Earn XP for learning
   - Level up system
   - Daily quests
   - Achievements
   - Leaderboard

   🔔 Stay motivated:
   - Daily reminders
   - Streak tracking
   - Push notifications

   Start learning Korean today! 🇰🇷
   ```

4. **Rollout Options:**

   **Option A: Full Rollout (100%)**
   - All users get update immediately
   - Use if very confident

   **Option B: Staged Rollout**
   - 5% → 10% → 20% → 50% → 100%
   - Monitor crashes at each stage
   - Safer approach (recommended)

5. **Click "Review Release"**

6. **Click "Start Rollout to Production"**

### **C. Review Process:**

**Timeline:**
- Google review: 1-7 days (usually 1-3 days)
- You'll get email notification

**Possible outcomes:**
1. ✅ **Approved** → App goes live
2. ⚠️ **Rejected** → Fix issues and resubmit
3. 🔄 **Needs info** → Respond to questions

**Common rejection reasons:**
- Misleading screenshots
- Incomplete privacy policy
- Permissions not justified
- Content policy violation
- Technical issues

### **D. After Approval:**

✅ **App is LIVE!**
- Usually within 2-3 hours after approval
- Searchable in Play Store
- Users can install

**Play Store URL:**
```
https://play.google.com/store/apps/details?id=com.webtech.learningkorea
```

---

## 8. Post-Release Monitoring

### **A. Monitor Dashboard:**

**Play Console → App Quality:**

1. **Crashes & ANRs:**
   - Target: < 1% crash rate
   - Monitor daily for first week
   - Use Firebase Crashlytics for details

2. **Performance:**
   - App startup time
   - Slow rendering
   - Excessive wakeups
   - Memory usage

3. **User Ratings:**
   - Target: > 4.0 stars
   - Respond to reviews (good and bad)

### **B. Firebase Analytics:**

Monitor key metrics:
- Daily Active Users (DAU)
- Retention rate (Day 1, 7, 30)
- Session duration
- Feature usage
- Conversion rate (free → premium)

### **C. User Feedback:**

**Respond to reviews:**
- Thank positive reviews
- Address negative reviews professionally
- Fix reported bugs quickly

**Example responses:**
```
Positive:
"Thank you for your support! 🙏 We're glad you're enjoying Learning Korea. Happy learning! 🇰🇷"

Negative:
"We're sorry to hear that. 😔 We've fixed the login issue in version 1.0.1. Please update and let us know if it works. If you still have issues, contact us at support@learningkorea.com"
```

### **D. Update Schedule:**

**Bug fixes:**
- Critical: Within 1-2 days
- Major: Within 1 week
- Minor: In next regular update

**Regular updates:**
- Every 2-4 weeks
- Add new features
- Improve performance
- Fix bugs

**Version numbering:**
```
1.0.0 → Initial release
1.0.1 → Bug fix
1.1.0 → New feature
2.0.0 → Major redesign
```

---

## 9. Common Issues & Solutions

### **Issue: "App not verified" warning**

**Solution:**
1. Go to Play Console → App Integrity
2. Upload app signing key
3. Wait 24-48 hours

### **Issue: Google Sign-In not working**

**Solution:**
1. Add SHA-1 certificate to Firebase:
   ```bash
   keytool -list -v -keystore learningkorea-release.jks -alias learningkorea
   ```
2. Copy SHA-1 and SHA-256
3. Add to Firebase Console → Project Settings → Your apps → Android app
4. Download new `google-services.json`
5. Rebuild and upload

### **Issue: ProGuard removes too much code**

**Solution:**
Edit `proguard-rules.pro`:
```proguard
# Keep specific classes
-keep class com.your.package.ClassName { *; }

# Keep all classes in package
-keep class com.your.package.** { *; }
```

### **Issue: Resource not found in release**

**Solution:**
Temporarily disable resource shrinking:
```kotlin
buildTypes {
    release {
        isShrinkResources = false  // Temporarily
    }
}
```

---

## 10. Checklist: Ready for Production

### **Pre-Launch Checklist:**

#### **App Quality:**
- [ ] No crashes in testing
- [ ] All features working
- [ ] Performance acceptable (< 3s startup)
- [ ] Tested on 3+ devices
- [ ] Works on Android 7.0+ (minSdk 24)

#### **Store Presence:**
- [ ] App icon (512x512)
- [ ] Feature graphic (1024x500)
- [ ] 4+ screenshots per device type
- [ ] Short description (80 chars)
- [ ] Full description (compelling, keyword-optimized)
- [ ] Privacy policy URL live

#### **Legal & Policy:**
- [ ] Privacy policy complete
- [ ] Terms of Service (if needed)
- [ ] Content ratings complete
- [ ] Data safety form complete
- [ ] Ad disclosures (if using ads)

#### **Technical:**
- [ ] `versionCode` incremented
- [ ] `versionName` updated
- [ ] Release build tested
- [ ] Backend ready for production traffic
- [ ] Firebase: Production mode
- [ ] API keys rotated (if needed)

#### **Business:**
- [ ] Billing/subscriptions tested
- [ ] Support email configured
- [ ] Marketing materials ready
- [ ] Launch announcement prepared

---

## 11. Launch Day Checklist

### **Morning of Launch:**

1. **Final Check:**
   - [ ] App still in testing works fine
   - [ ] Backend server healthy
   - [ ] Firebase quota sufficient

2. **Submit to Production:**
   - [ ] Follow steps in Section 7
   - [ ] Choose staged rollout (5% first)

3. **Monitor Closely:**
   - [ ] Keep Play Console open
   - [ ] Watch for crash reports
   - [ ] Monitor server load

### **First 24 Hours:**

- [ ] Respond to first reviews
- [ ] Check analytics
- [ ] Post on social media
- [ ] Send email to beta testers
- [ ] Monitor server costs

### **First Week:**

- [ ] Daily monitoring
- [ ] Address critical bugs immediately
- [ ] Increase rollout percentage
- [ ] Gather user feedback
- [ ] Plan v1.0.1 if needed

---

## 12. Marketing & ASO (App Store Optimization)

### **A. Optimize Store Listing:**

**Title (30 chars):**
```
Learning Korea - Kamus Korea
```

**Short Description (80 chars):**
```
Belajar Bahasa Korea dengan mudah! Kamus, Quiz, dan Gamification. Gratis! 🇰🇷
```

**Keywords to include:**
- Bahasa Korea
- Kamus Korea-Indonesia
- Belajar Korea
- Korean dictionary
- Korean learning
- Hangeul
- TOPIK

### **B. Launch Announcement:**

**Social Media:**
```
🎉 Learning Korea sudah tersedia di Play Store!

📚 Fitur:
✅ 10,000+ kata Korea-Indonesia
✅ Audio pronunciation
✅ Quiz interaktif
✅ Gamification (XP, level, quest)
✅ Leaderboard

Download GRATIS: [link]

#BelajarKorea #BahasaKorea #KamusKorea #LearningKorea
```

---

## 13. Resources

### **Official Documentation:**

- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [App Signing](https://developer.android.com/studio/publish/app-signing)
- [Android App Bundle](https://developer.android.com/guide/app-bundle)
- [Release Preparation](https://developer.android.com/studio/publish)

### **Useful Tools:**

- [bundletool](https://github.com/google/bundletool) - Test AAB locally
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [Firebase Analytics](https://firebase.google.com/docs/analytics)

---

## 14. Quick Command Reference

```bash
# Clean build
./gradlew clean

# Build release AAB
./gradlew bundleRelease

# Build release APK
./gradlew assembleRelease

# Verify signing
jarsigner -verify -verbose app/build/outputs/bundle/release/app-release.aab

# Get SHA-1 (for Firebase)
keytool -list -v -keystore learningkorea-release.jks -alias learningkorea

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Check app size
ls -lh app/build/outputs/bundle/release/app-release.aab

# Generate ProGuard mapping (for debugging crashes)
# Located at: app/build/outputs/mapping/release/mapping.txt
```

---

## ✅ **Summary Timeline**

| Phase | Duration | Testers | Purpose |
|-------|----------|---------|---------|
| **Internal Testing** | 1-2 weeks | 5-20 | Find critical bugs |
| **Closed Testing** | 2-4 weeks | 20-100 | Real-world testing |
| **Open Testing** | 4-8 weeks | Unlimited | Public beta (optional) |
| **Production** | Ongoing | Everyone | Official release |

**Total: 8-14 weeks** from first internal test to full production

**Fast track: 3-4 weeks** (skip open testing)

---

**Good luck with your launch! 🚀**

For questions or issues, refer to Play Console Help Center or Firebase documentation.

**Last Updated:** 2025-11-23
