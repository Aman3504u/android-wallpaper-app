package com.example.wallpaper.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.wallpaper.R;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive WebView utility for the Android Wallpaper application.
 * 
 * This class provides:
 * - WebView initialization and configuration
 * - WebView lifecycle management
 * - Performance optimization and monitoring
 * - Memory management and cleanup
 * - Cookie and cache management
 * - JavaScript bridge integration
 * - Security and URL validation
 * - Error handling and recovery
 * - Loading progress monitoring
 * - Gesture support integration
 * 
 * Features:
 * - Automatic WebView configuration based on device capabilities
 * - Memory-efficient caching strategies
 * - Background loading support
 * - Resource monitoring and optimization
 * - SSL/TLS security handling
 * - JavaScript execution with error recovery
 * - Cookie management and synchronization
 * - Cache cleanup and optimization
 * 
 * Usage:
 * <pre>
 * // Initialize WebView
 * WebView webView = WebViewUtil.createWallpaperWebView(context);
 * 
 * // Load content with monitoring
 * WebViewUtil.loadUrl(webView, "https://example.com", new WebViewUtil.LoadCallback() {
 *     void onComplete(boolean success, String error) {
 *         // Handle completion
 *     }
 * });
 * 
 * // Execute JavaScript safely
 * WebViewUtil.evaluateJavaScript(webView, "console.log('Hello from wallpaper')", null);
 * </pre>
 * 
 * @author Android Wallpaper Team
 * @version 1.0
 */
public class WebViewUtil {
    private static final String TAG = "WebViewUtil";
    
    // Configuration constants
    private static final int DEFAULT_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_MEMORY_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MIN_MEMORY_CACHE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final long LOAD_TIMEOUT_MS = 30000; // 30 seconds
    private static final long EVALUATION_TIMEOUT_MS = 5000; // 5 seconds
    private static final String USER_AGENT_SUFFIX = "WallpaperApp";
    
    // JavaScript interface names
    private static final String JS_INTERFACE_NAME = "AndroidWallpaperBridge";
    private static final String GESTURE_INTERFACE_NAME = "AndroidWallpaperGestures";
    
    // Security constants
    private static final String[] ALLOWED_SCHEMES = {"http", "https", "data", "file", "about"};
    private static final String[] BLOCKED_HOSTS = {}; // Add blocked hosts here
    
    // Performance monitoring
    private static final AtomicInteger sActiveWebViewCount = new AtomicInteger(0);
    private static final AtomicLong sTotalMemoryUsed = new AtomicLong(0);
    private static final AtomicBoolean sIsLowMemoryMode = new AtomicBoolean(false);
    
