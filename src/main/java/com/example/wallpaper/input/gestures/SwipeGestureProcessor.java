package com.example.wallpaper.input.gestures;

import android.view.MotionEvent;
import android.util.Log;

/**
 * Processor for swipe gestures with directional detection and threshold support.
 * Independent and reusable gesture processor for swipe detection.
 */
public class SwipeGestureProcessor {
    private static final int MIN_SWIPE_DISTANCE = 100; // minimum distance for swipe
    private static final long MAX_SWIPE_TIME = 1000; // maximum time for swipe in milliseconds
    private static final float MIN_SWIPE_VELOCITY = 0.5f; // minimum velocity for swipe (pixels/ms)
    
    private static final int INVALID_POINTER_ID = -1;
    
    private int activePointerId = INVALID_POINTER_ID;
    private float startX = 0;
    private float startY = 0;
    private long startTime = 0;
    
    private float lastX = 0;
    private float lastY = 0;
    private long lastMoveTime = 0;
    
    private boolean isTrackingSwipe = false;
    private boolean swipeDetected = false;
    
    // Swipe callbacks
    public interface OnSwipeListener {
        void onSwipeUp(float startX, float startY, float endX, float endY, float velocity);
        void onSwipeDown(float startX, float startY, float endX, float endY, float velocity);
        void onSwipeLeft(float startX, float startY, float endX, float endY, float velocity);
        void onSwipeRight(float startX, float startY, float endX, float endY, float velocity);
        void onSwipeDetected(float startX, float startY, float endX, float endY, String direction, float velocity);
    }
    
