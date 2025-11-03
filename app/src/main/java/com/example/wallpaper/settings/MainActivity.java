package com.example.wallpaper.settings;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.wallpaper.R;
import com.example.wallpaper.service.LiveWallpaperService;
import com.example.wallpaper.prefs.WallpaperPrefs;
import com.example.wallpaper.ui.SettingsActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Main Activity for testing and development purposes
 * 
 * This activity provides a simple interface for:
 * - Setting the wallpaper directly
 * - Accessing configuration settings
 * - Testing wallpaper functionality
 * 
 * In production, this would typically only be used for debugging
 * or as a fallback entry point. The main user interaction flow
 * should go through the system wallpaper picker.
 */
public class MainActivity extends AppCompatActivity {

    private WallpaperManager wallpaperManager;
    private WallpaperPrefs wallpaperPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupToolbar();
        setupButtons();
    }

    /**
     * Initialize core components
     */
    private void initializeComponents() {
        wallpaperManager = WallpaperManager.getInstance(this);
        wallpaperPrefs = new WallpaperPrefs(this);
    }

    /**
     * Setup the toolbar with navigation
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setSubtitle(R.string.app_description);
        }
    }

    /**
     * Setup button click handlers
     */
    private void setupButtons() {
        Button setWallpaperBtn = findViewById(R.id.btn_set_wallpaper);
        Button settingsBtn = findViewById(R.id.btn_settings);
        Button previewBtn = findViewById(R.id.btn_preview);
        Button infoBtn = findViewById(R.id.btn_info);

        setWallpaperBtn.setOnClickListener(v -> setWallpaper());
        settingsBtn.setOnClickListener(v -> openSettings());
        previewBtn.setOnClickListener(v -> openPreview());
        infoBtn.setOnClickListener(v -> showInfo());
    }

    /**
     * Set the wallpaper by launching the system picker
     * with our wallpaper service pre-selected
     */
    private void setWallpaper() {
        try {
            // Create intent for wallpaper picker
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, LiveWallpaperService.class)
            );
            
            // Launch picker
            startActivityForResult(intent, 1001);
            
        } catch (Exception e) {
            showError("Failed to launch wallpaper picker: " + e.getMessage());
        }
    }

    /**
     * Open the settings activity
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Open the preview activity
     */
    private void openPreview() {
        Intent intent = new Intent(this, PreviewActivity.class);
        startActivity(intent);
    }

    /**
     * Show information about the app
     */
    private void showInfo() {
        View rootView = findViewById(android.R.id.content);
        StringBuilder info = new StringBuilder();
        
        info.append("3D Live Wallpaper v1.0.0\n\n");
        info.append("Features:\n");
        info.append("• WebGL-powered 3D scenes\n");
        info.append("• Interactive touch controls\n");
        info.append("• Configurable quality presets\n");
        info.append("• Battery optimization\n");
        info.append("• HTTPS-only secure loading\n\n");
        
        info.append("Supported Android versions:\n");
        info.append("• API 24 (Android 7.0) and above\n");
        info.append("• Optimized for Android 15\n\n");
        
        info.append("Current Configuration:\n");
        info.append("• URL: ").append(wallpaperPrefs.getContentUrl()).append("\n");
        info.append("• Quality: ").append(wallpaperPrefs.getQualityPreset()).append("\n");
        info.append("• Touch: ").append(wallpaperPrefs.isTouchInteractivityEnabled() ? "Enabled" : "Disabled").append("\n");
        info.append("• Frame Rate: ").append(wallpaperPrefs.getFrameRate()).append(" FPS");
        
        Snackbar.make(rootView, info.toString(), Snackbar.LENGTH_LONG).show();
    }

    /**
     * Handle wallpaper picker result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            if (resultCode == Activity.RESULT_OK) {
                showMessage("Wallpaper set successfully!");
            } else if (resultCode == Activity.RESULT_CANCELED) {
                showMessage("Wallpaper selection cancelled");
            }
        }
    }

    /**
     * Show a success message
     */
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show an error message
     */
    private void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }
}
