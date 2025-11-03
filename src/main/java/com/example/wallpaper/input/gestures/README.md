# Android Wallpaper Gesture Processors

This directory contains reusable gesture event processors for the Android wallpaper application. Each processor is designed to be independent and can be used separately or in combination with others.

## Available Processors

### 1. TapGestureProcessor.java
Handles tap gesture detection including:
- Single tap detection
- Double tap detection with spatial threshold
- Long press detection with configurable timeout

**Usage Example:**
```java
TapGestureProcessor tapProcessor = new TapGestureProcessor();
tapProcessor.setOnTapListener(new TapGestureProcessor.OnTapListener() {
    @Override
    public void onSingleTap(float x, float y) {
        // Handle single tap
    }
    
    @Override
    public void onDoubleTap(float x, float y) {
        // Handle double tap
    }
    
    @Override
    public void onLongPress(float x, float y) {
        // Handle long press
    }
});

// In your onTouchEvent method:
public boolean onTouchEvent(MotionEvent event) {
    tapProcessor.onTouchEvent(event);
    return tapProcessor.checkLongPress(event); // For long press detection
}
```

### 2. PanGestureProcessor.java
Handles drag and pan gestures with:
- Multi-directional pan detection
- Velocity tracking (pixels per second)
- Pan direction analysis

**Usage Example:**
```java
PanGestureProcessor panProcessor = new PanGestureProcessor();
panProcessor.setOnPanListener(new PanGestureProcessor.OnPanListener() {
    @Override
    public void onPanStart(float startX, float startY) {
        // Pan gesture started
    }
    
    @Override
    public void onPan(float deltaX, float deltaY, float totalDeltaX, float totalDeltaY, float velocityX, float velocityY) {
        // Handle pan updates
        if (panProcessor.isPanningInDirection("LEFT")) {
            // Pan is going left
        }
    }
    
    @Override
    public void onPanEnd(float velocityX, float velocityY, float totalDeltaX, float totalDeltaY) {
        // Pan gesture ended
    }
    
    @Override
    public void onPanCancel() {
        // Pan gesture cancelled
    }
});

// In your onTouchEvent method:
public boolean onTouchEvent(MotionEvent event) {
    return panProcessor.onTouchEvent(event);
}
```

### 3. PinchGestureProcessor.java
Handles pinch-to-zoom gestures with:
- Multi-touch zoom detection
- Focus point tracking
- Continuous scale monitoring

**Usage Example:**
```java
PinchGestureProcessor pinchProcessor = new PinchGestureProcessor();
pinchProcessor.setOnPinchListener(new PinchGestureProcessor.OnPinchListener() {
    @Override
    public void onZoomStart(float focusX, float focusY) {
        // Zoom gesture started
    }
    
    @Override
    public void onZoom(float scale, float delta, float focusX, float focusY) {
        // Handle zoom updates
        if (pinchProcessor.isZoomingIn()) {
            // Zooming in
        } else if (pinchProcessor.isZoomingOut()) {
            // Zooming out
        }
    }
    
    @Override
    public void onZoomEnd(float finalScale) {
        // Zoom gesture ended
    }
    
    @Override
    public void onZoomCancel() {
        // Zoom gesture cancelled
    }
});

// In your onTouchEvent method:
public boolean onTouchEvent(MotionEvent event) {
    return pinchProcessor.onTouchEvent(event);
}
```

### 4. RotationGestureProcessor.java
Handles rotation gestures with:
- Multi-touch rotation detection
- Angle tracking in degrees and radians
- Clockwise/counter-clockwise direction detection

**Usage Example:**
```java
RotationGestureProcessor rotationProcessor = new RotationGestureProcessor();
rotationProcessor.setOnRotationListener(new RotationGestureProcessor.OnRotationListener() {
    @Override
    public void onRotationStart(float centerX, float centerY) {
        // Rotation gesture started
    }
    
    @Override
    public void onRotation(float rotation, float delta, float centerX, float centerY) {
        // Handle rotation updates
        if (rotationProcessor.isRotatingClockwise()) {
            // Rotating clockwise
        } else if (rotationProcessor.isRotatingCounterClockwise()) {
            // Rotating counter-clockwise
        }
    }
    
    @Override
    public void onRotationEnd(float totalRotation) {
        // Rotation gesture ended
    }
    
    @Override
    public void onRotationCancel() {
        // Rotation gesture cancelled
    }
});

// In your onTouchEvent method:
public boolean onTouchEvent(MotionEvent event) {
    return rotationProcessor.onTouchEvent(event);
}
```

### 5. SwipeGestureProcessor.java
Handles swipe gestures with:
- Directional swipe detection (up, down, left, right)
- Configurable thresholds and velocity detection
- Progress tracking during swipe

**Usage Example:**
```java
SwipeGestureProcessor swipeProcessor = new SwipeGestureProcessor();
swipeProcessor.setOnSwipeListener(new SwipeGestureProcessor.OnSwipeListener() {
    @Override
    public void onSwipeUp(float startX, float startY, float endX, float endY, float velocity) {
        // Handle swipe up
    }
    
    @Override
    public void onSwipeDown(float startX, float startY, float endX, float endY, float velocity) {
        // Handle swipe down
    }
    
    @Override
    public void onSwipeLeft(float startX, float startY, float endX, float endY, float velocity) {
        // Handle swipe left
    }
    
    @Override
    public void onSwipeRight(float startX, float startY, float endX, float endY, float velocity) {
        // Handle swipe right
    }
    
    @Override
    public void onSwipeDetected(float startX, float startY, float endX, float endY, String direction, float velocity) {
        // General swipe callback for all directions
    }
});

// In your onTouchEvent method:
public boolean onTouchEvent(MotionEvent event) {
    return swipeProcessor.onTouchEvent(event);
}
```

## Using Multiple Processors Together

You can combine multiple processors in a single view or activity:

```java
public class WallpaperView extends View {
    private TapGestureProcessor tapProcessor;
    private PanGestureProcessor panProcessor;
    private PinchGestureProcessor pinchProcessor;
    private RotationGestureProcessor rotationProcessor;
    private SwipeGestureProcessor swipeProcessor;
    
    public WallpaperView(Context context) {
        super(context);
        initProcessors();
    }
    
    private void initProcessors() {
        tapProcessor = new TapGestureProcessor();
        panProcessor = new PanGestureProcessor();
        pinchProcessor = new PinchGestureProcessor();
        rotationProcessor = new RotationGestureProcessor();
        swipeProcessor = new SwipeGestureProcessor();
        
        // Set up all callbacks...
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        
        // Process all gestures
        handled |= tapProcessor.onTouchEvent(event);
        handled |= panProcessor.onTouchEvent(event);
        handled |= pinchProcessor.onTouchEvent(event);
        handled |= rotationProcessor.onTouchEvent(event);
        handled |= swipeProcessor.onTouchEvent(event);
        
        return handled || super.onTouchEvent(event);
    }
}
```

## Configuration Options

Each processor provides methods to customize thresholds and behavior:

- **TapGestureProcessor**: Set timeout values for tap detection
- **PanGestureProcessor**: Set minimum pan distance threshold
- **PinchGestureProcessor**: Set zoom threshold for sensitivity
- **RotationGestureProcessor**: Set rotation threshold in degrees
- **SwipeGestureProcessor**: Set custom swipe parameters (distance, time, velocity)

All processors are designed to be efficient and independent, allowing you to use only the gestures your application needs.
