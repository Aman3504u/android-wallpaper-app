package com.example.wallpaper.optimization;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced device detection and capability analysis
 * Provides detailed hardware information for optimal WebView configuration
 */
public class AdvancedDeviceDetector {
    
    private static final String TAG = "AdvancedDeviceDetector";
    
    // Device classification constants
    public static final int DEVICE_CLASS_LOW_END = 1;
    public static final int DEVICE_CLASS_MID_RANGE = 2;
    public static final int DEVICE_CLASS_HIGH_END = 3;
    
    // GPU vendor constants
    public static final int GPU_VENDOR_UNKNOWN = 0;
    public static final int GPU_VENDOR_ADRENO = 1;
    public static final int GPU_VENDOR_MALI = 2;
    public static final int GPU_VENDOR_POWERVR = 3;
    public static final int GPU_VENDOR_VIDEOCORE = 4;
    public static final int GPU_VENDOR_INTEL = 5;
    public static final int GPU_VENDOR_ANGLE = 6;
    
    private final Context context;
    private final DeviceProfile deviceProfile;
    
    /**
     * Device profile containing detailed hardware information
     */
    public static class DeviceProfile {
        public final int deviceClass;
        public final int cpuCores;
        public final long totalMemoryMB;
        public final int gpuVendor;
        public final String gpuRenderer;
        public final int gpuVersionMajor;
        public final int gpuVersionMinor;
        public final int openGLESVersion;
        public final boolean supportsVulkan;
        public final boolean supportsOpenGLES30;
        public final boolean supportsOpenGLES31;
        public final boolean supportsOpenGLES32;
        public final int maxTextureSize;
        public final int maxVertexAttribs;
        public final int maxFragmentTextures;
        public final float gpuPerformanceScore;
        public final String socModel;
        public final boolean isEmulator;
        public final String manufacturer;
        public final String model;
        
        public DeviceProfile(Builder builder) {
            this.deviceClass = builder.deviceClass;
            this.cpuCores = builder.cpuCores;
            this.totalMemoryMB = builder.totalMemoryMB;
            this.gpuVendor = builder.gpuVendor;
            this.gpuRenderer = builder.gpuRenderer;
            this.gpuVersionMajor = builder.gpuVersionMajor;
            this.gpuVersionMinor = builder.gpuVersionMinor;
            this.openGLESVersion = builder.openGLESVersion;
            this.supportsVulkan = builder.supportsVulkan;
            this.supportsOpenGLES30 = builder.supportsOpenGLES30;
            this.supportsOpenGLES31 = builder.supportsOpenGLES31;
            this.supportsOpenGLES32 = builder.supportsOpenGLES32;
            this.maxTextureSize = builder.maxTextureSize;
            this.maxVertexAttribs = builder.maxVertexAttribs;
            this.maxFragmentTextures = builder.maxFragmentTextures;
            this.gpuPerformanceScore = builder.gpuPerformanceScore;
            this.socModel = builder.socModel;
            this.isEmulator = builder.isEmulator;
            this.manufacturer = builder.manufacturer;
            this.model = builder.model;
        }
        
        /**
         * Builder class for DeviceProfile
         */
        public static class Builder {
            private int deviceClass = DEVICE_CLASS_LOW_END;
            private int cpuCores = 1;
            private long totalMemoryMB = 512;
            private int gpuVendor = GPU_VENDOR_UNKNOWN;
            private String gpuRenderer = "Unknown";
            private int gpuVersionMajor = 2;
            private int gpuVersionMinor = 0;
            private int openGLESVersion = 0x20000; // OpenGL ES 2.0
            private boolean supportsVulkan = false;
            private boolean supportsOpenGLES30 = false;
            private boolean supportsOpenGLES31 = false;
            private boolean supportsOpenGLES32 = false;
            private int maxTextureSize = 2048;
            private int maxVertexAttribs = 8;
            private int maxFragmentTextures = 8;
            private float gpuPerformanceScore = 1.0f;
            private String socModel = "Unknown";
            private boolean isEmulator = false;
            private String manufacturer = "Unknown";
            private String model = "Unknown";
            
