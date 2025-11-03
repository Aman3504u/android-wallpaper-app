package com.example.wallpaper.input.gestures;

import android.view.MotionEvent;

/**
 * Processor for pinch-to-zoom gestures with focus tracking.
 * Independent and reusable gesture processor for zoom detection.
 */
public class PinchGestureProcessor {
    private static final float ZOOM_THRESHOLD = 0.1f; // minimum change to consider as zoom
    private static final int INVALID_POINTER_ID = -1;
    
    private int primaryPointerId = INVALID_POINTER_ID;
    private int secondaryPointerId = INVALID_POINTER_ID;
    
    private float primaryStartX = 0;
    private float primaryStartY = 0;
    private float secondaryStartX = 0;
    private float secondaryStartY = 0;
    
    private float initialDistance = 0;
    private float currentDistance = 0;
    
    private boolean isZooming = false;
    private float zoomFocusX = 0;
    private float zoomFocusY = 0;
    private float currentScale = 1.0f;
    
    // Pinch callbacks
    public interface OnPinchListener {
        void onZoomStart(float focusX, float focusY);
        void onZoom(float scale, float delta, float focusX, float focusY);
        void onZoomEnd(float finalScale);
        void onZoomCancel();
    }
    
    private OnPinchListener pinchListener;
    
    public void setOnPinchListener(OnPinchListener listener) {
        this.pinchListener = listener;
    }
    
