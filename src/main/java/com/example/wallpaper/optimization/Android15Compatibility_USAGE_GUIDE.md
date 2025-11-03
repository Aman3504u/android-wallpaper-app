# Android 15 Compatibility Layer Usage Guide

## Overview

The `Android15Compatibility` class provides a comprehensive compatibility layer for wallpaper applications targeting Android 15 and backward compatibility to older Android versions.

## Features

### 1. Predictive Back Gesture Support (API 34+)
- Enhanced back gesture detection and handling
- Smooth transition animations
- Proper integration with Android's system back handling
- Fallback support for older versions

### 2. Enhanced Wallpaper Service APIs
- Optimized wallpaper rendering with memory management
- Support for high-resolution wallpapers (up to 4K)
- Automatic bitmap optimization and scaling
- Enhanced error handling and recovery

### 3. New Permission Handling Patterns (Android 13+)
- Centralized permission management
- Support for new Android 13+ permissions
- Rational handling of permission denials
- Automatic permission checking and requesting

### 4. Background App Restrictions Compliance
- Monitoring background restrictions
- Automatic adaptation to background execution limits
- Battery optimization compliance
- Quick settings integration

### 5. Wallpaper Rendering Optimizations
- Hardware acceleration optimizations
- Memory-efficient bitmap handling
- Background rendering thread
- Automatic memory cleanup

### 6. Security Enhancements for Android 15
- Taint tracking support
- Secure mode initialization
- Enhanced privacy protections
- Background security monitoring

## Usage Examples

### Basic Initialization

```java
public class MainActivity extends ComponentActivity {
    private Android15Compatibility android15Compatibility;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize compatibility layer
        android15Compatibility = new Android15Compatibility(this);
        
        // Set up callbacks
        android15Compatibility.setCompatibilityCallback(new CompatibilityCallback() {
            @Override
            public void onBackgroundStateChanged(boolean isInBackground) {
                // Handle background state changes
                Log.d("MainActivity", "App background state: " + isInBackground);
            }
            
            @Override
            public void onSecurityEvent(String event, String details) {
                Log.d("MainActivity", "Security event: " + event + " - " + details);
            }
            
            @Override
            public void onOptimizationApplied(String optimization) {
                Log.d("MainActivity", "Optimization applied: " + optimization);
            }
            
            @Override
            public void onError(String error, Throwable throwable) {
                Log.e("MainActivity", "Compatibility error: " + error, throwable);
            }
        });
        
        // Setup predictive back gestures (if supported)
        android15Compatibility.setupOnBackPressedDispatcher(this);
    }
}
```

### Enhanced Wallpaper Operations

```java
public class WallpaperManagerActivity extends Activity {
    
    public void setOptimizedWallpaper(Bitmap sourceBitmap) {
        Android15Compatibility.EnhancedWallpaperService wallpaperService = 
            android15Compatibility.getEnhancedWallpaperService();
        
        // Create optimized wallpaper with automatic scaling
        wallpaperService.createOptimizedWallpaperBitmap(sourceBitmap, 
            new WallpaperCreationCallback() {
                @Override
                public void onWallpaperReady(Bitmap optimizedBitmap) {
                    if (optimizedBitmap != null) {
                        // Set the optimized wallpaper
                        wallpaperService.setWallpaperWithFallback(optimizedBitmap,
                            new SetWallpaperCallback() {
                                @Override
                                public void onSetComplete(boolean success, String message) {
                                    if (success) {
                                        Toast.makeText(WallpaperManagerActivity.this,
                                            "Wallpaper set successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(WallpaperManagerActivity.this,
                                            "Failed to set wallpaper: " + message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                    }
                }
            });
    }
    
    public void getOptimalWallpaperDimensions() {
        Android15Compatibility.EnhancedWallpaperService wallpaperService = 
            android15Compatibility.getEnhancedWallpaperService();
        
        wallpaperService.getOptimalWallpaperDimensions(
            new WallpaperDimensionsCallback() {
                @Override
                public void onDimensionsReady(int width, int height) {
                    Log.d("WallpaperManager", "Optimal dimensions: " + width + "x" + height);
                    // Use these dimensions for wallpaper creation
                }
            });
    }
}
```

### Permission Handling

```java
public class SettingsActivity extends Activity {
    
    private void requestNotificationPermission() {
        PermissionHandler permissionHandler = new PermissionHandler(this,
            new PermissionCallback() {
                @Override
                public void onPermissionGranted(PermissionHandler.PermissionType permissionType) {
                    Toast.makeText(SettingsActivity.this,
                        permissionType.name() + " permission granted", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onPermissionDenied(PermissionHandler.PermissionType permissionType) {
                    Toast.makeText(SettingsActivity.this,
                        permissionType.name() + " permission denied", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onPermissionRationale(PermissionHandler.PermissionType permissionType) {
                    // Show explanation dialog
                    showPermissionRationale(permissionType);
                }
                
                @Override
                public void onPermissionError(PermissionHandler.PermissionType permissionType, 
                                            String error) {
                    Log.e("SettingsActivity", "Permission error: " + error);
                }
            });
        
        // Check if permission is already granted
        if (!permissionHandler.isPermissionGranted(
                PermissionHandler.PermissionType.NOTIFICATION)) {
            
            // Request permission
            permissionHandler.requestPermission(this,
                PermissionHandler.PermissionType.NOTIFICATION);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, 
                                         int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Handle permission results through the compatibility layer
        if (android15Compatibility != null) {
            PermissionHandler permissionHandler = new PermissionHandler(this, null);
            permissionHandler.onPermissionResult(requestCode, permissions, grantResults);
        }
    }
    
    private void showPermissionRationale(PermissionHandler.PermissionType permissionType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
               .setMessage("This app needs " + permissionType.name() + 
                          " permission to function properly.")
               .setPositiveButton("Grant", (dialog, which) -> {
                   // Request permission after showing rationale
                   PermissionHandler permissionHandler = new PermissionHandler(this, null);
                   permissionHandler.requestPermission(this, permissionType);
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
               .show();
    }
}
```

