package com.example.wallpaper.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SharedPreferences manager for wallpaper settings with validation and defaults.
 * 
 * This class provides a centralized location for managing all wallpaper-related
 * preferences including:
 * - Visual settings (colors, themes, effects)
 * - Performance settings (quality, frame rate, memory management)
 * - User interaction settings (gesture controls, haptic feedback)
 * - Feature toggles (animations, transitions, special effects)
 * 
 * All preferences are validated with appropriate defaults and bounds checking
 * to ensure stable operation across different devices and Android versions.
 * 
 * @author Android 3D Live Wallpaper Team
 */
public class WallpaperPrefs {
    
    private static final String TAG = "WallpaperPrefs";
    
    // Preferences file name
    private static final String PREFS_FILE_NAME = "wallpaper_prefs";
    
    // Preference keys - Visual Settings
    private static final String KEY_THEME_ID = "theme_id";
    private static final String KEY_COLOR_PRIMARY = "color_primary";
    private static final String KEY_COLOR_SECONDARY = "color_secondary";
    private static final String KEY_COLOR_ACCENT = "color_accent";
    private static final String KEY_BACKGROUND_TYPE = "background_type";
    private static final String KEY_CUSTOM_BG_COLOR = "custom_bg_color";
    private static final String KEY_GRADIENT_ENABLED = "gradient_enabled";
    private static final String KEY_ANIMATION_SPEED = "animation_speed";
    
    // Preference keys - Performance Settings
    private static final String KEY_QUALITY_LEVEL = "quality_level";
    private static final String KEY_FRAME_RATE_LIMIT = "frame_rate_limit";
    private static final String KEY_ENABLE_HARDWARE_ACCEL = "enable_hw_accel";
    private static final String KEY_MEMORY_OPTIMIZATION = "memory_optimization";
    private static final String KEY_LOD_LEVEL = "lod_level";
    private static final String KEY_SHADOW_QUALITY = "shadow_quality";
    private static final String KEY_TEXTURE_QUALITY = "texture_quality";
    
    // Preference keys - Interaction Settings
    private static final String KEY_GESTURE_ENABLED = "gesture_enabled";
    private static final String KEY_TAP_TO_INTERACT = "tap_to_interact";
    private static final String KEY_SWIPE_SENSITIVITY = "swipe_sensitivity";
    private static final String KEY_ZOOM_ENABLED = "zoom_enabled";
    private static final String KEY_ROTATION_ENABLED = "rotation_enabled";
    private static final String KEY_PAN_ENABLED = "pan_enabled";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    
    // Preference keys - Feature Toggles
    private static final String KEY_PARTICLES_ENABLED = "particles_enabled";
    private static final String KEY_LIGHTING_ENABLED = "lighting_enabled";
    private static final String KEY_REFLECTIONS_ENABLED = "reflections_enabled";
    private static final String KEY_TRANSITIONS_ENABLED = "transitions_enabled";
    private static final String KEY_AUDIO_VISUALIZER = "audio_visualizer";
    private static final String KEY_BATTERY_SAVER = "battery_saver";
    
    // Preference keys - Advanced Settings
    private static final String KEY_CUSTOM_SCRIPT = "custom_script";
    private static final String KEY_MODEL_PATH = "model_path";
    private static final String KEY_TEXTURE_PACK = "texture_pack";
    private static final String KEY_SCENE_CONFIG = "scene_config";
    private static final String KEY_DEVICE_PROFILE = "device_profile";
    
    // Preference keys - MainActivity specific
    private static final String KEY_CONTENT_URL = "content_url";
    private static final String KEY_QUALITY_PRESET = "quality_preset";
    private static final String KEY_TOUCH_INTERACTIVITY = "touch_interactivity";
    private static final String KEY_FRAME_RATE = "frame_rate";
    
    // Default values
    private static final String DEFAULT_THEME_ID = "default";
    private static final String DEFAULT_COLOR_PRIMARY = "#3F51B5";
    private static final String DEFAULT_COLOR_SECONDARY = "#E91E63";
    private static final String DEFAULT_COLOR_ACCENT = "#FF5722";
    private static final String DEFAULT_BG_TYPE = "solid";
    private static final String DEFAULT_CUSTOM_BG = "#000000";
    private static final boolean DEFAULT_GRADIENT_ENABLED = false;
    private static final float DEFAULT_ANIMATION_SPEED = 1.0f;
    
