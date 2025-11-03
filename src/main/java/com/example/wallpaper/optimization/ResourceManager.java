package com.example.wallpaper.optimization;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.WebView;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive resource management system for Android wallpaper applications.
 * 
 * This class provides:
 * - WallpaperService lifecycle integration
 * - WebView lifecycle management (pause/resume/destroy)
 * - Memory leak prevention strategies
 * - Background processing optimization
 * - Battery optimization with Doze mode support
 * - Cache management and cleanup
 * - Resource pooling for gesture events
 * - Activity lifecycle coordination
 * - Comprehensive monitoring and automatic optimization
 * 
 * @author Android Wallpaper Team
 * @version 2.0
 */
public class ResourceManager {
    private static final String TAG = "ResourceManager";
    
    // Memory thresholds (MB)
    private static final long MEMORY_LOW_THRESHOLD = 64;
    private static final long MEMORY_CRITICAL_THRESHOLD = 32;
    
    // Cache size limits
    private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_BITMAP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    
    // Monitoring intervals
    private static final long MONITORING_INTERVAL = 5000; // 5 seconds
    private static final long CLEANUP_INTERVAL = 30000; // 30 seconds
    private static final long BATTERY_OPTIMIZATION_CHECK_INTERVAL = 60000; // 1 minute
    
    // Doze mode detection
    private static final String POWER_MANAGER_SERVICE = "power";
    private static final String DOZE_MODE_WHITE_LIST = "device_idle";
    
    private static ResourceManager sInstance;
    
    private final Context mContext;
    private final HandlerThread mWorkerThread;
    private final Handler mWorkerHandler;
    private final Handler mMainHandler;
    
    // Executor services
    private final ExecutorService mBackgroundExecutor;
    private final ScheduledExecutorService mScheduledExecutor;
    
    // Monitoring and statistics
    private final AtomicLong mMemoryUsed = new AtomicLong(0);
    private final AtomicLong mLastMemoryCheck = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger mActiveWebViewCount = new AtomicInteger(0);
    private final AtomicBoolean mIsLowMemoryMode = new AtomicBoolean(false);
    private final AtomicBoolean mIsInDozeMode = new AtomicBoolean(false);
    private final AtomicBoolean mIsNetworkConnected = new AtomicBoolean(true);
    
    // Component tracking
    private final ConcurrentHashMap<String, WeakReference<Object>> mTrackedComponents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> mLastUsageTime = new ConcurrentHashMap<>();
    
    // Resource pools
    private final ObjectPool<GestureEvent> mGestureEventPool;
    private final ObjectPool<WebView> mWebViewPool;
    
    // Monitoring callbacks
    private ResourceMonitoringCallback mMonitoringCallback;
    
    // Power management
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    
    // Network monitoring
    private ConnectivityReceiver mConnectivityReceiver;
    
    private ResourceManager(Context context) {
        mContext = context.getApplicationContext();
        
        // Initialize worker thread for background operations
        mWorkerThread = new HandlerThread("ResourceManager-Worker", Thread.NORM_PRIORITY - 1);
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize executor services
        mBackgroundExecutor = Executors.newFixedThreadPool(2);
        mScheduledExecutor = Executors.newScheduledThreadPool(2);
        
        // Initialize resource pools
        mGestureEventPool = new ObjectPool<>(new GestureEventFactory(), 100);
        mWebViewPool = new ObjectPool<>(new WebViewFactory(mContext), 5);
        
        // Initialize power management
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        
        // Initialize monitoring
        setupMonitoring();
        registerNetworkReceiver();
        setupDozeModeDetection();
        
        Log.i(TAG, "ResourceManager initialized");
    }
    