### Integration with Wallpaper Service

```java
public class OptimizedWallpaperService extends WallpaperService {
    private Android15Compatibility compatibility;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize compatibility layer
        compatibility = new Android15Compatibility(this);
        
        // Apply wallpaper optimizations
        if (Android15Compatibility.isAndroid15OrAbove()) {
            // Use Android 15 specific optimizations
            Log.i("WallpaperService", "Android 15 optimizations enabled");
        } else {
            // Use backward compatible features
            Log.i("WallpaperService", "Backward compatibility mode");
        }
    }
    
    @Override
    public void onDestroy() {
        // Cleanup compatibility layer
        if (compatibility != null) {
            compatibility.cleanup();
        }
        super.onDestroy();
    }
    
    @Override
    public Engine onCreateEngine() {
        return new OptimizedWallpaperEngine();
    }
    
    private class OptimizedWallpaperEngine extends Engine {
        // Custom engine implementation using compatibility features
    }
}
```

## API Compatibility Matrix

| Feature | Android 15 (API 35) | Android 14 (API 34) | Android 13 (API 33) | Android 12 (API 31) | Older Versions |
|---------|---------------------|---------------------|---------------------|---------------------|----------------|
| Predictive Back Gestures | ✅ Full | ✅ Full | ❌ Partial | ❌ Partial | ❌ Partial |
| Enhanced Wallpaper APIs | ✅ Full | ✅ Full | ✅ Full | ✅ Full | ✅ Partial |
| New Permission Patterns | ✅ Full | ✅ Full | ✅ Full | ❌ Partial | ❌ Partial |
| Background Restrictions | ✅ Full | ✅ Full | ✅ Full | ✅ Full | ✅ Full |
| Rendering Optimizations | ✅ Full | ✅ Full | ✅ Full | ✅ Full | ✅ Full |
| Security Enhancements | ✅ Full | ✅ Partial | ❌ Partial | ❌ Partial | ❌ Partial |

## Configuration

### Android Manifest Permissions

```xml
<!-- Add required permissions based on your usage -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.SET_WALLPAPER" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- For wallpaper service -->
<service
    android:name=".service.OptimizedWallpaperService"
    android:label="@string/service_name"
    android:permission="android.permission.BIND_WALLPAPER"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.wallpaper.WallpaperService" />
    </intent-filter>
    <meta-data
        android:name="android.service.wallpaper"
        android:resource="@xml/wallpaper" />
</service>
```

### Gradle Configuration

```gradle
android {
    compileSdk 35
    
    defaultConfig {
        minSdk 21
        targetSdk 35
    }
    
    buildTypes {
        debug {
            debuggable true
        }
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                         'proguard-rules.pro'
        }
    }
}
```

## Error Handling

The compatibility layer includes comprehensive error handling:

```java
// Example error handling callback
android15Compatibility.setCompatibilityCallback(new CompatibilityCallback() {
    @Override
    public void onError(String error, Throwable throwable) {
        // Log error for debugging
        Log.e("Compatibility", "Error: " + error, throwable);
        
        // Show user-friendly message if needed
        if (throwable instanceof OutOfMemoryError) {
            showMemoryErrorDialog();
        } else if (throwable instanceof SecurityException) {
            showPermissionErrorDialog();
        }
    }
    
    // ... other callback methods
});
```

## Performance Optimization

### Memory Management
- Automatic bitmap recycling
- Memory usage monitoring
- Background thread optimization
- Virtual display management

### Battery Optimization
- Background execution limits compliance
- Reduced CPU usage in background
- Scheduled task optimization
- Wake lock management

## Security Considerations

- Privacy data protection
- Secure wallpaper storage
- Permission validation
- Background activity monitoring

## Testing

### Compatibility Testing

```java
// Test different API levels
@Test
public void testAndroid15Features() {
    if (Android15Compatibility.isAndroid15OrAbove()) {
        // Test Android 15 specific features
        assertTrue(compatibility.isPredictiveBackEnabled());
    } else {
        // Test fallback behavior
        assertFalse(compatibility.isPredictiveBackEnabled());
    }
}

@Test
public void testPermissionHandling() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Android15Compatibility compatibility = new Android15Compatibility(context);
    
    PermissionHandler handler = new PermissionHandler(context, null);
    
    // Test permission checking
    assertNotNull(handler);
    // Test permission requesting logic
}
```

## Migration Guide

### From Existing Code

1. Replace direct permission handling with `PermissionHandler`
2. Replace manual wallpaper operations with `EnhancedWallpaperService`
3. Add background restriction monitoring
4. Integrate predictive back gesture handling
5. Implement security enhancements

### Best Practices

1. Always check API level before using newer features
2. Provide fallback implementations for older versions
3. Handle permissions gracefully with user feedback
4. Monitor background state and adapt behavior
5. Clean up resources properly in `onDestroy()`

## Troubleshooting

### Common Issues

1. **Permission Denials**: Use rationale handling for better UX
2. **Memory Issues**: Enable bitmap recycling and optimize sizes
3. **Background Restrictions**: Monitor app state changes
4. **Rendering Problems**: Check display dimensions and scaling

### Debug Information

Enable debug logging:
```java
private static final boolean DEBUG = true; // In Android15Compatibility class
```

## Conclusion

The Android 15 Compatibility Layer provides a robust foundation for wallpaper applications to support modern Android features while maintaining backward compatibility. Use this guide to integrate the compatibility layer effectively into your wallpaper application.