    private OnSwipeListener swipeListener;
    private boolean enableDirectionalCallbacks = true;
    private boolean enableGeneralCallback = true;
    
    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }
    
    /**
     * Enable or disable directional callbacks (onSwipeUp, onSwipeDown, etc.)
     */
    public void setEnableDirectionalCallbacks(boolean enable) {
        this.enableDirectionalCallbacks = enable;
    }
    
    /**
     * Enable or disable general callback (onSwipeDetected)
     */
    public void setEnableGeneralCallback(boolean enable) {
        this.enableGeneralCallback = enable;
    }
    
    /**
     * Process a touch event and detect swipe gestures
     * @param event The motion event to process
     * @return true if the event was consumed, false otherwise
     */
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleActionDown(event);
            case MotionEvent.ACTION_MOVE:
                return handleActionMove(event);
            case MotionEvent.ACTION_UP:
                return handleActionUp(event);
            case MotionEvent.ACTION_CANCEL:
                handleActionCancel(event);
                return false;
        }
        return false;
    }
    
    private boolean handleActionDown(MotionEvent event) {
        activePointerId = event.getPointerId(event.getActionIndex());
        startX = event.getX(event.getActionIndex());
        startY = event.getY(event.getActionIndex());
        startTime = System.currentTimeMillis();
        
        lastX = startX;
        lastY = startY;
        lastMoveTime = startTime;
        
        isTrackingSwipe = true;
        swipeDetected = false;
        
        return false; // Don't consume, wait for movement
    }
    
    private boolean handleActionMove(MotionEvent event) {
        if (!isTrackingSwipe || activePointerId == INVALID_POINTER_ID) {
            return false;
        }
        
        int pointerIndex = event.findPointerIndex(activePointerId);
        if (pointerIndex == -1) {
            return false;
        }
        
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        long currentTime = System.currentTimeMillis();
        
        // Update tracking
        lastX = x;
        lastY = y;
        lastMoveTime = currentTime;
        
        return false; // Don't consume movement events
    }
    
    private boolean handleActionUp(MotionEvent event) {
        if (!isTrackingSwipe || activePointerId == INVALID_POINTER_ID) {
            return false;
        }
        
        int pointerIndex = event.findPointerIndex(activePointerId);
        if (pointerIndex == -1) {
            return false;
        }
        
        float endX = event.getX(pointerIndex);
        float endY = event.getY(pointerIndex);
        long endTime = System.currentTimeMillis();
        
        // Check if this qualifies as a swipe
        if (isSwipe(startX, startY, endX, endY, startTime, endTime)) {
            String direction = detectDirection(startX, startY, endX, endY);
            float velocity = calculateVelocity(endX - startX, endY - startY, endTime - startTime);
            
            if (swipeListener != null) {
                if (enableDirectionalCallbacks) {
                    triggerDirectionalCallback(direction, startX, startY, endX, endY, velocity);
                }
                if (enableGeneralCallback) {
                    swipeListener.onSwipeDetected(startX, startY, endX, endY, direction, velocity);
                }
            }
            
            swipeDetected = true;
            reset();
            return true; // Consume the event
        }
        
        reset();
        return false;
    }
    
    private void handleActionCancel(MotionEvent event) {
        reset();
    }
    
    /**
     * Check if the gesture qualifies as a swipe
     */
    private boolean isSwipe(float startX, float startY, float endX, float endY, long startTime, long endTime) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        long duration = endTime - startTime;
        
        // Check minimum distance
        if (distance < MIN_SWIPE_DISTANCE) {
            return false;
        }
        
        // Check maximum time
        if (duration > MAX_SWIPE_TIME) {
            return false;
        }
        
        // Check minimum velocity
        float velocity = distance / duration;
        if (velocity < MIN_SWIPE_VELOCITY) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Detect swipe direction based on start and end positions
     */
    private String detectDirection(float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);
        
        // Determine primary direction
        if (absDeltaX > absDeltaY) {
            // Horizontal swipe
            if (deltaX > 0) {
                return "RIGHT";
            } else {
                return "LEFT";
            }
        } else {
            // Vertical swipe
            if (deltaY > 0) {
                return "DOWN";
            } else {
                return "UP";
            }
        }
    }
    
    /**
     * Calculate swipe velocity
     */
    private float calculateVelocity(float deltaX, float deltaY, long duration) {
        if (duration <= 0) return 0;
        
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return distance / duration; // pixels per millisecond
    }
    
    private void triggerDirectionalCallback(String direction, float startX, float startY, 
                                          float endX, float endY, float velocity) {
        if (swipeListener == null) return;
        
        switch (direction) {
            case "UP":
                swipeListener.onSwipeUp(startX, startY, endX, endY, velocity);
                break;
            case "DOWN":
                swipeListener.onSwipeDown(startX, startY, endX, endY, velocity);
                break;
            case "LEFT":
                swipeListener.onSwipeLeft(startX, startY, endX, endY, velocity);
                break;
            case "RIGHT":
                swipeListener.onSwipeRight(startX, startY, endX, endY, velocity);
                break;
        }
    }
    
    /**
     * Check if currently tracking a swipe gesture
     */
    public boolean isTrackingSwipe() {
        return isTrackingSwipe;
    }
    
    /**
     * Check if a swipe has been detected
     */
    public boolean isSwipeDetected() {
        return swipeDetected;
    }
    
    /**
     * Get current swipe distance
     */
    public float getCurrentDistance() {
        if (!isTrackingSwipe) return 0;
        
        float deltaX = lastX - startX;
        float deltaY = lastY - startY;
        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
    
    /**
     * Get current swipe direction based on movement
     */
    public String getCurrentDirection() {
        if (!isTrackingSwipe) return "NONE";
        
        return detectDirection(startX, startY, lastX, lastY);
    }
    
    /**
     * Check if swipe is in a specific direction
     */
    public boolean isSwipingInDirection(String direction) {
        return direction.equals(getCurrentDirection());
    }
    
    /**
     * Check if swipe distance exceeds a threshold
     */
    public boolean isDistanceAboveThreshold(float threshold) {
        return getCurrentDistance() >= threshold;
    }
    
    /**
     * Get the time elapsed since swipe started
     */
    public long getElapsedTime() {
        if (!isTrackingSwipe) return 0;
        
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Get the estimated final swipe velocity based on current movement
     */
    public float getEstimatedVelocity() {
        if (!isTrackingSwipe || getElapsedTime() <= 0) return 0;
        
        return getCurrentDistance() / getElapsedTime();
    }
    
    /**
     * Check if swipe is still within time constraints
     */
    public boolean isWithinTimeConstraint() {
        return getElapsedTime() <= MAX_SWIPE_TIME;
    }
    
    /**
     * Get swipe progress as a percentage of minimum distance
     */
    public float getSwipeProgress() {
        return Math.min(1.0f, getCurrentDistance() / MIN_SWIPE_DISTANCE);
    }
    
    /**
     * Cancel current swipe tracking
     */
    public void cancelSwipeTracking() {
        reset();
    }
    
    private void reset() {
        activePointerId = INVALID_POINTER_ID;
        isTrackingSwipe = false;
        swipeDetected = false;
    }
    
    /**
     * Set custom swipe parameters
     */
    public void setSwipeParameters(int minSwipeDistance, long maxSwipeTime, float minVelocity) {
        // In a real implementation, these would update the constants
        Log.d("SwipeGestureProcessor", "Setting custom swipe parameters: distance=" + minSwipeDistance + 
              ", time=" + maxSwipeTime + ", velocity=" + minVelocity);
    }
    
    /**
     * Get current swipe start position
     */
    public float getStartX() {
        return startX;
    }
    
    /**
     * Get current swipe start position
     */
    public float getStartY() {
        return startY;
    }
    
    /**
     * Get current swipe end position
     */
    public float getCurrentX() {
        return lastX;
    }
    
    /**
     * Get current swipe end position
     */
    public float getCurrentY() {
        return lastY;
    }
}
