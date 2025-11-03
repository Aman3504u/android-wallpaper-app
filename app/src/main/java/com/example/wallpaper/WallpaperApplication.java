package com.example.wallpaper;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.wallpaper.util.CrashHandler;
import com.example.wallpaper.util.WebViewUtil;

/**
 * Application class for 3D Live Wallpaper
 * 
 * Handles global initialization including:
 * - Crash reporting setup
 * - WebView configuration
 * - Theme configuration
 * - Permission handling
 * 
 * This runs before any activities and provides app-wide configuration
 */
public class WallpaperApplication extends Application {

    private static WallpaperApplication instance;
    private static CrashHandler crashHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initializeCrashReporting();
        initializeWebView();
        configureApplication();
    }

    /**
     * Initialize crash reporting and error handling
     */
    private void initializeCrashReporting() {
        if (!BuildConfig.DEBUG) {
            // In production, integrate with crash reporting service
            crashHandler = CrashHandler.getInstance(this);
            crashHandler.initialize();
        }
    }

    /**
     * Initialize WebView configuration
     * Critical for proper wallpaper functionality
     */
    private void initializeWebView() {
        try {
            // Set up WebView debugging for debug builds
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

            // Pre-initialize WebView if supported (Android 5.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                WebView.setWebContentsDebuggingEnabled(BuildConfig.WEBVIEW_DEBUG);
            }

        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Configure global application settings
     */
    private void configureApplication() {
        // Configure Material You theme support (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
        }

        // Set application-wide preferences
        configureLocale();
        configureAccessibility();
    }

    /**
     * Configure locale and internationalization
     */
    private void configureLocale() {
        // For now, using default locale
        // Future: implement dynamic locale switching
        // Configuration config = getResources().getConfiguration();
        // config.setLocale(new Locale("en"));
        // config.setLayoutDirection(config.locale);
    }

    /**
     * Configure accessibility features
     */
    private void configureAccessibility() {
        // Enable accessibility services
        // Future: integrate with TalkBack and other accessibility services
    }

    /**
     * Get application instance
     * 
     * @return WallpaperApplication instance
     */
    public static WallpaperApplication getInstance() {
        return instance;
    }

    /**
     * Get application context
     * 
     * @return Application context
     */
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    /**
     * Get crash handler
     * 
     * @return CrashHandler instance or null if not configured
     */
    public static CrashHandler getCrashHandler() {
        return crashHandler;
    }

    /**
     * Check if WebView debugging is enabled
     * 
     * @return true if WebView debugging is enabled
     */
    public static boolean isWebViewDebugEnabled() {
        return BuildConfig.WEBVIEW_DEBUG;
    }

    /**
     * Get WebView version information for debugging
     * 
     * @return WebView version string
     */
    public String getWebViewVersion() {
        try {
            // WebView.getVersion() is deprecated in API 35+
            // Alternative: query WebView package info
            try {
                return getPackageManager()
                    .getPackageInfo("com.google.android.webview", 0)
                    .versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Try alternative WebView packages
                try {
                    return getPackageManager()
                        .getPackageInfo("com.android.webview", 0)
                        .versionName;
                } catch (PackageManager.NameNotFoundException e2) {
                    return "WebView package not found";
                }
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Check if application is in debug mode
     * 
     * @return true if debug build
     */
    public boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Clear any caches or free memory
        if (BuildConfig.DEBUG) {
            System.gc();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        // Handle different memory warning levels
        if (level >= TRIM_MEMORY_MODERATE) {
            // Clear caches and free memory
            clearCaches();
        }
    }

    /**
     * Clear application caches
     */
    private void clearCaches() {
        // Clear WebView cache
        try {
            WebViewUtil.clearCache(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get application version information
     * 
     * @return Version info string
     */
    public String getVersionInfo() {
        return String.format("v%s (%d) - %s", 
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.DEBUG ? "Debug" : "Release");
    }
}
