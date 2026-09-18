# Strip debug logging in production release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ── Data models — keep all fields for JSON & Firestore deserialization ────────
-keep class com.brave.jsabmusic.api.model.** { *; }

# ── Firebase & Google Play Services Auth ──────────────────────────────────────
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.brave.jsabmusic.firebase.** { *; }

# ── OkHttp — required for network calls ──────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Preserve JavaScript Interfaces for WebView DOM Bridge ────────────────────
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.brave.jsabmusic.bridge.** { *; }
-keepclassmembers class com.brave.jsabmusic.bridge.** {
    public <methods>;
    public <fields>;
}

# ── Preserve AndroidX Media & MediaSession ────────────────────────────────────
-keep class androidx.media.** { *; }
-keep class android.support.v4.media.** { *; }
-keep class android.support.v4.media.session.** { *; }

# ── Jetpack Compose and Coroutines Rules ──────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ── WebKit Keep rules ──────────────────────────────────────────────────────────
-keep class androidx.webkit.** { *; }

# ── Aggressive Optimization flags ─────────────────────────────────────────────
-allowaccessmodification
-mergeinterfacesaggressively