    private static final int DEFAULT_QUALITY_LEVEL = 2; // Medium
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final boolean DEFAULT_HW_ACCEL = true;
    private static final boolean DEFAULT_MEMORY_OPT = true;
    private static final int DEFAULT_LOD_LEVEL = 1; // Medium
    private static final int DEFAULT_SHADOW_QUALITY = 1; // Medium
    private static final int DEFAULT_TEXTURE_QUALITY = 1; // Medium
    
    private static final boolean DEFAULT_GESTURE_ENABLED = true;
    private static final boolean DEFAULT_TAP_INTERACT = true;
    private static final float DEFAULT_SWIPE_SENS = 1.0f;
    private static final boolean DEFAULT_ZOOM_ENABLED = true;
    private static final boolean DEFAULT_ROTATION_ENABLED = true;
    private static final boolean DEFAULT_PAN_ENABLED = true;
    private static final boolean DEFAULT_HAPTIC = false;
    
    private static final boolean DEFAULT_PARTICLES = true;
    private static final boolean DEFAULT_LIGHTING = true;
    private static final boolean DEFAULT_REFLECTIONS = false;
    private static final boolean DEFAULT_TRANSITIONS = true;
    private static final boolean DEFAULT_AUDIO_VIZ = false;
    private static final boolean DEFAULT_BATTERY_SAVER = false;
    
    private static final String DEFAULT_MODEL_PATH = "";
    private static final String DEFAULT_TEXTURE_PACK = "default";
    private static final String DEFAULT_SCENE_CONFIG = "{}";
    private static final String DEFAULT_DEVICE_PROFILE = "auto";
    
    // Default values for MainActivity specific settings
    private static final String DEFAULT_CONTENT_URL = "https://example.com/default.html";
    private static final String DEFAULT_QUALITY_PRESET = "Medium";
    private static final boolean DEFAULT_TOUCH_INTERACTIVITY = true;
    private static final int DEFAULT_FRAME_RATE_VALUE = 30;
    
    // Constants for validation
    private static final int MIN_QUALITY_LEVEL = 0;
    private static final int MAX_QUALITY_LEVEL = 3;
    private static final int MIN_FRAME_RATE = 15;
    private static final int MAX_FRAME_RATE = 60;
    private static final float MIN_ANIMATION_SPEED = 0.1f;
    private static final float MAX_ANIMATION_SPEED = 3.0f;
    private static final float MIN_SWIPE_SENS = 0.1f;
    private static final float MAX_SWIPE_SENS = 3.0f;
    
    private static final int MIN_LOD_LEVEL = 0;
    private static final int MAX_LOD_LEVEL = 3;
    
    // Background types
    public enum BackgroundType {
        SOLID("solid"),
        GRADIENT("gradient"),
        TEXTURE("texture"),
        VIDEO("video"),
        CUSTOM("custom");
        
        private final String value;
        
        BackgroundType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static BackgroundType fromString(String value) {
            for (BackgroundType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return SOLID; // Default fallback
        }
    }
    
    // Quality levels
    public enum QualityLevel {
        LOW(0),
        MEDIUM(1),
        HIGH(2),
        ULTRA(3);
        
        private final int value;
        
        QualityLevel(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static QualityLevel fromInt(int value) {
            for (QualityLevel level : values()) {
                if (level.value == value) {
                    return level;
                }
            }
            return MEDIUM; // Default fallback
        }
    }
    
    // Internal state
    private final Context context;
    private final SharedPreferences prefs;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    /**
     * Creates a new WallpaperPrefs instance.
     * 
     * @param context Application context (should be application context, not activity)
     */
    public WallpaperPrefs(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
        
        initializeDefaults();
        isInitialized.set(true);
    }
    
    /**
     * Ensures all required preference keys have valid default values.
     */
    private void initializeDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        
        // Visual settings defaults
        if (!prefs.contains(KEY_THEME_ID)) {
            editor.putString(KEY_THEME_ID, DEFAULT_THEME_ID);
        }
        if (!prefs.contains(KEY_COLOR_PRIMARY)) {
            editor.putString(KEY_COLOR_PRIMARY, DEFAULT_COLOR_PRIMARY);
        }
        if (!prefs.contains(KEY_COLOR_SECONDARY)) {
            editor.putString(KEY_COLOR_SECONDARY, DEFAULT_COLOR_SECONDARY);
        }
        if (!prefs.contains(KEY_COLOR_ACCENT)) {
            editor.putString(KEY_COLOR_ACCENT, DEFAULT_COLOR_ACCENT);
        }
        if (!prefs.contains(KEY_BACKGROUND_TYPE)) {
            editor.putString(KEY_BACKGROUND_TYPE, DEFAULT_BG_TYPE);
        }
        if (!prefs.contains(KEY_CUSTOM_BG_COLOR)) {
            editor.putString(KEY_CUSTOM_BG_COLOR, DEFAULT_CUSTOM_BG);
        }
        if (!prefs.contains(KEY_GRADIENT_ENABLED)) {
            editor.putBoolean(KEY_GRADIENT_ENABLED, DEFAULT_GRADIENT_ENABLED);
        }
        if (!prefs.contains(KEY_ANIMATION_SPEED)) {
            editor.putFloat(KEY_ANIMATION_SPEED, DEFAULT_ANIMATION_SPEED);
        }
        
