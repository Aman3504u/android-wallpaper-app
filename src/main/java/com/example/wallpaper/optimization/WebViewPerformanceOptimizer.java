package com.example.wallpaper.optimization;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive WebView Performance Optimizer for Android Wallpaper Application
 * 
 * Features:
 * - Memory management and garbage collection optimization
 * - Hardware acceleration configuration
 * - WebGL performance tuning
 * - Network optimization with caching strategies
 * - Frame rate control based on device capability
 * - Battery usage monitoring and optimization
 * - Performance metrics collection and reporting
 * - Device-specific optimizations
 */
public class WebViewPerformanceOptimizer implements Choreographer.FrameCallback {
    
    private static final String TAG = "WebViewOptimizer";
    
    // Performance metrics
    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong lastFrameTime = new AtomicLong(0);
    private final AtomicReference<Float> currentFrameRate = new AtomicReference<>(0f);
    private final List<PerformanceMetric> metricsHistory = new ArrayList<>();
    private final ExecutorService metricsExecutor = Executors.newCachedThreadPool();
    
    // Device and system information
    private final Context context;
    private final DeviceCapabilities deviceCapabilities;
    private final BatteryOptimizer batteryOptimizer;
    private final NetworkOptimizer networkOptimizer;
    private final MemoryOptimizer memoryOptimizer;
    
    // WebView optimization state
    private volatile boolean isOptimized = false;
    private volatile int targetFrameRate = 60;
    private volatile boolean enableHardwareAcceleration = true;
    private volatile boolean enableWebGL = true;
    private volatile int memoryLimitMB = 128;
    
    // Callbacks and listeners
    private final List<PerformanceListener> performanceListeners = new ArrayList<>();
    private WeakReference<WebView> webViewWeakRef;
    
    // Frame rate control
    private final Choreographer choreographer = Choreographer.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int MIN_FRAME_INTERVAL = 1000 / 120; // 8.33ms
    private long lastScheduledFrame = 0;
    
    /**
     * Performance metric data structure
     */
    public static class PerformanceMetric {
        public final long timestamp;
        public final float frameRate;
        public final long memoryUsedMB;
        public final float cpuUsage;
        public final float batteryLevel;
        public final int networkType;
        public final long cacheSizeMB;
        public final boolean isLowEndDevice;
        
        public PerformanceMetric(long timestamp, float frameRate, long memoryUsedMB, 
                               float cpuUsage, float batteryLevel, int networkType, 
                               long cacheSizeMB, boolean isLowEndDevice) {
            this.timestamp = timestamp;
            this.frameRate = frameRate;
            this.memoryUsedMB = memoryUsedMB;
            this.cpuUsage = cpuUsage;
            this.batteryLevel = batteryLevel;
            this.networkType = networkType;
            this.cacheSizeMB = cacheSizeMB;
            this.isLowEndDevice = isLowEndDevice;
        }
    }
    
    /**
     * Performance listener interface
     */
    public interface PerformanceListener {
        void onPerformanceUpdate(PerformanceMetric metric);
        void onBatteryLow(boolean isLow);
        void onMemoryPressure(boolean isLow);
        void onNetworkQualityChanged(int quality);
        void onFrameRateAdjusted(int newFrameRate);
    }
    
    /**
     * Device capabilities detector
     */
    private static class DeviceCapabilities {
        public final int cpuCores;
        public final long totalMemoryMB;
        public final int gpuTier;
        public final boolean isLowEndDevice;
        public final String architecture;
        public final int apiLevel;
        
        public DeviceCapabilities(Context context) {
            this.cpuCores = Runtime.getRuntime().availableProcessors();
            this.totalMemoryMB = (long) (Runtime.getRuntime().totalMemory() / (1024 * 1024));
            this.gpuTier = detectGPUTier();
            this.isLowEndDevice = detectLowEndDevice();
            this.architecture = System.getProperty("os.arch");
            this.apiLevel = android.os.Build.VERSION.SDK_INT;
        }
        