    /**
     * Process a touch event and detect pinch gestures
     * @param event The motion event to process
     * @return true if the event was consumed, false otherwise
     */
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                return handleActionDown(event);
            case MotionEvent.ACTION_POINTER_DOWN:
                return handlePointerDown(event);
            case MotionEvent.ACTION_MOVE:
                return handleActionMove(event);
            case MotionEvent.ACTION_POINTER_UP:
                return handlePointerUp(event);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleActionUp(event);
        }
        return false;
    }
    
    private boolean handleActionDown(MotionEvent event) {
        // First touch - store as primary pointer
        primaryPointerId = event.getPointerId(event.getActionIndex());
        primaryStartX = event.getX(event.getActionIndex());
        primaryStartY = event.getY(event.getActionIndex());
        
        isZooming = false;
        currentScale = 1.0f;
        
        return false;
    }
    
    private boolean handlePointerDown(MotionEvent event) {
        // Second touch - store as secondary pointer
        if (primaryPointerId != INVALID_POINTER_ID) {
            secondaryPointerId = event.getPointerId(event.getActionIndex());
            int pointerIndex = event.getActionIndex();
            
            secondaryStartX = event.getX(pointerIndex);
            secondaryStartY = event.getY(pointerIndex);
            
            // Calculate initial distance
            initialDistance = calculateDistance(secondaryStartX, secondaryStartY, 
                                              primaryStartX, primaryStartY);
            currentDistance = initialDistance;
            
            if (initialDistance > 10) { // Minimum distance threshold
                // Calculate focus point
                zoomFocusX = (primaryStartX + secondaryStartX) / 2;
                zoomFocusY = (primaryStartY + secondaryStartY) / 2;
                
                isZooming = true;
                if (pinchListener != null) {
                    pinchListener.onZoomStart(zoomFocusX, zoomFocusY);
                }
            }
        }
        
        return true; // Consume the event
    }
    
    private boolean handleActionMove(MotionEvent event) {
        if (!isZooming || primaryPointerId == INVALID_POINTER_ID || 
            secondaryPointerId == INVALID_POINTER_ID) {
            return false;
        }
        
        int primaryIndex = event.findPointerIndex(primaryPointerId);
        int secondaryIndex = event.findPointerIndex(secondaryPointerId);
        
        if (primaryIndex == -1 || secondaryIndex == -1) {
            return false;
        }
        
        float primaryX = event.getX(primaryIndex);
        float primaryY = event.getY(primaryIndex);
        float secondaryX = event.getX(secondaryIndex);
        float secondaryY = event.getY(secondaryIndex);
        
        currentDistance = calculateDistance(secondaryX, secondaryY, primaryX, primaryY);
        
        if (initialDistance > 0) {
            float scale = currentDistance / initialDistance;
            float delta = scale - currentScale;
            
            // Update focus point for continuous tracking
            zoomFocusX = (primaryX + secondaryX) / 2;
            zoomFocusY = (primaryY + secondaryY) / 2;
            
            // Only report significant changes
            if (Math.abs(delta) > ZOOM_THRESHOLD) {
                currentScale = scale;
                
                if (pinchListener != null) {
                    pinchListener.onZoom(currentScale, delta, zoomFocusX, zoomFocusY);
                }
            }
        }
        
        return true; // Consume the event
    }
    
    private boolean handlePointerUp(MotionEvent event) {
        int pointerId = event.getPointerId(event.getActionIndex());
        
        // Check if secondary pointer is being released
        if (pointerId == secondaryPointerId) {
            if (isZooming && pinchListener != null) {
                pinchListener.onZoomEnd(currentScale);
            }
            resetSecondaryPointer();
        }
        // Check if primary pointer is being released
        else if (pointerId == primaryPointerId) {
            if (isZooming && pinchListener != null) {
                pinchListener.onZoomEnd(currentScale);
            }
            resetPrimaryPointer();
        }
        
        return isZooming;
    }
    
    private boolean handleActionUp(MotionEvent event) {
        if (isZooming && pinchListener != null) {
            pinchListener.onZoomEnd(currentScale);
        }
        
        reset();
        return isZooming;
    }
    
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Check if currently zooming
     */
    public boolean isZooming() {
        return isZooming;
    }
    
    /**
     * Get current zoom scale (1.0 = original size)
     */
    public float getCurrentScale() {
        return currentScale;
    }
    
    /**
     * Get zoom focus point X coordinate
     */
    public float getZoomFocusX() {
        return zoomFocusX;
    }
    
    /**
     * Get zoom focus point Y coordinate
     */
    public float getZoomFocusY() {
        return zoomFocusY;
    }
    
    /**
     * Check if zooming in (scale > 1.0)
     */
    public boolean isZoomingIn() {
        return currentScale > 1.0f;
    }
    
    /**
     * Check if zooming out (scale < 1.0)
     */
    public boolean isZoomingOut() {
        return currentScale < 1.0f;
    }
    
    /**
     * Get zoom factor relative to initial distance
     */
    public float getZoomFactor() {
        return (initialDistance > 0) ? currentDistance / initialDistance : 1.0f;
    }
    
    /**
     * Get the distance between fingers
     */
    public float getCurrentDistance() {
        return currentDistance;
    }
    
    /**
     * Get the initial distance between fingers
     */
    public float getInitialDistance() {
        return initialDistance;
    }
    
    /**
     * Check if zoom scale exceeds a threshold
     */
    public boolean isScaleAboveThreshold(float threshold) {
        return currentScale >= threshold;
    }
    
    /**
     * Check if zoom scale is below a threshold
     */
    public boolean isScaleBelowThreshold(float threshold) {
        return currentScale <= threshold;
    }
    
    private void resetSecondaryPointer() {
        secondaryPointerId = INVALID_POINTER_ID;
        secondaryStartX = 0;
        secondaryStartY = 0;
        initialDistance = 0;
        currentDistance = 0;
        
        // If primary pointer still exists, we can potentially continue with it
        if (primaryPointerId != INVALID_POINTER_ID) {
            isZooming = false;
        }
    }
    
    private void resetPrimaryPointer() {
        // This is more complex as we need to promote secondary to primary
        if (secondaryPointerId != INVALID_POINTER_ID) {
            primaryPointerId = secondaryPointerId;
            primaryStartX = secondaryStartX;
            primaryStartY = secondaryStartY;
            resetSecondaryPointer();
        } else {
            reset();
        }
    }
    
    private void reset() {
        primaryPointerId = INVALID_POINTER_ID;
        secondaryPointerId = INVALID_POINTER_ID;
        isZooming = false;
        currentScale = 1.0f;
        zoomFocusX = 0;
        zoomFocusY = 0;
    }
    
    /**
     * Set custom zoom threshold
     */
    public void setZoomThreshold(float threshold) {
        // In a real implementation, this would update the ZOOM_THRESHOLD constant
    }
}
