package com.example.wallpaper.input.gestures;

import android.view.MotionEvent;

/**
 * Processor for pan gestures with multi-directional support and velocity tracking.
 * Independent and reusable gesture processor for drag and pan detection.
 */
public class PanGestureProcessor {
    private static final int MIN_PAN_DISTANCE = 10; // minimum distance to consider as pan
    private static final int INVALID_POINTER_ID = -1;
    
    private int activePointerId = INVALID_POINTER_ID;
    private float lastX = 0;
    private float lastY = 0;
    private float startX = 0;
    private float startY = 0;
    private boolean isPanning = false;
    
    // Velocity tracking
    private float velocityX = 0;
    private float velocityY = 0;
    private long lastMoveTime = 0;
    
    // Pan callbacks
    public interface OnPanListener {
        void onPanStart(float startX, float startY);
        void onPan(float deltaX, float deltaY, float totalDeltaX, float totalDeltaY, float velocityX, float velocityY);
        void onPanEnd(float velocityX, float velocityY, float totalDeltaX, float totalDeltaY);
        void onPanCancel();
    }
    
    private OnPanListener panListener;
    
    public void setOnPanListener(OnPanListener listener) {
        this.panListener = listener;
    }
    
    /**
     * Process a touch event and detect pan gestures
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
                return handleActionCancel(event);
        }
        return false;
    }
    
    private boolean handleActionDown(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        activePointerId = event.getPointerId(pointerIndex);
        
        startX = event.getX(pointerIndex);
        startY = event.getY(pointerIndex);
        lastX = startX;
        lastY = startY;
        lastMoveTime = System.currentTimeMillis();
        
        isPanning = false;
        velocityX = 0;
        velocityY = 0;
        
        return false; // Don't consume the event yet, wait for movement
    }
    
    private boolean handleActionMove(MotionEvent event) {
        if (activePointerId == INVALID_POINTER_ID) {
            return false;
        }
        
        int pointerIndex = event.findPointerIndex(activePointerId);
        if (pointerIndex == -1) {
            return false;
        }
        
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        long currentTime = System.currentTimeMillis();
        
        float deltaX = x - lastX;
        float deltaY = y - lastY;
        float totalDeltaX = x - startX;
        float totalDeltaY = y - startY;
        
        float distance = (float) Math.sqrt(totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY);
        
        // Check if we've moved far enough to start pan
        if (!isPanning && distance > MIN_PAN_DISTANCE) {
            isPanning = true;
            if (panListener != null) {
                panListener.onPanStart(startX, startY);
            }
        }
        
        // Calculate velocity
        long timeDelta = currentTime - lastMoveTime;
        if (timeDelta > 0) {
            velocityX = deltaX / timeDelta * 1000; // pixels per second
            velocityY = deltaY / timeDelta * 1000; // pixels per second
        }
        
        // Notify pan updates
        if (isPanning && panListener != null) {
            panListener.onPan(deltaX, deltaY, totalDeltaX, totalDeltaY, velocityX, velocityY);
        }
        
        lastX = x;
        lastY = y;
        lastMoveTime = currentTime;
        
        return isPanning; // Consume events only when actively panning
    }
    
    private boolean handleActionUp(MotionEvent event) {
        if (activePointerId == INVALID_POINTER_ID) {
            return false;
        }
        
        int pointerIndex = event.findPointerIndex(activePointerId);
        float totalDeltaX = 0;
        float totalDeltaY = 0;
        
        if (pointerIndex != -1) {
            float endX = event.getX(pointerIndex);
            float endY = event.getY(pointerIndex);
            totalDeltaX = endX - startX;
            totalDeltaY = endY - startY;
        }
        
        if (isPanning && panListener != null) {
            panListener.onPanEnd(velocityX, velocityY, totalDeltaX, totalDeltaY);
        }
        
        reset();
        return isPanning;
    }
    
    private boolean handleActionCancel(MotionEvent event) {
        if (isPanning && panListener != null) {
            panListener.onPanCancel();
        }
        reset();
        return isPanning;
    }
    
    /**
     * Check if currently panning
     */
    public boolean isPanning() {
        return isPanning;
    }
    
    /**
     * Get current velocity in X direction (pixels per second)
     */
    public float getVelocityX() {
        return velocityX;
    }
    
    /**
     * Get current velocity in Y direction (pixels per second)
     */
    public float getVelocityY() {
        return velocityY;
    }
    
    /**
     * Get total distance moved in X direction
     */
    public float getTotalDeltaX() {
        return lastX - startX;
    }
    
    /**
     * Get total distance moved in Y direction
     */
    public float getTotalDeltaY() {
        return lastY - startY;
    }
    
    /**
     * Get pan direction based on velocity
     * @return Direction string: "LEFT", "RIGHT", "UP", "DOWN", or "UNKNOWN"
     */
    public String getPanDirection() {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX > 0) {
                return "RIGHT";
            } else if (velocityX < 0) {
                return "LEFT";
            }
        } else {
            if (velocityY > 0) {
                return "DOWN";
            } else if (velocityY < 0) {
                return "UP";
            }
        }
        return "UNKNOWN";
    }
    
    /**
     * Check if pan is in a specific direction
     */
    public boolean isPanningInDirection(String direction) {
        return direction.equals(getPanDirection());
    }
    
    private void reset() {
        activePointerId = INVALID_POINTER_ID;
        isPanning = false;
        velocityX = 0;
        velocityY = 0;
    }
    
    /**
     * Set custom minimum pan distance threshold
     */
    public void setMinPanDistance(int distance) {
        // In a real implementation, this would update the MIN_PAN_DISTANCE constant
    }
}
