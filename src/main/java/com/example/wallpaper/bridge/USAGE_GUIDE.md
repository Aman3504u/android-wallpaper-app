# JavaScript Gesture Bridge Usage Guide

This guide provides comprehensive instructions for integrating the JavaScript Gesture Bridge in your Android WebView application.

## Overview

The JavaScript Gesture Bridge enables seamless communication between native Android code and WebView JavaScript for gesture event handling. It provides:

- **WebView JavaScript interface** for gesture events
- **JSON serialization/deserialization** for gesture data
- **Event batching** for performance optimization
- **Gesture state synchronization** between native and web
- **Robust error handling** for JS bridge failures

## Quick Start

### 1. Initialize the Bridge in Android

```java
// In your Activity or Fragment
public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private JavascriptGestureBridge gestureBridge;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize WebView
        webView = findViewById(R.id.webview);
        
        // Enable JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // Create and initialize gesture bridge
        gestureBridge = new JavascriptGestureBridge(webView);
        gestureBridge.setGestureEventListener(new GestureEventListener() {
            @Override
            public void onGestureEvent(GestureEvent event) {
                handleGestureEvent(event);
            }
            
            @Override
            public void onBatchEvents(List<GestureEvent> events) {
                handleBatchEvents(events);
            }
            
            @Override
            public void onError(String error, String context) {
                Log.e(TAG, "Gesture error in " + context + ": " + error);
            }
            
            @Override
            public void onStateChanged(String key, Object oldValue, Object newValue) {
                Log.d(TAG, "State changed: " + key + ": " + oldValue + " -> " + newValue);
            }
        });
        
        // Initialize the bridge
        gestureBridge.initialize();
        
        // Load your HTML content
        webView.loadUrl("file:///android_asset/gesture_test.html");
    }
    
    private void handleGestureEvent(GestureEvent event) {
        switch (event.type) {
            case "tap":
                handleTap(event);
                break;
            case "swipe":
                handleSwipe(event);
                break;
            case "pinch":
                handlePinch(event);
                break;
            default:
                Log.d(TAG, "Received gesture: " + event.type);
        }
    }
}
```

### 2. Add JavaScript Bridge Code

Include the JavaScript bridge templates in your HTML:

```html
<!DOCTYPE html>
<html>
<head>
    <script src="gesture-bridge.js"></script>
</head>
<body>
    <!-- Your content -->
</body>
</html>
```

Or copy the JavaScript templates directly into your HTML file.

## Gesture Event Types

The bridge supports the following gesture types:

- **TAP**: Single tap gesture
- **DOUBLE_TAP**: Double tap gesture  
- **LONG_PRESS**: Long press gesture
- **SWIPE**: Swipe gesture with direction
- **PINCH**: Pinch to zoom gesture
- **ROTATION**: Rotation gesture
- **DRAG**: Drag gesture
- **SCALE**: Scale gesture

## JavaScript API Reference

### Initialization

```javascript
// Initialize the bridge
window.JavaScriptGestureBridge.initialize();

// Check if bridge is available
if (window.JavaScriptGestureBridge) {
    console.log('Gesture bridge ready');
}
```

### Sending Gesture Events

```javascript
// Send individual gesture event
const event = {
    type: 'tap',
    x: 150,
    y: 200,
    timestamp: Date.now()
};

window.JavaScriptGestureBridge.sendGestureEvent(event);

// Update gesture state
window.JavaScriptGestureBridge.updateGestureState('currentZoom', 1.5);

// Sync state with Android
window.JavaScriptGestureBridge.syncGestureState();
```

### Event Listeners

```javascript
// Listen for gesture events
window.JavaScriptGestureBridge.addEventListener('gesture', (event) => {
    console.log('Gesture received:', event.type);
});

// Listen for batch events
window.JavaScriptGestureBridge.addEventListener('batch', (events) => {
    console.log('Batch received:', events.length, 'events');
});

// Listen for state changes
window.JavaScriptGestureBridge.addEventListener('stateChanged', (change) => {
    console.log('State changed:', change.key, change.value);
});

// Listen for errors
window.JavaScriptGestureBridge.addEventListener('error', (error) => {
    console.error('Bridge error:', error.error);
});
```

### Configuration

```javascript
// Enable/disable bridge
window.JavaScriptGestureBridge.setEnabled(true);

// Get queue size
const queueSize = window.JavaScriptGestureBridge.getQueueSize();

// Clear event queue
window.JavaScriptGestureBridge.clearQueue();

// Force batch processing
window.JavaScriptGestureBridge.forceBatchProcess();

// Get current gesture state
const state = window.JavaScriptGestureBridge.getGestureState();
```

## Android API Reference

### Creating the Bridge

```java
JavascriptGestureBridge bridge = new JavascriptGestureBridge(webView);
```

### Setting Event Listener

