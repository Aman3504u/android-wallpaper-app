# ProGuard configuration for 3D Live Wallpaper
# This file contains rules for code obfuscation and optimization in release builds

# ========================================
# General ProGuard Rules
# ========================================

# Keep native methods (if any are added later)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all model/data classes for JSON serialization
-keep class com.example.wallpaper.model.** { *; }

# Keep preference and settings classes
-keep class com.example.wallpaper.prefs.** { *; }

# Keep service and engine classes
-keep class com.example.wallpaper.service.** { *; }

# Keep JavaScript bridge classes
-keep class com.example.wallpaper.bridge.** { *; }

# Keep gesture handling classes
-keep class com.example.wallpaper.gesture.** { *; }

# Keep utility classes
-keep class com.example.wallpaper.util.** { *; }

# ========================================
# WebView and WebGL Specific Rules
# ========================================

# Keep WebView-related classes and methods
-keep class android.webkit.** { *; }
-keepclassmembers class android.webkit.** {
    *;
}

# Keep WebViewAssetLoader classes
-keep class androidx.webkit.** { *; }
-keepclassmembers class androidx.webkit.** {
    *;
}

# Keep JavaScript interface methods (critical for bridge functionality)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView initialization methods
-keepclassmembers class com.example.wallpaper.web.** {
    *;
}

# ========================================
# Wallpaper Service Specific Rules
# ========================================

# Keep WallpaperService and Engine implementations
-keep public class * extends android.service.wallpaper.WallpaperService
-keep public class * extends android.service.wallpaper.WallpaperService.Engine

# Keep WallpaperService lifecycle methods
-keepclassmembers class * extends android.service.wallpaper.WallpaperService {
    protected void onCreateEngine();
    public void onVisibilityChanged(boolean);
    public void onSurfaceCreated(android.view.SurfaceHolder);
    public void onSurfaceDestroyed(android.view.SurfaceHolder);
    public void onSurfaceChanged(android.view.SurfaceHolder, int, int, int);
}

# Keep SurfaceHolder callbacks
-keepclassmembers class * extends android.view.SurfaceHolder {
    public void surfaceCreated(android.view.SurfaceHolder);
    public void surfaceChanged(android.view.SurfaceHolder, int, int, int);
    public void surfaceDestroyed(android.view.SurfaceHolder);
}

# ========================================
# JavaScript Bridge Rules
# ========================================

# Keep JSON serialization/deserialization
-keepclassmembers class com.example.wallpaper.bridge.* {
    *;
}

# Keep message queue and bridge communication classes
-keep class com.example.wallpaper.bridge.JsMessage { *; }
-keep class com.example.wallpaper.bridge.JsBridge { *; }

# Preserve bridge method signatures
-keepclassmembers class * {
    @com.example.wallpaper.bridge.BridgeMethod <methods>;
    @android.webkit.JavascriptInterface <methods>;
}

# ========================================
# Optimization Rules
# ========================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove debug assertions
-assumenosideeffects class java.lang.AssertionError {
    <init>(...);
    <init>(java.lang.String);
}

# Remove System.out.println calls
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Remove Thread.dumpStack calls
-assumenosideeffects class java.lang.Thread {
    public static void dumpStack();
}

# ========================================
# Third-Party Library Rules
# ========================================

# Gson (JSON library) rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ========================================
# AndroidX Rules
# ========================================

# Keep AndroidX WebKit classes
-keep class androidx.webkit.** { *; }

# Keep LiveData and ViewModel classes
-keep class androidx.lifecycle.** { *; }

# Keep Activity and Fragment classes
-keep class androidx.activity.** { *; }
-keep class androidx.fragment.** { *; }

# Keep Preference classes
-keep class androidx.preference.** { *; }

# ========================================
# Material Design Components Rules
# ========================================

# Keep Material Components classes
-keep class com.google.android.material.** { *; }

# Keep Material3 classes if using Material3
-keep class androidx.compose.material3.** { *; }

# ========================================
# Reflection and Dynamic Code Rules
# ========================================

# Keep classes that might be accessed via reflection
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep classes with dynamic method registration
-keep @interface com.example.wallpaper.util.DynamicBind

# ========================================
# Resource and Asset Rules
# ========================================

# Keep resource references for WebView assets
-keepclassmembers class com.example.wallpaper.web.** {
    public static final java.lang.String ASSET_*;
    public static final int RES_*;
}

# Keep wallpaper metadata
-keep class com.example.wallpaper.xml.** { *; }

# ========================================
# Performance Optimization Rules
# ========================================

# Remove unused method parameters
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimize and obfuscate
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Allow access to modify final classes
-allowaccessmodification

# ========================================
# Security and Obfuscation Rules
# ========================================

# Preserve obfuscated class names for Wallpaper classes
-keepnames class com.example.wallpaper.service.** {
    public <fields>;
    public <methods>;
}

# Keep JavaScript bridge method names obfuscated but accessible
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ========================================
# Error Handling and Crash Reporting
# ========================================

# Keep error handling classes
-keep class com.example.wallpaper.util.** {
    *;
}

# Keep exception classes
-keep class java.lang.** { *; }
-keepclassmembers class java.lang.** {
    public java.lang.String getMessage();
    public void printStackTrace();
}

# ========================================
# Configuration and Settings Rules
# ========================================

# Keep settings activity classes
-keep class com.example.wallpaper.settings.** { *; }

# Keep wallpaper change receiver
-keep class com.example.wallpaper.prefs.** { *; }

# Keep URL validation and configuration classes
-keep class com.example.wallpaper.prefs.UrlValidator { *; }
-keep class com.example.wallpaper.prefs.WallpaperPrefs { *; }

# ========================================
# Compatibility and Compatibility Rules
# ========================================

# Keep classes referenced in XML layouts
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
    *** get*();
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable implementations
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================================
# Debug and Development Rules (Disabled in Release)
# ========================================

# If you want to keep debug-specific code in release builds, uncomment these:
# -keep class com.example.wallpaper.debug.** { *; }
# -keepclassmembers class com.example.wallpaper.debug.** {
#     *;
# }

# ========================================
# Final Configuration Notes
# ========================================

# This configuration balances obfuscation with functionality
# Key areas preserved:
# 1. WallpaperService and Engine implementations
# 2. JavaScript bridge interfaces
# 3. WebView and WebGL compatibility
# 4. JSON serialization/deserialization
# 5. Parcelable implementations
# 6. Native methods and reflection

# Test thoroughly after applying these rules
# WebView interactions and JS bridge functionality are critical
# Any obfuscation that breaks JS interface methods will cause failures