            public Builder deviceClass(int deviceClass) {
                this.deviceClass = deviceClass;
                return this;
            }
            
            public Builder cpuCores(int cpuCores) {
                this.cpuCores = cpuCores;
                return this;
            }
            
            public Builder totalMemoryMB(long totalMemoryMB) {
                this.totalMemoryMB = totalMemoryMB;
                return this;
            }
            
            public Builder gpuVendor(int gpuVendor) {
                this.gpuVendor = gpuVendor;
                return this;
            }
            
            public Builder gpuRenderer(String gpuRenderer) {
                this.gpuRenderer = gpuRenderer;
                return this;
            }
            
            public Builder gpuVersion(int major, int minor) {
                this.gpuVersionMajor = major;
                this.gpuVersionMinor = minor;
                return this;
            }
            
            public Builder openGLESVersion(int version) {
                this.openGLESVersion = version;
                return this;
            }
            
            public Builder supportsVulkan(boolean supportsVulkan) {
                this.supportsVulkan = supportsVulkan;
                return this;
            }
            
            public Builder supportsOpenGLES30(boolean supports) {
                this.supportsOpenGLES30 = supports;
                this.openGLESVersion = Math.max(this.openGLESVersion, 0x30000);
                return this;
            }
            
            public Builder supportsOpenGLES31(boolean supports) {
                this.supportsOpenGLES31 = supports;
                this.openGLESVersion = Math.max(this.openGLESVersion, 0x30001);
                return this;
            }
            
            public Builder supportsOpenGLES32(boolean supports) {
                this.supportsOpenGLES32 = supports;
                this.openGLESVersion = Math.max(this.openGLESVersion, 0x30002);
                return this;
            }
            
            public Builder maxTextureSize(int maxTextureSize) {
                this.maxTextureSize = maxTextureSize;
                return this;
            }
            
            public Builder maxVertexAttribs(int maxVertexAttribs) {
                this.maxVertexAttribs = maxVertexAttribs;
                return this;
            }
            
            public Builder maxFragmentTextures(int maxFragmentTextures) {
                this.maxFragmentTextures = maxFragmentTextures;
                return this;
            }
            
            public Builder gpuPerformanceScore(float score) {
                this.gpuPerformanceScore = score;
                return this;
            }
            
            public Builder socModel(String socModel) {
                this.socModel = socModel;
                return this;
            }
            
            public Builder isEmulator(boolean isEmulator) {
                this.isEmulator = isEmulator = isEmulator;
                return this;
            }
            
            public Builder manufacturer(String manufacturer) {
                this.manufacturer = manufacturer;
                return this;
            }
            
            public Builder model(String model) {
                this.model = model;
                return this;
            }
            
            public DeviceProfile build() {
                return new DeviceProfile(this);
            }
        }
    }
    
    /**
     * Constructor
     */
    public AdvancedDeviceDetector(Context context) {
        this.context = context.getApplicationContext();
        this.deviceProfile = detectDeviceProfile();
    }
    
    /**
     * Detect comprehensive device profile
     */
    private DeviceProfile detectDeviceProfile() {
        DeviceProfile.Builder builder = new DeviceProfile.Builder();
        
        try {
            // Basic device information
            builder.manufacturer(android.os.Build.MANUFACTURER)
                   .model(android.os.Build.MODEL)
                   .isEmulator(isEmulator());
            
            // CPU detection
            int cpuCores = Runtime.getRuntime().availableProcessors();
            builder.cpuCores(cpuCores);
            
            // Memory detection
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);
            long totalMemoryMB = memInfo.totalMem / (1024 * 1024);
            builder.totalMemoryMB(totalMemoryMB);
            
            // GPU and OpenGL detection
            detectGPUCapabilities(builder);
            
            // SoC detection
            builder.socModel(detectSoCModel());
            
            // Device classification
            int deviceClass = classifyDevice(builder.cpuCores, builder.totalMemoryMB, 
                                            builder.gpuPerformanceScore, builder.gpuVendor);
            builder.deviceClass(deviceClass);
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting device profile", e);
            // Return basic profile on error
            builder.deviceClass(DEVICE_CLASS_LOW_END)
                   .cpuCores(1)
                   .totalMemoryMB(512)
                   .gpuVendor(GPU_VENDOR_UNKNOWN);
        }
        
