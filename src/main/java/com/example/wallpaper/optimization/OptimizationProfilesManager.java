package com.example.wallpaper.optimization;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Optimization Profiles Manager for WebView Wallpaper
 * 
 * Provides pre-configured optimization profiles for different use cases:
 * - Performance: Maximum performance with higher resource usage
 * - Battery Saver: Extended battery life with reduced performance
 * - Balanced: Balanced performance and power consumption
 * - Gaming: Optimized for gaming experiences
 * - Low-end Device: Optimized for resource-constrained devices
 */
public class OptimizationProfilesManager {
    
    private static final String TAG = "OptimizationProfiles";
    
    public enum OptimizationProfile {
        PERFORMANCE("performance"),
        BATTERY_SAVER("battery_saver"),
        BALANCED("balanced"),
        GAMING("gaming"),
        LOW_END_DEVICE("low_end_device"),
        NETWORK_SAVER("network_saver"),
        QUIET_MODE("quiet_mode");
        
        private final String profileName;
        
        OptimizationProfile(String profileName) {
            this.profileName = profileName;
        }
        
        public String getProfileName() {
            return profileName;
        }
    }
    
    /**
     * Optimization profile configuration
     */
    public static class ProfileConfig {
        public final OptimizationProfile profile;
        public final int targetFrameRate;
        public final boolean hardwareAcceleration;
        public final boolean webGLEnabled;
        public final boolean webGLAntialiasing;
        public final int memoryLimitMB;
        public final int renderPriority;
        public final boolean enableSmoothScroll;
        public final boolean enableHybridComposition;
        public final String cacheMode;
        public final boolean enableDiskCache;
        public final boolean enableImageCache;
        public final boolean enableJavaScript;
        public final boolean enableWebViewDebugging;
        public final int gpuTier;
        public final boolean enableVulkan;
        public final boolean enableHardwareRendering;
        public final boolean enableTextureCompression;
        public final int maxConcurrentConnections;
        public final boolean enablePreload;
        public final boolean enableLazyLoading;
        public final int batteryOptimizationLevel;
        public final int networkOptimizationLevel;
        public final int memoryOptimizationLevel;
        
        public ProfileConfig(Builder builder) {
            this.profile = builder.profile;
            this.targetFrameRate = builder.targetFrameRate;
            this.hardwareAcceleration = builder.hardwareAcceleration;
            this.webGLEnabled = builder.webGLEnabled;
            this.webGLAntialiasing = builder.webGLAntialiasing;
            this.memoryLimitMB = builder.memoryLimitMB;
            this.renderPriority = builder.renderPriority;
            this.enableSmoothScroll = builder.enableSmoothScroll;
            this.enableHybridComposition = builder.enableHybridComposition;
            this.cacheMode = builder.cacheMode;
            this.enableDiskCache = builder.enableDiskCache;
            this.enableImageCache = builder.enableImageCache;
            this.enableJavaScript = builder.enableJavaScript;
            this.enableWebViewDebugging = builder.enableWebViewDebugging;
            this.gpuTier = builder.gpuTier;
            this.enableVulkan = builder.enableVulkan;
            this.enableHardwareRendering = builder.enableHardwareRendering;
            this.enableTextureCompression = builder.enableTextureCompression;
            this.maxConcurrentConnections = builder.maxConcurrentConnections;
            this.enablePreload = builder.enablePreload;
            this.enableLazyLoading = builder.enableLazyLoading;
            this.batteryOptimizationLevel = builder.batteryOptimizationLevel;
            this.networkOptimizationLevel = builder.networkOptimizationLevel;
            this.memoryOptimizationLevel = builder.memoryOptimizationLevel;
        }
        
        /**
         * Builder class for ProfileConfig
         */
        public static class Builder {
            private OptimizationProfile profile;
            private int targetFrameRate = 60;
            private boolean hardwareAcceleration = true;
            private boolean webGLEnabled = true;
            private boolean webGLAntialiasing = false;
            private int memoryLimitMB = 128;
            private int renderPriority = 1; // Normal priority
            private boolean enableSmoothScroll = true;
            private boolean enableHybridComposition = false;
            private String cacheMode = "default";
            private boolean enableDiskCache = true;
            private boolean enableImageCache = true;
            private boolean enableJavaScript = true;
            private boolean enableWebViewDebugging = false;
            private int gpuTier = 2;
            private boolean enableVulkan = false;
            private boolean enableHardwareRendering = true;
            private boolean enableTextureCompression = false;
            private int maxConcurrentConnections = 6;
            private boolean enablePreload = true;
            private boolean enableLazyLoading = false;
            private int batteryOptimizationLevel = 1; // Low optimization
            private int networkOptimizationLevel = 1; // Low optimization
            private int memoryOptimizationLevel = 1; // Low optimization
            
