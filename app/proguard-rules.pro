# Strip debug logging in production release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve JavaScript Interfaces for WebView DOM Bridge
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.brave.jsabmusic.bridge.** { *; }
-keepclassmembers class com.brave.jsabmusic.bridge.** {
    public <methods>;
    public <fields>;
}

# Preserve AndroidX Media & MediaSession
-keep class androidx.media.** { *; }
-keep class android.support.v4.media.** { *; }
-keep class android.support.v4.media.session.** { *; }

# Jetpack Compose and Coroutines Rules
-keep class androidx.compose.runtime.** { *; }
-keep class kotlinx.coroutines.** { *; }

# WebKit Keep rules
-keep class androidx.webkit.** { *; }

# Aggressive Optimization flags
-repackageclasses 'com.brave.jsabmusic.internal'
-allowaccessmodification
-mergeinterfacesaggressively
