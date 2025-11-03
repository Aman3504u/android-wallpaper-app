package com.example.wallpaper.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Specialized WebView wrapper for 3D content rendering with WebGL support.
 * Handles gesture communication, asset optimization, security, and performance monitoring.
 */
public class WebView3DRenderer extends WebView {
    private static final String TAG = "WebView3DRenderer";
    
    // JavaScript interface names
    private static final String JS_INTERFACE_NAME = "Android3D";
    private static final String JS_GESTURE_INTERFACE = "Android3DGesture";
    
    // Performance thresholds
    private static final long FRAME_TIME_THRESHOLD_MS = 16; // 60 FPS
    private static final long ASSET_LOAD_TIMEOUT_MS = 30000;
    private static final long JS_EXECUTION_TIMEOUT_MS = 5000;
    
    // Asset optimization settings
    private static final int MAX_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int MAX_DISK_CACHE_SIZE = 200 * 1024 * 1024; // 200MB
    
    // Performance monitoring
    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong totalFrameTime = new AtomicLong(0);
    private final Map<String, Long> assetLoadTimes = new ConcurrentHashMap<>();
    private final Map<String, WebResourceResponse> responseCache = new ConcurrentHashMap<>();
    
    // Gesture handling
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private boolean isGestureEnabled = true;
    
    // Event callbacks
    private On3DReadyListener on3DReadyListener;
    private OnGestureListener onGestureListener;
    private OnPerformanceListener onPerformanceListener;
    private OnErrorListener onErrorListener;
    
    // JavaScript bridges
    private JSGestureBridge gestureBridge;
    
    // Background handler for heavy operations
    private Handler backgroundHandler;
    
    // Current URL
    private String currentUrl;
    
    /**
     * Interface for 3D scene ready callbacks
     */
    public interface On3DReadyListener {
        void on3DReady();
        void on3DError(String error);
    }
    
    /**
     * Interface for gesture callbacks
     */
    public interface OnGestureListener {
        void onTap(float x, float y);
        void onDoubleTap(float x, float y);
        void onPinchStart(float scale);
        void onPinch(float scale);
        void onPinchEnd(float scale);
        void onRotate(float angle, float x, float y);
        void onPan(float deltaX, float deltaY);
    }
    
    /**
     * Interface for performance monitoring callbacks
     */
    public interface OnPerformanceListener {
        void onFPSUpdate(float fps);
        void onMemoryUsage(long usedMemory, long maxMemory);
        void onAssetLoadTime(String assetName, long loadTime);
    }
    
    /**
     * Interface for error callbacks
     */
    public interface OnErrorListener {
        void onJSError(String error, String url, int lineNumber);
        void onResourceError(String resourceUrl, String error);
        void onWebViewError(int errorCode, String description, String failingUrl);
    }
    
    /**
     * JavaScript bridge for gesture communication
     */
    public static class JSGestureBridge {
        private final WebView3DRenderer webViewRenderer;
        private OnGestureListener gestureListener;
        
        public JSGestureBridge(WebView3DRenderer renderer) {
            this.webViewRenderer = renderer;
        }
        
        public void setGestureListener(OnGestureListener listener) {
            this.gestureListener = listener;
        }
        
        @JavascriptInterface
        public void onTap(float x, float y) {
            if (gestureListener != null) {
                gestureListener.onTap(x, y);
            }
        }
        
        @JavascriptInterface
        public void onDoubleTap(float x, float y) {
            if (gestureListener != null) {
                gestureListener.onDoubleTap(x, y);
            }
        }
        
        @JavascriptInterface
        public void onPinchStart(float scale) {
            if (gestureListener != null) {
                gestureListener.onPinchStart(scale);
            }
        }
        
        @JavascriptInterface
        public void onPinch(float scale) {
            if (gestureListener != null) {
                gestureListener.onPinch(scale);
            }
        }
        
        @JavascriptInterface
        public void onPinchEnd(float scale) {
            if (gestureListener != null) {
                gestureListener.onPinchEnd(scale);
            }
        }
        
        @JavascriptInterface
        public void onRotate(float angle, float x, float y) {
            if (gestureListener != null) {
                gestureListener.onRotate(angle, x, y);
            }
        }
        
        @JavascriptInterface
        public void onPan(float deltaX, float deltaY) {
            if (gestureListener != null) {
                gestureListener.onPan(deltaX, deltaY);
            }
        }
        
        @JavascriptInterface
        public void log(String message) {
            Log.d(JS_INTERFACE_NAME, message);
        }
        
