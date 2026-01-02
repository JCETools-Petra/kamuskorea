# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ================================
# Firebase
# ================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ================================
# Retrofit & OkHttp
# ================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ================================
# Gson
# ================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ================================
# Data Models (SECURITY: Only keep what's needed for serialization)
# ================================
# Keep only network DTOs for Gson/Retrofit (not internal logic classes)
-keep class com.webtech.learningkorea.data.network.*Request { *; }
-keep class com.webtech.learningkorea.data.network.*Response { *; }
-keep class com.webtech.learningkorea.data.network.DataTransferObjects { *; }
-keep class com.webtech.learningkorea.data.network.*Word { *; }

# Keep assessment data classes (used for Gson serialization)
-keep class com.webtech.learningkorea.data.assessment.** { *; }

# Keep Room entities (needed for database)
-keep @androidx.room.Entity class * { *; }

# Let R8 obfuscate ViewModels, Repositories, and internal classes
# (Do NOT keep entire packages - allow obfuscation for security)

# ================================
# Room Database
# ================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAO methods
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** getDatabase(...);
}

# ================================
# Kotlin Coroutines
# ================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ================================
# Hilt / Dagger
# ================================
-dontwarn com.google.errorprone.annotations.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager

# ================================
# Jetpack Compose
# ================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime internals
-keepclassmembers class androidx.compose.runtime.** { *; }

# ================================
# AdMob
# ================================
-keep public class com.google.android.gms.ads.** {
   public *;
}

# ================================
# Parcelize
# ================================
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ================================
# Keep custom classes (SECURITY: Minimal keeps for functionality)
# ================================
# REMOVED: -keep class com.webtech.learningkorea.** { *; }
# This was keeping EVERYTHING and defeating ProGuard's purpose!

# Only keep classes that MUST be kept (serialization, reflection, etc.)
# Let R8 obfuscate and optimize everything else for security

# ================================
# Navigation Component
# ================================
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.**

# ================================
# ViewBinding & DataBinding
# ================================
-keep class * extends androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# ================================
# Reflection-related
# ================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ================================
# Firebase Crashlytics
# ================================
# Keep custom exceptions for better crash reports
-keep public class * extends java.lang.Exception {
    public <init>(...);
}

# Keep Crashlytics classes
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# ================================
# Remove logging in release
# ================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