            public Builder(OptimizationProfile profile) {
                this.profile = profile;
            }
            
            public Builder targetFrameRate(int frameRate) {
                this.targetFrameRate = frameRate;
                return this;
            }
            
            public Builder hardwareAcceleration(boolean enabled) {
                this.hardwareAcceleration = enabled;
                return this;
            }
            
            public Builder webGLEnabled(boolean enabled) {
                this.webGLEnabled = enabled;
                return this;
            }
            
            public Builder webGLAntialiasing(boolean enabled) {
                this.webGLAntialiasing = enabled;
                return this;
            }
            
            public Builder memoryLimitMB(int limit) {
                this.memoryLimitMB = limit;
                return this;
            }
            
            public Builder renderPriority(int priority) {
                this.renderPriority = priority;
                return this;
            }
            
            public Builder enableSmoothScroll(boolean enabled) {
                this.enableSmoothScroll = enabled;
                return this;
            }
            
            public Builder enableHybridComposition(boolean enabled) {
                this.enableHybridComposition = enabled;
                return this;
            }
            
            public Builder cacheMode(String mode) {
                this.cacheMode = mode;
                return this;
            }
            
            public Builder enableDiskCache(boolean enabled) {
                this.enableDiskCache = enabled;
                return this;
            }
            
            public Builder enableImageCache(boolean enabled) {
                this.enableImageCache = enabled;
                return this;
            }
            
            public Builder enableJavaScript(boolean enabled) {
                this.enableJavaScript = enabled;
                return this;
            }
            
            public Builder enableWebViewDebugging(boolean enabled) {
                this.enableWebViewDebugging = enabled;
                return this;
            }
            
            public Builder gpuTier(int tier) {
                this.gpuTier = tier;
                return this;
            }
            
            public Builder enableVulkan(boolean enabled) {
                this.enableVulkan = enabled;
                return this;
            }
            
            public Builder enableHardwareRendering(boolean enabled) {
                this.enableHardwareRendering = enabled;
                return this;
            }
            
            public Builder enableTextureCompression(boolean enabled) {
                this.enableTextureCompression = enabled;
                return this;
            }
            
            public Builder maxConcurrentConnections(int connections) {
                this.maxConcurrentConnections = connections;
                return this;
            }
            
            public Builder enablePreload(boolean enabled) {
                this.enablePreload = enabled;
                return this;
            }
            
            public Builder enableLazyLoading(boolean enabled) {
                this.enableLazyLoading = enabled;
                return this;
            }
            
            public Builder batteryOptimizationLevel(int level) {
                this.batteryOptimizationLevel = level;
                return this;
            }
            
            public Builder networkOptimizationLevel(int level) {
                this.networkOptimizationLevel = level;
                return this;
            }
            
            public Builder memoryOptimizationLevel(int level) {
                this.memoryOptimizationLevel = level;
                return this;
            }
            
            public ProfileConfig build() {
                return new ProfileConfig(this);
            }
        }
    }
    
    private final Context context;
    private final WebViewPerformanceOptimizer optimizer;
    private final AdvancedDeviceDetector deviceDetector;
    private OptimizationProfile currentProfile = OptimizationProfile.BALANCED;
    private ProfileConfig currentConfig;
    
    /**
     * Constructor
     */
    public OptimizationProfilesManager(Context context, WebViewPerformanceOptimizer optimizer) {
        this.context = context.getApplicationContext();
        this.optimizer = optimizer;
        this.deviceDetector = new AdvancedDeviceDetector(this.context);
        
        // Initialize with balanced profile
        setProfile(OptimizationProfile.BALANCED);
        
        Log.i(TAG, "Optimization Profiles Manager initialized");
    }
    
    /**
     * Set optimization profile
     */
    public void setProfile(OptimizationProfile profile) {
        this.currentProfile = profile;
        this.currentConfig = createProfileConfig(profile);
        
        applyProfileToOptimizer();
        
        Log.i(TAG, "Optimization profile set to: " + profile.getProfileName());
    }
    
    /**
     * Get current optimization profile
     */
    public OptimizationProfile getCurrentProfile() {
        return currentProfile;
    }
    