```java
bridge.setGestureEventListener(new JavascriptGestureBridge.GestureEventListener() {
    @Override
    public void onGestureEvent(GestureEvent event) {
        // Handle individual gesture event
    }
    
    @Override
    public void onBatchEvents(List<GestureEvent> events) {
        // Handle batch of gesture events
    }
    
    @Override
    public void onError(String error, String context) {
        // Handle errors
    }
    
    @Override
    public void onStateChanged(String key, Object oldValue, Object newValue) {
        // Handle state changes
    }
});
```

### Bridge Configuration

```java
// Initialize the bridge
bridge.initialize();

// Enable/disable bridge
bridge.setEnabled(true);

// Get gesture state
JavascriptGestureBridge.GestureState state = bridge.getGestureState();

// Clear pending events
bridge.clearEvents();

// Get bridge configuration
String config = bridge.getBridgeConfiguration();
```

### Gesture Event Properties

```java
// Create a gesture event
JavascriptGestureBridge.GestureEvent event = new JavascriptGestureBridge.GestureEvent("tap");
event.x = 150.0f;
event.y = 200.0f;
event.timestamp = System.currentTimeMillis();

// Convert to JSON
try {
    JSONObject json = event.toJSON();
} catch (JSONException e) {
    Log.e(TAG, "Failed to convert to JSON", e);
}
```

## Performance Optimization

### Event Batching

The bridge automatically batches events for better performance:

- **Batch Size**: 10 events per batch
- **Batch Timeout**: 50ms
- **Max Batch Size**: 50 events

Events are sent to Android when:
1. Batch size limit is reached
2. Batch timeout expires
3. Manual batch processing is triggered

### Configuration Tuning

```java
// Access internal configuration (modify bridge source if needed)
int batchSize = bridge.getBatchSize();
long batchTimeout = bridge.getBatchTimeout();
```

## Error Handling

### JavaScript Error Handling

```javascript
try {
    window.JavaScriptGestureBridge.sendGestureEvent(event);
} catch (error) {
    console.error('Failed to send gesture:', error);
    // Fallback handling
}
```

### Android Error Handling

```java
@Override
public void onError(String error, String context) {
    // Log the error
    Log.e(TAG, "Gesture bridge error in " + context + ": " + error);
    
    // Implement fallback logic
    if (error.contains("bridge")) {
        // Try to reinitialize bridge
        bridge.initialize();
    }
}
```

## Testing and Debugging

### Enable Debug Logging

```java
// Enable verbose logging
Log.d(TAG, bridge.getBridgeConfiguration());
```

### Test HTML Template

Use the provided HTML template for testing:

```java
// Load test HTML
String testHtml = loadTestHtml();
webView.loadData(testHtml, "text/html", "UTF-8");
```

### Monitor Event Flow

```javascript
// Add comprehensive event logging
window.JavaScriptGestureBridge.addEventListener('gesture', (event) => {
    console.log('Received gesture:', JSON.stringify(event));
});

window.JavaScriptGestureBridge.addEventListener('batch', (events) => {
    console.log('Batch size:', events.length);
});

window.JavaScriptGestureBridge.addEventListener('stateChanged', (change) => {
    console.log('State sync:', change.key, change.value);
});
```

## Best Practices

### 1. Event Handling

- Process gesture events asynchronously
- Avoid blocking the main thread
- Use event batching for better performance
- Implement proper error handling

### 2. State Management

- Keep state changes minimal
- Use descriptive state keys
- Handle state synchronization properly
- Implement state validation

### 3. Memory Management

- Clean up event listeners when no longer needed
- Clear event queue when appropriate
- Implement proper cleanup in onDestroy()

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (gestureBridge != null) {
        gestureBridge.cleanup();
    }
}
```

### 4. Security

- Validate gesture event data
- Sanitize state updates
- Implement proper permission checks
- Use secure WebView settings

```java
// Secure WebView settings
WebSettings webSettings = webView.getSettings();
webSettings.setJavaScriptEnabled(true);
webSettings.setAllowFileAccess(false);
webSettings.setAllowContentAccess(false);
```

## Troubleshooting

### Bridge Not Available

**Problem**: JavaScript bridge not detected
**Solution**: 
1. Ensure WebView JavaScript is enabled
2. Check bridge initialization
3. Verify Android version compatibility

### Event Loss

**Problem**: Gesture events not reaching Android
**Solution**:
1. Check event batching configuration
2. Monitor event queue size
3. Verify WebView settings

### Performance Issues

**Problem**: Slow gesture processing
**Solution**:
1. Adjust batch size and timeout
2. Optimize event processing logic
3. Use background threads for heavy processing

### Memory Leaks

**Problem**: Memory usage increases over time
**Solution**:
1. Implement proper cleanup
2. Clear event listeners
3. Monitor event queue size

## Migration Guide

### From Custom Bridge

1. Replace custom bridge with `JavascriptGestureBridge`
2. Update JavaScript event handlers
3. Migrate existing gesture data format
4. Update state management logic

### Version Updates

- **1.0.0**: Initial release with basic gesture support
- **1.1.0**: Added event batching and state synchronization
- **1.2.0**: Enhanced error handling and performance optimization

## Support

For issues and feature requests:
- Check the troubleshooting section
- Review the test examples
- Ensure proper integration steps
- Validate WebView configuration