        // Performance settings defaults
        if (!prefs.contains(KEY_QUALITY_LEVEL)) {
            editor.putInt(KEY_QUALITY_LEVEL, DEFAULT_QUALITY_LEVEL);
        }
        if (!prefs.contains(KEY_FRAME_RATE_LIMIT)) {
            editor.putInt(KEY_FRAME_RATE_LIMIT, DEFAULT_FRAME_RATE);
        }
        if (!prefs.contains(KEY_ENABLE_HARDWARE_ACCEL)) {
            editor.putBoolean(KEY_ENABLE_HARDWARE_ACCEL, DEFAULT_HW_ACCEL);
        }
        if (!prefs.contains(KEY_MEMORY_OPTIMIZATION)) {
            editor.putBoolean(KEY_MEMORY_OPTIMIZATION, DEFAULT_MEMORY_OPT);
        }
        if (!prefs.contains(KEY_LOD_LEVEL)) {
            editor.putInt(KEY_LOD_LEVEL, DEFAULT_LOD_LEVEL);
        }
        if (!prefs.contains(KEY_SHADOW_QUALITY)) {
            editor.putInt(KEY_SHADOW_QUALITY, DEFAULT_SHADOW_QUALITY);
        }
        if (!prefs.contains(KEY_TEXTURE_QUALITY)) {
            editor.putInt(KEY_TEXTURE_QUALITY, DEFAULT_TEXTURE_QUALITY);
        }
        
        // Interaction settings defaults
        if (!prefs.contains(KEY_GESTURE_ENABLED)) {
            editor.putBoolean(KEY_GESTURE_ENABLED, DEFAULT_GESTURE_ENABLED);
        }
        if (!prefs.contains(KEY_TAP_TO_INTERACT)) {
            editor.putBoolean(KEY_TAP_TO_INTERACT, DEFAULT_TAP_INTERACT);
        }
        if (!prefs.contains(KEY_SWIPE_SENSITIVITY)) {
            editor.putFloat(KEY_SWIPE_SENSITIVITY, DEFAULT_SWIPE_SENS);
        }
        if (!prefs.contains(KEY_ZOOM_ENABLED)) {
            editor.putBoolean(KEY_ZOOM_ENABLED, DEFAULT_ZOOM_ENABLED);
        }
        if (!prefs.contains(KEY_ROTATION_ENABLED)) {
            editor.putBoolean(KEY_ROTATION_ENABLED, DEFAULT_ROTATION_ENABLED);
        }
        if (!prefs.contains(KEY_PAN_ENABLED)) {
            editor.putBoolean(KEY_PAN_ENABLED, DEFAULT_PAN_ENABLED);
        }
        if (!prefs.contains(KEY_HAPTIC_FEEDBACK)) {
            editor.putBoolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC);
        }
        
        // Feature toggles defaults
        if (!prefs.contains(KEY_PARTICLES_ENABLED)) {
            editor.putBoolean(KEY_PARTICLES_ENABLED, DEFAULT_PARTICLES);
        }
        if (!prefs.contains(KEY_LIGHTING_ENABLED)) {
            editor.putBoolean(KEY_LIGHTING_ENABLED, DEFAULT_LIGHTING);
        }
        if (!prefs.contains(KEY_REFLECTIONS_ENABLED)) {
            editor.putBoolean(KEY_REFLECTIONS_ENABLED, DEFAULT_REFLECTIONS);
        }
        if (!prefs.contains(KEY_TRANSITIONS_ENABLED)) {
            editor.putBoolean(KEY_TRANSITIONS_ENABLED, DEFAULT_TRANSITIONS);
        }
        if (!prefs.contains(KEY_AUDIO_VISUALIZER)) {
            editor.putBoolean(KEY_AUDIO_VISUALIZER, DEFAULT_AUDIO_VIZ);
        }
        if (!prefs.contains(KEY_BATTERY_SAVER)) {
            editor.putBoolean(KEY_BATTERY_SAVER, DEFAULT_BATTERY_SAVER);
        }
        
