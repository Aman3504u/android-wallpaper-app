# ResourceManager Usage Guide

## Overview

The ResourceManager is a comprehensive lifecycle management and resource optimization system designed specifically for Android wallpaper applications. It provides automatic memory management, battery optimization, and resource pooling to ensure optimal performance and battery life.

## Key Features

### 1. WallpaperService Lifecycle Integration
- Automatic initialization when wallpaper service is created
- Proper cleanup when service is destroyed
- Visibility change handling for optimal resource usage

### 2. WebView Lifecycle Management
- WebView pooling for memory efficiency
- Automatic pause/resume/destroy handling
- Cache management and cleanup
- Memory leak prevention

### 3. Memory Management
- Real-time memory usage monitoring
- Automatic cleanup when memory is low
- WeakReference-based component tracking
- Aggressive cleanup mode for critical situations

### 4. Battery Optimization
- Doze mode detection and handling
- Automatic battery optimization features
- User-requested battery exemption handling
- Network-aware operation scheduling

### 5. Background Processing
- Optimized thread pool management
- Scheduled task execution
- Background operation coordination
- Network connectivity awareness

### 6. Resource Pooling
- Gesture event pooling for memory efficiency
- WebView pooling to reduce creation overhead
- Automatic pool management and cleanup

### 7. Comprehensive Monitoring
- Real-time resource statistics
- Memory usage tracking
- Network connectivity monitoring
- Callback-based notifications

## Basic Usage

### 1. Initialize ResourceManager

```java
// In your WallpaperService
public class MyWallpaperService extends WallpaperService {
    private ResourceManager mResourceManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mResourceManager = ResourceManager.getInstance(this);
        mResourceManager.onWallpaperServiceCreated();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mResourceManager != null) {
            mResourceManager.onWallpaperServiceDestroyed();
        }
    }
    
    @Override
    public Engine onCreateEngine() {
        return new MyWallpaperEngine();
    }
    
    class MyWallpaperEngine extends Engine {
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (mResourceManager != null) {
                mResourceManager.onWallpaperVisibilityChanged(visible);
            }
        }
    }
}
```

### 2. WebView Management

```java
// Acquire WebView from pool
WebView webView = mResourceManager.acquireWebView();

// Configure and use WebView
webView.loadUrl("https://example.com");

// When done with WebView
mResourceManager.releaseWebView(webView);

// Pause/Resume WebView based on visibility
if (isVisible) {
    mResourceManager.resumeWebView(webView);
} else {
    mResourceManager.pauseWebView(webView);
}
```

### 3. Gesture Event Pooling

```java
// Acquire gesture event from pool
ResourceManager.GestureEvent event = mResourceManager.acquireGestureEvent();

// Fill event data
event.x = touchX;
event.y = touchY;
event.timestamp = System.currentTimeMillis();
event.type = GestureEvent.TYPE_TAP;

// Process event
processGestureEvent(event);

// Return event to pool
mResourceManager.releaseGestureEvent(event);
```

### 4. Component Tracking

```java
// Register component for lifecycle tracking
mResourceManager.trackComponent("my_component", myObject);

// Update usage time when component is used
mResourceManager.markComponentUsed("my_component");

// Unregister when no longer needed
mResourceManager.untrackComponent("my_component");
```

### 5. Background Processing

```java
// Execute task in background thread
mResourceManager.executeInBackground(() -> {
    // Your background work here
    processData();
});

// Schedule task with delay
mResourceManager.scheduleInBackground(() -> {
    // Your delayed work here
    updateWallpaper();
}, 5, TimeUnit.MINUTES);
```

### 6. Cache Management

```java
// Clear caches manually
mResourceManager.clearCaches();

// Force aggressive cleanup (e.g., when receiving low memory warning)
mResourceManager.performAggressiveCleanup();
```

### 7. Monitoring and Callbacks

```java
// Set up monitoring callback
mResourceManager.setMonitoringCallback(new ResourceManager.ResourceMonitoringCallback() {
    @Override
    public void onLowMemoryModeChanged(boolean isLowMemoryMode) {
        if (isLowMemoryMode) {
            // Reduce resource usage
            reduceQualitySettings();
        }
    }
    
    @Override
    public void onDozeModeChanged(boolean isInDozeMode) {
        if (isInDozeMode) {
            // Pause non-essential operations
            pauseBackgroundTasks();
        } else {
            // Resume operations
            resumeBackgroundTasks();
        }
    }
    
    @Override
    public void onNetworkStateChanged(boolean isConnected) {
        if (isConnected) {
            // Sync data, download updates
            scheduleNetworkOperations();
        }
    }
    
    @Override
    public void onStatsUpdated(ResourceManager.MemoryStats stats) {
        // Update UI with resource statistics
        updateStatsDisplay(stats);
    }
});

// Get current memory statistics
ResourceManager.MemoryStats stats = mResourceManager.getMemoryStats();
Log.d(TAG, "Memory: " + stats.usedMemory + " / " + stats.totalMemory);
Log.d(TAG, "Active WebViews: " + stats.activeWebViewCount);
Log.d(TAG, "Low Memory Mode: " + stats.isLowMemoryMode);
```

### 8. Battery Optimization

