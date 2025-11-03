package com.example.wallpaper.input;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.webkit.WebView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.VelocityTracker;
import android.view.InputDevice;
import android.view.ViewConfiguration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.lang.ref.WeakReference;

/**
 * Advanced gesture handling system for Android wallpaper application.
 * Provides comprehensive touch event processing, gesture recognition, and WebView integration.
 */
public class GestureHandler {
    private static final String TAG = "GestureHandler";
    
    // Gesture type constants
    public static final int GESTURE_TYPE_TAP = 1;
    public static final int GESTURE_TYPE_SCROLL = 2;
    public static final int GESTURE_TYPE_PINCH = 3;
    public static final int GESTURE_TYPE_ROTATION = 4;
    public static final int GESTURE_TYPE_FLING = 5;
    public static final int GESTURE_TYPE_LONG_PRESS = 6;
    public static final int GESTURE_TYPE_DOUBLE_TAP = 7;
    
    // Performance thresholds
    private static final long TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final long DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final float FLING_VELOCITY_THRESHOLD = 1500f;
    private static final float PINCH_SCALE_THRESHOLD = 1.1f;
    private static final float ROTATION_THRESHOLD = 15f; // degrees
    private static final float SCROLL_THRESHOLD = 15f; // pixels
    
    private final Context context;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;
    private final Handler mainHandler;
    private final VelocityTracker velocityTracker;
    
    // Event processing queues for performance optimization
    private final ConcurrentLinkedQueue<MotionEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final Handler eventProcessingHandler;
    private volatile boolean isProcessingEvents = false;
    
    // Gesture state tracking
    private final Map<Integer, GestureState> activeGestures = new HashMap<>();
    private final List<WeakReference<GestureListener>> gestureListeners = new ArrayList<>();
    
    // WebView integration
    private WeakReference<WebView> targetWebView;
    private boolean webViewEnabled = true;
    private boolean gestureCollisionEnabled = true;
    
    // Performance metrics
    private long lastProcessingTime = 0;
    private int processedEventsCount = 0;
    private float averageProcessingTime = 0f;
    
    /**
     * Gesture state tracking for individual touch points
     */
    private static class GestureState {
        int pointerId;
        PointF startPosition;
        long startTime;
        float totalDistance;
        int tapCount;
        boolean isConsumed;
        
        GestureState(int pointerId, float x, float y) {
            this.pointerId = pointerId;
            this.startPosition = new PointF(x, y);
            this.startTime = System.currentTimeMillis();
            this.totalDistance = 0f;
            this.tapCount = 1;
            this.isConsumed = false;
        }
    }
    
    /**
     * Interface for gesture event callbacks
     */
    public interface GestureListener {
        void onGestureDetected(int gestureType, MotionEvent event, Map<String, Object> data);
        void onGestureCancelled(int gestureType, MotionEvent event);
        boolean onGestureBegin(int gestureType, MotionEvent event);
    }
    
    /**
     * Constructor
     */
    public GestureHandler(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gestureDetector = new GestureDetector(context, new GestureListenerImpl());
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListenerImpl());
        this.velocityTracker = VelocityTracker.obtain();
        
        // Create dedicated thread for event processing
        eventProcessingHandler = new Handler(Looper.myLooper());
        if (Looper.myLooper() == null) {
            Looper.prepare();
            eventProcessingHandler = new Handler(Looper.myLooper());
            Looper.loop();
        }
        
