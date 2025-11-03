package com.example.wallpaper.bridge;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaScript Gesture Bridge for Android WebView
 * 
 * This bridge facilitates seamless communication between native Android and WebView JavaScript
 * for gesture event handling. It provides:
 * - WebView JavaScript interface for gesture events
 * - JSON serialization/deserialization for gesture data
 * - Event batching for performance optimization
 * - Gesture state synchronization
 * - Robust error handling for JS bridge failures
 */
public class JavascriptGestureBridge {
    private static final String TAG = "GestureBridge";
    private static final String JAVASCRIPT_INTERFACE_NAME = "AndroidGestureBridge";
    
    // Event batching configuration
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 50;
    private static final int MAX_BATCH_SIZE = 50;
    
    // Gesture event types
    public enum GestureType {
        TAP("tap"),
        DOUBLE_TAP("double_tap"),
        LONG_PRESS("long_press"),
        SWIPE("swipe"),
        PINCH("pinch"),
        ROTATION("rotation"),
        DRAG("drag"),
        SCALE("scale");
        
        private final String type;
        
        GestureType(String type) {
            this.type = type;
        }
        
        public String getType() {
            return type;
        }
    }
    
    // Gesture event data structure
    public static class GestureEvent {
        public String id;
        public String type;
        public float x;
        public float y;
        public float deltaX;
        public float deltaY;
        public float scale;
        public float rotation;
        public long timestamp;
        public int pointerCount;
        public Map<String, Object> properties;
        
        public GestureEvent(String type) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.properties = new HashMap<>();
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("type", type);
            json.put("x", x);
            json.put("y", y);
            json.put("deltaX", deltaX);
            json.put("deltaY", deltaY);
            json.put("scale", scale);
            json.put("rotation", rotation);
            json.put("timestamp", timestamp);
            json.put("pointerCount", pointerCount);
            
            if (properties != null && !properties.isEmpty()) {
                JSONObject propsJson = new JSONObject();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    propsJson.put(entry.getKey(), entry.getValue());
                }
                json.put("properties", propsJson);
            }
            