```java
// Check if battery optimizations should be ignored
if (!mResourceManager.isIgnoringBatteryOptimizations()) {
    // Request exemption from user (should be called from user action)
    mResourceManager.requestBatteryOptimizationExemption();
}

// Enable battery optimization features
mResourceManager.enableBatteryOptimization();

// Check if device is in Doze mode
if (mResourceManager.isInDozeMode()) {
    // Adjust behavior for Doze mode
    reduceUpdateFrequency();
}
```

## Best Practices

### 1. Always Use Resource Pooling
- Use `acquireWebView()` and `releaseWebView()` instead of creating new WebViews
- Use `acquireGestureEvent()` and `releaseGestureEvent()` for gesture processing
- This significantly reduces memory allocation overhead

### 2. Proper Lifecycle Management
- Always call lifecycle methods in your WallpaperService
- Handle visibility changes appropriately
- Clean up resources when no longer needed

### 3. Memory Management
- Monitor memory usage through callbacks
- Implement graceful degradation in low memory situations
- Use component tracking for complex objects

### 4. Battery Optimization
- Respect Doze mode limitations
- Request battery exemption only when necessary
- Implement user-facing explanations for why exemption is needed

### 5. Background Processing
- Use background executors for network operations
- Schedule intensive tasks appropriately
- Consider network connectivity in background work

### 6. Error Handling
- Always wrap resource operations in try-catch blocks
- Handle cases where resources might be unavailable
- Log errors appropriately for debugging

## Advanced Configuration

### Custom Pool Sizes
```java
// The default pool sizes can be adjusted in ResourceManager constructor
// WebView pool: 5 WebViews
// Gesture event pool: 100 events
// Consider adjusting based on your app's needs
```

### Monitoring Intervals
```java
// Default monitoring intervals (can be customized in constructor)
// Memory monitoring: 5 seconds
// Periodic cleanup: 30 seconds
// Battery optimization check: 1 minute
```

### Memory Thresholds
```java
// Default memory thresholds (can be customized)
// Low memory threshold: 64MB
// Critical memory threshold: 32MB
// These trigger automatic cleanup and optimization
```

## Integration Examples

### With GestureHandler
```java
public class OptimizedGestureHandler implements GestureHandler {
    private final ResourceManager mResourceManager;
    
    public OptimizedGestureHandler(Context context) {
        mResourceManager = ResourceManager.getInstance(context);
    }
    
    @Override
    public void onTouchEvent(MotionEvent event) {
        // Use pooled gesture event for memory efficiency
        ResourceManager.GestureEvent gestureEvent = mResourceManager.acquireGestureEvent();
        
        gestureEvent.x = event.getX();
        gestureEvent.y = event.getY();
        gestureEvent.timestamp = event.getEventTime();
        gestureEvent.type = getGestureType(event);
        
        processGesture(gestureEvent);
        
        // Return to pool immediately
        mResourceManager.releaseGestureEvent(gestureEvent);
    }
}
```

### With WebViewWallpaperService
```java
public class OptimizedWebViewService extends WebViewWallpaperService {
    private ResourceManager mResourceManager;
    private WebView mCurrentWebView;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mResourceManager = ResourceManager.getInstance(this);
        mResourceManager.onWallpaperServiceCreated();
    }
    
    @Override
    public Engine onCreateEngine() {
        return new OptimizedWebViewEngine();
    }
    
    class OptimizedWebViewEngine extends Engine {
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            mCurrentWebView = mResourceManager.acquireWebView();
            setTouchEventsEnabled(true);
        }
        
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            mResourceManager.onWallpaperVisibilityChanged(visible);
            
            if (visible) {
                mResourceManager.resumeWebView(mCurrentWebView);
            } else {
                mResourceManager.pauseWebView(mCurrentWebView);
            }
        }
        
        @Override
        public void onDestroy() {
            if (mCurrentWebView != null) {
                mResourceManager.releaseWebView(mCurrentWebView);
                mCurrentWebView = null;
            }
            super.onDestroy();
        }
    }
    
    @Override
    public void onDestroy() {
        mResourceManager.onWallpaperServiceDestroyed();
        super.onDestroy();
    }
}
```

## Troubleshooting

### Common Issues

1. **WebView Pool Exhaustion**
   - Increase pool size if needed
   - Ensure WebViews are properly released
   - Check for memory leaks in WebView usage

2. **High Memory Usage**
   - Monitor memory statistics regularly
   - Implement aggressive cleanup in low memory callbacks
   - Review component tracking for potential leaks

3. **Battery Optimization Issues**
   - Check if app is properly handling Doze mode
   - Request battery exemption only when necessary
   - Provide user education about battery usage

4. **Background Task Failures**
   - Handle network connectivity appropriately
   - Use appropriate exception handling
   - Consider task retry logic

### Debug Logging

Enable debug logging to monitor ResourceManager behavior:
```java
// Enable detailed logging
Log.setLogLevel(Log.DEBUG);

// Monitor key operations
// - WebView lifecycle events
// - Memory usage changes
// - Pool operations
// - Battery optimization state
```

### Performance Monitoring

Use the monitoring callbacks to track:
- Memory usage patterns
- WebView lifecycle efficiency
- Battery optimization effectiveness
- Network operation success rates

## Version History

- **v2.0**: Complete rewrite with comprehensive lifecycle management
- **v1.0**: Initial resource management implementation

## License

This ResourceManager implementation is part of the Android Wallpaper project.