        Log.i(TAG, "GestureHandler initialized with performance optimization");
    }
    
    /**
     * Process touch events with comprehensive pipeline
     */
    public boolean onTouchEvent(MotionEvent event) {
        long startTime = System.nanoTime();
        
        try {
            // 1. Event preprocessing and filtering
            MotionEvent processedEvent = preprocessEvent(event);
            if (processedEvent == null) {
                return false;
            }
            
            // 2. Velocity tracking for gesture analysis
            updateVelocityTracker(processedEvent);
            
            // 3. Gesture state management
            updateGestureStates(processedEvent);
            
            // 4. Collision detection with Android's gesture system
            if (gestureCollisionEnabled && detectCollision(processedEvent)) {
                return handleCollision(processedEvent);
            }
            
            // 5. Forward to gesture detectors
            boolean handled = processThroughDetectors(processedEvent);
            
            // 6. WebView event forwarding
            if (webViewEnabled && !handled) {
                handled = forwardToWebView(processedEvent);
            }
            
            // 7. Custom gesture recognition
            handled |= processCustomGestures(processedEvent);
            
            // 8. Queue for performance optimization
            if (isHighFrequencyEvent(processedEvent)) {
                queueEventForBatchProcessing(processedEvent);
            }
            
            // 9. Update performance metrics
            updatePerformanceMetrics(startTime);
            
            return handled;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing touch event", e);
            return false;
        } finally {
            // Recycle processed events to prevent memory leaks
            if (event != processedEvent) {
                event.recycle();
            }
        }
    }
    
    /**
     * Event preprocessing and filtering
     */
    private MotionEvent preprocessEvent(MotionEvent event) {
        if (event == null) {
            return null;
        }
        
        // Filter out invalid events
        if (event.getAction() == MotionEvent.ACTION_MOVE && 
            (event.getEventTime() - event.getDownTime()) < TAP_TIMEOUT) {
            return null;
        }
        
        // Apply noise filtering for move events
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return applyNoiseFiltering(event);
        }
        
        return event;
    }
    
    /**
     * Apply noise filtering to reduce false gestures
     */
    private MotionEvent applyNoiseFiltering(MotionEvent event) {
        // Simple noise filtering based on movement threshold
        if (event.getHistorySize() > 0) {
            float lastX = event.getHistoricalX(event.getHistorySize() - 1);
            float lastY = event.getHistoricalY(event.getHistorySize() - 1);
            float currentX = event.getX();
            float currentY = event.getY();
            
            float deltaX = Math.abs(currentX - lastX);
            float deltaY = Math.abs(currentY - lastY);
            
            // If movement is below noise threshold, skip processing
            if (deltaX < 2f && deltaY < 2f) {
                return null;
            }
        }
        
        return event;
    }
    
    /**
     * Update velocity tracker for gesture analysis
     */
    private void updateVelocityTracker(MotionEvent event) {
        velocityTracker.addMovement(event);
        
        if (event.getAction() == MotionEvent.ACTION_UP || 
            event.getAction() == MotionEvent.ACTION_CANCEL) {
            velocityTracker.computeCurrentVelocity(1000);
            velocityTracker.recycle();
            velocityTracker = VelocityTracker.obtain();
        }
    }
    
    /**
     * Update gesture states for individual touch points
     */
    private void updateGestureStates(MotionEvent event) {
        int action = event.getAction();
        int pointerCount = event.getPointerCount();
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                for (int i = 0; i < pointerCount; i++) {
                    int pointerId = event.getPointerId(i);
                    float x = event.getX(i);
                    float y = event.getY(i);
                    activeGestures.put(pointerId, new GestureState(pointerId, x, y));
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < pointerCount; i++) {
                    int pointerId = event.getPointerId(i);
                    GestureState state = activeGestures.get(pointerId);
                    if (state != null) {
                        float x = event.getX(i);
                        float y = event.getY(i);
                        float deltaX = x - state.startPosition.x;
                        float deltaY = y - state.startPosition.y;
                        state.totalDistance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                for (int i = 0; i < pointerCount; i++) {
                    int pointerId = event.getPointerId(i);
                    activeGestures.remove(pointerId);
                }
                break;
        }
    }
    
    /**
     * Detect collision with Android's gesture system
     */
    private boolean detectCollision(MotionEvent event) {
        // Check if the event should be consumed by Android's gesture system
        int action = event.getAction();
        
        // Always allow scroll and fling gestures through Android's system
        if (action == MotionEvent.ACTION_SCROLL) {
            return true;
        }
        
        // Allow basic tap detection if it matches standard patterns
        if (action == MotionEvent.ACTION_UP) {
            for (GestureState state : activeGestures.values()) {
                if (state.totalDistance < SCROLL_THRESHOLD && 
                    (System.currentTimeMillis() - state.startTime) < TAP_TIMEOUT) {
                    return true; // Allow Android to handle as tap
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handle collision with Android's gesture system
     */
    private boolean handleCollision(MotionEvent event) {
        // Forward to gesture detector and potentially consume the event
        boolean handled = gestureDetector.onTouchEvent(event);
        
        // If handled by gesture detector, mark as consumed
        if (handled) {
            markGestureConsumed(event);
        }
        
        return handled;
    }
    
    /**
     * Process events through Android gesture detectors
     */
    private boolean processThroughDetectors(MotionEvent event) {
        boolean handled = false;
        
        // Process through scale detector (for pinch gestures)
        if (scaleGestureDetector.onTouchEvent(event)) {
            handled = true;
        }
        
        // Process through gesture detector
        if (gestureDetector.onTouchEvent(event)) {
            handled = true;
        }
        
        return handled;
    }
    
    /**
     * Forward events to WebView for web-based gesture handling
     */
    private boolean forwardToWebView(MotionEvent event) {
        WebView webView = targetWebView != null ? targetWebView.get() : null;
        if (webView == null) {
            return false;
        }
        
        try {
            // Convert Android touch event to JavaScript touch event
            String jsEvent = convertToJSTouchEvent(event);
            
            // Evaluate JavaScript in WebView context
            webView.evaluateJavascript(
                "window.dispatchEvent(new TouchEvent('touchstart', {" +
                "touches: [new Touch({identifier: " + event.getPointerId(0) + ", target: document.body, clientX: " + event.getX() + ", clientY: " + event.getY() + "})] }));", 
                null
            );
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to forward event to WebView", e);
            return false;
        }
    }
    
    /**
     * Convert Android MotionEvent to JavaScript touch event
     */
    private String convertToJSTouchEvent(MotionEvent event) {
        int action = event.getAction();
        String actionName;
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                actionName = "touchstart";
                break;
            case MotionEvent.ACTION_MOVE:
                actionName = "touchmove";
                break;
            case MotionEvent.ACTION_UP:
                actionName = "touchend";
                break;
            case MotionEvent.ACTION_CANCEL:
                actionName = "touchcancel";
                break;
            default:
                return "";
        }
        
        // Build JavaScript event creation code
        StringBuilder js = new StringBuilder();
        js.append("var touch = new Touch({identifier: ").append(event.getPointerId(0));
        js.append(", target: document.body, clientX: ").append(event.getX());
        js.append(", clientY: ").append(event.getY()).append("});");
        js.append("var touchEvent = new TouchEvent('").append(actionName).append("', {touches: [touch]});");
        js.append("document.dispatchEvent(touchEvent);");
        
        return js.toString();
    }
    
    /**
     * Process custom gestures beyond Android's built-in support
     */
    private boolean processCustomGestures(MotionEvent event) {
        int action = event.getAction();
        boolean handled = false;
        
        switch (action) {
            case MotionEvent.ACTION_UP:
                handled = processTapGesture(event);
                if (!handled) {
                    handled = processCustomTapSequence(event);
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    handled = processMultiTouchGesture(event);
                }
                break;
        }
        
        return handled;
    }
    
    /**
     * Process tap gesture with custom recognition logic
     */
    private boolean processTapGesture(MotionEvent event) {
        for (GestureState state : activeGestures.values()) {
            if (state.totalDistance < SCROLL_THRESHOLD && 
                (System.currentTimeMillis() - state.startTime) < TAP_TIMEOUT &&
                !state.isConsumed) {
                
                // Notify listeners
                notifyGestureListeners(GESTURE_TYPE_TAP, event, createGestureData("tap", state));
                state.isConsumed = true;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Process custom tap sequences (double-tap, triple-tap, etc.)
     */
    private boolean processCustomTapSequence(MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        
        for (GestureState state : activeGestures.values()) {
            // Detect double-tap
            if (currentTime - state.startTime < DOUBLE_TAP_TIMEOUT && 
                state.tapCount == 2 && state.totalDistance < SCROLL_THRESHOLD) {
                
                notifyGestureListeners(GESTURE_TYPE_DOUBLE_TAP, event, createGestureData("double_tap", state));
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Process multi-touch gestures (pinch, rotation)
     */
    private boolean processMultiTouchGesture(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return false;
        }
        
        // Process pinch gesture
        if (event.getPointerCount() == 2) {
            return processPinchGesture(event) || processRotationGesture(event);
        }
        
        return false;
    }
    
    /**
     * Process pinch gesture with custom detection
     */
    private boolean processPinchGesture(MotionEvent event) {
        // Calculate current distance between two touches
        float currentDistance = calculateDistance(event.getX(0), event.getY(0), 
                                                   event.getX(1), event.getY(1));
        
        // Use historical data to calculate initial distance
        if (event.getHistorySize() > 0) {
            float initialDistance = calculateDistance(event.getHistoricalX(0), event.getHistoricalY(0),
                                                     event.getHistoricalX(1), event.getHistoricalY(1));
            
            float scale = currentDistance / initialDistance;
            
            if (Math.abs(scale - 1f) > PINCH_SCALE_THRESHOLD) {
                Map<String, Object> data = createGestureData("pinch", null);
                data.put("scale", scale);
                data.put("focus_x", (event.getX(0) + event.getX(1)) / 2f);
                data.put("focus_y", (event.getY(0) + event.getY(1)) / 2f);
                
                notifyGestureListeners(GESTURE_TYPE_PINCH, event, data);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Process rotation gesture
     */
    private boolean processRotationGesture(MotionEvent event) {
        if (event.getPointerCount() != 2) {
            return false;
        }
        
        float angle1 = (float) Math.atan2(event.getY(1) - event.getY(0), 
                                         event.getX(1) - event.getX(0));
        
        if (event.getHistorySize() > 0) {
            float angle0 = (float) Math.atan2(event.getHistoricalY(1) - event.getHistoricalY(0),
                                             event.getHistoricalX(1) - event.getHistoricalX(0));
            
            float rotationDelta = (float) Math.toDegrees(angle1 - angle0);
            
            if (Math.abs(rotationDelta) > ROTATION_THRESHOLD) {
                Map<String, Object> data = createGestureData("rotation", null);
                data.put("rotation", rotationDelta);
                data.put("center_x", (event.getX(0) + event.getX(1)) / 2f);
                data.put("center_y", (event.getY(0) + event.getY(1)) / 2f);
                
                notifyGestureListeners(GESTURE_TYPE_ROTATION, event, data);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Queue events for batch processing to optimize performance
     */
    private void queueEventForBatchProcessing(MotionEvent event) {
        pendingEvents.offer(MotionEvent.obtain(event));
        
        if (!isProcessingEvents) {
            isProcessingEvents = true;
            eventProcessingHandler.post(this::processBatchEvents);
        }
    }
    
    /**
     * Process queued events in batch for better performance
     */
    private void processBatchEvents() {
        long batchStartTime = System.nanoTime();
        int processedCount = 0;
        
        while (!pendingEvents.isEmpty()) {
            MotionEvent event = pendingEvents.poll();
            if (event != null) {
                // Process queued event (implement batch processing logic here)
                processedCount++;
                event.recycle();
            }
        }
        
        long batchEndTime = System.nanoTime();
        long batchDuration = (batchEndTime - batchStartTime) / 1_000_000; // Convert to ms
        
        Log.d(TAG, String.format("Batch processed %d events in %d ms", processedCount, batchDuration));
        
        isProcessingEvents = false;
    }
    
    /**
     * Check if event is high frequency and should be batched
     */
    private boolean isHighFrequencyEvent(MotionEvent event) {
        return event.getAction() == MotionEvent.ACTION_MOVE && 
               event.getPointerCount() == 1 &&
               (System.currentTimeMillis() - lastProcessingTime) < 16; // ~60 FPS
    }
    
    /**
     * Update performance metrics
     */
    private void updatePerformanceMetrics(long startTime) {
        long endTime = System.nanoTime();
        long processingTime = (endTime - startTime) / 1_000_000; // Convert to ms
        
        processedEventsCount++;
        lastProcessingTime = System.currentTimeMillis();
        
        // Calculate rolling average
        averageProcessingTime = ((averageProcessingTime * (processedEventsCount - 1)) + processingTime) / processedEventsCount;
        
        if (processedEventsCount % 100 == 0) {
            Log.i(TAG, String.format("Performance: %d events, avg %.2f ms per event", 
                                   processedEventsCount, averageProcessingTime));
        }
    }
    
    /**
     * Mark gesture as consumed to prevent duplicate processing
     */
    private void markGestureConsumed(MotionEvent event) {
        for (GestureState state : activeGestures.values()) {
            if (event.getPointerId(0) == state.pointerId) {
                state.isConsumed = true;
                break;
            }
        }
    }
    
    /**
     * Create gesture data map for listener callbacks
     */
    private Map<String, Object> createGestureData(String gestureType, GestureState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("gesture_type", gestureType);
        data.put("timestamp", System.currentTimeMillis());
        
        if (state != null) {
            data.put("start_x", state.startPosition.x);
            data.put("start_y", state.startPosition.y);
            data.put("total_distance", state.totalDistance);
            data.put("duration", System.currentTimeMillis() - state.startTime);
            data.put("tap_count", state.tapCount);
        }
        
        return data;
    }
    
    /**
     * Calculate distance between two points
     */
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Notify all registered gesture listeners
     */
    private void notifyGestureListeners(int gestureType, MotionEvent event, Map<String, Object> data) {
        for (int i = gestureListeners.size() - 1; i >= 0; i--) {
            WeakReference<GestureListener> listenerRef = gestureListeners.get(i);
            GestureListener listener = listenerRef.get();
            
            if (listener != null) {
                listener.onGestureDetected(gestureType, event, data);
            } else {
                gestureListeners.remove(i); // Clean up dead references
            }
        }
    }
    
    /**
     * Add gesture listener
     */
    public void addGestureListener(GestureListener listener) {
        if (listener != null) {
            gestureListeners.add(new WeakReference<>(listener));
        }
    }
    
    /**
     * Remove gesture listener
     */
    public void removeGestureListener(GestureListener listener) {
        for (int i = gestureListeners.size() - 1; i >= 0; i--) {
            GestureListener existing = gestureListeners.get(i).get();
            if (existing == listener || existing == null) {
                gestureListeners.remove(i);
            }
        }
    }
    
    /**
     * Set target WebView for event forwarding
     */
    public void setTargetWebView(WebView webView) {
        this.targetWebView = new WeakReference<>(webView);
    }
    
    /**
     * Enable or disable WebView integration
     */
    public void setWebViewEnabled(boolean enabled) {
        this.webViewEnabled = enabled;
    }
    
    /**
     * Enable or disable gesture collision detection
     */
    public void setGestureCollisionEnabled(boolean enabled) {
        this.gestureCollisionEnabled = enabled;
    }
    
    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("processed_events", processedEventsCount);
        metrics.put("average_processing_time", averageProcessingTime);
        metrics.put("active_gestures", activeGestures.size());
        metrics.put("pending_events", pendingEvents.size());
        return metrics;
    }
    
    /**
     * Cleanup resources
     */
    public void destroy() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
        }
        if (eventProcessingHandler != null) {
            eventProcessingHandler.removeCallbacksAndMessages(null);
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        activeGestures.clear();
        gestureListeners.clear();
        Log.i(TAG, "GestureHandler destroyed and resources cleaned up");
    }
    
    /**
     * GestureDetector listener implementation
     */
    private class GestureListenerImpl extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            notifyGestureListeners(GESTURE_TYPE_TAP, e, createGestureData("single_tap", null));
            return true;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            notifyGestureListeners(GESTURE_TYPE_DOUBLE_TAP, e, createGestureData("double_tap", null));
            return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Map<String, Object> data = createGestureData("scroll", null);
            data.put("distance_x", -distanceX);
            data.put("distance_y", -distanceY);
            notifyGestureListeners(GESTURE_TYPE_SCROLL, e2, data);
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Map<String, Object> data = createGestureData("fling", null);
            data.put("velocity_x", velocityX);
            data.put("velocity_y", velocityY);
            
            if (Math.abs(velocityX) > FLING_VELOCITY_THRESHOLD || 
                Math.abs(velocityY) > FLING_VELOCITY_THRESHOLD) {
                notifyGestureListeners(GESTURE_TYPE_FLING, e2, data);
                return true;
            }
            return false;
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
            notifyGestureListeners(GESTURE_TYPE_LONG_PRESS, e, createGestureData("long_press", null));
        }
    }
    
    /**
     * ScaleGestureDetector listener implementation
     */
    private class ScaleListenerImpl extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            
            Map<String, Object> data = createGestureData("pinch", null);
            data.put("scale", scaleFactor);
            data.put("focus_x", focusX);
            data.put("focus_y", focusY);
            
            notifyGestureListeners(GESTURE_TYPE_PINCH, null, data);
            return true;
        }
        
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }
        
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // Cleanup after scale gesture
        }
    }
}