        @JavascriptInterface
        public void error(String error) {
            Log.e(JS_INTERFACE_NAME, error);
            if (webViewRenderer.onErrorListener != null) {
                webViewRenderer.onErrorListener.onJSError(error, webViewRenderer.currentUrl, 0);
            }
        }
    }
    
    // Constructors
    public WebView3DRenderer(Context context) {
        super(context);
        initialize();
    }
    
    public WebView3DRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }
    
    public WebView3DRenderer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }
    
    /**
     * Initialize the WebView with 3D-optimized settings
     */
    private void initialize() {
        backgroundHandler = new Handler(Looper.getMainLooper());
        
        // Configure WebView for 3D rendering
        configure3DSettings();
        
        // Setup gesture detection
        setupGestureDetection();
        
        // Setup JavaScript bridge
        setupJavaScriptBridge();
        
        // Setup WebView clients
        setupWebViewClients();
        
        // Setup performance monitoring
        setupPerformanceMonitoring();
        
        Log.d(TAG, "WebView3DRenderer initialized");
    }
    
    /**
     * Configure WebView settings for optimal 3D rendering
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configure3DSettings() {
        WebSettings settings = getSettings();
        
        // Enable JavaScript for 3D interactions
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // WebGL support
        settings.setWebGLEnabled(true);
        settings.setBuiltInZoomControls(false); // We'll handle zoom ourselves
        settings.setDisplayZoomControls(false);
        
        // Rendering optimization
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setEnableSupportedSmoothZoom(true);
        
        // DOM and database storage
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // Cache settings for asset optimization
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAppCacheMaxSize(MAX_CACHE_SIZE);
        
        // Mixed content handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        // Hardware acceleration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setLayerType(LAYER_TYPE_HARDWARE, null);
        }
        
        // Media settings
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        
        // Performance settings
        settings.setGeolocationEnabled(false); // Disable for performance
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
        
        // Hardware acceleration flags
        setBackgroundColor(Color.BLACK);
        
        Log.d(TAG, "3D settings configured");
    }
    
    /**
     * Setup gesture detection for 3D interactions
     */
    private void setupGestureDetection() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isGestureEnabled && onGestureListener != null) {
                    onGestureListener.onTap(e.getX(), e.getY());
                    sendGestureToJS("tap", e.getX(), e.getY());
                }
                return true;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isGestureEnabled && onGestureListener != null) {
                    onGestureListener.onDoubleTap(e.getX(), e.getY());
                    sendGestureToJS("doubleTap", e.getX(), e.getY());
                }
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isGestureEnabled && onGestureListener != null) {
                    onGestureListener.onPan(distanceX, distanceY);
                    sendGestureToJS("pan", distanceX, distanceY);
                }
                return true;
            }
        });
        
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float lastScale = 1.0f;
            
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastScale = 1.0f;
                if (isGestureEnabled && onGestureListener != null) {
                    onGestureListener.onPinchStart(lastScale);
                }
                sendGestureToJS("pinchStart", lastScale, 0);
                return true;
            }
            
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                if (isGestureEnabled && onGestureListener != null) {
                    onGestureListener.onPinch(scale);
                }
                lastScale = scale;
                sendGestureToJS("pinch", scale, detector.getFocusX(), detector.getFocusY());
                return true;
            }
            
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                if (isGestureEnabled && onGestureListener != null) {
                    onGestureListener.onPinchEnd(scale);
                }
                sendGestureToJS("pinchEnd", scale, 0);
            }
        });
    }
    
    /**
     * Setup JavaScript bridges for communication
     */
    private void setupJavaScriptBridge() {
        gestureBridge = new JSGestureBridge(this);
        addJavascriptInterface(gestureBridge, JS_GESTURE_INTERFACE);
    }
    
    /**
     * Setup WebView clients for 3D content handling
     */
    private void setupWebViewClients() {
        // WebViewClient for page loading and resource management
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                currentUrl = url;
                Log.d(TAG, "Page started loading: " + url);
                super.onPageStarted(view, url, favicon);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page finished loading: " + url);
                initialize3DScene();
                super.onPageFinished(view, url);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + errorCode + " - " + description);
                if (onErrorListener != null) {
                    onErrorListener.onWebViewError(errorCode, description, failingUrl);
                }
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
            
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Log.e(TAG, "Main frame error: " + error.getDescription());
                    if (onErrorListener != null) {
                        onErrorListener.onWebViewError(
                            error.getErrorCode(),
                            error.getDescription().toString(),
                            request.getUrl().toString()
                        );
                    }
                }
                super.onReceivedError(view, request, error);
            }
            
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                Log.e(TAG, "HTTP error: " + errorResponse.getStatusCode() + " for " + request.getUrl());
                if (onErrorListener != null) {
                    onErrorListener.onResourceError(
                        request.getUrl().toString(),
                        "HTTP " + errorResponse.getStatusCode()
                    );
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }
            
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Cache management and optimization
                long startTime = System.currentTimeMillis();
                WebResourceResponse cached = responseCache.get(url);
                
                if (cached != null) {
                    Log.d(TAG, "Serving cached resource: " + url);
                    return cached;
                }
                
                WebResourceResponse response = optimizeAssetLoading(url, request);
                if (response != null) {
                    // Cache the response for future use
                    responseCache.put(url, response);
                    
                    long loadTime = System.currentTimeMillis() - startTime;
                    assetLoadTimes.put(url, loadTime);
                    
                    if (onPerformanceListener != null) {
                        onPerformanceListener.onAssetLoadTime(url, loadTime);
                    }
                }
                
                return response;
            }
        });
        
        // WebChromeClient for console messages and JavaScript alerts
        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                String sourceId = consoleMessage.sourceId();
                int lineNumber = consoleMessage.lineNumber();
                
                Log.d(TAG, "JS Console: " + message + " at " + sourceId + ":" + lineNumber);
                
                if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    if (onErrorListener != null) {
                        onErrorListener.onJSError(message, sourceId, lineNumber);
                    }
                }
                
                return super.onConsoleMessage(consoleMessage);
            }
            
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "JS Alert: " + message);
                return super.onJsAlert(view, url, message, result);
            }
            
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "JS Confirm: " + message);
                return super.onJsConfirm(view, url, message, result);
            }
        });
    }
    
    /**
     * Setup performance monitoring
     */
    private void setupPerformanceMonitoring() {
        // Frame rate monitoring
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Monitor every second
                    
                    long currentFrameCount = frameCount.get();
                    long currentTotalTime = totalFrameTime.get();
                    
                    if (currentFrameCount > 0) {
                        float avgFrameTime = (float) currentTotalTime / currentFrameCount;
                        float fps = 1000.0f / avgFrameTime;
                        
                        if (onPerformanceListener != null) {
                            onPerformanceListener.onFPSUpdate(fps);
                        }
                        
                        Log.d(TAG, String.format("FPS: %.2f, Avg Frame Time: %.2fms", fps, avgFrameTime));
                    }
                    
                    // Memory monitoring
                    Runtime runtime = Runtime.getRuntime();
                    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                    long maxMemory = runtime.maxMemory();
                    
                    if (onPerformanceListener != null) {
                        onPerformanceListener.onMemoryUsage(usedMemory, maxMemory);
                    }
                    
                } catch (InterruptedException e) {
                    Log.e(TAG, "Performance monitoring interrupted", e);
                    break;
                }
            }
        }).start();
        
        Log.d(TAG, "Performance monitoring started");
    }
    
    /**
     * Initialize the 3D scene after page load
     */
    private void initialize3DScene() {
        backgroundHandler.post(() -> {
            // Inject gesture initialization script
            String initScript = getGestureInitScript();
            evaluateJavascript(initScript, null);
            
            // Check if 3D context is available
            String checkScript = 
                "if (typeof window !== 'undefined' && window.WebGLRenderingContext) {\n" +
                "  window.Android3D.notify3DReady();\n" +
                "} else {\n" +
                "  window.Android3D.notify3DError('WebGL not supported');\n" +
                "}";
            
            evaluateJavascript(checkScript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "3D scene initialization complete");
                }
            });
        });
    }
    
    /**
     * Get the gesture initialization JavaScript
     */
    private String getGestureInitScript() {
        return "(function() {\n" +
               "  if (typeof window.Android3D === 'undefined') {\n" +
               "    window.Android3D = {};\n" +
               "  }\n" +
               "  \n" +
               "  // Gesture event listeners\n" +
               "  let touchStartTime = 0;\n" +
               "  let lastTapTime = 0;\n" +
               "  let touchStartX = 0;\n" +
               "  let touchStartY = 0;\n" +
               "  \n" +
               "  document.addEventListener('touchstart', function(e) {\n" +
               "    touchStartTime = Date.now();\n" +
               "    if (e.touches.length > 0) {\n" +
               "      touchStartX = e.touches[0].clientX;\n" +
               "      touchStartY = e.touches[0].clientY;\n" +
               "    }\n" +
               "  });\n" +
               "  \n" +
               "  document.addEventListener('touchend', function(e) {\n" +
               "    let currentTime = Date.now();\n" +
               "    let touchDuration = currentTime - touchStartTime;\n" +
               "    \n" +
               "    if (e.changedTouches.length > 0) {\n" +
               "      let touchEndX = e.changedTouches[0].clientX;\n" +
               "      let touchEndY = e.changedTouches[0].clientY;\n" +
               "      let touchDistance = Math.sqrt(\n" +
               "        Math.pow(touchEndX - touchStartX, 2) + \n" +
               "        Math.pow(touchEndY - touchStartY, 2)\n" +
               "      );\n" +
               "      \n" +
               "      if (touchDuration < 300 && touchDistance < 10) {\n" +
               "        let timeSinceLastTap = currentTime - lastTapTime;\n" +
               "        if (timeSinceLastTap < 300) {\n" +
               "          Android3DGesture.onDoubleTap(touchEndX, touchEndY);\n" +
               "        } else {\n" +
               "          Android3DGesture.onTap(touchEndX, touchEndY);\n" +
               "        }\n" +
               "        lastTapTime = currentTime;\n" +
               "      }\n" +
               "    }\n" +
               "  });\n" +
               "  \n" +
               "  // Prevent default gestures for 3D canvas\n" +
               "  document.addEventListener('gesturestart', function(e) {\n" +
               "    e.preventDefault();\n" +
               "  });\n" +
               "  \n" +
               "  // 3D ready callbacks\n" +
               "  window.Android3D.notify3DReady = function() {\n" +
               "    if (window.Android3D && window.Android3D.onReady) {\n" +
               "      window.Android3D.onReady();\n" +
               "    }\n" +
               "  };\n" +
               "  \n" +
               "  window.Android3D.notify3DError = function(error) {\n" +
               "    if (window.Android3D && window.Android3D.onError) {\n" +
               "      window.Android3D.onError(error);\n" +
               "    }\n" +
               "  };\n" +
               "  \n" +
               "  console.log('3D gesture system initialized');\n" +
               "})();";
    }
    
    /**
     * Optimize asset loading with caching and compression
     */
    private WebResourceResponse optimizeAssetLoading(String url, WebResourceRequest request) {
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            
            // Optimize common 3D asset types
            if (path != null && isOptimizableAsset(path)) {
                Log.d(TAG, "Optimizing asset: " + url);
                
                // Check if it's a local asset we can serve
                InputStream optimizedStream = getOptimizedAssetStream(url);
                if (optimizedStream != null) {
                    String mimeType = getMimeType(url);
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cache-Control", "public, max-age=86400"); // 24h cache
                    headers.put("X-Optimized", "true");
                    
                    return new WebResourceResponse(mimeType, "utf-8", headers, optimizedStream);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error optimizing asset: " + url, e);
        }
        
        return null;
    }
    
    /**
     * Check if asset is optimizable
     */
    private boolean isOptimizableAsset(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".js") || 
               lowerPath.endsWith(".css") || 
               lowerPath.endsWith(".png") || 
               lowerPath.endsWith(".jpg") || 
               lowerPath.endsWith(".jpeg") || 
               lowerPath.endsWith(".webp") ||
               lowerPath.endsWith(".3d") ||
               lowerPath.endsWith(".obj") ||
               lowerPath.endsWith(".gltf") ||
               lowerPath.endsWith(".glb");
    }
    
    /**
     * Get optimized asset stream
     */
    private InputStream getOptimizedAssetStream(String url) {
        // Implement asset optimization logic here
        // This could include:
        // - Minification of JS/CSS
        // - Image compression
        // - 3D model compression
        
        return null; // Return null to use default loading
    }
    
    /**
     * Get MIME type for URL
     */
    private String getMimeType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".html")) return "text/html";
        if (lowerUrl.endsWith(".js")) return "application/javascript";
        if (lowerUrl.endsWith(".css")) return "text/css";
        if (lowerUrl.endsWith(".png")) return "image/png";
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) return "image/jpeg";
        if (lowerUrl.endsWith(".webp")) return "image/webp";
        if (lowerUrl.endsWith(".3d")) return "application/octet-stream";
        if (lowerUrl.endsWith(".obj")) return "application/octet-stream";
        if (lowerUrl.endsWith(".gltf") || lowerUrl.endsWith(".glb")) return "application/octet-stream";
        return "text/plain";
    }
    
    /**
     * Send gesture data to JavaScript
     */
    private void sendGestureToJS(String gestureType, float... params) {
        StringBuilder script = new StringBuilder();
        script.append("if (window.Android3DGesture && window.Android3DGesture.on").append(capitalize(gestureType)).append(") {");
        script.append("window.Android3DGesture.on").append(capitalize(gestureType)).append("(");
        
        for (int i = 0; i < params.length; i++) {
            if (i > 0) script.append(", ");
            script.append(params[i]);
        }
        
        script.append(");}");
        
        evaluateJavascript(script.toString(), null);
    }
    
    /**
     * Capitalize string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    // Public API methods
    
    /**
     * Load 3D content URL
     */
    public void load3DContent(String url) {
        Log.d(TAG, "Loading 3D content: " + url);
        loadUrl(url);
    }
    
    /**
     * Load 3D content from string
     */
    public void load3DContentFromData(String data, String mimeType) {
        Log.d(TAG, "Loading 3D content from data");
        loadDataWithBaseURL(null, data, mimeType, "utf-8", null);
    }
    
    /**
     * Enable or disable gesture handling
     */
    public void setGestureEnabled(boolean enabled) {
        isGestureEnabled = enabled;
        Log.d(TAG, "Gesture handling: " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Execute JavaScript in the 3D context
     */
    public void execute3DScript(String script, ValueCallback<String> callback) {
        Log.d(TAG, "Executing 3D script: " + script.substring(0, Math.min(50, script.length())));
        evaluateJavascript(script, callback);
    }
    
    /**
     * Set gesture listener
     */
    public void setOnGestureListener(OnGestureListener listener) {
        this.onGestureListener = listener;
        if (gestureBridge != null) {
            gestureBridge.setGestureListener(listener);
        }
    }
    
    /**
     * Set 3D ready listener
     */
    public void setOn3DReadyListener(On3DReadyListener listener) {
        this.on3DReadyListener = listener;
        
        // Add JavaScript callbacks for 3D ready events
        String script = "if (typeof window.Android3D !== 'undefined') {\n" +
                       "  window.Android3D.onReady = function() {\n" +
                       "    Android3D.log('3D Scene is ready');\n" +
                       "  };\n" +
                       "  window.Android3D.onError = function(error) {\n" +
                       "    Android3D.log('3D Error: ' + error);\n" +
                       "  };\n" +
                       "}";
        evaluateJavascript(script, null);
    }
    
    /**
     * Set performance listener
     */
    public void setOnPerformanceListener(OnPerformanceListener listener) {
        this.onPerformanceListener = listener;
    }
    
    /**
     * Set error listener
     */
    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }
    
    /**
     * Clear caches and performance data
     */
    public void clearCache() {
        responseCache.clear();
        assetLoadTimes.clear();
        clearCache(true, true);
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Get performance statistics
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("frameCount", frameCount.get());
        stats.put("totalFrameTime", totalFrameTime.get());
        stats.put("assetLoadTimes", new HashMap<>(assetLoadTimes));
        stats.put("responseCacheSize", responseCache.size());
        stats.put("currentUrl", currentUrl);
        return stats;
    }
    
    /**
     * Pause 3D rendering
     */
    public void pause3DRendering() {
        onPause();
        Log.d(TAG, "3D rendering paused");
    }
    
    /**
     * Resume 3D rendering
     */
    public void resume3DRendering() {
        onResume();
        Log.d(TAG, "3D rendering resumed");
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        
        if (isGestureEnabled) {
            if (scaleGestureDetector.onTouchEvent(event)) {
                handled = true;
            } else if (gestureDetector.onTouchEvent(event)) {
                handled = true;
            }
        }
        
        if (!handled) {
            handled = super.onTouchEvent(event);
        }
        
        return handled;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        clearCache();
        super.onDetachedFromWindow();
        Log.d(TAG, "WebView3DRenderer detached");
    }
    
    /**
     * Get current URL
     */
    public String getCurrentUrl() {
        return currentUrl;
    }
    
    /**
     * Set security settings for 3D content
     */
    public void setSecuritySettings() {
        WebSettings settings = getSettings();
        
        // HTTPS enforcement for production
        if (!Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        
        // Disable potential security risks
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        
        Log.d(TAG, "Security settings applied");
    }
}
