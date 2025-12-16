plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // Tambahkan baris ini untuk mendaftarkan Hilt
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // Tambahkan baris ini untuk mendaftarkan KSP
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    // Pastikan baris ini ada
    id("com.google.gms.google-services") version "4.4.1" apply false
    // Firebase Crashlytics - for crash reporting
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}