        return builder.build();
    }
    
    /**
     * Detect GPU capabilities using OpenGL ES
     */
    private void detectGPUCapabilities(DeviceProfile.Builder builder) {
        try {
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String version = GLES20.glGetString(GLES20.GL_VERSION);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            
            if (renderer != null) {
                builder.gpuRenderer(renderer);
                Log.d(TAG, "GPU Renderer: " + renderer);
                Log.d(TAG, "GPU Version: " + version);
                Log.d(TAG, "GPU Vendor: " + vendor);
                
                // Detect GPU vendor
                int gpuVendor = detectGPUVendor(renderer, vendor);
                builder.gpuVendor(gpuVendor);
                
                // Parse OpenGL ES version
                detectOpenGLESVersion(version, builder);
                
                // Get GPU capabilities
                getGpuCapabilities(builder);
                
                // Calculate performance score
                float perfScore = calculateGPUPerformanceScore(renderer, gpuVendor);
                builder.gpuPerformanceScore(perfScore);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting GPU capabilities", e);
            builder.gpuVendor(GPU_VENDOR_UNKNOWN)
                   .gpuRenderer("Unknown");
        }
    }
    
    /**
     * Detect GPU vendor from renderer and vendor strings
     */
    private int detectGPUVendor(String renderer, String vendor) {
        String combined = (renderer + " " + vendor).toLowerCase();
        
        if (combined.contains("adreno")) {
            return GPU_VENDOR_ADRENO;
        } else if (combined.contains("mali")) {
            return GPU_VENDOR_MALI;
        } else if (combined.contains("powervr") || combined.contains("imgtech")) {
            return GPU_VENDOR_POWERVR;
        } else if (combined.contains("videocore")) {
            return GPU_VENDOR_VIDEOCORE;
        } else if (combined.contains("intel")) {
            return GPU_VENDOR_INTEL;
        } else if (combined.contains("angle")) {
            return GPU_VENDOR_ANGLE;
        }
        
        return GPU_VENDOR_UNKNOWN;
    }
    
    /**
     * Detect OpenGL ES version
     */
    private void detectOpenGLESVersion(String version, DeviceProfile.Builder builder) {
        if (version == null) return;
        
        // Parse version string format: "OpenGL ES 3.2 ..."
        Pattern pattern = Pattern.compile("OpenGL ES (\\d+)\\.(\\d+)");
        Matcher matcher = pattern.matcher(version);
        
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            
            builder.gpuVersion(major, minor)
                   .supportsOpenGLES30(major >= 3)
                   .supportsOpenGLES31(major > 3 || (major == 3 && minor >= 1))
                   .supportsOpenGLES32(major > 3 || (major == 3 && minor >= 2));
        }
    }
    
    /**
     * Get detailed GPU capabilities
     */
    private void getGpuCapabilities(DeviceProfile.Builder builder) {
        try {
            // Query GPU limits
            int[] params = new int[1];
            
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, params, 0);
            builder.maxTextureSize(params[0]);
            
            GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params, 0);
            builder.maxVertexAttribs(params[0]);
            
            // Some devices report fragment texture limits differently
            try {
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, params, 0);
                builder.maxFragmentTextures(params[0]);
            } catch (Exception e) {
                // Fallback for devices that don't support this query
                builder.maxFragmentTextures(8);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error getting GPU capabilities", e);
            // Use reasonable defaults
            builder.maxTextureSize(2048)
                   .maxVertexAttribs(8)
                   .maxFragmentTextures(8);
        }
    }
    
    /**
     * Calculate GPU performance score
     */
    private float calculateGPUPerformanceScore(String renderer, int vendor) {
        String rendererLower = renderer.toLowerCase();
        
        // Base scores by vendor and model
        float baseScore = 1.0f;
        
        switch (vendor) {
            case GPU_VENDOR_ADRENO:
                if (rendererLower.contains("730") || rendererLower.contains("740") || 
                    rendererLower.contains("750") || rendererLower.contains("660")) {
                    baseScore = 4.5f;
                } else if (rendererLower.contains("530") || rendererLower.contains("540") || 
                          rendererLower.contains("610") || rendererLower.contains("620") || 
                          rendererLower.contains("630") || rendererLower.contains("640") || 
                          rendererLower.contains("650")) {
                    baseScore = 3.5f;
                } else {
                    baseScore = 2.5f;
                }
                break;
                
            case GPU_VENDOR_MALI:
                if (rendererLower.contains("g78") || rendererLower.contains("g77") || 
                    rendererLower.contains("g76") || rendererLower.contains("g72")) {
                    baseScore = 4.0f;
                } else if (rendererLower.contains("g71") || rendererLower.contains("g51") || 
                          rendererLower.contains("t860") || rendererLower.contains("t830")) {
                    baseScore = 3.0f;
                } else {
                    baseScore = 2.0f;
                }
                break;
                
            case GPU_VENDOR_POWERVR:
                baseScore = 2.5f; // Generally good performance
                break;
                
            case GPU_VENDOR_VIDEOCORE:
                baseScore = 1.5f; // Raspberry Pi and similar
                break;
                
            case GPU_VENDOR_INTEL:
                baseScore = 3.0f; // Intel integrated graphics
                break;
                
            default:
                baseScore = 2.0f; // Unknown vendor, moderate performance
                break;
        }
        
        return baseScore;
    }
    
    /**
     * Detect SoC model
     */
    private String detectSoCModel() {
        try {
            // Try to read from system properties
            String socModel = System.getProperty("ro.chipname");
            if (socModel != null && !socModel.isEmpty()) {
                return socModel;
            }
            
            // Alternative methods for different manufacturers
            String hardware = System.getProperty("ro.hardware");
            if (hardware != null && !hardware.isEmpty()) {
                return hardware;
            }
            
            // Read from /proc/cpuinfo as fallback
            return readCpuInfo();
            
        } catch (Exception e) {
            Log.w(TAG, "Error detecting SoC model", e);
            return "Unknown";
        }
    }
    
    /**
     * Read CPU information from /proc/cpuinfo
     */
    private String readCpuInfo() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Hardware")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        return parts[1].trim();
                    }
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Error reading CPU info", e);
        }
        return "Unknown";
    }
    
    /**
     * Detect if running on emulator
     */
    private boolean isEmulator() {
        return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic") ||
                "google_sdk".equals(android.os.Build.PRODUCT));
    }
    
    /**
     * Classify device based on hardware capabilities
     */
    private int classifyDevice(int cpuCores, long memoryMB, float gpuScore, int gpuVendor) {
        int score = 0;
        
        // CPU cores scoring
        if (cpuCores >= 8) score += 3;
        else if (cpuCores >= 6) score += 2;
        else if (cpuCores >= 4) score += 1;
        
        // Memory scoring
        if (memoryMB >= 6144) score += 3;      // 6GB+
        else if (memoryMB >= 4096) score += 2; // 4GB+
        else if (memoryMB >= 2048) score += 1; // 2GB+
        
        // GPU scoring
        if (gpuScore >= 4.0f) score += 3;
        else if (gpuScore >= 3.0f) score += 2;
        else if (gpuScore >= 2.0f) score += 1;
        
        // Vendor bonus
        if (gpuVendor == GPU_VENDOR_ADRENO || gpuVendor == GPU_VENDOR_MALI) {
            score += 1;
        }
        
        if (score >= 7) return DEVICE_CLASS_HIGH_END;
        else if (score >= 4) return DEVICE_CLASS_MID_RANGE;
        else return DEVICE_CLASS_LOW_END;
    }
    
    /**
     * Get optimal WebView settings based on device profile
     */
    public Map<String, Object> getOptimalWebViewSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        switch (deviceProfile.deviceClass) {
            case DEVICE_CLASS_HIGH_END:
                settings.put("hardwareAcceleration", true);
                settings.put("webGLEnabled", true);
                settings.put("webGLAntialiasing", true);
                settings.put("renderPriority", "high");
                settings.put("targetFrameRate", 60);
                settings.put("memoryLimitMB", 256);
                settings.put("enableHybridComposition", true);
                settings.put("enableSmoothScroll", true);
                break;
                
            case DEVICE_CLASS_MID_RANGE:
                settings.put("hardwareAcceleration", true);
                settings.put("webGLEnabled", true);
                settings.put("webGLAntialiasing", false);
                settings.put("renderPriority", "normal");
                settings.put("targetFrameRate", 45);
                settings.put("memoryLimitMB", 128);
                settings.put("enableHybridComposition", true);
                settings.put("enableSmoothScroll", true);
                break;
                
            case DEVICE_CLASS_LOW_END:
            default:
                settings.put("hardwareAcceleration", true);
                settings.put("webGLEnabled", deviceProfile.gpuPerformanceScore >= 2.0f);
                settings.put("webGLAntialiasing", false);
                settings.put("renderPriority", "low");
                settings.put("targetFrameRate", 30);
                settings.put("memoryLimitMB", 64);
                settings.put("enableHybridComposition", false);
                settings.put("enableSmoothScroll", false);
                break;
        }
        
        return settings;
    }
    
    /**
     * Get device-specific performance hints
     */
    public Map<String, Object> getPerformanceHints() {
        Map<String, Object> hints = new HashMap<>();
        
        hints.put("reduceAnimationQuality", deviceProfile.deviceClass == DEVICE_CLASS_LOW_END);
        hints.put("disableWebGL", deviceProfile.gpuPerformanceScore < 2.0f);
        hints.put("reduceTextureQuality", deviceProfile.maxTextureSize < 4096);
        hints.put("limitConcurrentOperations", deviceProfile.cpuCores < 4);
        hints.put("aggressiveMemoryManagement", deviceProfile.totalMemoryMB < 2048);
        hints.put("useSoftwareRendering", deviceProfile.deviceClass == DEVICE_CLASS_LOW_END && 
                                             deviceProfile.gpuVendor == GPU_VENDOR_UNKNOWN);
        
        return hints;
    }
    
    /**
     * Get recommended cache settings
     */
    public Map<String, Object> getRecommendedCacheSettings() {
        Map<String, Object> cacheSettings = new HashMap<>();
        
        long cacheSize = Math.min(deviceProfile.totalMemoryMB * 1024 * 1024 / 8, 50 * 1024 * 1024);
        
        cacheSettings.put("cacheSize", cacheSize);
        cacheSettings.put("enableDiskCache", deviceProfile.deviceClass != DEVICE_CLASS_LOW_END);
        cacheSettings.put("cacheMode", deviceProfile.deviceClass == DEVICE_CLASS_LOW_END ? 
                                    "load_cache_else_network" : "load_default");
        cacheSettings.put("clearCacheOnLowMemory", true);
        
        return cacheSettings;
    }
    
    /**
     * Get device profile
     */
    public DeviceProfile getDeviceProfile() {
        return deviceProfile;
    }
    
    /**
     * Get device classification name
     */
    public String getDeviceClassName() {
        switch (deviceProfile.deviceClass) {
            case DEVICE_CLASS_HIGH_END: return "High-End";
            case DEVICE_CLASS_MID_RANGE: return "Mid-Range";
            case DEVICE_CLASS_LOW_END: return "Low-End";
            default: return "Unknown";
        }
    }
    
    /**
     * Get GPU vendor name
     */
    public String getGPUVendorName() {
        switch (deviceProfile.gpuVendor) {
            case GPU_VENDOR_ADRENO: return "Adreno (Qualcomm)";
            case GPU_VENDOR_MALI: return "Mali (ARM)";
            case GPU_VENDOR_POWERVR: return "PowerVR (Imagination)";
            case GPU_VENDOR_VIDEOCORE: return "VideoCore (Broadcom)";
            case GPU_VENDOR_INTEL: return "Intel";
            case GPU_VENDOR_ANGLE: return "ANGLE";
            default: return "Unknown";
        }
    }
    
    /**
     * Get detailed device information report
     */
    public String getDeviceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Advanced Device Detection Report ===\n\n");
        
        report.append("=== Basic Information ===\n");
        report.append(String.format("Manufacturer: %s\n", deviceProfile.manufacturer));
        report.append(String.format("Model: %s\n", deviceProfile.model));
        report.append(String.format("SoC: %s\n", deviceProfile.socModel));
        report.append(String.format("Is Emulator: %s\n", deviceProfile.isEmulator ? "Yes" : "No"));
        
        report.append("\n=== Hardware Capabilities ===\n");
        report.append(String.format("CPU Cores: %d\n", deviceProfile.cpuCores));
        report.append(String.format("Total Memory: %dMB\n", deviceProfile.totalMemoryMB));
        report.append(String.format("Device Class: %s (%d)\n", getDeviceClassName(), deviceProfile.deviceClass));
        
        report.append("\n=== GPU Information ===\n");
        report.append(String.format("GPU Vendor: %s (%d)\n", getGPUVendorName(), deviceProfile.gpuVendor));
        report.append(String.format("GPU Renderer: %s\n", deviceProfile.gpuRenderer));
        report.append(String.format("GPU Performance Score: %.1f\n", deviceProfile.gpuPerformanceScore));
        report.append(String.format("OpenGL ES Version: %d.%d\n", deviceProfile.gpuVersionMajor, deviceProfile.gpuVersionMinor));
        report.append(String.format("Max Texture Size: %d\n", deviceProfile.maxTextureSize));
        report.append(String.format("Max Vertex Attributes: %d\n", deviceProfile.maxVertexAttribs));
        report.append(String.format("Max Fragment Textures: %d\n", deviceProfile.maxFragmentTextures));
        
        report.append("\n=== API Support ===\n");
        report.append(String.format("OpenGL ES 3.0: %s\n", deviceProfile.supportsOpenGLES30 ? "Yes" : "No"));
        report.append(String.format("OpenGL ES 3.1: %s\n", deviceProfile.supportsOpenGLES31 ? "Yes" : "No"));
        report.append(String.format("OpenGL ES 3.2: %s\n", deviceProfile.supportsOpenGLES32 ? "Yes" : "No"));
        report.append(String.format("Vulkan: %s\n", deviceProfile.supportsVulkan ? "Yes" : "No"));
        
        report.append("\n=== Recommended WebView Settings ===\n");
        Map<String, Object> webViewSettings = getOptimalWebViewSettings();
        for (Map.Entry<String, Object> entry : webViewSettings.entrySet()) {
            report.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
        }
        
        report.append("\n=== Performance Hints ===\n");
        Map<String, Object> hints = getPerformanceHints();
        for (Map.Entry<String, Object> entry : hints.entrySet()) {
            report.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
        }
        
        return report.toString();
    }
}