/**
 * JavaScript Gesture Bridge Templates for WebView Integration
 * 
 * This file contains comprehensive JavaScript code templates for integrating
 * with the Android JavaScript Gesture Bridge. These templates provide:
 * - Gesture event capture and transmission
 * - State synchronization between WebView and native
 * - Event batching for performance
 * - Error handling and fallback mechanisms
 * - Gesture recognition utilities
 */

(function() {
    'use strict';
    
    // Bridge configuration
    const BRIDGE_CONFIG = {
        batchSize: 10,
        batchTimeout: 50,
        maxEvents: 1000,
        retryAttempts: 3,
        retryDelay: 1000,
        enabled: true
    };
    
    // Global bridge instance
    let AndroidGestureBridge = null;
    let eventQueue = [];
    let isProcessing = false;
    let gestureState = new Map();
    let eventListeners = new Map();
    
    /**
     * Initialize the JavaScript Gesture Bridge
     */
    function initializeBridge() {
        try {
            // Check if Android bridge is available
            if (typeof Android !== 'undefined' && AndroidGestureBridge) {
                console.log('Android Gesture Bridge detected');
                
                // Set up event listeners
                setupGestureListeners();
                setupTouchListeners();
                setupGestureStateListeners();
                
                // Sync initial state
                syncGestureState();
                
                // Start batch processing
                startBatchProcessor();
                
                // Notify initialization complete
                if (window.GestureBridge && window.GestureBridge.onInitialized) {
                    window.GestureBridge.onInitialized();
                }
                
                return true;
            } else {
                console.warn('Android Gesture Bridge not available');
                return false;
            }
        } catch (error) {
            console.error('Failed to initialize gesture bridge:', error);
            return false;
        }
    }
    
    /**
     * Send gesture event to Android bridge
     */
    function sendGestureEvent(event) {
        if (!BRIDGE_CONFIG.enabled) return false;
        
        try {
            const gestureEvent = {
                id: generateEventId(),
                type: event.type,
                x: event.x || 0,
                y: event.y || 0,
                deltaX: event.deltaX || 0,
                deltaY: event.deltaY || 0,
                scale: event.scale || 1.0,
                rotation: event.rotation || 0,
                timestamp: Date.now(),
                pointerCount: event.pointerCount || 1,
                properties: event.properties || {}
            };
            
            eventQueue.push(gestureEvent);
            
            // Process batch if size limit reached
            if (eventQueue.length >= BRIDGE_CONFIG.batchSize) {
                processEventBatch();
            }
            
            // Emit event to local listeners
            emitLocalEvent('gesture', gestureEvent);
            
            return true;
        } catch (error) {
            console.error('Failed to send gesture event:', error);
            return false;
        }
    }
    
    /**
     * Send batch of events to Android bridge
     */
    function sendBatchEvents(events) {
        if (!BRIDGE_CONFIG.enabled || !events.length) return false;
        
        try {
            const batchData = events.map(event => ({
                id: generateEventId(),
                type: event.type,
                x: event.x || 0,
                y: event.y || 0,
                deltaX: event.deltaX || 0,
                deltaY: event.deltaY || 0,
                scale: event.scale || 1.0,
                rotation: event.rotation || 0,
                timestamp: Date.now(),
                pointerCount: event.pointerCount || 1,
                properties: event.properties || {}
            }));
            
            const jsonData = JSON.stringify(batchData);
            const response = AndroidGestureBridge.receiveBatchEvents(jsonData);
            
            if (response && response.success) {
                console.log('Batch events sent successfully');
                emitLocalEvent('batch', batchData);
                return true;
            } else {
                console.warn('Failed to send batch events:', response);
                return false;
            }
        } catch (error) {
            console.error('Failed to send batch events:', error);
            return false;
        }
    }
    
    /**
     * Update gesture state in Android bridge
     */
    function updateGestureState(key, value) {
        try {
            const stateData = {
                key: key,
                value: value
            };
            
            const response = AndroidGestureBridge.updateGestureState(JSON.stringify(stateData));
            
            if (response && response.success) {
                gestureState.set(key, value);
                emitLocalEvent('stateChanged', { key, value, oldValue: gestureState.get(key) });
                return true;
            } else {
                console.warn('Failed to update gesture state:', response);
                return false;
            }
        } catch (error) {
            console.error('Failed to update gesture state:', error);
            return false;
        }
    }
    
    /**
     * Sync gesture state with Android bridge
     */
    function syncGestureState() {
        try {
            const response = AndroidGestureBridge.syncGestureState();
            
            if (response && response.success) {
                console.log('Gesture state sync initiated');
                return true;
            } else {
                console.warn('Failed to sync gesture state:', response);
                return false;
            }
        } catch (error) {
            console.error('Failed to sync gesture state:', error);
            return false;
        }
    }
    
    /**
     * Process event batch and send to Android
     */
    function processEventBatch() {
        if (isProcessing || eventQueue.length === 0) return;
        
        isProcessing = true;
        
        try {
            const batch = eventQueue.splice(0, BRIDGE_CONFIG.batchSize);
            
            if (batch.length > 0) {
                const response = sendBatchEventsWithRetry(batch);
                
                if (response) {
                    console.log(`Processed batch of ${batch.length} events`);
                }
            }
        } finally {
            isProcessing = false;
        }
    }
    
    /**
     * Send batch events with retry logic
     */
    function sendBatchEventsWithRetry(events, attempts = 0) {
        try {
            const jsonData = JSON.stringify(events);
            const response = AndroidGestureBridge.receiveBatchEvents(jsonData);
            
            if (response && response.success) {
                return response;
            } else if (attempts < BRIDGE_CONFIG.retryAttempts) {
                console.warn(`Batch send failed, retrying (${attempts + 1}/${BRIDGE_CONFIG.retryAttempts})`);
                setTimeout(() => {
                    return sendBatchEventsWithRetry(events, attempts + 1);
                }, BRIDGE_CONFIG.retryDelay);
            } else {
                console.error('Failed to send batch after all retry attempts');
                return null;
            }
        } catch (error) {
            console.error('Error sending batch events:', error);
            
            if (attempts < BRIDGE_CONFIG.retryAttempts) {
                setTimeout(() => {
                    return sendBatchEventsWithRetry(events, attempts + 1);
                }, BRIDGE_CONFIG.retryDelay);
            } else {
                return null;
            }
        }
    }
    
    /**
     * Start batch processing timer
     */
    function startBatchProcessor() {
        setInterval(() => {
            processEventBatch();
        }, BRIDGE_CONFIG.batchTimeout);
    }
    
    /**
     * Set up gesture event listeners
     */
    function setupGestureListeners() {
        // Touch events
        document.addEventListener('touchstart', handleTouchStart, { passive: false });
        document.addEventListener('touchend', handleTouchEnd, { passive: false });
        document.addEventListener('touchmove', handleTouchMove, { passive: false });
        
        // Mouse events for desktop testing
        document.addEventListener('mousedown', handleMouseDown);
        document.addEventListener('mouseup', handleMouseUp);
        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('wheel', handleWheel);
        
        // Prevent default touch behaviors for gesture recognition
        document.addEventListener('touchstart', preventDefault, { passive: false });
        document.addEventListener('touchmove', preventDefault, { passive: false });
        document.addEventListener('touchend', preventDefault, { passive: false });
    }
    
    /**
     * Handle touch start events
     */
    function handleTouchStart(event) {
        const touch = event.touches[0];
        const gestureEvent = {
            type: 'touch_start',
            x: touch.clientX,
            y: touch.clientY,
            timestamp: Date.now(),
            pointerCount: event.touches.length
        };
        
        sendGestureEvent(gestureEvent);
    }
    
    /**
     * Handle touch move events
     */
    function handleTouchMove(event) {
        if (event.touches.length === 1) {
            const touch = event.touches[0];
            const gestureEvent = {
                type: 'touch_move',
                x: touch.clientX,
                y: touch.clientY,
                deltaX: event.touches[0].clientX - (event.touches[0].clientX - event.movementX),
                deltaY: event.touches[0].clientY - (event.touches[0].clientY - event.movementY),
                timestamp: Date.now(),
                pointerCount: event.touches.length
            };
            
            sendGestureEvent(gestureEvent);
        }
    }
    
    /**
     * Handle touch end events
     */
    function handleTouchEnd(event) {
        const touch = event.changedTouches[0];
        const gestureEvent = {
            type: 'touch_end',
            x: touch.clientX,
            y: touch.clientY,
            timestamp: Date.now(),
            pointerCount: event.touches.length
        };
        
        sendGestureEvent(gestureEvent);
    }
    
    /**
     * Handle mouse events for desktop testing
     */
    function handleMouseDown(event) {
        const gestureEvent = {
            type: 'mouse_down',
            x: event.clientX,
            y: event.clientY,
            timestamp: Date.now(),
            pointerCount: 1
        };
        
        sendGestureEvent(gestureEvent);
    }
    
    function handleMouseUp(event) {
        const gestureEvent = {
            type: 'mouse_up',
            x: event.clientX,
            y: event.clientY,
            timestamp: Date.now(),
            pointerCount: 1
        };
        
        sendGestureEvent(gestureEvent);
    }
    
    function handleMouseMove(event) {
        const gestureEvent = {
            type: 'mouse_move',
            x: event.clientX,
            y: event.clientY,
            deltaX: event.movementX,
            deltaY: event.movementY,
            timestamp: Date.now(),
            pointerCount: 1
        };
        
        sendGestureEvent(gestureEvent);
    }
    
    function handleWheel(event) {
        const gestureEvent = {
            type: 'wheel',
            x: event.clientX,
            y: event.clientY,
            deltaX: event.deltaX,
            deltaY: event.deltaY,
            timestamp: Date.now(),
            pointerCount: 1
        };
        
        sendGestureEvent(gestureEvent);
    }
    
    /**
     * Prevent default touch behaviors
     */
    function preventDefault(event) {
        event.preventDefault();
    }
    
    /**
     * Set up gesture state listeners
     */
    function setupGestureStateListeners() {
        // Listen for state changes from Android
        window.GestureBridge = window.GestureBridge || {};
        
        window.GestureBridge.onStateSync = function(state) {
            console.log('Received state sync from Android:', state);
            
            // Update local state
            for (const [key, value] of Object.entries(state)) {
                gestureState.set(key, value);
                emitLocalEvent('stateChanged', { key, value, oldValue: gestureState.get(key) });
            }
        };
        
        window.GestureBridge.onError = function(error, context) {
            console.error('Gesture bridge error:', error, 'Context:', context);
            emitLocalEvent('error', { error, context });
        };
    }
    
    /**
     * Generate unique event ID
     */
    function generateEventId() {
        return 'evt_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
    
    /**
     * Emit event to local listeners
     */
    function emitLocalEvent(type, data) {
        const listeners = eventListeners.get(type);
        if (listeners) {
            listeners.forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error('Error in gesture event listener:', error);
                }
            });
        }
    }
    
    /**
     * Add event listener
     */
    function addEventListener(type, callback) {
        if (!eventListeners.has(type)) {
            eventListeners.set(type, []);
        }
        eventListeners.get(type).push(callback);
    }
    
    /**
     * Remove event listener
     */
    function removeEventListener(type, callback) {
        const listeners = eventListeners.get(type);
        if (listeners) {
            const index = listeners.indexOf(callback);
            if (index > -1) {
                listeners.splice(index, 1);
            }
        }
    }
    
    /**
     * Gesture recognition utilities
     */
    const GestureRecognition = {
        // Tap detection
        tap: {
            maxDistance: 10,
            maxDuration: 200,
            detect: function(startEvent, endEvent) {
                const distance = Math.sqrt(
                    Math.pow(endEvent.x - startEvent.x, 2) + 
                    Math.pow(endEvent.y - startEvent.y, 2)
                );
                const duration = endEvent.timestamp - startEvent.timestamp;
                
                return distance <= this.maxDistance && duration <= this.maxDuration;
            }
        },
        
        // Swipe detection
        swipe: {
            minDistance: 50,
            minVelocity: 0.3,
            detect: function(startEvent, endEvent, moveEvents) {
                const distance = Math.sqrt(
                    Math.pow(endEvent.x - startEvent.x, 2) + 
                    Math.pow(endEvent.y - startEvent.y, 2)
                );
                const duration = endEvent.timestamp - startEvent.timestamp;
                const velocity = distance / duration;
                
                if (distance < this.minDistance) return null;
                
                const direction = Math.atan2(endEvent.y - startEvent.y, endEvent.x - startEvent.x);
                
                let directionName = 'unknown';
                const degrees = direction * 180 / Math.PI;
                
                if (degrees >= -45 && degrees < 45) directionName = 'right';
                else if (degrees >= 45 && degrees < 135) directionName = 'down';
                else if (degrees >= 135 || degrees < -135) directionName = 'left';
                else if (degrees >= -135 && degrees < -45) directionName = 'up';
                
                return {
                    type: 'swipe',
                    direction: directionName,
                    distance: distance,
                    duration: duration,
                    velocity: velocity,
                    startX: startEvent.x,
                    startY: startEvent.y,
                    endX: endEvent.x,
                    endY: endEvent.y
                };
            }
        },
        
        // Pinch detection
        pinch: {
            minScale: 0.1,
            detect: function(touches) {
                if (touches.length < 2) return null;
                
                const touch1 = touches[0];
                const touch2 = touches[1];
                const distance = Math.sqrt(
                    Math.pow(touch2.clientX - touch1.clientX, 2) + 
                    Math.pow(touch2.clientY - touch1.clientY, 2)
                );
                
                return {
                    type: 'pinch',
                    distance: distance,
                    scale: distance / 100, // Normalize against initial distance
                    centerX: (touch1.clientX + touch2.clientX) / 2,
                    centerY: (touch1.clientY + touch2.clientY) / 2
                };
            }
        }
    };
    
    /**
     * Public API
     */
    window.JavaScriptGestureBridge = {
        initialize: initializeBridge,
        sendGestureEvent: sendGestureEvent,
        updateGestureState: updateGestureState,
        syncGestureState: syncGestureState,
        addEventListener: addEventListener,
        removeEventListener: removeEventListener,
        GestureRecognition: GestureRecognition,
        
        // Configuration
        config: BRIDGE_CONFIG,
        
        // State
        getGestureState: () => Object.fromEntries(gestureState),
        isEnabled: () => BRIDGE_CONFIG.enabled,
        setEnabled: (enabled) => { BRIDGE_CONFIG.enabled = enabled; },
        
        // Event queue management
        getQueueSize: () => eventQueue.length,
        clearQueue: () => { eventQueue = []; },
        forceBatchProcess: processEventBatch
    };
    
    // Auto-initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeBridge);
    } else {
        initializeBridge();
    }
    
    // Export for module systems
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = window.JavaScriptGestureBridge;
    }
    
})();