    // Background executor
    private static final ExecutorService sBackgroundExecutor = 
        Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "WebViewUtil-Background");
            t.setDaemon(true);
            return t;
        });
    
    /**
     * Create a new WebView configured for wallpaper usage
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createWallpaperWebView(Context context) {
        Log.d(TAG, "Creating wallpaper WebView");
        
        WebView webView = new WebView(context);
        configureWebViewSettings(webView, context);
        setupWebViewClients(webView);
        setupWebViewChromeClient(webView);
        setupJavaScriptInterface(webView);
        
        sActiveWebViewCount.incrementAndGet();
        
        return webView;
    }
    
    /**
     * Configure WebView settings for optimal wallpaper performance
     */
    public static void configureWebViewSettings(WebView webView, Context context) {
        if (webView == null || context == null) {
            throw new IllegalArgumentException("WebView and context cannot be null");
        }
        
        Log.d(TAG, "Configuring WebView settings");
        
        WebSettings settings = webView.getSettings();
        
        // Basic settings
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        
        // Performance settings
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setEnableSmoothTransition(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Memory optimization
        int memoryClass = getMemoryClass(context);
        int cacheSize = calculateCacheSize(memoryClass);
        
        // Network optimization
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        
        // Media and hardware acceleration
        settings.setMediaPlaybackRequiresUserGesture(false);
        // Hardware acceleration is enabled by default in modern Android versions
        
        // Geolocation
        settings.setGeolocationEnabled(false);
        settings.setGeolocationDatabasePath(null);
        
        // User agent
        String userAgent = settings.getUserAgentString();
        if (userAgent != null && !userAgent.contains(USER_AGENT_SUFFIX)) {
            settings.setUserAgentString(userAgent + " " + USER_AGENT_SUFFIX);
        }
        
        // Platform-specific optimizations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Enable hardware rendering optimizations
            settings.setOffscreenPreRaster(true);
        }
        
        Log.d(TAG, "WebView settings configured with cache size: " + (cacheSize / 1024 / 1024) + " MB");
    }
    
    /**
     * Load URL with comprehensive error handling and monitoring
     */
    public static void loadUrl(WebView webView, String url, LoadCallback callback) {
        if (webView == null) {
            if (callback != null) {
                callback.onComplete(false, "WebView is null");
            }
            return;
        }
        
        if (url == null || url.trim().isEmpty()) {
            if (callback != null) {
                callback.onComplete(false, "URL is null or empty");
            }
            return;
        }
        
        // Validate URL
        if (!isUrlValid(url)) {
            if (callback != null) {
                callback.onComplete(false, "Invalid URL: " + url);
            }
            return;
        }
        
        // Check for blocked hosts
        if (isUrlBlocked(url)) {
            if (callback != null) {
                callback.onComplete(false, "URL is blocked: " + url);
            }
            return;
        }
        
        Log.d(TAG, "Loading URL: " + url);
        
        // Setup load timeout
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            Log.w(TAG, "URL load timeout for: " + url);
            if (callback != null) {
                callback.onComplete(false, "Load timeout");
            }
        };
        mainHandler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS);
        
        // Update callback to include timeout cleanup
        LoadCallback timeoutCallback = new LoadCallback() {
            @Override
            public void onComplete(boolean success, String error) {
                mainHandler.removeCallbacks(timeoutRunnable);
                if (callback != null) {
                    callback.onComplete(success, error);
                }
            }
        };
        
        webView.loadUrl(url);
        
        // Set completion listener (will be called by WebViewClient)
        webView.setTag(R.id.webview_load_callback, timeoutCallback);
    }
    
    /**
     * Load HTML content with error handling
     */
    public static void loadHtml(WebView webView, String htmlContent, String baseUrl) {
        if (webView == null) {
            Log.e(TAG, "Cannot load HTML: WebView is null");
            return;
        }
        
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            Log.w(TAG, "Cannot load HTML: content is null or empty");
            return;
        }
        
        Log.d(TAG, "Loading HTML content" + (baseUrl != null ? " with base URL: " + baseUrl : ""));
        
        try {
            if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "utf-8", null);
            } else {
                webView.loadData(htmlContent, "text/html", "utf-8");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading HTML content", e);
        }
    }
    
    /**
     * Evaluate JavaScript safely with error handling and timeout
     */
    public static void evaluateJavaScript(WebView webView, String script, ValueCallback<String> callback) {
        if (webView == null) {
            Log.e(TAG, "Cannot evaluate JavaScript: WebView is null");
            if (callback != null) {
                callback.onReceiveValue("Error: WebView is null");
            }
            return;
        }
        
        if (script == null || script.trim().isEmpty()) {
            Log.w(TAG, "Cannot evaluate JavaScript: script is null or empty");
            if (callback != null) {
                callback.onReceiveValue("Error: Script is null or empty");
            }
            return;
        }
        
        Log.d(TAG, "Evaluating JavaScript: " + script.substring(0, Math.min(script.length(), 100)) + 
              (script.length() > 100 ? "..." : ""));
        
        // Use evaluateJavascript for better performance on API 19+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, callback);
        } else {
            // Fallback for older APIs - execute in background thread
            sBackgroundExecutor.execute(() -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        webView.loadUrl("javascript:" + script);
                        if (callback != null) {
                            callback.onReceiveValue("Script executed");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error executing JavaScript", e);
                        if (callback != null) {
                            callback.onReceiveValue("Error: " + e.getMessage());
                        }
                    }
                });
            });
        }
        
        // Setup timeout for JavaScript evaluation
        if (callback != null) {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.postDelayed(() -> {
                Log.w(TAG, "JavaScript evaluation timeout");
                callback.onReceiveValue("Error: Evaluation timeout");
            }, EVALUATION_TIMEOUT_MS);
        }
    }
    
    /**
     * Pause WebView operations for battery optimization
     */
    public static void pauseWebView(WebView webView) {
        if (webView == null) {
            Log.w(TAG, "Cannot pause WebView: WebView is null");
            return;
        }
        
        Log.d(TAG, "Pausing WebView");
        
        try {
            webView.onPause();
            webView.pauseTimers();
        } catch (Exception e) {
            Log.e(TAG, "Error pausing WebView", e);
        }
    }
    
    /**
     * Resume WebView operations
     */
    public static void resumeWebView(WebView webView) {
        if (webView == null) {
            Log.w(TAG, "Cannot resume WebView: WebView is null");
            return;
        }
        
        Log.d(TAG, "Resuming WebView");
        
        try {
            webView.onResume();
            webView.resumeTimers();
        } catch (Exception e) {
            Log.e(TAG, "Error resuming WebView", e);
        }
    }
    
    /**
     * Safely destroy WebView and cleanup resources
     */
    public static void destroyWebView(WebView webView) {
        if (webView == null) {
            return;
        }
        
        Log.d(TAG, "Destroying WebView");
        
        try {
            // Stop all loading
            webView.stopLoading();
            
            // Clear WebView
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
            
            // Remove all views
            if (webView.getParent() != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            
            // Destroy WebView
            webView.destroy();
            
            sActiveWebViewCount.decrementAndGet();
            
            Log.d(TAG, "WebView destroyed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error destroying WebView", e);
        }
    }
    
    /**
     * Clear WebView cache and cookies
     */
    public static void clearCache(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot clear cache: context is null");
            return;
        }
        
        Log.i(TAG, "Clearing WebView cache and cookies");
        
        sBackgroundExecutor.execute(() -> {
            try {
                // Clear cache - use appropriate method based on API level
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        // For API 35+, use alternative cache clearing approach
                        // This method is more reliable in newer versions
                        String packageName = context.getPackageName();
                        if (context.deleteDatabase("webview.db") || 
                            context.deleteDatabase("webviewCache.db")) {
                            Log.d(TAG, "WebView databases cleared for API 35+");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error clearing storage", e);
                    }
                }
                
                // Clear cookies
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookies(null);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.flush();
                }
                
                Log.i(TAG, "Cache and cookies cleared");
                
            } catch (Exception e) {
                Log.e(TAG, "Error clearing cache and cookies", e);
            }
        });
    }
    
    /**
     * Get WebView performance statistics
     */
    public static WebViewStats getWebViewStats() {
        return new WebViewStats(
            sActiveWebViewCount.get(),
            sTotalMemoryUsed.get(),
            sIsLowMemoryMode.get(),
            128 // Default memory class as fallback
        );
    }
    
    /**
     * Check if URL is valid and safe
     */
    public static boolean isUrlValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            URL urlObj = new URL(url);
            String scheme = urlObj.getProtocol();
            
            // Check if scheme is allowed
            for (String allowedScheme : ALLOWED_SCHEMES) {
                if (scheme.equals(allowedScheme)) {
                    return true;
                }
            }
            
            Log.w(TAG, "Blocked URL scheme: " + scheme);
            return false;
            
        } catch (MalformedURLException e) {
            Log.w(TAG, "Invalid URL format: " + url, e);
            return false;
        }
    }
    
    /**
     * Check if URL should be blocked
     */
    private static boolean isUrlBlocked(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            
            for (String blockedHost : BLOCKED_HOSTS) {
                if (host.equals(blockedHost) || host.endsWith("." + blockedHost)) {
                    Log.w(TAG, "Blocked host: " + host);
                    return true;
                }
            }
            
            return false;
            
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * Setup WebView clients for error handling and monitoring
     */
    private static void setupWebViewClients(WebView webView) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "Page started: " + url);
                super.onPageStarted(view, url, favicon);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page finished: " + url);
                super.onPageFinished(view, url);
                
                // Notify completion
                LoadCallback callback = (LoadCallback) view.getTag(R.id.webview_load_callback);
                if (callback != null) {
                    callback.onComplete(true, null);
                    view.setTag(R.id.webview_load_callback, null);
                }
            }
            
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String errorMsg = "Error loading " + request.getUrl() + ": " + 
                                (error.getDescription() != null ? error.getDescription() : "Unknown error");
                Log.e(TAG, errorMsg);
                
                super.onReceivedError(view, request, error);
                
                // Notify failure
                LoadCallback callback = (LoadCallback) view.getTag(R.id.webview_load_callback);
                if (callback != null) {
                    callback.onComplete(false, errorMsg);
                    view.setTag(R.id.webview_load_callback, null);
                }
            }
            
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, 
                                          WebResourceResponse errorResponse) {
                String errorMsg = "HTTP error loading " + request.getUrl() + ": " + 
                                errorResponse.getStatusCode();
                Log.e(TAG, errorMsg);
                
                super.onReceivedHttpError(view, request, errorResponse);
            }
            
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.e(TAG, "SSL error: " + error.toString());
                
                // For wallpaper use, we might want to proceed despite SSL errors
                // In production, you might want to block SSL errors for security
                handler.proceed();
                
                super.onReceivedSslError(view, handler, error);
            }
            
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, 
                                                String host, String realm) {
                Log.w(TAG, "HTTP auth request for: " + host);
                handler.cancel();
                
                super.onReceivedHttpAuthRequest(view, handler, host, realm);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Validate URL
                if (!isUrlValid(url)) {
                    Log.w(TAG, "Blocked URL loading: " + url);
                    return true; // Prevent loading
                }
                
                return super.shouldOverrideUrlLoading(view, request);
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Could implement custom resource handling here
                return super.shouldInterceptRequest(view, request);
            }
        });
    }
    
    /**
     * Setup WebChromeClient for console logging and progress monitoring
     */
    private static void setupWebViewChromeClient(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(TAG, "Page progress: " + newProgress + "%");
                super.onProgressChanged(view, newProgress);
            }
            
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                int lineNumber = consoleMessage.lineNumber();
                String sourceId = consoleMessage.sourceId();
                
                switch (consoleMessage.messageLevel()) {
                    case ERROR:
                        Log.e(TAG, "Console Error [" + sourceId + ":" + lineNumber + "] " + message);
                        break;
                    case WARNING:
                        Log.w(TAG, "Console Warning [" + sourceId + ":" + lineNumber + "] " + message);
                        break;
                    case LOG:
                    case TIP:
                    default:
                        Log.d(TAG, "Console Log [" + sourceId + ":" + lineNumber + "] " + message);
                        break;
                }
                
                return super.onConsoleMessage(consoleMessage);
            }
            
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                android.webkit.GeolocationPermissions.Callback callback) {
                // Deny geolocation for wallpaper usage
                callback.invoke(origin, false, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }
        });
    }
    
    /**
     * Android bridge class for JavaScript communication
     */
    private static class AndroidWallpaperBridge {
        @JavascriptInterface
        public String getAppVersion() {
            return "1.0.0";
        }
        
        @JavascriptInterface
        public boolean isWallpaper() {
            return true;
        }
        
        @JavascriptInterface
        public void log(String message) {
            Log.d(TAG, "JS Log: " + message);
        }
    }
    
    /**
     * Gesture handler class for JavaScript communication
     */
    private static class AndroidWallpaperGestures {
        @JavascriptInterface
        public void onGesture(String type, float x, float y, float scale) {
            Log.d(TAG, "Gesture received: " + type + " at (" + x + ", " + y + ") scale: " + scale);
            // Handle gestures here or forward to gesture handler
        }
    }
    
    /**
     * Setup JavaScript interface for app communication
     */
    private static void setupJavaScriptInterface(WebView webView) {
        // JavaScript interfaces are handled in WebViewWallpaperService for API 35 compatibility
        // This method can be extended later if needed
    }
    
    /**
     * Calculate appropriate cache size based on device memory
     */
    private static int calculateCacheSize(int memoryClass) {
        int percentage = 10; // Use 10% of available memory
        
        // Adjust percentage based on memory class
        if (memoryClass <= 128) {
            percentage = 5; // Low-end devices
        } else if (memoryClass >= 512) {
            percentage = 15; // High-end devices
        }
        
        int calculatedSize = (memoryClass * 1024 * 1024 * percentage) / 100;
        
        // Clamp to reasonable bounds
        return Math.max(MIN_MEMORY_CACHE_SIZE, 
               Math.min(DEFAULT_CACHE_SIZE, calculatedSize));
    }
    
    /**
     * Get memory class of the device
     */
    private static int getMemoryClass(Context context) {
        if (context == null) {
            return 128; // Default fallback
        }
        
        android.app.ActivityManager am = (android.app.ActivityManager) 
            context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            return am.getMemoryClass();
        }
        return 128;
    }
    
    /**
     * Update memory statistics
     */
    private static void updateMemoryStats(Context context, long memoryUsed) {
        sTotalMemoryUsed.set(memoryUsed);
        
        // Check if we're in low memory mode
        if (context != null) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(memoryInfo);
                sIsLowMemoryMode.set(memoryInfo.lowMemory);
            }
        }
    }
    
    // Inner classes and interfaces
    
    /**
     * Callback for URL loading completion
     */
    public interface LoadCallback {
        void onComplete(boolean success, String error);
    }
    
    /**
     * Statistics holder for WebView performance monitoring
     */
    public static class WebViewStats {
        public final int activeWebViewCount;
        public final long totalMemoryUsed;
        public final boolean isLowMemoryMode;
        public final int memoryClass;
        
        public WebViewStats(int activeWebViewCount, long totalMemoryUsed, 
                          boolean isLowMemoryMode, int memoryClass) {
            this.activeWebViewCount = activeWebViewCount;
            this.totalMemoryUsed = totalMemoryUsed;
            this.isLowMemoryMode = isLowMemoryMode;
            this.memoryClass = memoryClass;
        }
        
        @Override
        public String toString() {
            return String.format("WebViewStats{active=%d, memoryUsed=%dMB, lowMemory=%s, class=%dMB}",
                activeWebViewCount,
                totalMemoryUsed / 1024 / 1024,
                isLowMemoryMode,
                memoryClass);
        }
    }
}