    /**
     * Get current profile configuration
     */
    public ProfileConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * Create profile configuration based on profile type and device capabilities
     */
    private ProfileConfig createProfileConfig(OptimizationProfile profile) {
        AdvancedDeviceDetector.DeviceProfile deviceProfile = deviceDetector.getDeviceProfile();
        Builder builder = new Builder(profile);
        
        switch (profile) {
            case PERFORMANCE:
                return buildPerformanceProfile(builder, deviceProfile);
                
            case BATTERY_SAVER:
                return buildBatterySaverProfile(builder, deviceProfile);
                
            case BALANCED:
                return buildBalancedProfile(builder, deviceProfile);
                
            case GAMING:
                return buildGamingProfile(builder, deviceProfile);
                
            case LOW_END_DEVICE:
                return buildLowEndDeviceProfile(builder, deviceProfile);
                
            case NETWORK_SAVER:
                return buildNetworkSaverProfile(builder, deviceProfile);
                
            case QUIET_MODE:
                return buildQuietModeProfile(builder, deviceProfile);
                
            default:
                return buildBalancedProfile(builder, deviceProfile);
        }
    }
    
    /**
     * Build performance profile (maximum performance)
     */
    private ProfileConfig buildPerformanceProfile(Builder builder, 
                                                 AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        return builder
            .targetFrameRate(Math.min(120, Math.max(60, deviceProfile.cpuCores * 15)))
            .hardwareAcceleration(true)
            .webGLEnabled(true)
            .webGLAntialiasing(true)
            .memoryLimitMB((int) (deviceProfile.totalMemoryMB * 0.3)) // 30% of total memory
            .renderPriority(3) // High priority
            .enableSmoothScroll(true)
            .enableHybridComposition(true)
            .cacheMode("default")
            .enableDiskCache(true)
            .enableImageCache(true)
            .enableJavaScript(true)
            .enableWebViewDebugging(false)
            .gpuTier(3) // Request high-tier GPU features
            .enableVulkan(true)
            .enableHardwareRendering(true)
            .enableTextureCompression(false) // Disable for maximum quality
            .maxConcurrentConnections(8)
            .enablePreload(true)
            .enableLazyLoading(false)
            .batteryOptimizationLevel(3) // High battery usage
            .networkOptimizationLevel(1) // Minimal network optimization
            .memoryOptimizationLevel(1)  // Minimal memory optimization
            .build();
    }
    
    /**
     * Build battery saver profile (maximum battery life)
     */
    private ProfileConfig buildBatterySaverProfile(Builder builder, 
                                                  AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        return builder
            .targetFrameRate(30)
            .hardwareAcceleration(true) // Keep enabled but limit usage
            .webGLEnabled(false) // Disable WebGL for battery saving
            .webGLAntialiasing(false)
            .memoryLimitMB(Math.min(64, (int) (deviceProfile.totalMemoryMB * 0.15))) // 15% of total memory
            .renderPriority(1) // Low priority
            .enableSmoothScroll(false)
            .enableHybridComposition(false)
            .cacheMode("cache_else_network") // Prefer cache
            .enableDiskCache(false) // Disable disk cache
            .enableImageCache(true)
            .enableJavaScript(false) // Disable JS for battery saving
            .enableWebViewDebugging(false)
            .gpuTier(1) // Use low GPU features
            .enableVulkan(false)
            .enableHardwareRendering(true)
            .enableTextureCompression(true)
            .maxConcurrentConnections(2)
            .enablePreload(false)
            .enableLazyLoading(true)
            .batteryOptimizationLevel(5) // Maximum battery optimization
            .networkOptimizationLevel(3) // Aggressive network optimization
            .memoryOptimizationLevel(3)  // Aggressive memory optimization
            .build();
    }
    
