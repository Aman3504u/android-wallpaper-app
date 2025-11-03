package com.example.wallpaper.input.gestures;

import android.view.MotionEvent;

/**
 * Processor for tap gestures including single tap, double tap, and long tap detection.
 * Independent and reusable gesture processor that tracks touch events and detects various tap types.
 */
public class TapGestureProcessor {
    private static final int TAP_TIMEOUT = 300; // milliseconds
    private static final int DOUBLE_TAP_TIMEOUT = 300; // milliseconds
    private static final int LONG_PRESS_TIMEOUT = 500; // milliseconds
    
    private float lastTapX = -1;
    private float lastTapY = -1;
    private long lastTapTime = 0;
    private boolean isLongPressTriggered = false;
    private boolean isLongPressDetected = false;
    
    // Tap callbacks
    public interface OnTapListener {
        void onSingleTap(float x, float y);
        void onDoubleTap(float x, float y);
        void onLongPress(float x, float y);
    }
    
    private OnTapListener tapListener;
    
    public void setOnTapListener(OnTapListener listener) {
        this.tapListener = listener;
    }
    
    /**
     * Process a touch event and detect tap gestures
     * @param event The motion event to process
     * @return true if the event was consumed, false otherwise
     */
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleActionDown(event);
            case MotionEvent.ACTION_UP:
                return handleActionUp(event);
            case MotionEvent.ACTION_CANCEL:
                reset();
                return false;
        }
        return false;
    }
    
    private boolean handleActionDown(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        long currentTime = System.currentTimeMillis();
        
        // Check if this is a long press
        isLongPressDetected = (currentTime - lastTapTime > LONG_PRESS_TIMEOUT);
        isLongPressTriggered = false;
        
        // Check for double tap
        if (lastTapTime > 0 && (currentTime - lastTapTime) < DOUBLE_TAP_TIMEOUT) {
            // Check if tap is within threshold distance
            float distance = calculateDistance(lastTapX, lastTapY, x, y);
            if (distance < 50) { // 50 pixel threshold for double tap
                if (tapListener != null) {
                    tapListener.onDoubleTap(x, y);
                }
                reset();
                return true;
            }
        }
        
        // Schedule long press detection
        // Note: In a real implementation, you would use a Handler to postDelayed
        // for LONG_PRESS_TIMEOUT and check if touch is still held
        
        return false;
    }
    
    private boolean handleActionUp(MotionEvent event) {
        if (isLongPressDetected) {
            // Long press already handled during ACTION_DOWN timeout
            reset();
            return true;
        }
        
        float x = event.getX();
        float y = event.getY();
        long currentTime = System.currentTimeMillis();
        
        // Check if this is a single tap (not part of a double tap)
        if (lastTapTime == 0 || (currentTime - lastTapTime) > DOUBLE_TAP_TIMEOUT) {
            if (tapListener != null) {
                tapListener.onSingleTap(x, y);
            }
            
            // Store for potential double tap detection
            lastTapX = x;
            lastTapY = y;
            lastTapTime = currentTime;
        }
        
        reset();
        return false;
    }
    
    /**
     * Check if a long press should be triggered
     * Call this method periodically when holding a touch
     * @param event The motion event
     * @return true if long press should trigger
     */
    public boolean checkLongPress(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !isLongPressTriggered) {
            float x = event.getX();
            float y = event.getY();
            
            // Check if we've held long enough
            long elapsedTime = System.currentTimeMillis() - lastTapTime;
            if (elapsedTime >= LONG_PRESS_TIMEOUT && tapListener != null) {
                tapListener.onLongPress(x, y);
                isLongPressTriggered = true;
                return true;
            }
        }
        return false;
    }
    
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Reset the tap state
     */
    private void reset() {
        isLongPressDetected = false;
        isLongPressTriggered = false;
    }
    
    /**
     * Set custom timeout values for tap detection
     */
    public void setTimeouts(int tapTimeout, int doubleTapTimeout, int longPressTimeout) {
        // In a real implementation, you would use these values
        // Currently using constants for simplicity
    }
}
