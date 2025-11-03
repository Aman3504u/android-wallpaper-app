package com.example.wallpaper.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive crash handling utility for the Android Wallpaper application.
 * 
 * This class provides:
 * - Global uncaught exception handler
 * - Detailed crash logging with system information
 * - Crash report persistence to disk
 * - Automatic cleanup of old crash reports
 * - Memory and performance metrics collection
 * - Thread-specific crash analysis
 * - Battery and network state logging
 * 
 * The CrashHandler automatically captures:
 * - Stack traces with detailed line information
 * - Device and OS information
 * - Memory usage at crash time
 * - App version and build information
 * - Running services and processes
 * - Network connectivity status
 * - Battery level and charging state
 * 
 * Usage:
 * <pre>
 * // Initialize in Application class or main activity
 * CrashHandler.getInstance().initialize(context);
 * 
 * // Add custom crash listener
 * CrashHandler.getInstance().setCrashListener(new CrashListener() {
 *     void onCrash(CrashReport report) {
 *         // Handle crash report (e.g., send to server)
 *     }
 * });
 * </pre>
 * 
 * @author Android Wallpaper Team
 * @version 1.0
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    
    // Configuration constants
    private static final int MAX_CRASH_REPORTS = 10;
    private static final long MAX_CRASH_REPORT_AGE_DAYS = 30;
    private static final String CRASH_REPORTS_DIR = "crash_reports";
    private static final String CRASH_LOG_FILE_PREFIX = "crash_";
    private static final String CRASH_LOG_FILE_EXTENSION = ".log";
    private static final String THREAD_NAME_MAIN = "main";
    private static final String THREAD_NAME_WORKER = "worker";
    
    // System property keys for device information
    private static final String PROP_MANUFACTURER = "ro.product.manufacturer";
    private static final String PROP_MODEL = "ro.product.model";
    private static final String PROP_BRAND = "ro.product.brand";
    private static final String PROP_DEVICE = "ro.product.device";
    
    // Singleton instance
    private static CrashHandler sInstance;
    
    // Context and executors
    private final Context mContext;
    private final ExecutorService mExecutor;
    
    // Crash reporting
    private CrashListener mCrashListener;
    private boolean mIsInitialized = false;
    
    /**
     * Private constructor for singleton pattern
     */
    private CrashHandler(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CrashHandler-Worker");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, "CrashHandler worker thread crashed", e);
                }
            });
            return t;
        });
    }
    
    /**
     * Get singleton instance of CrashHandler
     */
    public static synchronized CrashHandler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrashHandler(context);
        }
        return sInstance;
    }
    
    /**
     * Uncaught exception handler implementation
     * This is called when a thread terminates due to an uncaught exception
     */
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Store original handler
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        try {
            handleUncaughtException(thread, throwable, defaultHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error in crash handler", e);
            // Fall back to original handler
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        }
    }
    
    /**
     * Initialize the crash handler with global exception handling
     */
    public synchronized void initialize() {
        if (mIsInitialized) {
            Log.w(TAG, "CrashHandler already initialized");
            return;
        }
        
        Log.i(TAG, "Initializing CrashHandler");
        
        // Store original handler
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        // Set our custom handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                handleUncaughtException(thread, throwable, defaultHandler);
            } catch (Exception e) {
                Log.e(TAG, "Error in crash handler", e);
                // Fall back to original handler
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
        
        // Clean up old crash reports
        cleanupOldCrashReports();
        
        mIsInitialized = true;
        Log.i(TAG, "CrashHandler initialized successfully");
    }
    
    /**
     * Set crash listener for custom crash handling
     */
    public void setCrashListener(CrashListener listener) {
        mCrashListener = listener;
    }
    
    /**
     * Manually log a crash or exception
     */
    public void logException(Throwable throwable) {
        logException(throwable, null);
    }
    
    /**
     * Manually log a crash or exception with context
     */
    public void logException(Throwable throwable, String context) {
        if (throwable == null) {
            Log.w(TAG, "Attempted to log null exception");
            return;
        }
        
        mExecutor.execute(() -> {
            try {
                CrashReport report = createCrashReport(throwable, context);
                saveCrashReport(report);
                
                Log.e(TAG, "Manual exception logged: " + throwable.getMessage(), throwable);
                
                if (mCrashListener != null) {
                    try {
                        mCrashListener.onCrash(report);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in crash listener", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error logging exception", e);
            }
        });
    }
    
    /**
     * Get all saved crash reports
     */
    public File[] getCrashReports() {
        File reportsDir = getCrashReportsDir();
        if (reportsDir.exists()) {
            File[] files = reportsDir.listFiles((dir, name) -> 
                name.startsWith(CRASH_LOG_FILE_PREFIX) && name.endsWith(CRASH_LOG_FILE_EXTENSION));
            if (files != null) {
                // Sort by modification time, newest first
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                return files;
            }
        }
        return new File[0];
    }
    
    /**
     * Clear all saved crash reports
     */
    public void clearCrashReports() {
        mExecutor.execute(() -> {
            File[] reports = getCrashReports();
            for (File report : reports) {
                try {
                    if (report.delete()) {
                        Log.d(TAG, "Deleted crash report: " + report.getName());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting crash report: " + report.getName(), e);
                }
            }
        });
    }
    
    /**
     * Check if CrashHandler is initialized
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }
    
    /**
     * Shutdown crash handler and cleanup resources
     */
    public void shutdown() {
        Log.i(TAG, "Shutting down CrashHandler");
        
        mIsInitialized = false;
        
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
            try {
                if (!mExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    mExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                mExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Private methods
    
    private void handleUncaughtException(Thread thread, Throwable throwable, 
                                       Thread.UncaughtExceptionHandler originalHandler) {
        Log.e(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
        
        // Save crash report
        mExecutor.execute(() -> {
            try {
                CrashReport report = createCrashReport(throwable, "Uncaught Exception");
                saveCrashReport(report);
                
                if (mCrashListener != null) {
                    try {
                        mCrashListener.onCrash(report);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in crash listener", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving crash report", e);
            }
        });
        
        // Log the crash details
        Log.e(TAG, "CRASH DETAILS:");
        Log.e(TAG, "Thread: " + thread.getName() + " (ID: " + thread.getId() + ")");
        Log.e(TAG, "Thread Priority: " + thread.getPriority());
        Log.e(TAG, "Thread State: " + thread.getState());
        Log.e(TAG, "Is Daemon: " + thread.isDaemon());
        
        // Log memory information
        logMemoryInfo();
        
        // Log system information
        logSystemInfo();
        
        // Wait a bit for async operations to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Call original handler
        if (originalHandler != null) {
            originalHandler.uncaughtException(thread, throwable);
        }
    }
    
    private CrashReport createCrashReport(Throwable throwable, String context) {
        CrashReport report = new CrashReport();
        report.timestamp = System.currentTimeMillis();
        report.threadName = Thread.currentThread().getName();
        report.threadId = Thread.currentThread().getId();
        report.exceptionType = throwable.getClass().getSimpleName();
        report.exceptionMessage = throwable.getMessage();
        report.context = context;
        report.stackTrace = getStackTraceString(throwable);
        report.causes = getCauses(throwable);
        
        // Collect system information
        collectSystemInfo(report);
        collectMemoryInfo(report);
        collectProcessInfo(report);
        collectNetworkInfo(report);
        collectBatteryInfo(report);
        
        return report;
    }
    
    private void saveCrashReport(CrashReport report) {
        try {
            File reportsDir = getCrashReportsDir();
            if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                Log.e(TAG, "Failed to create crash reports directory");
                return;
            }
            
            String fileName = CRASH_LOG_FILE_PREFIX + 
                            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date(report.timestamp)) + 
                            CRASH_LOG_FILE_EXTENSION;
            File reportFile = new File(reportsDir, fileName);
            
            try (FileWriter writer = new FileWriter(reportFile)) {
                writeCrashReport(writer, report);
            }
            
            Log.i(TAG, "Crash report saved: " + fileName);
            
            // Cleanup old reports
            cleanupOldCrashReports();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving crash report", e);
        }
    }
    
    private void writeCrashReport(FileWriter writer, CrashReport report) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        
        writer.append("=== ANDROID WALLPAPER CRASH REPORT ===\n");
        writer.append("Report Date: ").append(dateFormat.format(new Date(report.timestamp))).append('\n');
        writer.append("===========================================\n\n");
        
        writer.append("CRASH INFORMATION:\n");
        writer.append("------------------\n");
        writer.append("Thread Name: ").append(report.threadName).append('\n');
        writer.append("Thread ID: ").append(String.valueOf(report.threadId)).append('\n');
        writer.append("Exception Type: ").append(report.exceptionType).append('\n');
        writer.append("Exception Message: ").append(report.exceptionMessage != null ? report.exceptionMessage : "None").append('\n');
        writer.append("Context: ").append(report.context != null ? report.context : "None").append('\n');
        writer.append("Is Main Thread: ").append(String.valueOf(THREAD_NAME_MAIN.equals(report.threadName))).append('\n');
        writer.append("Is Daemon Thread: ").append(String.valueOf(Thread.currentThread().isDaemon())).append('\n');
        writer.append('\n');
        
        writer.append("SYSTEM INFORMATION:\n");
        writer.append("-------------------\n");
        writer.append("OS Version: ").append(Build.VERSION.RELEASE).append('\n');
        writer.append("API Level: ").append(String.valueOf(Build.VERSION.SDK_INT)).append('\n');
        writer.append("Device: ").append(Build.DEVICE).append('\n');
        writer.append("Model: ").append(Build.MODEL).append('\n');
        writer.append("Brand: ").append(Build.BRAND).append('\n');
        writer.append("Manufacturer: ").append(getSystemProperty(PROP_MANUFACTURER, "Unknown")).append('\n');
        writer.append("Product: ").append(Build.PRODUCT).append('\n');
        writer.append("Build Type: ").append(Build.TYPE).append('\n');
        writer.append("Build Tags: ").append(Build.TAGS).append('\n');
        writer.append("Build Time: ").append(String.valueOf(Build.TIME)).append('\n');
        writer.append('\n');
        
        writer.append("APP INFORMATION:\n");
        writer.append("----------------\n");
        writer.append("Package Name: ").append(mContext.getPackageName()).append('\n');
        writer.append("Version Code: ").append(String.valueOf(getVersionCode())).append('\n');
        writer.append("Version Name: ").append(getVersionName()).append('\n');
        writer.append("Debug Build: ").append(String.valueOf(com.example.wallpaper.BuildConfig.DEBUG)).append('\n');
        writer.append('\n');
        
        writer.append("MEMORY INFORMATION:\n");
        writer.append("-------------------\n");
        writer.append("Total Memory: ").append(String.valueOf(report.totalMemory / 1024 / 1024)).append(" MB\n");
        writer.append("Available Memory: ").append(String.valueOf(report.availableMemory / 1024 / 1024)).append(" MB\n");
        writer.append("Used Memory: ").append(String.valueOf(report.usedMemory / 1024 / 1024)).append(" MB\n");
        writer.append("Memory Class: ").append(String.valueOf(report.memoryClass)).append(" MB\n");
        writer.append("Large Memory Class: ").append(String.valueOf(report.largeMemoryClass)).append(" MB\n");
        writer.append("Low Memory Mode: ").append(String.valueOf(report.lowMemoryMode)).append('\n');
        writer.append("Native Heap Size: ").append(String.valueOf(report.nativeHeapSize / 1024 / 1024)).append(" MB\n");
        writer.append("Native Heap Alloc: ").append(String.valueOf(report.nativeHeapAllocatedSize / 1024 / 1024)).append(" MB\n");
        writer.append("Native Heap Free: ").append(String.valueOf(report.nativeHeapFreeSize / 1024 / 1024)).append(" MB\n");
        writer.append('\n');
        
        writer.append("PROCESS INFORMATION:\n");
        writer.append("--------------------\n");
        writer.append("Process Name: ").append(report.processName).append('\n');
        writer.append("PID: ").append(String.valueOf(report.pid)).append('\n');
        writer.append("Running Processes Count: ").append(report.runningProcessesCount >= 0 ? String.valueOf(report.runningProcessesCount) : "N/A").append('\n');
        writer.append("Background App Processes Count: ").append(report.backgroundAppProcessesCount >= 0 ? String.valueOf(report.backgroundAppProcessesCount) : "N/A").append('\n');
        writer.append('\n');
        
        if (report.networkType != null) {
            writer.append("NETWORK INFORMATION:\n");
            writer.append("--------------------\n");
            writer.append("Network Type: ").append(report.networkType).append('\n');
            writer.append("Network Connected: ").append(String.valueOf(report.networkConnected)).append('\n');
            writer.append('\n');
        }
        
        if (report.batteryLevel >= 0) {
            writer.append("BATTERY INFORMATION:\n");
            writer.append("--------------------\n");
            writer.append("Battery Level: ").append(String.valueOf(report.batteryLevel)).append("%\n");
            writer.append("Battery Status: ").append(report.batteryStatus != null ? report.batteryStatus : "Unknown").append('\n');
            writer.append("Power Connected: ").append(String.valueOf(report.powerConnected)).append('\n');
            writer.append('\n');
        }
        
        writer.append("STACK TRACE:\n");
        writer.append("------------\n");
        writer.append(report.stackTrace);
        
        if (report.causes != null && !report.causes.isEmpty()) {
            writer.append("\n\nCAUSE CHAINS:\n");
            writer.append("------------\n");
            for (String cause : report.causes) {
                writer.append(cause).append("\n");
            }
        }
        
        writer.append("\n\n=== END OF REPORT ===\n");
    }
    
    private void collectSystemInfo(CrashReport report) {
        try {
            report.osVersion = Build.VERSION.RELEASE;
            report.apiLevel = Build.VERSION.SDK_INT;
            report.deviceModel = Build.MODEL;
            report.deviceBrand = Build.BRAND;
            report.deviceManufacturer = getSystemProperty(PROP_MANUFACTURER, "Unknown");
            report.productName = Build.PRODUCT;
            report.buildType = Build.TYPE;
            report.buildTags = Build.TAGS;
            report.buildTime = Build.TIME;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting system info", e);
        }
    }
    
    private void collectMemoryInfo(CrashReport report) {
        try {
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memoryInfo);
            
            report.totalMemory = memoryInfo.totalMem;
            report.availableMemory = memoryInfo.availMem;
            report.usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
            report.lowMemoryMode = memoryInfo.lowMemory;
            
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);
            if (am.isLowRamDevice()) {
                report.memoryClass = (int) (memInfo.totalMem / 1024 / 1024);
            } else {
                report.memoryClass = am.getMemoryClass();
            }
            report.largeMemoryClass = am.getLargeMemoryClass();
            
            Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(debugMemoryInfo);
            report.nativeHeapSize = Debug.getNativeHeapSize();
            report.nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize();
            report.nativeHeapFreeSize = Debug.getNativeHeapFreeSize();
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting memory info", e);
        }
    }
    
    private void collectProcessInfo(CrashReport report) {
        try {
            report.processName = mContext.getPackageName();
            report.pid = android.os.Process.myPid();
            
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                try {
                    // Note: getAppMemoryInfos() was deprecated and removed in API 35
                    // Using alternative approach for counting background processes
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        report.runningProcessesCount = am.getRunningAppProcesses().size();
                    } else {
                        // For API 35+, use getRunningAppProcesses() with fallback
                        try {
                            report.runningProcessesCount = am.getRunningAppProcesses().size();
                        } catch (Exception e) {
                            Log.w(TAG, "getRunningAppProcesses() not available on API " + Build.VERSION.SDK_INT);
                            report.runningProcessesCount = -1;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting running app processes", e);
                    report.runningProcessesCount = -1;
                }
                
                // Note: getAppMemoryInfos() and AppMemoryUsage class removed in API 35
                // Setting to -1 to indicate not available
                report.backgroundAppProcessesCount = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error collecting process info", e);
        }
    }
    
    private void collectNetworkInfo(CrashReport report) {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    report.networkConnected = activeNetwork.isConnected();
                    report.networkType = activeNetwork.getTypeName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error collecting network info", e);
        }
    }
    
    private void collectBatteryInfo(CrashReport report) {
        try {
            Intent batteryIntent = mContext.registerReceiver(null, 
                new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    report.batteryLevel = (int) ((level * 100.0f) / scale);
                }
                
                int status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                report.powerConnected = (status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                       status == android.os.BatteryManager.BATTERY_STATUS_FULL);
                
                switch (status) {
                    case android.os.BatteryManager.BATTERY_STATUS_CHARGING:
                        report.batteryStatus = "Charging";
                        break;
                    case android.os.BatteryManager.BATTERY_STATUS_FULL:
                        report.batteryStatus = "Full";
                        break;
                    case android.os.BatteryManager.BATTERY_STATUS_DISCHARGING:
                        report.batteryStatus = "Discharging";
                        break;
                    case android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        report.batteryStatus = "Not Charging";
                        break;
                    case android.os.BatteryManager.BATTERY_STATUS_UNKNOWN:
                        report.batteryStatus = "Unknown";
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error collecting battery info", e);
        }
    }
    
    private void logMemoryInfo() {
        try {
            Log.d(TAG, "Memory Info:");
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memoryInfo);
            
            Log.d(TAG, "  Total Memory: " + (memoryInfo.totalMem / 1024 / 1024) + " MB");
            Log.d(TAG, "  Available Memory: " + (memoryInfo.availMem / 1024 / 1024) + " MB");
            Log.d(TAG, "  Low Memory: " + memoryInfo.lowMemory);
            Log.d(TAG, "  Native Heap: " + (Debug.getNativeHeapSize() / 1024 / 1024) + " MB");
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging memory info", e);
        }
    }
    
    private void logSystemInfo() {
        try {
            Log.d(TAG, "System Info:");
            Log.d(TAG, "  Device: " + Build.DEVICE);
            Log.d(TAG, "  Model: " + Build.MODEL);
            Log.d(TAG, "  Brand: " + Build.BRAND);
            Log.d(TAG, "  Manufacturer: " + getSystemProperty(PROP_MANUFACTURER, "Unknown"));
            Log.d(TAG, "  OS Version: " + Build.VERSION.RELEASE);
            Log.d(TAG, "  API Level: " + Build.VERSION.SDK_INT);
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging system info", e);
        }
    }
    
    private String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return "No stack trace available";
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    private java.util.List<String> getCauses(Throwable throwable) {
        java.util.List<String> causes = new java.util.ArrayList<>();
        Throwable cause = throwable.getCause();
        
        int causeCount = 1;
        while (cause != null && causeCount <= 10) { // Limit to prevent infinite loops
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            cause.printStackTrace(pw);
            causes.add("Cause " + causeCount + ": " + cause.getClass().getSimpleName() + ": " + 
                      (cause.getMessage() != null ? cause.getMessage() : "No message") + "\n" + sw.toString());
            
            cause = cause.getCause();
            causeCount++;
        }
        
        return causes;
    }
    
    private String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = systemProperties.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private int getVersionCode() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode();
            } else {
                return packageInfo.versionCode;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting version code", e);
            return -1;
        }
    }
    
    private String getVersionName() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting version name", e);
            return "Unknown";
        }
    }
    
    private File getCrashReportsDir() {
        File externalDir = mContext.getExternalFilesDir(null);
        if (externalDir == null) {
            // Fallback to internal storage
            return new File(mContext.getFilesDir(), CRASH_REPORTS_DIR);
        }
        return new File(externalDir, CRASH_REPORTS_DIR);
    }
    
    private void cleanupOldCrashReports() {
        mExecutor.execute(() -> {
            try {
                File reportsDir = getCrashReportsDir();
                if (!reportsDir.exists()) {
                    return;
                }
                
                File[] files = reportsDir.listFiles((dir, name) -> 
                    name.startsWith(CRASH_LOG_FILE_PREFIX) && name.endsWith(CRASH_LOG_FILE_EXTENSION));
                
                if (files == null || files.length <= MAX_CRASH_REPORTS) {
                    return;
                }
                
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                
                // Delete oldest files
                int filesToDelete = files.length - MAX_CRASH_REPORTS;
                for (int i = 0; i < filesToDelete; i++) {
                    if (files[i].delete()) {
                        Log.d(TAG, "Deleted old crash report: " + files[i].getName());
                    }
                }
                
                // Delete old files based on age
                long currentTime = System.currentTimeMillis();
                long maxAge = MAX_CRASH_REPORT_AGE_DAYS * 24 * 60 * 60 * 1000;
                
                for (File file : files) {
                    if ((currentTime - file.lastModified()) > maxAge) {
                        if (file.delete()) {
                            Log.d(TAG, "Deleted aged crash report: " + file.getName());
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up old crash reports", e);
            }
        });
    }
    
    // Inner classes and interfaces
    
    /**
     * Data holder for crash report information
     */
    public static class CrashReport {
        // Timestamp and thread info
        public long timestamp;
        public String threadName;
        public long threadId;
        
        // Exception details
        public String exceptionType;
        public String exceptionMessage;
        public String context;
        public String stackTrace;
        public java.util.List<String> causes;
        
        // System information
        public String osVersion;
        public int apiLevel;
        public String deviceModel;
        public String deviceBrand;
        public String deviceManufacturer;
        public String productName;
        public String buildType;
        public String buildTags;
        public long buildTime;
        
        // App information
        public int versionCode;
        public String versionName;
        
        // Memory information
        public long totalMemory;
        public long availableMemory;
        public long usedMemory;
        public int memoryClass;
        public int largeMemoryClass;
        public boolean lowMemoryMode;
        public long nativeHeapSize;
        public long nativeHeapAllocatedSize;
        public long nativeHeapFreeSize;
        
        // Process information
        public String processName;
        public int pid;
        public int runningProcessesCount;
        public int backgroundAppProcessesCount;
        
        // Network information
        public String networkType;
        public boolean networkConnected;
        
        // Battery information
        public int batteryLevel = -1;
        public String batteryStatus;
        public boolean powerConnected;
    }
    
    /**
     * Interface for listening to crashes
     */
    public interface CrashListener {
        void onCrash(CrashReport report);
    }
}
