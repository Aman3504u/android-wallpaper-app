# Android 3D Live Wallpaper

A high-performance 3D live wallpaper for Android 15+ powered by WebGL and WebView integration. Features interactive 3D scenes, customizable quality presets, and optimized battery usage.

## 📥 Quick Download

### Latest Release APK
**[📱 Download APK](wallpaper-app.apk)** *(2.9 MB)*

- **Version**: 1.0.0
- **Compatibility**: Android 5.0+ (API 21+)
- **Architecture**: Universal APK (All devices)
- **Installation**: See [Installation Guide](#-installation) below

## 📱 Features

## 📱 Features

- **WebGL-Powered 3D Scenes**: Real-time 3D graphics rendered through WebView
- **Interactive Touch Controls**: Gesture-based interaction with 3D scenes
- **Quality Presets**: Battery Saver, Standard, and High Quality rendering modes
- **Secure Asset Loading**: HTTPS-only connections with certificate validation
- **Performance Optimized**: Visibility-based rendering and frame rate control
- **Android 15 Ready**: Full support for Android 15 (API 35) features

## 🔧 Installation

### Quick Install
1. **Download**: Click the APK link above or go to [Releases](../../releases)
2. **Enable Unknown Sources**:
   - Android 8+: Settings → Apps & notifications → Special app access → Install unknown apps
   - Android 7-: Settings → Security → Unknown sources
3. **Install**: Open downloaded APK and tap "Install"
4. **Set Wallpaper**: Settings → Display → Wallpaper → Live Wallpapers → Select this app

### For Developers
```bash
# Download and build from source
git clone https://github.com/Aman3504u/android-wallpaper-app.git
cd android-wallpaper-app
./gradlew assembleRelease
```

## 🏗️ Architecture Overview

The app follows a modular architecture with clear separation of concerns:

```
app/src/main/java/com/example/wallpaper/
├── service/           # WallpaperService and Engine
├── web/              # WebView integration and management
├── bridge/           # Android-JavaScript communication
├── gesture/          # Touch handling and policies
├── settings/         # UI for configuration and preview
├── prefs/            # Settings storage and validation
└── util/             # Utility classes and helpers
```

### Core Components

1. **LiveWallpaperService**: Main service that creates and manages the wallpaper engine
2. **LiveWallpaperEngine**: Handles surface lifecycle, drawing, and WebView integration
3. **JsBridge**: Secure communication channel between Android and JavaScript
4. **WebViewFactory**: Creates and configures WebView instances
5. **SettingsActivity**: User interface for configuration and presets

## 🛠️ Build Configuration

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or later
- Android SDK 35 (Android 15)
- Gradle 8.5+
- Java 17 / Kotlin 1.9.20

### Project Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd android_project
   ```

2. **Open in Android Studio**
   - File → Open → Select `android_project` folder
   - Wait for Gradle sync to complete

3. **Configure Keystore** (for release builds)
   ```bash
   # Create keystore
   keytool -genkey -v -keystore wallpaper.keystore \
           -alias wallpaper_key -keyalg RSA -keysize 2048 -validity 10000

   # Update gradle.properties with your keystore info
   ```

4. **Build the project**
   ```bash
   # Debug build
   ./gradlew assembleDebug

   # Release build
   ./gradlew assembleRelease

   # Install on connected device
   ./gradlew installDebug
   ```

### Build Variants

- **Debug**: Includes WebView debugging, logging, and development features
- **Release**: Optimized, obfuscated, with security hardening enabled

## 🔧 Configuration

### Network Security

The app enforces strict security policies:

- **HTTPS Only**: All network traffic must use HTTPS
- **Domain Allowlist**: Only approved domains can be accessed
- **Certificate Pinning**: Supported for additional security

Update `app/src/main/res/xml/network_security_config.xml` to configure allowed domains:

```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">your-domain.com</domain>
    <!-- Add more domains as needed -->
</domain-config>
```

### Wallpaper Settings

Configure default settings in `WallpaperPrefs.java`:

```java
// Set default content URL
wallpaperPrefs.setContentUrl("https://your-domain.com/scene.html");

// Set default quality preset
wallpaperPrefs.setQualityPreset(QualityPreset.STANDARD);

// Enable/disable touch interaction
wallpaperPrefs.setTouchInteractivityEnabled(true);
```

## 📦 Dependencies

### Core Dependencies

- **AndroidX**: Modern Android libraries
- **WebView**: AndroidX WebKit for WebView integration
- **Material Design**: UI components and styling
- **Gson**: JSON serialization for JS bridge
- **Kotlin Coroutines**: Asynchronous programming

### Key Dependencies by Module

```gradle
// WebView and WebGL support
implementation 'androidx.webkit:webkit:1.9.0'

// UI and Material Design
implementation 'com.google.android.material:material:1.11.0'

// Preferences
implementation 'androidx.preference:preference-ktx:1.2.1'

// JSON handling
implementation 'com.google.code.gson:gson:2.10.1'
```

## 🎨 Customization

### Adding Custom 3D Scenes

1. **Package assets**: Add your HTML/JS/CSS files to `app/src/main/assets/web/`
2. **Update presets**: Modify `SettingsActivity` to include your scenes
3. **Configure loading**: Update `WebViewAssetLoaderConfig` for asset paths

### Modifying JavaScript Bridge

Extend the Android-JavaScript communication in `JsBridge.java`:

```java
@JavascriptInterface
public void setCustomParameter(String value) {
    // Handle custom parameter from JavaScript
}
```

### Theme Customization

Update `themes.xml` and `colors.xml` to match your brand:

```xml
<!-- Update primary color -->
<color name="primary">#YOUR_COLOR</color>
<color name="primary_dark">#YOUR_DARK_COLOR</color>
```

## 🚀 Testing

### Development Testing

1. **Install debug build**
   ```bash
   ./gradlew installDebug
   ```

2. **Enable WebView debugging** (automatic in debug builds)
   - Open Chrome and navigate to `chrome://inspect`
   - Your wallpaper WebView will appear for debugging

3. **Test wallpapers**
   - Use MainActivity to set wallpaper quickly
   - Or use system wallpaper picker: Settings → Display → Wallpaper

### Manual Testing Checklist

- [ ] Wallpaper renders correctly on different screen sizes
- [ ] Touch gestures work as expected
- [ ] Performance is smooth on target devices
- [ ] Battery usage is acceptable
- [ ] Network security rules are enforced
- [ ] Settings are properly saved and applied

### Automated Testing

```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew jacocoTestReport
```

## 📱 Deployment

### Release Checklist

- [ ] Update version code and name
- [ ] Configure keystore and signing
- [ ] Test on representative device matrix
- [ ] Verify security policies
- [ ] Enable ProGuard rules
- [ ] Test wallpaper picker flow
- [ ] Validate performance on target devices

### Play Store Publishing

1. **Generate signed bundle**
   ```bash
   ./gradlew bundleRelease
   ```

2. **Upload to Play Console**
   - Go to Play Console → Apps & Games
   - Create new app listing
   - Upload AAB file
   - Complete store listing

3. **Store Listing Requirements**
   - App name: "3D Live Wallpaper"
   - Category: Personalization
   - Content rating: Everyone
   - Graphics: App preview screenshots

## 🔒 Security Considerations

### Implemented Security Measures

- **HTTPS Enforcement**: All network traffic encrypted
- **Certificate Validation**: Prevents man-in-the-middle attacks
- **Asset Security**: No file:// URLs, secure asset loading
- **JS Bridge Protection**: Limited exposed interface
- **Code Obfuscation**: Release builds are obfuscated

### Security Best Practices

1. **Validate all inputs** in JS bridge methods
2. **Use HTTPS only** for external content
3. **Limit exposed JS interfaces** to minimum required
4. **Implement proper error handling** for security exceptions
5. **Regular security updates** for WebView dependencies

## 🐛 Troubleshooting

### Common Issues

1. **WebView not rendering**
   - Check network security configuration
   - Verify asset loading paths
   - Enable WebView debugging for diagnostics

2. **Poor performance**
   - Verify quality preset settings
   - Check battery optimization settings
   - Reduce scene complexity

3. **Touch not working**
   - Confirm touch interactivity is enabled
   - Check gesture forwarding policies
   - Verify launcher compatibility

4. **Wallpaper crashes**
   - Check logcat for WebView render process errors
   - Verify memory constraints
   - Test on target device specifications

### Debug Commands

```bash
# View detailed logs
adb logcat | grep -i wallpaper

# Check WebView rendering
adb logcat | grep -i webview

# Monitor memory usage
adb shell dumpsys meminfo com.example.wallpaper
```

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Development Guidelines

- Follow Android coding conventions
- Add comprehensive comments for complex logic
- Include unit tests for new features
- Update documentation for API changes
- Test on multiple device configurations

## 📞 Support

For issues and questions:

1. Check existing GitHub issues
2. Review troubleshooting guide
3. Create detailed issue with:
   - Android version and device
   - Reproduction steps
   - Logcat output
   - Screenshots if applicable

## 🔄 Version History

### v1.0.0 (Initial Release)
- Initial WebGL-based wallpaper implementation
- Android 15 compatibility
- Security hardening
- Performance optimizations
- Complete settings UI

---

**Built with ❤️ for Android developers**