        // Advanced settings defaults
        if (!prefs.contains(KEY_MODEL_PATH)) {
            editor.putString(KEY_MODEL_PATH, DEFAULT_MODEL_PATH);
        }
        if (!prefs.contains(KEY_TEXTURE_PACK)) {
            editor.putString(KEY_TEXTURE_PACK, DEFAULT_TEXTURE_PACK);
        }
        if (!prefs.contains(KEY_SCENE_CONFIG)) {
            editor.putString(KEY_SCENE_CONFIG, DEFAULT_SCENE_CONFIG);
        }
        if (!prefs.contains(KEY_DEVICE_PROFILE)) {
            editor.putString(KEY_DEVICE_PROFILE, DEFAULT_DEVICE_PROFILE);
        }
        
        // MainActivity specific defaults
        if (!prefs.contains(KEY_CONTENT_URL)) {
            editor.putString(KEY_CONTENT_URL, DEFAULT_CONTENT_URL);
        }
        if (!prefs.contains(KEY_QUALITY_PRESET)) {
            editor.putString(KEY_QUALITY_PRESET, DEFAULT_QUALITY_PRESET);
        }
        if (!prefs.contains(KEY_TOUCH_INTERACTIVITY)) {
            editor.putBoolean(KEY_TOUCH_INTERACTIVITY, DEFAULT_TOUCH_INTERACTIVITY);
        }
        if (!prefs.contains(KEY_FRAME_RATE)) {
            editor.putInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE_VALUE);
        }
        