    /**
     * Build balanced profile (default)
     */
    private ProfileConfig buildBalancedProfile(Builder builder, 
                                             AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        int recommendedFrameRate = deviceProfile.deviceClass == AdvancedDeviceDetector.DEVICE_CLASS_HIGH_END ? 60 : 
                                   deviceProfile.deviceClass == AdvancedDeviceDetector.DEVICE_CLASS_MID_RANGE ? 45 : 30;
        
        return builder
            .targetFrameRate(recommendedFrameRate)
            .hardwareAcceleration(true)
            .webGLEnabled(deviceProfile.gpuPerformanceScore >= 2.0f)
            .webGLAntialiasing(false)
            .memoryLimitMB(Math.min(128, (int) (deviceProfile.totalMemoryMB * 0.2))) // 20% of total memory
            .renderPriority(2) // Normal priority
            .enableSmoothScroll(true)
            .enableHybridComposition(deviceProfile.deviceClass != AdvancedDeviceDetector.DEVICE_CLASS_LOW_END)
            .cacheMode("default")
            .enableDiskCache(true)
            .enableImageCache(true)
            .enableJavaScript(true)
            .enableWebViewDebugging(false)
            .gpuTier(deviceProfile.gpuTier)
            .enableVulkan(false)
            .enableHardwareRendering(true)
            .enableTextureCompression(true)
            .maxConcurrentConnections(4)
            .enablePreload(true)
            .enableLazyLoading(false)
            .batteryOptimizationLevel(2) // Moderate battery optimization
            .networkOptimizationLevel(2) // Moderate network optimization
            .memoryOptimizationLevel(2)  // Moderate memory optimization
            .build();
    }
    
    /**
     * Build gaming profile (optimized for games)
     */
    private ProfileConfig buildGamingProfile(Builder builder, 
                                           AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        return builder
            .targetFrameRate(Math.min(90, Math.max(60, deviceProfile.gpuPerformanceScore * 20)))
            .hardwareAcceleration(true)
            .webGLEnabled(true)
            .webGLAntialiasing(true) // Enable antialiasing for better quality
            .memoryLimitMB(Math.min(256, (int) (deviceProfile.totalMemoryMB * 0.25))) // 25% of total memory
            .renderPriority(3) // High priority for games
            .enableSmoothScroll(false) // Disable for games
            .enableHybridComposition(true)
            .cacheMode("no_cache") // No cache for real-time content
            .enableDiskCache(false) // Disable disk cache for performance
            .enableImageCache(true)
            .enableJavaScript(true)
            .enableWebViewDebugging(false)
            .gpuTier(3) // Request high GPU features
            .enableVulkan(true)
            .enableHardwareRendering(true)
            .enableTextureCompression(false) // Disable for quality
            .maxConcurrentConnections(6)
            .enablePreload(true)
            .enableLazyLoading(false)
            .batteryOptimizationLevel(2) // Moderate battery usage
            .networkOptimizationLevel(1) // Minimal network optimization for latency
            .memoryOptimizationLevel(2)  // Moderate memory optimization
            .build();
    }
    
    /**
     * Build low-end device profile
     */
    private ProfileConfig buildLowEndDeviceProfile(Builder builder, 
                                                  AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        return builder
            .targetFrameRate(24) // Low frame rate
            .hardwareAcceleration(true) // Keep enabled
            .webGLEnabled(false) // Disable WebGL on low-end devices
            .webGLAntialiasing(false)
            .memoryLimitMB(Math.min(32, (int) (deviceProfile.totalMemoryMB * 0.1))) // 10% of total memory
            .renderPriority(1) // Low priority
            .enableSmoothScroll(false)
            .enableHybridComposition(false)
            .cacheMode("cache_only") // Only use cache
            .enableDiskCache(false) // Disable disk cache
            .enableImageCache(false) // Disable image cache
            .enableJavaScript(false) // Disable JavaScript
            .enableWebViewDebugging(false)
            .gpuTier(1) // Low GPU features
            .enableVulkan(false)
            .enableHardwareRendering(true)
            .enableTextureCompression(true)
            .maxConcurrentConnections(1) // Single connection
            .enablePreload(false)
            .enableLazyLoading(true)
            .batteryOptimizationLevel(4) // High battery optimization
            .networkOptimizationLevel(3) // Aggressive network optimization
            .memoryOptimizationLevel(4)  // High memory optimization
            .build();
    }
    
    /**
     * Build network saver profile
     */
    private ProfileConfig buildNetworkSaverProfile(Builder builder, 
                                                  AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        return builder
            .targetFrameRate(30)
            .hardwareAcceleration(true)
            .webGLEnabled(false) // Disable WebGL (uses more data)
            .webGLAntialiasing(false)
            .memoryLimitMB(Math.min(96, (int) (deviceProfile.totalMemoryMB * 0.15)))
            .renderPriority(1) // Low priority
            .enableSmoothScroll(false)
            .enableHybridComposition(false)
            .cacheMode("cache_only") // Only use cached content
            .enableDiskCache(true) // Enable disk cache for offline
            .enableImageCache(true)
            .enableJavaScript(true)
            .enableWebViewDebugging(false)
            .gpuTier(2)
            .enableVulkan(false)
            .enableHardwareRendering(true)
            .enableTextureCompression(true)
            .maxConcurrentConnections(2) // Limit connections to save data
            .enablePreload(false)
            .enableLazyLoading(true) // Load content as needed
            .batteryOptimizationLevel(2) // Moderate battery optimization
            .networkOptimizationLevel(5) // Maximum network optimization
            .memoryOptimizationLevel(3)  // High memory optimization
            .build();
    }
    
