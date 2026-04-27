# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ---- Hilt ----
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.* <fields>;
}
-keepclasseswithmembernames class * {
    @javax.inject.* <fields>;
}

# ---- Room ----
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ---- DataStore ----
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ---- Kotlin Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ---- Kotlin serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Model classes ----
-keep class com.taxeca.calculator.domain.model.** { *; }
-keep class com.taxeca.calculator.data.model.** { *; }

# ---- Kotlin metadata ----
-keepattributes RuntimeVisibleAnnotations
-keepattributes AnnotationDefault

# ---- Firebase Crashlytics ----
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# ---- Firebase Analytics ----
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

# ---- AdMob / Google Mobile Ads ----
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.ads.** { *; }
-dontwarn com.google.ads.**

# ---- BillingClient (Play Billing) ----
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ---- Play Review ----
-keep class com.google.android.play.core.review.** { *; }
-dontwarn com.google.android.play.core.review.**

# ---- General Android ----
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