/**
 * HTML Template for Testing WebView Integration
 */
const HTML_TEMPLATE = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gesture Bridge Test</title>
    <style>
        body {
            margin: 0;
            padding: 20px;
            font-family: Arial, sans-serif;
            background: linear-gradient(45deg, #ff6b6b, #4ecdc4);
            height: 100vh;
            overflow: hidden;
        }
        
        .container {
            background: rgba(255, 255, 255, 0.9);
            border-radius: 10px;
            padding: 20px;
            height: calc(100vh - 40px);
            overflow-y: auto;
        }
        
        .gesture-area {
            background: #f0f0f0;
            border: 2px solid #333;
            border-radius: 10px;
            height: 300px;
            margin: 20px 0;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            color: #666;
        }
        
        .status {
            padding: 10px;
            margin: 10px 0;
            border-radius: 5px;
            font-weight: bold;
        }
        
        .status.connected { background: #d4edda; color: #155724; }
        .status.disconnected { background: #f8d7da; color: #721c24; }
        .status.active { background: #d1ecf1; color: #0c5460; }
        
        .log {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 5px;
            padding: 10px;
            height: 200px;
            overflow-y: auto;
            font-family: monospace;
            font-size: 12px;
        }
        
        .controls {
            display: flex;
            gap: 10px;
            margin: 20px 0;
        }
        
        button {
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            background: #007bff;
            color: white;
            cursor: pointer;
            font-size: 14px;
        }
        
        button:hover {
            background: #0056b3;
        }
        
        button.danger {
            background: #dc3545;
        }
        
        button.danger:hover {
            background: #c82333;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>JavaScript Gesture Bridge Test</h1>
        
        <div id="status" class="status disconnected">
            Status: Initializing...
        </div>
        
        <div class="gesture-area" id="gestureArea">
            Gesture Area - Touch/Click Here
        </div>
        
        <div class="controls">
            <button onclick="testTap()">Test Tap</button>
            <button onclick="testSwipe()">Test Swipe</button>
            <button onclick="testPinch()">Test Pinch</button>
            <button onclick="testState()">Test State</button>
            <button onclick="clearLog()" class="danger">Clear Log</button>
        </div>
        
        <h3>Event Log:</h3>
        <div class="log" id="eventLog"></div>
    </div>

    <script src="gesture-bridge.js"></script>
    <script>
        const gestureArea = document.getElementById('gestureArea');
        const eventLog = document.getElementById('eventLog');
        const statusDiv = document.getElementById('status');
        
        let touchStartTime = null;
        let touchStartPos = null;
        let lastTapTime = 0;
        
        // Initialize event listeners
        document.addEventListener('DOMContentLoaded', () => {
            updateStatus('Connected', 'connected');
            log('Gesture bridge initialized');
        });
        
        // Update status display
        function updateStatus(message, className) {
            statusDiv.textContent = 'Status: ' + message;
            statusDiv.className = 'status ' + className;
        }
        
        // Log events
        function log(message) {
            const timestamp = new Date().toLocaleTimeString();
            const logEntry = document.createElement('div');
            logEntry.textContent = '[' + timestamp + '] ' + message;
            eventLog.appendChild(logEntry);
            eventLog.scrollTop = eventLog.scrollHeight;
        }
        
        // Clear log
        function clearLog() {
            eventLog.innerHTML = '';
        }
        
        // Test functions
        function testTap() {
            const event = {
                type: 'tap',
                x: Math.random() * 400,
                y: Math.random() * 300,
                timestamp: Date.now(),
                pointerCount: 1
            };
            
            if (window.JavaScriptGestureBridge.sendGestureEvent(event)) {
                log('Test tap sent: (' + event.x.toFixed(0) + ', ' + event.y.toFixed(0) + ')');
            } else {
                log('Failed to send test tap');
            }
        }
        
        function testSwipe() {
            const startEvent = {
                type: 'touch_start',
                x: 50,
                y: 150,
                timestamp: Date.now()
            };
            
            const endEvent = {
                type: 'touch_end',
                x: 350,
                y: 150,
                timestamp: Date.now() + 500
            };
            
            window.JavaScriptGestureBridge.sendGestureEvent(startEvent);
            setTimeout(() => {
                window.JavaScriptGestureBridge.sendGestureEvent(endEvent);
                log('Test swipe sent: (50,150) -> (350,150)');
            }, 100);
        }
        
        function testPinch() {
            const pinchEvent = {
                type: 'pinch',
                x: 200,
                y: 150,
                scale: 1.5,
                timestamp: Date.now(),
                pointerCount: 2
            };
            
            if (window.JavaScriptGestureBridge.sendGestureEvent(pinchEvent)) {
                log('Test pinch sent: scale=' + pinchEvent.scale);
            } else {
                log('Failed to send test pinch');
            }
        }
        
        function testState() {
            const stateKey = 'test_state_' + Date.now();
            const stateValue = 'test_value_' + Math.random().toString(36).substr(2, 5);
            
            if (window.JavaScriptGestureBridge.updateGestureState(stateKey, stateValue)) {
                log('Test state updated: ' + stateKey + ' = ' + stateValue);
            } else {
                log('Failed to update test state');
            }
        }
        
        // Add gesture bridge event listeners
        if (window.JavaScriptGestureBridge) {
            window.JavaScriptGestureBridge.addEventListener('gesture', (event) => {
                log('Gesture event: ' + event.type + ' at (' + event.x + ', ' + event.y + ')');
            });
            
            window.JavaScriptGestureBridge.addEventListener('batch', (events) => {
                log('Batch processed: ' + events.length + ' events');
            });
            
            window.JavaScriptGestureBridge.addEventListener('stateChanged', (change) => {
                log('State changed: ' + change.key + ' = ' + change.value);
            });
            
            window.JavaScriptGestureBridge.addEventListener('error', (error) => {
                log('Error: ' + error.error + ' (' + error.context + ')');
            });
        }
    </script>
</body>
</html>
`;

// Export HTML template
if (typeof window !== 'undefined') {
    window.HTML_TEMPLATE = HTML_TEMPLATE;
}