    /**
     * Build quiet mode profile (minimal activity)
     */
    private ProfileConfig buildQuietModeProfile(Builder builder, 
                                              AdvancedDeviceDetector.DeviceProfile deviceProfile) {
        return builder
            .targetFrameRate(15) // Very low frame rate
            .hardwareAcceleration(true)
            .webGLEnabled(false)
            .webGLAntialiasing(false)
            .memoryLimitMB(32) // Very small memory limit
            .renderPriority(1) // Lowest priority
            .enableSmoothScroll(false)
            .enableHybridComposition(false)
            .cacheMode("no_cache") // No caching
            .enableDiskCache(false)
            .enableImageCache(false)
            .enableJavaScript(false)
            .enableWebViewDebugging(false)
            .gpuTier(1)
            .enableVulkan(false)
            .enableHardwareRendering(true)
            .enableTextureCompression(true)
            .maxConcurrentConnections(1) // Single connection
            .enablePreload(false)
            .enableLazyLoading(true)
            .batteryOptimizationLevel(5) // Maximum battery optimization
            .networkOptimizationLevel(5) // Maximum network optimization
            .memoryOptimizationLevel(5)  // Maximum memory optimization
            .build();
    }
    
    /**
     * Apply current profile configuration to the optimizer
     */
    private void applyProfileToOptimizer() {
        if (optimizer == null || currentConfig == null) {
            Log.w(TAG, "Cannot apply profile: optimizer or config is null");
            return;
        }
        
        try {
            // Apply frame rate
            optimizer.setTargetFrameRate(currentConfig.targetFrameRate);
            
            // Apply hardware acceleration
            optimizer.setHardwareAcceleration(currentConfig.hardwareAcceleration);
            
            // Apply WebGL
            optimizer.setWebGLEnabled(currentConfig.webGLEnabled);
            
            Log.d(TAG, String.format("Applied profile %s: %d FPS, HW Accel: %s, WebGL: %s",
                    currentProfile.getProfileName(),
                    currentConfig.targetFrameRate,
                    currentConfig.hardwareAcceleration ? "ON" : "OFF",
                    currentConfig.webGLEnabled ? "ON" : "OFF"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying profile to optimizer", e);
        }
    }
    
    /**
     * Get profile recommendation based on current conditions
     */
    public OptimizationProfile getRecommendedProfile() {
        AdvancedDeviceDetector.DeviceProfile deviceProfile = deviceDetector.getDeviceProfile();
        
        // Consider battery level, device class, and network quality
        android.os.BatteryManager batteryManager = 
            (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        float batteryLevel = batteryManager != null ? 
            (float) batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f : 1.0f;
        
        // Network quality check
        android.net.ConnectivityManager connectivityManager = 
            (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = connectivityManager != null ? 
            connectivityManager.getActiveNetworkInfo() : null;
        boolean isWifi = activeNetwork != null && activeNetwork.getType() == android.net.ConnectivityManager.TYPE_WIFI;
        
        // Recommendation logic
        if (deviceProfile.isEmulator) {
            return OptimizationProfile.PERFORMANCE;
        }
        
        if (batteryLevel < 0.20f) {
            return OptimizationProfile.BATTERY_SAVER;
        }
        
        if (deviceProfile.deviceClass == AdvancedDeviceDetector.DEVICE_CLASS_LOW_END) {
            return OptimizationProfile.LOW_END_DEVICE;
        }
        
        if (!isWifi) {
            return OptimizationProfile.NETWORK_SAVER;
        }
        
        if (deviceProfile.deviceClass == AdvancedDeviceDetector.DEVICE_CLASS_HIGH_END) {
            return OptimizationProfile.PERFORMANCE;
        }
        
        return OptimizationProfile.BALANCED;
    }
    
    /**
     * Get all available profiles with descriptions
     */
    public Map<OptimizationProfile, String> getAvailableProfiles() {
        Map<OptimizationProfile, String> profiles = new HashMap<>();
        
        profiles.put(OptimizationProfile.PERFORMANCE, "Maximum performance, high resource usage");
        profiles.put(OptimizationProfile.BATTERY_SAVER, "Extended battery life, reduced performance");
        profiles.put(OptimizationProfile.BALANCED, "Balanced performance and power consumption");
        profiles.put(OptimizationProfile.GAMING, "Optimized for gaming experiences");
        profiles.put(OptimizationProfile.LOW_END_DEVICE, "Optimized for resource-constrained devices");
        profiles.put(OptimizationProfile.NETWORK_SAVER, "Minimizes network usage and data consumption");
        profiles.put(OptimizationProfile.QUIET_MODE, "Minimal activity and resource usage");
        
        return profiles;
    }
    
    /**
     * Create custom profile with specific parameters
     */
    public ProfileConfig createCustomProfile(Map<String, Object> customSettings) {
        Builder builder = new Builder(OptimizationProfile.BALANCED);
        
        // Apply custom settings
        if (customSettings.containsKey("targetFrameRate")) {
            builder.targetFrameRate((Integer) customSettings.get("targetFrameRate"));
        }
        if (customSettings.containsKey("hardwareAcceleration")) {
            builder.hardwareAcceleration((Boolean) customSettings.get("hardwareAcceleration"));
        }
        if (customSettings.containsKey("webGLEnabled")) {
            builder.webGLEnabled((Boolean) customSettings.get("webGLEnabled"));
        }
        if (customSettings.containsKey("memoryLimitMB")) {
            builder.memoryLimitMB((Integer) customSettings.get("memoryLimitMB"));
        }
        // Add more custom settings as needed
        
        return builder.build();
    }
    
    /**
     * Get profile comparison report
     */
    public String getProfileComparisonReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Optimization Profiles Comparison ===\n\n");
        
        for (OptimizationProfile profile : OptimizationProfile.values()) {
            ProfileConfig config = createProfileConfig(profile);
            
            report.append(String.format("=== %s Profile ===\n", profile.getProfileName().toUpperCase()));
            report.append(String.format("Frame Rate: %d FPS\n", config.targetFrameRate));
            report.append(String.format("Hardware Acceleration: %s\n", config.hardwareAcceleration ? "Yes" : "No"));
            report.append(String.format("WebGL: %s\n", config.webGLEnabled ? "Yes" : "No"));
            report.append(String.format("Memory Limit: %d MB\n", config.memoryLimitMB));
            report.append(String.format("Battery Optimization: %d/5\n", config.batteryOptimizationLevel));
            report.append(String.format("Network Optimization: %d/5\n", config.networkOptimizationLevel));
            report.append(String.format("Memory Optimization: %d/5\n", config.memoryOptimizationLevel));
            report.append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Get current profile detailed information
     */
    public String getCurrentProfileReport() {
        if (currentConfig == null) {
            return "No profile currently active";
        }
        
        StringBuilder report = new StringBuilder();
        report.append(String.format("=== Current Profile: %s ===\n", currentProfile.getProfileName()));
        report.append(String.format("Target Frame Rate: %d FPS\n", currentConfig.targetFrameRate));
        report.append(String.format("Hardware Acceleration: %s\n", currentConfig.hardwareAcceleration ? "Enabled" : "Disabled"));
        report.append(String.format("WebGL Enabled: %s\n", currentConfig.webGLEnabled ? "Yes" : "No"));
        report.append(String.format("WebGL Antialiasing: %s\n", currentConfig.webGLAntialiasing ? "Yes" : "No"));
        report.append(String.format("Memory Limit: %d MB\n", currentConfig.memoryLimitMB));
        report.append(String.format("Render Priority: %d\n", currentConfig.renderPriority));
        report.append(String.format("Smooth Scroll: %s\n", currentConfig.enableSmoothScroll ? "Enabled" : "Disabled"));
        report.append(String.format("Hybrid Composition: %s\n", currentConfig.enableHybridComposition ? "Enabled" : "Disabled"));
        report.append(String.format("Cache Mode: %s\n", currentConfig.cacheMode));
        report.append(String.format("JavaScript: %s\n", currentConfig.enableJavaScript ? "Enabled" : "Disabled"));
        report.append(String.format("Battery Optimization Level: %d/5\n", currentConfig.batteryOptimizationLevel));
        report.append(String.format("Network Optimization Level: %d/5\n", currentConfig.networkOptimizationLevel));
        report.append(String.format("Memory Optimization Level: %d/5\n", currentConfig.memoryOptimizationLevel));
        
        return report.toString();
    }
}