            return json;
        }
        
        public static GestureEvent fromJSON(JSONObject json) throws JSONException {
            GestureEvent event = new GestureEvent(json.getString("type"));
            event.id = json.getString("id");
            event.x = (float) json.optDouble("x", 0.0);
            event.y = (float) json.optDouble("y", 0.0);
            event.deltaX = (float) json.optDouble("deltaX", 0.0);
            event.deltaY = (float) json.optDouble("deltaY", 0.0);
            event.scale = (float) json.optDouble("scale", 1.0);
            event.rotation = (float) json.optDouble("rotation", 0.0);
            event.timestamp = json.optLong("timestamp", System.currentTimeMillis());
            event.pointerCount = json.optInt("pointerCount", 1);
            
            JSONObject propsJson = json.optJSONObject("properties");
            if (propsJson != null) {
                Iterator<String> keys = propsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    event.properties.put(key, propsJson.opt(key));
                }
            }
            
            return event;
        }
    }
    
    // Gesture state management
    public static class GestureState {
        private final Map<String, Object> state = new HashMap<>();
        private final AtomicBoolean isProcessing = new AtomicBoolean(false);
        private final long lastUpdate = System.currentTimeMillis();
        
        public synchronized void update(String key, Object value) {
            state.put(key, value);
            // Note: In a real implementation, you'd want to track the actual last update time
        }
        
        public synchronized Object get(String key) {
            return state.get(key);
        }
        
        public synchronized Map<String, Object> getAll() {
            return new HashMap<>(state);
        }
        
        public boolean isProcessing() {
            return isProcessing.get();
        }
        
        public void setProcessing(boolean processing) {
            isProcessing.set(processing);
        }
    }
    
    // Interface for gesture event callbacks
    public interface GestureEventListener {
        void onGestureEvent(GestureEvent event);
        void onBatchEvents(List<GestureEvent> events);
        void onError(String error, String context);
        void onStateChanged(String key, Object oldValue, Object newValue);
    }
    
    // Internal components
    private final WebView webView;
    private final ExecutorService executorService;
    private final Queue<GestureEvent> eventQueue;
    private final Timer batchTimer;
    private final GestureState gestureState;
    private GestureEventListener listener;
    private final AtomicBoolean isEnabled = new AtomicBoolean(true);
    private final Object batchLock = new Object();
    
    public JavascriptGestureBridge(WebView webView) {
        this.webView = webView;
        this.executorService = Executors.newCachedThreadPool();
        this.eventQueue = new LinkedList<>();
        this.batchTimer = new Timer(true);
        this.gestureState = new GestureState();
        
        // Start batch processing timer
        startBatchTimer();
    }
    
    /**
     * Set the gesture event listener
     */
    public void setGestureEventListener(GestureEventListener listener) {
        this.listener = listener;
    }
    
    /**
     * Initialize the JavaScript bridge in the WebView
     */
    public void initialize() {
        try {
            if (webView != null) {
                webView.addJavascriptInterface(this, JAVASCRIPT_INTERFACE_NAME);
                Log.d(TAG, "JavaScript interface initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize JavaScript interface", e);
        }
    }
    
    /**
     * Enable or disable gesture bridge
     */
    public void setEnabled(boolean enabled) {
        isEnabled.set(enabled);
        Log.d(TAG, "Gesture bridge " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Clear all pending events
     */
    public void clearEvents() {
        synchronized (batchLock) {
            eventQueue.clear();
        }
    }
    
    /**
     * Get current gesture state
     */
    public GestureState getGestureState() {
        return gestureState;
    }
    
    /**
     * JavaScript interface method for receiving gesture events
     */
    @JavascriptInterface
    public String receiveGestureEvent(String eventData) {
        if (!isEnabled.get()) {
            return createResponse(false, "Bridge is disabled");
        }
        
        executorService.execute(() -> {
            try {
                GestureEvent event = parseGestureEvent(eventData);
                if (event != null) {
                    processGestureEvent(event);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse gesture event", e);
                notifyError("Failed to parse gesture event: " + e.getMessage(), "receiveGestureEvent");
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error processing gesture event", e);
                notifyError("Unexpected error: " + e.getMessage(), "receiveGestureEvent");
            }
        });
        
        return createResponse(true, "Event received");
    }
    
    /**
     * JavaScript interface method for batch gesture events
     */
    @JavascriptInterface
    public String receiveBatchEvents(String eventsData) {
        if (!isEnabled.get()) {
            return createResponse(false, "Bridge is disabled");
        }
        
        executorService.execute(() -> {
            try {
                List<GestureEvent> events = parseBatchEvents(eventsData);
                if (events != null && !events.isEmpty()) {
                    processBatchEvents(events);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse batch events", e);
                notifyError("Failed to parse batch events: " + e.getMessage(), "receiveBatchEvents");
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error processing batch events", e);
                notifyError("Unexpected error: " + e.getMessage(), "receiveBatchEvents");
            }
        });
        
        return createResponse(true, "Batch received");
    }
    
    /**
     * JavaScript interface method for gesture state updates
     */
    @JavascriptInterface
    public String updateGestureState(String stateData) {
        if (!isEnabled.get()) {
            return createResponse(false, "Bridge is disabled");
        }
        
        executorService.execute(() -> {
            try {
                JSONObject stateJson = new JSONObject(stateData);
                String key = stateJson.getString("key");
                Object oldValue = gestureState.get(key);
                Object newValue = stateJson.opt("value");
                
                gestureState.update(key, newValue);
                
                if (listener != null) {
                    listener.onStateChanged(key, oldValue, newValue);
                }
                
                Log.d(TAG, "Gesture state updated: " + key);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse gesture state", e);
                notifyError("Failed to parse gesture state: " + e.getMessage(), "updateGestureState");
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error updating gesture state", e);
                notifyError("Unexpected error: " + e.getMessage(), "updateGestureState");
            }
        });
        
        return createResponse(true, "State updated");
    }
    
    /**
     * JavaScript interface method for gesture synchronization
     */
    @JavascriptInterface
    public String syncGestureState() {
        if (!isEnabled.get()) {
            return createResponse(false, "Bridge is disabled");
        }
        
        executorService.execute(() -> {
            try {
                Map<String, Object> state = gestureState.getAll();
                JSONObject stateJson = new JSONObject();
                
                for (Map.Entry<String, Object> entry : state.entrySet()) {
                    stateJson.put(entry.getKey(), entry.getValue());
                }
                
                // Send state back to JavaScript
                executeJavaScript("window.GestureBridge.onStateSync(" + stateJson.toString() + ")");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to sync gesture state", e);
                notifyError("Failed to sync gesture state: " + e.getMessage(), "syncGestureState");
            }
        });
        
        return createResponse(true, "Sync initiated");
    }
    
    /**
     * Process individual gesture event
     */
    private void processGestureEvent(GestureEvent event) {
        synchronized (batchLock) {
            eventQueue.offer(event);
            
            if (eventQueue.size() >= BATCH_SIZE) {
                processBatch();
            }
        }
        
        if (listener != null) {
            listener.onGestureEvent(event);
        }
    }
    
    /**
     * Process batch of gesture events
     */
    private void processBatchEvents(List<GestureEvent> events) {
        synchronized (batchLock) {
            for (GestureEvent event : events) {
                eventQueue.offer(event);
            }
            processBatch();
        }
        
        if (listener != null) {
            listener.onBatchEvents(new ArrayList<>(events));
        }
    }
    
    /**
     * Process the current event batch
     */
    private void processBatch() {
        gestureState.setProcessing(true);
        
        try {
            List<GestureEvent> batch = new ArrayList<>();
            
            synchronized (batchLock) {
                int count = Math.min(eventQueue.size(), BATCH_SIZE);
                for (int i = 0; i < count; i++) {
                    batch.add(eventQueue.poll());
                }
            }
            
            if (!batch.isEmpty()) {
                Log.d(TAG, "Processed batch of " + batch.size() + " events");
            }
        } finally {
            gestureState.setProcessing(false);
        }
    }
    
    /**
     * Start batch processing timer
     */
    private void startBatchTimer() {
        batchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (batchLock) {
                    if (!eventQueue.isEmpty()) {
                        processBatch();
                    }
                }
            }
        }, BATCH_TIMEOUT_MS, BATCH_TIMEOUT_MS);
    }
    
    /**
     * Parse gesture event from JSON
     */
    private GestureEvent parseGestureEvent(String eventData) throws JSONException {
        JSONObject json = new JSONObject(eventData);
        return GestureEvent.fromJSON(json);
    }
    
    /**
     * Parse batch events from JSON
     */
    private List<GestureEvent> parseBatchEvents(String eventsData) throws JSONException {
        JSONArray eventsArray = new JSONArray(eventsData);
        List<GestureEvent> events = new ArrayList<>();
        
        for (int i = 0; i < eventsArray.length(); i++) {
            JSONObject eventJson = eventsArray.getJSONObject(i);
            events.add(GestureEvent.fromJSON(eventJson));
        }
        
        return events;
    }
    
    /**
     * Create standardized response
     */
    private String createResponse(boolean success, String message) {
        try {
            JSONObject response = new JSONObject();
            response.put("success", success);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());
            return response.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create response", e);
            return "{\"success\":false,\"message\":\"Failed to create response\"}";
        }
    }
    
    /**
     * Execute JavaScript code in WebView
     */
    private void executeJavaScript(String script) {
        if (webView != null) {
            webView.post(() -> {
                try {
                    webView.evaluateJavascript(script, null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute JavaScript", e);
                }
            });
        }
    }
    
    /**
     * Notify error to listener
     */
    private void notifyError(String error, String context) {
        if (listener != null) {
            listener.onError(error, context);
        }
        Log.e(TAG, "Error in " + context + ": " + error);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        try {
            batchTimer.cancel();
            executorService.shutdown();
            clearEvents();
            Log.d(TAG, "Gesture bridge cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
    
    /**
     * Get JavaScript bridge configuration
     */
    public String getBridgeConfiguration() {
        try {
            JSONObject config = new JSONObject();
            config.put("interfaceName", JAVASCRIPT_INTERFACE_NAME);
            config.put("batchSize", BATCH_SIZE);
            config.put("batchTimeout", BATCH_TIMEOUT_MS);
            config.put("maxBatchSize", MAX_BATCH_SIZE);
            config.put("enabled", isEnabled.get());
            return config.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create bridge configuration", e);
            return "{}";
        }
    }
}