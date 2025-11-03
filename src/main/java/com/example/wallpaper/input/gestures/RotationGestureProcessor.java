package com.example.wallpaper.input.gestures;

import android.view.MotionEvent;

/**
 * Processor for rotation gestures with multi-touch support.
 * Independent and reusable gesture processor for rotation detection.
 */
public class RotationGestureProcessor {
    private static final float ROTATION_THRESHOLD = 1.0f; // degrees - minimum rotation to consider
    private static final int INVALID_POINTER_ID = -1;
    
    private int primaryPointerId = INVALID_POINTER_ID;
    private int secondaryPointerId = INVALID_POINTER_ID;
    
    private float primaryStartX = 0;
    private float primaryStartY = 0;
    private float secondaryStartX = 0;
    private float secondaryStartY = 0;
    
    private float initialAngle = 0;
    private float currentAngle = 0;
    private float totalRotation = 0;
    
    private boolean isRotating = false;
    private float rotationCenterX = 0;
    private float rotationCenterY = 0;
    
    // Rotation callbacks
    public interface OnRotationListener {
        void onRotationStart(float centerX, float centerY);
        void onRotation(float rotation, float delta, float centerX, float centerY);
        void onRotationEnd(float totalRotation);
        void onRotationCancel();
    }
    
    private OnRotationListener rotationListener;
    
    public void setOnRotationListener(OnRotationListener listener) {
        this.rotationListener = listener;
    }
    
    /**
     * Process a touch event and detect rotation gestures
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
        
        isRotating = false;
        totalRotation = 0;
        
        return false;
    }
    
    private boolean handlePointerDown(MotionEvent event) {
        // Second touch - store as secondary pointer
        if (primaryPointerId != INVALID_POINTER_ID) {
            secondaryPointerId = event.getPointerId(event.getActionIndex());
            int pointerIndex = event.getActionIndex();
            
            secondaryStartX = event.getX(pointerIndex);
            secondaryStartY = event.getY(pointerIndex);
            
            // Calculate initial angle
            initialAngle = calculateAngle(secondaryStartX, secondaryStartY, 
                                        primaryStartX, primaryStartY);
            currentAngle = initialAngle;
            totalRotation = 0;
            
            // Calculate rotation center
            rotationCenterX = (primaryStartX + secondaryStartX) / 2;
            rotationCenterY = (primaryStartY + secondaryStartY) / 2;
            
            isRotating = true;
            if (rotationListener != null) {
                rotationListener.onRotationStart(rotationCenterX, rotationCenterY);
            }
        }
        
        return true; // Consume the event
    }
    
    private boolean handleActionMove(MotionEvent event) {
        if (!isRotating || primaryPointerId == INVALID_POINTER_ID || 
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
        
        currentAngle = calculateAngle(secondaryX, secondaryY, primaryX, primaryY);
        
        // Calculate rotation delta
        float delta = calculateRotationDelta(initialAngle, currentAngle);
        
        // Update total rotation
        totalRotation += delta;
        
        // Update rotation center for continuous tracking
        rotationCenterX = (primaryX + secondaryX) / 2;
        rotationCenterY = (primaryY + secondaryY) / 2;
        
        // Only report significant changes
        if (Math.abs(delta) > ROTATION_THRESHOLD) {
            if (rotationListener != null) {
                rotationListener.onRotation(totalRotation, delta, rotationCenterX, rotationCenterY);
            }
        }
        
        return true; // Consume the event
    }
    
    private boolean handlePointerUp(MotionEvent event) {
        int pointerId = event.getPointerId(event.getActionIndex());
        
        // Check if secondary pointer is being released
        if (pointerId == secondaryPointerId) {
            if (isRotating && rotationListener != null) {
                rotationListener.onRotationEnd(totalRotation);
            }
            resetSecondaryPointer();
        }
        // Check if primary pointer is being released
        else if (pointerId == primaryPointerId) {
            if (isRotating && rotationListener != null) {
                rotationListener.onRotationEnd(totalRotation);
            }
            resetPrimaryPointer();
        }
        
        return isRotating;
    }
    
    private boolean handleActionUp(MotionEvent event) {
        if (isRotating && rotationListener != null) {
            rotationListener.onRotationEnd(totalRotation);
        }
        
        reset();
        return isRotating;
    }
    
    /**
     * Calculate angle between two points relative to horizontal axis
     * @param x1 First point X
     * @param y1 First point Y
     * @param x2 Second point X
     * @param y2 Second point Y
     * @return Angle in degrees
     */
    private float calculateAngle(float x1, float y1, float x2, float y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
    }
    