        private int detectGPUTier() {
            // Simple GPU detection based on OpenGL ES version and device info
            String renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER);
            if (renderer == null) return 1; // Default to low tier
            
            // Adreno (Qualcomm)
            if (renderer.toLowerCase().contains("adreno")) {
                String version = renderer.toLowerCase();
                if (version.contains("630") || version.contains("640") || version.contains("650") || 
                    version.contains("660") || version.contains("730") || version.contains("740")) {
                    return 3; // High tier
                } else if (version.contains("530") || version.contains("540") || version.contains("610")) {
                    return 2; // Medium tier
                }
                return 1; // Low tier
            }
            
            // Mali (ARM)
            if (renderer.toLowerCase().contains("mali")) {
                if (renderer.toLowerCase().contains("g78") || renderer.toLowerCase().contains("g77")) {
                    return 3; // High tier
                } else if (renderer.toLowerCase().contains("g71") || renderer.toLowerCase().contains("g72")) {
                    return 2; // Medium tier
                }
                return 1; // Low tier
            }
            
            // PowerVR (Imagination)
            if (renderer.toLowerCase().contains("powervr")) {
                return 2; // Generally medium tier
            }
            
            return 2; // Default to medium tier
        }
        
        private boolean detectLowEndDevice() {
            // Detect low-end devices based on multiple factors
            boolean lowMemory = totalMemoryMB < 1024; // Less than 1GB RAM
            boolean fewCores = cpuCores <= 2;
            boolean lowTierGPU = gpuTier == 1;
            boolean oldAPI = apiLevel < 21;
            
            return (lowMemory && fewCores) || (lowTierGPU && (lowMemory || fewCores));
        }
    }
    
    /**
     * Battery optimization manager
     */
    private static class BatteryOptimizer {
        private final Context context;
        private BatteryManager batteryManager;
        private boolean isLowBattery = false;
        private final List<Runnable> lowBatteryCallbacks = new ArrayList<>();
        
        public BatteryOptimizer(Context context) {
            this.context = context;
            batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        }
        
        public float getBatteryLevel() {
            if (batteryManager != null) {
                return (float) batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f;
            }
            return 1.0f; // Assume full battery if unavailable
        }
        
        public boolean isLowBattery() {
            float level = getBatteryLevel();
            boolean currentLow = level < 0.15f; // 15% threshold
            if (currentLow != isLowBattery) {
                isLowBattery = currentLow;
                notifyLowBatteryCallbacks();
            }
            return isLowBattery;
        }
        
        public void addLowBatteryCallback(Runnable callback) {
            lowBatteryCallbacks.add(callback);
        }
        
        private void notifyLowBatteryCallbacks() {
            for (Runnable callback : lowBatteryCallbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    Log.e(TAG, "Error in low battery callback", e);
                }
            }
        }
    }
    
    /**
     * Network optimization manager
     */
    private static class NetworkOptimizer {
        private final Context context;
        private ConnectivityManager connectivityManager;
        private int currentNetworkQuality = 3; // 1=poor, 2=fair, 3=good, 4=excellent
        
        public NetworkOptimizer(Context context) {
            this.context = context;
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        
        public int getNetworkQuality() {
            if (connectivityManager != null) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (capabilities != null) {
                        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            currentNetworkQuality = 4; // Excellent
                        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            currentNetworkQuality = 2; // Fair (cellular)
                        } else {
                            currentNetworkQuality = 3; // Good (ethernet)
                        }
                    }
                }
            }
            return currentNetworkQuality;
        }
        
        public boolean isWifiConnected() {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }
        
        public boolean isCellularConnected() {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
        }
    }
    
    /**
     * Memory optimization manager
     */
    private static class MemoryOptimizer {
        private final Runtime runtime = Runtime.getRuntime();
        private long lastGCTime = 0;
        private static final long GC_THRESHOLD_MS = 5000; // 5 seconds
        
        public long getUsedMemoryMB() {
            return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        }
        
        public long getMaxMemoryMB() {
            return runtime.maxMemory() / (1024 * 1024);
        }
        
        public float getMemoryUsageRatio() {
            return (float) getUsedMemoryMB() / getMaxMemoryMB();
        }
        
        public boolean shouldTriggerGC() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastGCTime > GC_THRESHOLD_MS) {
                System.gc();
                lastGCTime = currentTime;
                return true;
            }
            return false;
        }
        
        public boolean isLowMemory() {
            return getMemoryUsageRatio() > 0.8f; // 80% threshold
        }
    }
    
    /**
     * Constructor
     */
    public WebViewPerformanceOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.deviceCapabilities = new DeviceCapabilities(context);
        this.batteryOptimizer = new BatteryOptimizer(context);
        this.networkOptimizer = new NetworkOptimizer(context);
        this.memoryOptimizer = new MemoryOptimizer();
        
        initializeOptimizations();
        
        // Set up battery monitoring
        batteryOptimizer.addLowBatteryCallback(() -> {
            if (isOptimized) {
                adjustForLowBattery();
            }
        });
        
        Log.i(TAG, "WebView Performance Optimizer initialized");
        Log.i(TAG, String.format("Device: %d cores, %dMB RAM, GPU tier %d, Low-end: %b", 
                deviceCapabilities.cpuCores, deviceCapabilities.totalMemoryMB, 
                deviceCapabilities.gpuTier, deviceCapabilities.isLowEndDevice));
    }
    
    /**
     * Initialize optimizations based on device capabilities
     */
    private void initializeOptimizations() {
        // Set memory limit based on device capabilities
        if (deviceCapabilities.isLowEndDevice) {
            memoryLimitMB = 64;
            targetFrameRate = 30;
            enableHardwareAcceleration = true; // Still enable but with lower settings
            enableWebGL = deviceCapabilities.gpuTier >= 2; // Only enable WebGL on medium+ GPUs
        } else if (deviceCapabilities.totalMemoryMB < 2048) {
            memoryLimitMB = 96;
            targetFrameRate = 45;
            enableWebGL = true;
        } else {
            memoryLimitMB = 128;
            targetFrameRate = 60;
            enableWebGL = true;
        }
        
        Log.i(TAG, String.format("Initial settings: Memory limit %dMB, Frame rate %d, WebGL %b", 
                memoryLimitMB, targetFrameRate, enableWebGL));
    }
    
    /**
     * Apply optimizations to WebView
     */
    public void optimizeWebView(WebView webView) {
        if (webView == null) {
            Log.e(TAG, "WebView is null, cannot optimize");
            return;
        }
        
        this.webViewWeakRef = new WeakReference<>(webView);
        WebSettings settings = webView.getSettings();
        
        try {
            // Memory management optimization
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setAppCacheEnabled(true);
            settings.setAppCachePath(context.getCacheDir().getAbsolutePath());
            settings.setDatabasePath(context.getDatabasePath("webview.db").getAbsolutePath());
            settings.setDomStorageQuota(memoryLimitMB * 1024 * 1024); // Convert to bytes
            
            // JavaScript and rendering optimization
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setSupportMultipleWindows(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            
            // Hardware acceleration
            if (enableHardwareAcceleration) {
                webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
                settings.setEnableSmoothScroll(true);
                settings.setEnableLightTouchCompressor(true);
            } else {
                webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            }
            
            // WebGL optimization
            settings.setWebGLEnabled(enableWebGL);
            if (enableWebGL) {
                settings.setWebGLAntialiasingEnabled(deviceCapabilities.gpuTier >= 2);
                settings.setWebGLBackend(enableWebGL ? "opengl_es_2_0" : "software");
            }
            
            // Network optimization
            optimizeNetworkSettings(settings);
            
            // Rendering quality based on device capability
            if (deviceCapabilities.isLowEndDevice) {
                settings.setRenderPriority(WebSettings.RenderPriority.LOW);
                settings.setGeolocationEnabled(false);
                settings.setMediaPlaybackRequiresUserGesture(true);
            } else if (deviceCapabilities.gpuTier >= 2) {
                settings.setRenderPriority(WebSettings.RenderPriority.NORMAL);
                settings.setEnableHybridComposition(true);
            }
            
            // Privacy and security optimization
            settings.setGeolocationEnabled(false);
            settings.setSavePassword(false);
            settings.setSaveFormData(false);
            settings.setBlockNetworkLoads(false);
            settings.setBlockNetworkImageLoads(false);
            
            // Performance specific settings
            settings.setUserAgentString(buildOptimizedUserAgent(settings.getUserAgentString()));
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setDefaultTextEncodingName("utf-8");
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            
            isOptimized = true;
            
            Log.i(TAG, "WebView optimization applied successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying WebView optimizations", e);
        }
    }
    
    /**
     * Build optimized user agent string
     */
    private String buildOptimizedUserAgent(String originalUA) {
        StringBuilder optimizedUA = new StringBuilder(originalUA);
        
        // Add performance optimization hints
        if (deviceCapabilities.isLowEndDevice) {
            optimizedUA.append(" Wallpaper/LowEnd");
        } else {
            optimizedUA.append(" Wallpaper/Optimized");
        }
        
        // Add GPU info for WebGL optimization
        optimizedUA.append(" GPU/").append(deviceCapabilities.gpuTier);
        
        return optimizedUA.toString();
    }
    
    /**
     * Optimize network settings based on current network quality
     */
    private void optimizeNetworkSettings(WebSettings settings) {
        int networkQuality = networkOptimizer.getNetworkQuality();
        
        switch (networkQuality) {
            case 1: // Poor
                settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                settings.setLoadsImagesAutomatically(false);
                settings.setJavaScriptEnabled(false);
                break;
                
            case 2: // Fair
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
                settings.setLoadsImagesAutomatically(true);
                settings.setJavaScriptEnabled(true);
                settings.setLoadWithOverviewMode(false);
                break;
                
            case 3: // Good
            case 4: // Excellent
                settings.setCacheMode(WebSettings.LOAD_DEFAULT);
                settings.setLoadsImagesAutomatically(true);
                settings.setJavaScriptEnabled(true);
                settings.setLoadWithOverviewMode(true);
                break;
        }
        
        Log.d(TAG, String.format("Network quality: %d, Cache mode: %d", 
                networkQuality, settings.getCacheMode()));
    }
    
    /**
     * Adjust settings for low battery
     */
    private void adjustForLowBattery() {
        targetFrameRate = Math.max(15, targetFrameRate - 15);
        memoryLimitMB = Math.max(32, memoryLimitMB - 16);
        enableWebGL = false; // Disable WebGL to save power
        
        // Notify listeners
        for (PerformanceListener listener : performanceListeners) {
            listener.onBatteryLow(true);
            listener.onFrameRateAdjusted(targetFrameRate);
        }
        
        Log.i(TAG, String.format("Adjusted for low battery: Frame rate %d, Memory limit %dMB, WebGL %b", 
                targetFrameRate, memoryLimitMB, enableWebGL));
    }
    
    /**
     * Start performance monitoring
     */
    public void startPerformanceMonitoring() {
        choreographer.postFrameCallback(this);
        Log.i(TAG, "Performance monitoring started");
    }
    
    /**
     * Stop performance monitoring
     */
    public void stopPerformanceMonitoring() {
        choreographer.removeFrameCallback(this);
        Log.i(TAG, "Performance monitoring stopped");
    }
    
    /**
     * Frame callback for Choreographer
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        updatePerformanceMetrics();
        
        // Control frame rate
        if (System.currentTimeMillis() - lastScheduledFrame >= MIN_FRAME_INTERVAL) {
            scheduleNextFrame();
            lastScheduledFrame = System.currentTimeMillis();
        }
    }
    
    /**
     * Schedule next frame
     */
    private void scheduleNextFrame() {
        // Check for system optimizations
        if (batteryOptimizer.isLowBattery() && targetFrameRate > 30) {
            targetFrameRate = 30;
            notifyFrameRateChanged();
        }
        
        if (memoryOptimizer.isLowMemory()) {
            optimizeMemoryUsage();
        }
        
        // Schedule next frame
        choreographer.postFrameCallback(this);
    }
    
    /**
     * Update performance metrics
     */
    private void updatePerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        frameCount.incrementAndGet();
        
        // Calculate frame rate
        if (lastFrameTime.get() > 0) {
            long frameTime = currentTime - lastFrameTime.get();
            float fps = frameTime > 0 ? 1000f / frameTime : 0f;
            currentFrameRate.set(fps);
        }
        lastFrameTime.set(currentTime);
        
        // Create performance metric
        PerformanceMetric metric = new PerformanceMetric(
            currentTime,
            currentFrameRate.get(),
            memoryOptimizer.getUsedMemoryMB(),
            getCPUUsage(),
            batteryOptimizer.getBatteryLevel(),
            networkOptimizer.getNetworkQuality(),
            getCacheSizeMB(),
            deviceCapabilities.isLowEndDevice
        );
        
        // Store metric
        metricsHistory.add(metric);
        if (metricsHistory.size() > 100) {
            metricsHistory.remove(0);
        }
        
        // Notify listeners
        for (PerformanceListener listener : performanceListeners) {
            listener.onPerformanceUpdate(metric);
        }
    }
    
    /**
     * Get CPU usage (simplified implementation)
     */
    private float getCPUUsage() {
        // Simple CPU usage estimation
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return (float) memoryInfo.getTotalPss() / (deviceCapabilities.totalMemoryMB * 1024) * 100f;
    }
    
    /**
     * Get cache size in MB
     */
    private long getCacheSizeMB() {
        try {
            java.io.File cacheDir = context.getCacheDir();
            if (cacheDir.exists()) {
                return cacheDir.length() / (1024 * 1024);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting cache size", e);
        }
        return 0;
    }
    
    /**
     * Optimize memory usage
     */
    private void optimizeMemoryUsage() {
        if (memoryOptimizer.shouldTriggerGC()) {
            Log.d(TAG, "Triggered garbage collection");
        }
        
        // Clear WebView cache if memory is critically low
        if (memoryOptimizer.getMemoryUsageRatio() > 0.9f) {
            WebView webView = webViewWeakRef.get();
            if (webView != null) {
                webView.clearCache(true);
                Log.w(TAG, "Cleared WebView cache due to memory pressure");
            }
        }
    }
    
    /**
     * Notify frame rate changed
     */
    private void notifyFrameRateChanged() {
        for (PerformanceListener listener : performanceListeners) {
            listener.onFrameRateAdjusted(targetFrameRate);
        }
    }
    
    /**
     * Get current performance summary
     */
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("frameRate", currentFrameRate.get());
        summary.put("memoryUsedMB", memoryOptimizer.getUsedMemoryMB());
        summary.put("memoryMaxMB", memoryOptimizer.getMaxMemoryMB());
        summary.put("batteryLevel", batteryOptimizer.getBatteryLevel());
        summary.put("networkQuality", networkOptimizer.getNetworkQuality());
        summary.put("targetFrameRate", targetFrameRate);
        summary.put("isOptimized", isOptimized);
        summary.put("deviceCapabilities", deviceCapabilities);
        
        return summary;
    }
    
    /**
     * Add performance listener
     */
    public void addPerformanceListener(PerformanceListener listener) {
        if (listener != null) {
            performanceListeners.add(listener);
        }
    }
    
    /**
     * Remove performance listener
     */
    public void removePerformanceListener(PerformanceListener listener) {
        performanceListeners.remove(listener);
    }
    
    /**
     * Set target frame rate
     */
    public void setTargetFrameRate(int frameRate) {
        this.targetFrameRate = Math.max(15, Math.min(120, frameRate));
        notifyFrameRateChanged();
    }
    
    /**
     * Enable or disable hardware acceleration
     */
    public void setHardwareAcceleration(boolean enabled) {
        this.enableHardwareAcceleration = enabled;
        WebView webView = webViewWeakRef.get();
        if (webView != null) {
            webView.setLayerType(enabled ? WebView.LAYER_TYPE_HARDWARE : WebView.LAYER_TYPE_SOFTWARE, null);
        }
    }
    
    /**
     * Enable or disable WebGL
     */
    public void setWebGLEnabled(boolean enabled) {
        this.enableWebGL = enabled && deviceCapabilities.gpuTier >= 2;
        WebView webView = webViewWeakRef.get();
        if (webView != null) {
            webView.getSettings().setWebGLEnabled(this.enableWebGL);
        }
    }
    
    /**
     * Force optimization reset
     */
    public void resetOptimizations() {
        isOptimized = false;
        initializeOptimizations();
        WebView webView = webViewWeakRef.get();
        if (webView != null) {
            optimizeWebView(webView);
        }
    }
    
    /**
     * Get optimization status
     */
    public boolean isOptimized() {
        return isOptimized;
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopPerformanceMonitoring();
        metricsExecutor.shutdown();
        performanceListeners.clear();
        
        Log.i(TAG, "WebView Performance Optimizer cleaned up");
    }
    
    /**
     * Get detailed performance report for debugging
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== WebView Performance Report ===\n");
        report.append(String.format("Current Frame Rate: %.2f FPS\n", currentFrameRate.get()));
        report.append(String.format("Target Frame Rate: %d FPS\n", targetFrameRate));
        report.append(String.format("Memory Usage: %dMB / %dMB (%.1f%%)\n", 
                memoryOptimizer.getUsedMemoryMB(), 
                memoryOptimizer.getMaxMemoryMB(), 
                memoryOptimizer.getMemoryUsageRatio() * 100));
        report.append(String.format("Battery Level: %.1f%%\n", batteryOptimizer.getBatteryLevel() * 100));
        report.append(String.format("Network Quality: %d/4\n", networkOptimizer.getNetworkQuality()));
        report.append(String.format("Hardware Acceleration: %s\n", enableHardwareAcceleration ? "Enabled" : "Disabled"));
        report.append(String.format("WebGL: %s\n", enableWebGL ? "Enabled" : "Disabled"));
        report.append(String.format("Optimized: %s\n", isOptimized ? "Yes" : "No"));
        
        report.append("\n=== Device Capabilities ===\n");
        report.append(String.format("CPU Cores: %d\n", deviceCapabilities.cpuCores));
        report.append(String.format("Total Memory: %dMB\n", deviceCapabilities.totalMemoryMB));
        report.append(String.format("GPU Tier: %d\n", deviceCapabilities.gpuTier));
        report.append(String.format("Low-end Device: %s\n", deviceCapabilities.isLowEndDevice ? "Yes" : "No"));
        report.append(String.format("Architecture: %s\n", deviceCapabilities.architecture));
        report.append(String.format("API Level: %d\n", deviceCapabilities.apiLevel));
        
        if (!metricsHistory.isEmpty()) {
            report.append("\n=== Recent Metrics (last 5) ===\n");
            int startIndex = Math.max(0, metricsHistory.size() - 5);
            for (int i = startIndex; i < metricsHistory.size(); i++) {
                PerformanceMetric metric = metricsHistory.get(i);
                report.append(String.format("Time: %d, FPS: %.1f, Memory: %dMB, CPU: %.1f%%\n",
                        metric.timestamp, metric.frameRate, metric.memoryUsedMB, metric.cpuUsage));
            }
        }
        
        return report.toString();
    }
}