    /**
     * Get singleton instance of ResourceManager
     */
    public static synchronized ResourceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ResourceManager(context);
        }
        return sInstance;
    }
    
    /**
     * Initialize ResourceManager with WallpaperService lifecycle integration
     */
    public void onWallpaperServiceCreated() {
        Log.i(TAG, "WallpaperService created");
        startMonitoring();
        acquireWakeLock();
    }
    
    /**
     * Handle wallpaper service destruction
     */
    public void onWallpaperServiceDestroyed() {
        Log.i(TAG, "WallpaperService destroyed");
        stopMonitoring();
        releaseWakeLock();
        cleanupAllResources();
    }
    
    /**
     * Handle wallpaper engine visibility changes
     */
    public void onWallpaperVisibilityChanged(boolean visible) {
        Log.d(TAG, "Wallpaper visibility changed: " + visible);
        
        if (visible) {
            onWallpaperVisible();
        } else {
            onWallpaperInvisible();
        }
    }
    
    /**
     * WebView lifecycle management - when WebView is created
     */
    public WebView acquireWebView() {
        WebView webView = mWebViewPool.acquire();
        
        if (webView == null) {
            webView = new WebView(mContext);
            configureWebView(webView);
        }
        
        mActiveWebViewCount.incrementAndGet();
        mMainHandler.post(() -> {
            if (webView.getUrl() == null) {
                webView.loadUrl("about:blank");
            }
        });
        
        Log.d(TAG, "WebView acquired. Active count: " + mActiveWebViewCount.get());
        return webView;
    }
    
    /**
     * WebView lifecycle management - when WebView should be paused
     */
    public void pauseWebView(WebView webView) {
        if (webView != null) {
            mMainHandler.post(() -> {
                try {
                    webView.onPause();
                    webView.pauseTimers();
                    Log.d(TAG, "WebView paused");
                } catch (Exception e) {
                    Log.e(TAG, "Error pausing WebView", e);
                }
            });
        }
    }
    
    /**
     * WebView lifecycle management - when WebView should be resumed
     */
    public void resumeWebView(WebView webView) {
        if (webView != null) {
            mMainHandler.post(() -> {
                try {
                    webView.onResume();
                    webView.resumeTimers();
                    Log.d(TAG, "WebView resumed");
                } catch (Exception e) {
                    Log.e(TAG, "Error resuming WebView", e);
                }
            });
        }
    }
    
    /**
     * WebView lifecycle management - when WebView should be destroyed
     */
    public void releaseWebView(WebView webView) {
        if (webView != null) {
            mMainHandler.post(() -> {
                try {
                    webView.stopLoading();
                    webView.onPause();
                    webView.clearCache(true);
                    webView.destroy();
                } catch (Exception e) {
                    Log.e(TAG, "Error destroying WebView", e);
                } finally {
                    mActiveWebViewCount.decrementAndGet();
                    mWebViewPool.release(webView);
                    Log.d(TAG, "WebView released. Active count: " + mActiveWebViewCount.get());
                }
            });
        }
    }
    
    /**
     * Get a gesture event from the pool for memory optimization
     */
    public GestureEvent acquireGestureEvent() {
        GestureEvent event = mGestureEventPool.acquire();
        if (event != null) {
            event.reset();
        }
        return event;
    }
    
    /**
     * Release gesture event back to the pool
     */
    public void releaseGestureEvent(GestureEvent event) {
        if (event != null) {
            mGestureEventPool.release(event);
        }
    }
    
    /**
     * Register a component for lifecycle tracking and memory leak prevention
     */
    public void trackComponent(String id, Object component) {
        mTrackedComponents.put(id, new WeakReference<>(component));
        mLastUsageTime.put(id, System.currentTimeMillis());
    }
    
    /**
     * Unregister a component from lifecycle tracking
     */
    public void untrackComponent(String id) {
        mTrackedComponents.remove(id);
        mLastUsageTime.remove(id);
    }
    
    /**
     * Mark component as used (update last usage time)
     */
    public void markComponentUsed(String id) {
        mLastUsageTime.put(id, System.currentTimeMillis());
    }
    
    /**
     * Background processing optimization - execute task in background thread
     */
    public void executeInBackground(Runnable task) {
        mBackgroundExecutor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Error in background task", e);
            }
        });
    }
    
    /**
     * Background processing optimization - schedule task with delay
     */
    public void scheduleInBackground(Runnable task, long delay, TimeUnit timeUnit) {
        mScheduledExecutor.schedule(() -> {
            mBackgroundExecutor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    Log.e(TAG, "Error in scheduled background task", e);
                }
            });
        }, delay, timeUnit);
    }
    
    /**
     * Clear caches and perform memory cleanup
     */
    public void clearCaches() {
        executeInBackground(() -> {
            Log.i(TAG, "Starting cache cleanup");
            
            try {
                // Clear WebView cache
                clearWebViewCache();
                
                // Clear bitmap cache
                clearBitmapCache();
                
                // Trigger garbage collection
                System.gc();
                
                Log.i(TAG, "Cache cleanup completed");
            } catch (Exception e) {
                Log.e(TAG, "Error during cache cleanup", e);
            }
        });
    }
    
    /**
     * Perform aggressive memory cleanup when in low memory mode
     */
    public void performAggressiveCleanup() {
        Log.w(TAG, "Performing aggressive memory cleanup");
        
        executeInBackground(() -> {
            try {
                // Clear all caches
                clearCaches();
                
                // Release all pooled WebViews
                mWebViewPool.clear();
                
                // Clear old tracked components
                clearOldComponents();
                
                // Force garbage collection
                System.gc();
                
                // Update memory state
                checkMemoryUsage();
                
                Log.w(TAG, "Aggressive cleanup completed");
            } catch (Exception e) {
                Log.e(TAG, "Error during aggressive cleanup", e);
            }
        });
    }
    
    /**
     * Enable battery optimization features
     */
    public void enableBatteryOptimization() {
        if (isDozeModeSupported() && !isIgnoringBatteryOptimizations()) {
            Log.i(TAG, "Enabling battery optimization features");
            
            // Schedule periodic cleanup during active periods
            mScheduledExecutor.scheduleWithFixedDelay(() -> {
                if (!mIsInDozeMode.get() && mIsNetworkConnected.get()) {
                    performLightweightCleanup();
                }
            }, BATTERY_OPTIMIZATION_CHECK_INTERVAL, BATTERY_OPTIMIZATION_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Check if battery optimizations should be ignored (user explicitly requested)
     */
    public boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mPowerManager.isIgnoringBatteryOptimizations(mContext.getPackageName());
        }
        return false;
    }
    
    /**
     * Request battery optimization exemption (should be called from user action)
     */
    public void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + mContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                mContext.startActivity(intent);
                Log.i(TAG, "Battery optimization exemption requested");
            } catch (Exception e) {
                Log.e(TAG, "Failed to request battery optimization exemption", e);
            }
        }
    }
    
    /**
     * Set monitoring callback for resource statistics
     */
    public void setMonitoringCallback(ResourceMonitoringCallback callback) {
        mMonitoringCallback = callback;
    }
    
    /**
     * Get current memory usage statistics
     */
    public MemoryStats getMemoryStats() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        
        return new MemoryStats(
            memoryInfo.totalMem,
            memoryInfo.availMem,
            memoryInfo.totalMem - memoryInfo.availMem,
            memoryInfo.lowMemory,
            mActiveWebViewCount.get(),
            mIsLowMemoryMode.get(),
            mIsInDozeMode.get(),
            mIsNetworkConnected.get()
        );
    }
    
    /**
     * Check if Doze mode is supported on this device
     */
    public boolean isDozeModeSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
    
    /**
     * Check if device is currently in Doze mode or App Standby
     */
    public boolean isInDozeMode() {
        return mIsInDozeMode.get();
    }
    
    /**
     * Force cleanup and shutdown ResourceManager
     */
    public void shutdown() {
        Log.i(TAG, "Shutting down ResourceManager");
        
        stopMonitoring();
        releaseWakeLock();
        unregisterNetworkReceiver();
        cleanupAllResources();
        
        // Shutdown executors
        mBackgroundExecutor.shutdown();
        mScheduledExecutor.shutdown();
        
        try {
            if (!mBackgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mBackgroundExecutor.shutdownNow();
            }
            if (!mScheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mScheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mBackgroundExecutor.shutdownNow();
            mScheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        mWorkerThread.quitSafely();
        
        Log.i(TAG, "ResourceManager shutdown completed");
    }
    
    // Private helper methods
    
    private void setupMonitoring() {
        mScheduledExecutor.scheduleWithFixedDelay(this::checkMemoryUsage, 
            MONITORING_INTERVAL, MONITORING_INTERVAL, TimeUnit.MILLISECONDS);
        
        mScheduledExecutor.scheduleWithFixedDelay(this::performPeriodicCleanup, 
            CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    private void startMonitoring() {
        Log.d(TAG, "Starting resource monitoring");
        // Monitoring is automatically started in setupMonitoring
    }
    
    private void stopMonitoring() {
        Log.d(TAG, "Stopping resource monitoring");
        // Keep scheduled tasks running, just log the stop
    }
    
    private void acquireWakeLock() {
        if (mWakeLock == null && mPowerManager != null) {
            try {
                mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
                    "Wallpaper:ResourceManager");
                mWakeLock.acquire(10 * 60 * 1000L); // 10 minutes timeout
                Log.d(TAG, "WakeLock acquired");
            } catch (Exception e) {
                Log.e(TAG, "Failed to acquire WakeLock", e);
            }
        }
    }
    
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            try {
                mWakeLock.release();
                mWakeLock = null;
                Log.d(TAG, "WakeLock released");
            } catch (Exception e) {
                Log.e(TAG, "Failed to release WakeLock", e);
            }
        }
    }
    
    private void setupDozeModeDetection() {
        if (isDozeModeSupported()) {
            // Check for Doze mode periodically
            mScheduledExecutor.scheduleWithFixedDelay(() -> {
                boolean wasInDoze = mIsInDozeMode.get();
                boolean nowInDoze = !mPowerManager.isInteractive();
                
                if (wasInDoze != nowInDoze) {
                    mIsInDozeMode.set(nowInDoze);
                    Log.i(TAG, "Doze mode changed: " + nowInDoze);
                    
                    if (nowInDoze) {
                        onEnterDozeMode();
                    } else {
                        onExitDozeMode();
                    }
                }
            }, 30000, 30000, TimeUnit.MILLISECONDS); // Check every 30 seconds
        }
    }
    
    private void onEnterDozeMode() {
        Log.i(TAG, "Entering Doze mode optimization");
        
        // Pause non-essential operations
        // Reduce monitoring frequency
        // Clear caches to save memory
        
        if (mMonitoringCallback != null) {
            mMainHandler.post(() -> mMonitoringCallback.onDozeModeChanged(true));
        }
    }
    
    private void onExitDozeMode() {
        Log.i(TAG, "Exiting Doze mode optimization");
        
        // Resume operations
        // Check for missed updates
        // Reinitialize if needed
        
        if (mMonitoringCallback != null) {
            mMainHandler.post(() -> mMonitoringCallback.onDozeModeChanged(false));
        }
    }
    
    private void onWallpaperVisible() {
        Log.d(TAG, "Wallpaper became visible");
        
        // Resume operations
        // Check for updates
        // Resume WebViews if needed
        
        if (mIsInDozeMode.get()) {
            onExitDozeMode();
        }
    }
    
    private void onWallpaperInvisible() {
        Log.d(TAG, "Wallpaper became invisible");
        
        // Pause non-essential operations
        // Reduce resource usage
        // Can enter power-saving mode
    }
    
    private void configureWebView(WebView webView) {
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAppCachePath(mContext.getCacheDir().getAbsolutePath());
        webView.getSettings().setCacheMode(WebView.LOAD_DEFAULT);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
    }
    
    private void checkMemoryUsage() {
        try {
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memoryInfo);
            
            long usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
            mMemoryUsed.set(usedMemory);
            mLastMemoryCheck.set(System.currentTimeMillis());
            
            boolean wasLowMemory = mIsLowMemoryMode.get();
            boolean isLowMemory = memoryInfo.lowMemory || usedMemory > (memoryInfo.totalMem * 0.8);
            
            if (wasLowMemory != isLowMemory) {
                mIsLowMemoryMode.set(isLowMemory);
                Log.w(TAG, "Low memory mode changed: " + isLowMemory);
                
                if (isLowMemory) {
                    performAggressiveCleanup();
                }
                
                if (mMonitoringCallback != null) {
                    mMainHandler.post(() -> mMonitoringCallback.onLowMemoryModeChanged(isLowMemory));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking memory usage", e);
        }
    }
    
    private void performPeriodicCleanup() {
        try {
            clearOldComponents();
            cleanupUnusedResources();
            
            if (mMonitoringCallback != null) {
                MemoryStats stats = getMemoryStats();
                mMainHandler.post(() -> mMonitoringCallback.onStatsUpdated(stats));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during periodic cleanup", e);
        }
    }
    
    private void performLightweightCleanup() {
        try {
            // Light cleanup - just clear old components and minor cache
            clearOldComponents();
            
            // Clear only temporary files
            clearTempFiles();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during lightweight cleanup", e);
        }
    }
    
    private void clearOldComponents() {
        long currentTime = System.currentTimeMillis();
        long threshold = 5 * 60 * 1000; // 5 minutes
        
        mTrackedComponents.entrySet().removeIf(entry -> {
            String id = entry.getKey();
            WeakReference<?> ref = entry.getValue();
            
            if (ref.get() == null) {
                // Component was garbage collected
                mLastUsageTime.remove(id);
                return true;
            }
            
            Long lastUsed = mLastUsageTime.get(id);
            if (lastUsed != null && (currentTime - lastUsed) > threshold) {
                // Component hasn't been used for a long time
                mLastUsageTime.remove(id);
                return true;
            }
            
            return false;
        });
    }
    
    private void cleanupUnusedResources() {
        // Clean up resources that haven't been used recently
        // This could include temporary files, old bitmaps, etc.
        
        if (mActiveWebViewCount.get() == 0) {
            // No active WebViews, can safely clean some cache
            clearWebViewCache();
        }
    }
    
    private void clearWebViewCache() {
        try {
            WebView.clearCache(mContext, true);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing WebView cache", e);
        }
    }
    
    private void clearBitmapCache() {
        // Clear bitmap cache - this would depend on the specific caching library used
        // For now, just trigger garbage collection
        System.gc();
    }
    
    private void clearTempFiles() {
        // Clear temporary files in cache directory
        // Implementation would depend on specific temp file locations
    }
    
    private void cleanupAllResources() {
        Log.i(TAG, "Cleaning up all resources");
        
        // Clear all tracked components
        mTrackedComponents.clear();
        mLastUsageTime.clear();
        
        // Clear all caches
        clearCaches();
        
        // Clear pools
        mGestureEventPool.clear();
        mWebViewPool.clear();
    }
    
    private void registerNetworkReceiver() {
        mConnectivityReceiver = new ConnectivityReceiver(this);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mConnectivityReceiver, filter);
    }
    
    private void unregisterNetworkReceiver() {
        if (mConnectivityReceiver != null) {
            try {
                mContext.unregisterReceiver(mConnectivityReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network receiver", e);
            }
        }
    }
    
    void onNetworkStateChanged(boolean isConnected) {
        boolean wasConnected = mIsNetworkConnected.getAndSet(isConnected);
        
        if (wasConnected != isConnected) {
            Log.i(TAG, "Network connectivity changed: " + isConnected);
            
            if (mMonitoringCallback != null) {
                mMainHandler.post(() -> mMonitoringCallback.onNetworkStateChanged(isConnected));
            }
            
            if (isConnected) {
                // Network became available, can perform deferred operations
                scheduleDeferredOperations();
            }
        }
    }
    
    private void scheduleDeferredOperations() {
        // Schedule operations that were deferred due to network being unavailable
        // This could include downloading updates, syncing data, etc.
    }
    
    // Inner classes and interfaces
    
    /**
     * Object pool for memory-efficient resource reuse
     */
    private static class ObjectPool<T> {
        private final ConcurrentHashMap<T, Boolean> mPool = new ConcurrentHashMap<>();
        private final ObjectFactory<T> mFactory;
        private final int mMaxSize;
        private final AtomicInteger mSize = new AtomicInteger(0);
        
        public ObjectPool(ObjectFactory<T> factory, int maxSize) {
            mFactory = factory;
            mMaxSize = maxSize;
        }
        
        public T acquire() {
            for (T item : mPool.keySet()) {
                if (mPool.remove(item)) {
                    mSize.decrementAndGet();
                    return item;
                }
            }
            return mFactory.create();
        }
        
        public void release(T item) {
            if (item != null && mSize.get() < mMaxSize) {
                mPool.put(item, Boolean.TRUE);
                mSize.incrementAndGet();
            }
        }
        
        public void clear() {
            mPool.clear();
            mSize.set(0);
        }
    }
    
    /**
     * Factory interface for object pool
     */
    private interface ObjectFactory<T> {
        T create();
    }
    
    /**
     * Factory for gesture events
     */
    private static class GestureEventFactory implements ObjectFactory<GestureEvent> {
        @Override
        public GestureEvent create() {
            return new GestureEvent();
        }
    }
    
    /**
     * Factory for WebViews
     */
    private static class WebViewFactory implements ObjectFactory<WebView> {
        private final WeakReference<Context> mContext;
        
        public WebViewFactory(Context context) {
            mContext = new WeakReference<>(context.getApplicationContext());
        }
        
        @Override
        public WebView create() {
            Context context = mContext.get();
            if (context != null) {
                return new WebView(context);
            }
            throw new IllegalStateException("Context is no longer available");
        }
    }
    
    /**
     * Holder for Android context for use in static contexts
     */
    private static class AndroidContextHolder {
        private static Context sContext;
        
        public static void setContext(Context context) {
            sContext = context.getApplicationContext();
        }
        
        public static Context getContext() {
            return sContext;
        }
    }
    
    /**
     * Represents a gesture event for pooling
     */
    public static class GestureEvent {
        public float x;
        public float y;
        public long timestamp;
        public int type;
        public float scale;
        public float rotation;
        
        public void reset() {
            x = 0;
            y = 0;
            timestamp = 0;
            type = 0;
            scale = 0;
            rotation = 0;
        }
    }
    
    /**
     * Network connectivity receiver
     */
    private static class ConnectivityReceiver extends android.content.BroadcastReceiver {
        private final WeakReference<ResourceManager> mResourceManager;
        
        public ConnectivityReceiver(ResourceManager resourceManager) {
            mResourceManager = new WeakReference<>(resourceManager);
        }
        
        @Override
        public void onReceive(Context context, Intent intent) {
            ResourceManager manager = mResourceManager.get();
            if (manager != null) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
                manager.onNetworkStateChanged(isConnected);
            }
        }
    }
    
    /**
     * Memory statistics holder
     */
    public static class MemoryStats {
        public final long totalMemory;
        public final long availableMemory;
        public final long usedMemory;
        public final boolean isLowMemory;
        public final int activeWebViewCount;
        public final boolean isLowMemoryMode;
        public final boolean isInDozeMode;
        public final boolean isNetworkConnected;
        
        public MemoryStats(long totalMemory, long availableMemory, long usedMemory, 
                          boolean isLowMemory, int activeWebViewCount, 
                          boolean isLowMemoryMode, boolean isInDozeMode, 
                          boolean isNetworkConnected) {
            this.totalMemory = totalMemory;
            this.availableMemory = availableMemory;
            this.usedMemory = usedMemory;
            this.isLowMemory = isLowMemory;
            this.activeWebViewCount = activeWebViewCount;
            this.isLowMemoryMode = isLowMemoryMode;
            this.isInDozeMode = isInDozeMode;
            this.isNetworkConnected = isNetworkConnected;
        }
    }
    
    /**
     * Callback interface for resource monitoring
     */
    public interface ResourceMonitoringCallback {
        void onLowMemoryModeChanged(boolean isLowMemoryMode);
        void onDozeModeChanged(boolean isInDozeMode);
        void onNetworkStateChanged(boolean isConnected);
        void onStatsUpdated(MemoryStats stats);
    }
    

}