    /**
     * Calculate rotation delta between two angles, handling wraparound
     * @param fromAngle Starting angle in degrees
     * @param toAngle Ending angle in degrees
     * @return Delta rotation in degrees
     */
    private float calculateRotationDelta(float fromAngle, float toAngle) {
        float delta = toAngle - fromAngle;
        
        // Normalize to [-180, 180] range
        while (delta > 180) {
            delta -= 360;
        }
        while (delta < -180) {
            delta += 360;
        }
        
        return delta;
    }
    
    /**
     * Check if currently rotating
     */
    public boolean isRotating() {
        return isRotating;
    }
    
    /**
     * Get total rotation in degrees
     */
    public float getTotalRotation() {
        return totalRotation;
    }
    
    /**
     * Get current rotation angle in degrees
     */
    public float getCurrentAngle() {
        return currentAngle;
    }
    
    /**
     * Get rotation center X coordinate
     */
    public float getRotationCenterX() {
        return rotationCenterX;
    }
    
    /**
     * Get rotation center Y coordinate
     */
    public float getRotationCenterY() {
        return rotationCenterY;
    }
    
    /**
     * Check if rotating clockwise
     */
    public boolean isRotatingClockwise() {
        return totalRotation > 0;
    }
    
    /**
     * Check if rotating counter-clockwise
     */
    public boolean isRotatingCounterClockwise() {
        return totalRotation < 0;
    }
    
    /**
     * Get rotation direction as a string
     * @return "CLOCKWISE", "COUNTER_CLOCKWISE", or "NONE"
     */
    public String getRotationDirection() {
        if (Math.abs(totalRotation) < ROTATION_THRESHOLD) {
            return "NONE";
        }
        return totalRotation > 0 ? "CLOCKWISE" : "COUNTER_CLOCKWISE";
    }
    
    /**
     * Check if rotation exceeds a threshold
     */
    public boolean isRotationAboveThreshold(float threshold) {
        return Math.abs(totalRotation) >= threshold;
    }
    
    /**
     * Get rotation in radians
     */
    public float getTotalRotationRadians() {
        return (float) Math.toRadians(totalRotation);
    }
    
    /**
     * Get normalized rotation angle (0-360 degrees)
     */
    public float getNormalizedAngle() {
        float normalized = totalRotation % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }
    
    /**
     * Check if rotation is near a specific angle
     * @param targetAngle Target angle in degrees
     * @param tolerance Tolerance range in degrees
     */
    public boolean isRotationNear(float targetAngle, float tolerance) {
        float diff = Math.abs(normalizeAngleDifference(totalRotation, targetAngle));
        return diff <= tolerance;
    }
    
    /**
     * Normalize angle difference to shortest path
     */
    private float normalizeAngleDifference(float angle1, float angle2) {
        float diff = angle2 - angle1;
        while (diff > 180) {
            diff -= 360;
        }
        while (diff < -180) {
            diff += 360;
        }
        return diff;
    }
    
    private void resetSecondaryPointer() {
        secondaryPointerId = INVALID_POINTER_ID;
        secondaryStartX = 0;
        secondaryStartY = 0;
        initialAngle = 0;
        currentAngle = 0;
        
        // If primary pointer still exists, we can potentially continue with it
        if (primaryPointerId != INVALID_POINTER_ID) {
            isRotating = false;
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
        isRotating = false;
        totalRotation = 0;
        currentAngle = 0;
        rotationCenterX = 0;
        rotationCenterY = 0;
    }
    
    /**
     * Set custom rotation threshold
     */
    public void setRotationThreshold(float threshold) {
        // In a real implementation, this would update the ROTATION_THRESHOLD constant
    }
}