        editor.apply();
    }
    
    // ========== Visual Settings ==========
    
    /**
     * Gets the current theme ID.
     */
    public String getThemeId() {
        return validateString(prefs.getString(KEY_THEME_ID, DEFAULT_THEME_ID));
    }
    
    /**
     * Sets the theme ID.
     */
    public void setThemeId(String themeId) {
        if (themeId != null && !themeId.isEmpty()) {
            prefs.edit().putString(KEY_THEME_ID, themeId).apply();
        }
    }
    
    /**
     * Gets the primary color.
     */
    public String getPrimaryColor() {
        return validateColor(prefs.getString(KEY_COLOR_PRIMARY, DEFAULT_COLOR_PRIMARY));
    }
    
    /**
     * Sets the primary color.
     */
    public void setPrimaryColor(String color) {
        if (validateColor(color) != null) {
            prefs.edit().putString(KEY_COLOR_PRIMARY, color).apply();
        }
    }
    
    /**
     * Gets the secondary color.
     */
    public String getSecondaryColor() {
        return validateColor(prefs.getString(KEY_COLOR_SECONDARY, DEFAULT_COLOR_SECONDARY));
    }
    
    /**
     * Sets the secondary color.
     */
    public void setSecondaryColor(String color) {
        if (validateColor(color) != null) {
            prefs.edit().putString(KEY_COLOR_SECONDARY, color).apply();
        }
    }
    
    /**
     * Gets the accent color.
     */
    public String getAccentColor() {
        return validateColor(prefs.getString(KEY_COLOR_ACCENT, DEFAULT_COLOR_ACCENT));
    }
    
    /**
     * Sets the accent color.
     */
    public void setAccentColor(String color) {
        if (validateColor(color) != null) {
            prefs.edit().putString(KEY_COLOR_ACCENT, color).apply();
        }
    }
    
    /**
     * Gets the background type.
     */
    public BackgroundType getBackgroundType() {
        return BackgroundType.fromString(
            prefs.getString(KEY_BACKGROUND_TYPE, DEFAULT_BG_TYPE)
        );
    }
    
    /**
     * Sets the background type.
     */
    public void setBackgroundType(BackgroundType type) {
        prefs.edit().putString(KEY_BACKGROUND_TYPE, type.getValue()).apply();
    }
    
    /**
     * Gets the custom background color.
     */
    public String getCustomBackgroundColor() {
        return validateColor(prefs.getString(KEY_CUSTOM_BG_COLOR, DEFAULT_CUSTOM_BG));
    }
    
    /**
     * Sets the custom background color.
     */
    public void setCustomBackgroundColor(String color) {
        if (validateColor(color) != null) {
            prefs.edit().putString(KEY_CUSTOM_BG_COLOR, color).apply();
        }
    }
    
    /**
     * Checks if gradient is enabled.
     */
    public boolean isGradientEnabled() {
        return prefs.getBoolean(KEY_GRADIENT_ENABLED, DEFAULT_GRADIENT_ENABLED);
    }
    
    /**
     * Sets gradient enabled state.
     */
    public void setGradientEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GRADIENT_ENABLED, enabled).apply();
    }
    
    /**
     * Gets the animation speed multiplier.
     */
    public float getAnimationSpeed() {
        return validateFloat(
            prefs.getFloat(KEY_ANIMATION_SPEED, DEFAULT_ANIMATION_SPEED),
            MIN_ANIMATION_SPEED,
            MAX_ANIMATION_SPEED
        );
    }
    
    /**
     * Sets the animation speed multiplier.
     */
    public void setAnimationSpeed(float speed) {
        float validated = validateFloat(speed, MIN_ANIMATION_SPEED, MAX_ANIMATION_SPEED);
        prefs.edit().putFloat(KEY_ANIMATION_SPEED, validated).apply();
    }
    
    // ========== Performance Settings ==========
    
    /**
     * Gets the quality level.
     */
    public QualityLevel getQualityLevel() {
        return QualityLevel.fromInt(
            validateInt(
                prefs.getInt(KEY_QUALITY_LEVEL, DEFAULT_QUALITY_LEVEL),
                MIN_QUALITY_LEVEL,
                MAX_QUALITY_LEVEL
            )
        );
    }
    
    /**
     * Sets the quality level.
     */
    public void setQualityLevel(QualityLevel level) {
        int value = validateInt(level.getValue(), MIN_QUALITY_LEVEL, MAX_QUALITY_LEVEL);
        prefs.edit().putInt(KEY_QUALITY_LEVEL, value).apply();
    }
    
    /**
     * Gets the frame rate limit.
     */
    public int getFrameRateLimit() {
        return validateInt(
            prefs.getInt(KEY_FRAME_RATE_LIMIT, DEFAULT_FRAME_RATE_VALUE),
            MIN_FRAME_RATE,
            MAX_FRAME_RATE
        );
    }
    
    /**
     * Sets the frame rate limit.
     */
    public void setFrameRateLimit(int fps) {
        int validated = validateInt(fps, MIN_FRAME_RATE, MAX_FRAME_RATE);
        prefs.edit().putInt(KEY_FRAME_RATE_LIMIT, validated).apply();
    }
    
    /**
     * Checks if hardware acceleration is enabled.
     */
    public boolean isHardwareAccelerationEnabled() {
        return prefs.getBoolean(KEY_ENABLE_HARDWARE_ACCEL, DEFAULT_HW_ACCEL);
    }
    
    /**
     * Sets hardware acceleration state.
     */
    public void setHardwareAccelerationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLE_HARDWARE_ACCEL, enabled).apply();
    }
    
    /**
     * Checks if memory optimization is enabled.
     */
    public boolean isMemoryOptimizationEnabled() {
        return prefs.getBoolean(KEY_MEMORY_OPTIMIZATION, DEFAULT_MEMORY_OPT);
    }
    
    /**
     * Sets memory optimization state.
     */
    public void setMemoryOptimizationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MEMORY_OPTIMIZATION, enabled).apply();
    }
    
    /**
     * Gets the LOD (Level of Detail) level.
     */
    public int getLodLevel() {
        return validateInt(
            prefs.getInt(KEY_LOD_LEVEL, DEFAULT_LOD_LEVEL),
            MIN_LOD_LEVEL,
            MAX_LOD_LEVEL
        );
    }
    
    /**
     * Sets the LOD level.
     */
    public void setLodLevel(int level) {
        int validated = validateInt(level, MIN_LOD_LEVEL, MAX_LOD_LEVEL);
        prefs.edit().putInt(KEY_LOD_LEVEL, validated).apply();
    }
    
    /**
     * Gets the shadow quality level.
     */
    public int getShadowQuality() {
        return validateInt(
            prefs.getInt(KEY_SHADOW_QUALITY, DEFAULT_SHADOW_QUALITY),
            MIN_LOD_LEVEL,
            MAX_LOD_LEVEL
        );
    }
    
    /**
     * Sets the shadow quality level.
     */
    public void setShadowQuality(int quality) {
        int validated = validateInt(quality, MIN_LOD_LEVEL, MAX_LOD_LEVEL);
        prefs.edit().putInt(KEY_SHADOW_QUALITY, validated).apply();
    }
    
    /**
     * Gets the texture quality level.
     */
    public int getTextureQuality() {
        return validateInt(
            prefs.getInt(KEY_TEXTURE_QUALITY, DEFAULT_TEXTURE_QUALITY),
            MIN_LOD_LEVEL,
            MAX_LOD_LEVEL
        );
    }
    
    /**
     * Sets the texture quality level.
     */
    public void setTextureQuality(int quality) {
        int validated = validateInt(quality, MIN_LOD_LEVEL, MAX_LOD_LEVEL);
        prefs.edit().putInt(KEY_TEXTURE_QUALITY, validated).apply();
    }
    
    // ========== Interaction Settings ==========
    
    /**
     * Checks if gestures are enabled.
     */
    public boolean areGesturesEnabled() {
        return prefs.getBoolean(KEY_GESTURE_ENABLED, DEFAULT_GESTURE_ENABLED);
    }
    
    /**
     * Sets gesture enabled state.
     */
    public void setGesturesEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GESTURE_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if tap-to-interact is enabled.
     */
    public boolean isTapToInteractEnabled() {
        return prefs.getBoolean(KEY_TAP_TO_INTERACT, DEFAULT_TAP_INTERACT);
    }
    
    /**
     * Sets tap-to-interact state.
     */
    public void setTapToInteractEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TAP_TO_INTERACT, enabled).apply();
    }
    
    /**
     * Gets the swipe sensitivity.
     */
    public float getSwipeSensitivity() {
        return validateFloat(
            prefs.getFloat(KEY_SWIPE_SENSITIVITY, DEFAULT_SWIPE_SENS),
            MIN_SWIPE_SENS,
            MAX_SWIPE_SENS
        );
    }
    
    /**
     * Sets the swipe sensitivity.
     */
    public void setSwipeSensitivity(float sensitivity) {
        float validated = validateFloat(sensitivity, MIN_SWIPE_SENS, MAX_SWIPE_SENS);
        prefs.edit().putFloat(KEY_SWIPE_SENSITIVITY, validated).apply();
    }
    
    /**
     * Checks if zoom is enabled.
     */
    public boolean isZoomEnabled() {
        return prefs.getBoolean(KEY_ZOOM_ENABLED, DEFAULT_ZOOM_ENABLED);
    }
    
    /**
     * Sets zoom enabled state.
     */
    public void setZoomEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ZOOM_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if rotation is enabled.
     */
    public boolean isRotationEnabled() {
        return prefs.getBoolean(KEY_ROTATION_ENABLED, DEFAULT_ROTATION_ENABLED);
    }
    
    /**
     * Sets rotation enabled state.
     */
    public void setRotationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ROTATION_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if pan is enabled.
     */
    public boolean isPanEnabled() {
        return prefs.getBoolean(KEY_PAN_ENABLED, DEFAULT_PAN_ENABLED);
    }
    
    /**
     * Sets pan enabled state.
     */
    public void setPanEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PAN_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if haptic feedback is enabled.
     */
    public boolean isHapticFeedbackEnabled() {
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC);
    }
    
    /**
     * Sets haptic feedback state.
     */
    public void setHapticFeedbackEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply();
    }
    
    // ========== Feature Toggles ==========
    
    /**
     * Checks if particles are enabled.
     */
    public boolean areParticlesEnabled() {
        return prefs.getBoolean(KEY_PARTICLES_ENABLED, DEFAULT_PARTICLES);
    }
    
    /**
     * Sets particles enabled state.
     */
    public void setParticlesEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PARTICLES_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if lighting is enabled.
     */
    public boolean isLightingEnabled() {
        return prefs.getBoolean(KEY_LIGHTING_ENABLED, DEFAULT_LIGHTING);
    }
    
    /**
     * Sets lighting enabled state.
     */
    public void setLightingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LIGHTING_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if reflections are enabled.
     */
    public boolean areReflectionsEnabled() {
        return prefs.getBoolean(KEY_REFLECTIONS_ENABLED, DEFAULT_REFLECTIONS);
    }
    
    /**
     * Sets reflections enabled state.
     */
    public void setReflectionsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REFLECTIONS_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if transitions are enabled.
     */
    public boolean areTransitionsEnabled() {
        return prefs.getBoolean(KEY_TRANSITIONS_ENABLED, DEFAULT_TRANSITIONS);
    }
    
    /**
     * Sets transitions enabled state.
     */
    public void setTransitionsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TRANSITIONS_ENABLED, enabled).apply();
    }
    
    /**
     * Checks if audio visualizer is enabled.
     */
    public boolean isAudioVisualizerEnabled() {
        return prefs.getBoolean(KEY_AUDIO_VISUALIZER, DEFAULT_AUDIO_VIZ);
    }
    
    /**
     * Sets audio visualizer state.
     */
    public void setAudioVisualizerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUDIO_VISUALIZER, enabled).apply();
    }
    
    /**
     * Checks if battery saver mode is enabled.
     */
    public boolean isBatterySaverEnabled() {
        return prefs.getBoolean(KEY_BATTERY_SAVER, DEFAULT_BATTERY_SAVER);
    }
    
    /**
     * Sets battery saver mode.
     */
    public void setBatterySaverEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BATTERY_SAVER, enabled).apply();
    }
    
    // ========== Advanced Settings ==========
    
    /**
     * Gets the custom JavaScript/Three.js script.
     */
    public String getCustomScript() {
        return prefs.getString(KEY_CUSTOM_SCRIPT, "");
    }
    
    /**
     * Sets the custom script.
     */
    public void setCustomScript(String script) {
        if (script != null && script.length() < 50000) { // Limit to 50KB
            prefs.edit().putString(KEY_CUSTOM_SCRIPT, script).apply();
        }
    }
    
    /**
     * Gets the 3D model path.
     */
    public String getModelPath() {
        return validatePath(prefs.getString(KEY_MODEL_PATH, DEFAULT_MODEL_PATH));
    }
    
    /**
     * Sets the 3D model path.
     */
    public void setModelPath(String path) {
        if (validatePath(path) != null) {
            prefs.edit().putString(KEY_MODEL_PATH, path).apply();
        }
    }
    
    /**
     * Gets the texture pack identifier.
     */
    public String getTexturePack() {
        return validateString(prefs.getString(KEY_TEXTURE_PACK, DEFAULT_TEXTURE_PACK));
    }
    
    /**
     * Sets the texture pack.
     */
    public void setTexturePack(String pack) {
        if (pack != null && !pack.isEmpty()) {
            prefs.edit().putString(KEY_TEXTURE_PACK, pack).apply();
        }
    }
    
    /**
     * Gets the scene configuration JSON.
     */
    public JSONObject getSceneConfig() {
        try {
            return new JSONObject(prefs.getString(KEY_SCENE_CONFIG, DEFAULT_SCENE_CONFIG));
        } catch (JSONException e) {
            Log.w(TAG, "Invalid scene config JSON, using defaults", e);
            try {
                return new JSONObject(DEFAULT_SCENE_CONFIG);
            } catch (JSONException e2) {
                return new JSONObject();
            }
        }
    }
    
    /**
     * Sets the scene configuration JSON.
     */
    public void setSceneConfig(JSONObject config) {
        if (config != null) {
            prefs.edit().putString(KEY_SCENE_CONFIG, config.toString()).apply();
        }
    }
    
    /**
     * Gets the device profile.
     */
    public String getDeviceProfile() {
        return validateString(prefs.getString(KEY_DEVICE_PROFILE, DEFAULT_DEVICE_PROFILE));
    }
    
    /**
     * Sets the device profile.
     */
    public void setDeviceProfile(String profile) {
        if (profile != null && !profile.isEmpty()) {
            prefs.edit().putString(KEY_DEVICE_PROFILE, profile).apply();
        }
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Resets all preferences to default values.
     */
    public void resetToDefaults() {
        prefs.edit().clear().apply();
        initializeDefaults();
        Log.i(TAG, "Preferences reset to defaults");
    }
    
    /**
     * Exports all preferences as a JSON string for backup/restore.
     */
    public String exportPreferences() {
        try {
            JSONObject export = new JSONObject();
            
            // Get all preferences
            Map<String, ?> allPrefs = prefs.getAll();
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                export.put(entry.getKey(), entry.getValue());
            }
            
            return export.toString(2);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export preferences", e);
            return "{}";
        }
    }
    
    /**
     * Imports preferences from a JSON string.
     */
    public void importPreferences(String json) {
        try {
            JSONObject importData = new JSONObject(json);
            SharedPreferences.Editor editor = prefs.edit();
            
            Iterator<String> keys = importData.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = importData.get(key);
                
                if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof JSONArray) {
                    // Handle String arrays from JSONArray
                    JSONArray arr = (JSONArray) value;
                    Set<String> stringSet = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        stringSet.add(arr.getString(i));
                    }
                    editor.putStringSet(key, stringSet);
                }
            }
            
            editor.apply();
            Log.i(TAG, "Preferences imported successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to import preferences", e);
        }
    }
    
    /**
     * Validates that preferences are consistent and sensible.
     * Returns a list of issues found, or empty list if all good.
     */
    public java.util.List<String> validatePreferences() {
        java.util.List<String> issues = new java.util.ArrayList<>();
        
        // Check quality vs battery saver conflict
        if (isBatterySaverEnabled() && getQualityLevel() == QualityLevel.ULTRA) {
            issues.add("Battery saver enabled but quality is set to Ultra");
        }
        
        // Check frame rate vs battery saver
        if (isBatterySaverEnabled() && getFrameRateLimit() > 24) {
            issues.add("Battery saver enabled but frame rate is above 24 FPS");
        }
        
        // Check hardware acceleration conflict
        if (!isHardwareAccelerationEnabled() && getQualityLevel() == QualityLevel.ULTRA) {
            issues.add("Hardware acceleration disabled but quality is set to Ultra");
        }
        
        // Check feature combinations that might be too heavy
        if (areReflectionsEnabled() && areParticlesEnabled() && getQualityLevel() == QualityLevel.ULTRA) {
            issues.add("Reflections, particles, and Ultra quality may cause performance issues");
        }
        
        return issues;
    }
    
    // ========== Validation Methods ==========
    
    private String validateString(String value) {
        if (value == null || value.isEmpty()) {
            return ""; // Return empty string instead of null
        }
        return value.trim();
    }
    
    private String validateColor(String color) {
        if (color == null || color.isEmpty()) {
            return DEFAULT_COLOR_PRIMARY;
        }
        
        // Validate hex color format
        if (color.matches("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")) {
            return color;
        }
        
        Log.w(TAG, "Invalid color format: " + color + ", using default");
        return DEFAULT_COLOR_PRIMARY;
    }
    
    private int validateInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private float validateFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private String validatePath(String path) {
        if (path == null || path.isEmpty()) {
            return DEFAULT_MODEL_PATH;
        }
        
        // Basic path validation
        if (path.contains("..") || path.contains("~") || path.contains(":")) {
            Log.w(TAG, "Invalid path format: " + path);
            return DEFAULT_MODEL_PATH;
        }
        
        return path;
    }
    
    /**
     * Gets a preference change listener that can be used to register for changes.
     */
    public SharedPreferences.OnSharedPreferenceChangeListener createChangeListener() {
        return (prefs, key) -> {
            Log.d(TAG, "Preference changed: " + key);
            // Subclasses can override this to handle preference changes
        };
    }
    
    // ========== MainActivity Specific Methods ==========
    
    /**
     * Gets the content URL for the wallpaper.
     */
    public String getContentUrl() {
        return validateString(prefs.getString(KEY_CONTENT_URL, DEFAULT_CONTENT_URL));
    }
    
    /**
     * Sets the content URL for the wallpaper.
     */
    public void setContentUrl(String url) {
        if (url != null && !url.isEmpty()) {
            prefs.edit().putString(KEY_CONTENT_URL, url).apply();
        }
    }
    
    /**
     * Gets the quality preset string.
     */
    public String getQualityPreset() {
        return validateString(prefs.getString(KEY_QUALITY_PRESET, DEFAULT_QUALITY_PRESET));
    }
    
    /**
     * Sets the quality preset.
     */
    public void setQualityPreset(String preset) {
        if (preset != null && !preset.isEmpty()) {
            prefs.edit().putString(KEY_QUALITY_PRESET, preset).apply();
        }
    }
    
    /**
     * Checks if touch interactivity is enabled.
     */
    public boolean isTouchInteractivityEnabled() {
        return prefs.getBoolean(KEY_TOUCH_INTERACTIVITY, DEFAULT_TOUCH_INTERACTIVITY);
    }
    
    /**
     * Sets touch interactivity enabled state.
     */
    public void setTouchInteractivityEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TOUCH_INTERACTIVITY, enabled).apply();
    }
    
    /**
     * Gets the frame rate value.
     */
    public int getFrameRate() {
        return validateInt(
            prefs.getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE_VALUE),
            MIN_FRAME_RATE,
            MAX_FRAME_RATE
        );
    }
    
    /**
     * Sets the frame rate.
     */
    public void setFrameRate(int fps) {
        int validated = validateInt(fps, MIN_FRAME_RATE, MAX_FRAME_RATE);
        prefs.edit().putInt(KEY_FRAME_RATE, validated).apply();
    }
}
