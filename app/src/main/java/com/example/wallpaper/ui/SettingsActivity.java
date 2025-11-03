package com.example.wallpaper.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.wallpaper.R;

/**
 * Settings Activity for Wallpaper Application
 * Handles URL input validation, screen selection, and quality settings
 */
public class SettingsActivity extends AppCompatActivity {
    
    private EditText etWallpaperUrl;
    private RadioGroup rgScreenSelection;
    private RadioButton rbHomeScreen;
    private RadioButton rbLockAndHome;
    private Spinner spinnerQuality;
    private Button btnTestUrl;
    private Button btnSaveSettings;
    
    // Settings constants
    private static final String PREF_NAME = "wallpaper_settings";
    private static final String KEY_URL = "wallpaper_url";
    private static final String KEY_SCREEN_SELECTION = "screen_selection";
    private static final String KEY_QUALITY = "quality_setting";
    private static final String KEY_URL_TESTED = "url_tested";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        setupToolbar();
        loadSavedSettings();
        setupListeners();
    }
    
    /**
     * Initialize all UI views
     */
    private void initializeViews() {
        etWallpaperUrl = findViewById(R.id.et_wallpaper_url);
        rgScreenSelection = findViewById(R.id.rg_screen_selection);
        rbHomeScreen = findViewById(R.id.rb_home_screen);
        rbLockAndHome = findViewById(R.id.rb_lock_and_home);
        spinnerQuality = findViewById(R.id.spinner_quality);
        btnTestUrl = findViewById(R.id.btn_test_url);
        btnSaveSettings = findViewById(R.id.btn_save_settings);
    }
    
    /**
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
    }
    
    /**
     * Setup click listeners for buttons and interactions
     */
    private void setupListeners() {
        btnTestUrl.setOnClickListener(v -> testWallpaperUrl());
        btnSaveSettings.setOnClickListener(v -> saveSettings());
        
        // Clear validation error when user starts typing
        etWallpaperUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etWallpaperUrl.setError(null);
            }
        });
        
        // Auto-validate URL when user stops typing
        etWallpaperUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 6) {
                validateAndUpdateUrlTestStatus();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Test if the wallpaper URL is valid and accessible
     */
    private void testWallpaperUrl() {
        String url = etWallpaperUrl.getText().toString().trim();
        
        if (!isValidUrl(url)) {
            showError(getString(R.string.error_invalid_url));
            etWallpaperUrl.setError(getString(R.string.error_invalid_url_format));
            return;
        }
        
        // Disable test button to prevent multiple clicks
        btnTestUrl.setEnabled(false);
        btnTestUrl.setText(R.string.testing_url);
        
        // In a real implementation, you would test the URL here
        // For now, we'll simulate the test after a short delay
        etWallpaperUrl.postDelayed(() -> {
            boolean urlIsValid = validateUrlAccessibility(url);
            btnTestUrl.setEnabled(true);
            btnTestUrl.setText(R.string.test_url);
            
            if (urlIsValid) {
                showSuccess(getString(R.string.url_test_success));
                saveUrlTestedStatus(true);
            } else {
                showError(getString(R.string.url_test_failed));
                saveUrlTestedStatus(false);
            }
        }, 1500);
    }
    
    /**
     * Validate URL format and basic accessibility
     */
    private boolean isValidUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        
        // Check if URL matches web URL pattern
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            return false;
        }
        
        // Additional validation for common image formats
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
               lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp") ||
               lowerUrl.endsWith(".gif") || lowerUrl.contains("image");
    }
    
    /**
     * Simulate URL accessibility validation
     * In a real implementation, this would make an HTTP request
     */
    private boolean validateUrlAccessibility(String url) {
        // Basic simulation - check if URL is not empty and contains image extension
        return !TextUtils.isEmpty(url) && 
               (url.contains(".") || url.contains("image"));
    }
    
    /**
     * Validate URL and update test status
     */
    private void validateAndUpdateUrlTestStatus() {
        String url = etWallpaperUrl.getText().toString().trim();
        if (!TextUtils.isEmpty(url) && isValidUrl(url)) {
            saveUrlTestedStatus(false); // Reset test status when URL changes
        }
    }
    
    /**
     * Save all settings to SharedPreferences
     */
    private void saveSettings() {
        String url = etWallpaperUrl.getText().toString().trim();
        int screenSelection = rgScreenSelection.getCheckedRadioButtonId();
        String quality = spinnerQuality.getSelectedItem().toString();
        
        // Validate URL if provided
        if (!TextUtils.isEmpty(url) && !isValidUrl(url)) {
            showError(getString(R.string.error_save_failed_invalid_url));
            etWallpaperUrl.setError(getString(R.string.error_invalid_url_format));
            return;
        }
        
        // Save to SharedPreferences
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, url)
            .putInt(KEY_SCREEN_SELECTION, screenSelection)
            .putString(KEY_QUALITY, quality)
            .apply();
        
        showSuccess(getString(R.string.settings_saved_successfully));
        
        // Close activity after a short delay
        etWallpaperUrl.postDelayed(() -> finish(), 1000);
    }
    
    /**
     * Load saved settings from SharedPreferences
     */
    private void loadSavedSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        String savedUrl = prefs.getString(KEY_URL, "");
        int savedScreenSelection = prefs.getInt(KEY_SCREEN_SELECTION, R.id.rb_home_screen);
        String savedQuality = prefs.getString(KEY_QUALITY, "");
        boolean urlWasTested = prefs.getBoolean(KEY_URL_TESTED, false);
        
        // Load URL
        if (!TextUtils.isEmpty(savedUrl)) {
            etWallpaperUrl.setText(savedUrl);
        }
        
        // Load screen selection
        rgScreenSelection.check(savedScreenSelection);
        
        // Load quality setting
        if (!TextUtils.isEmpty(savedQuality)) {
            int position = getQualityPosition(savedQuality);
            if (position >= 0) {
                spinnerQuality.setSelection(position);
            }
        }
        
        // Update test button state based on previous test
        updateTestButtonState(urlWasTested);
    }
    
    /**
     * Get position of quality setting in spinner
     */
    private int getQualityPosition(String quality) {
        String[] qualities = getResources().getStringArray(R.array.quality_options);
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i].equals(quality)) {
                return i;
            }
        }
        return 0; // Default to first option
    }
    
    /**
     * Update test button state based on URL test status
     */
    private void updateTestButtonState(boolean urlWasTested) {
        if (urlWasTested) {
            btnTestUrl.setText(R.string.url_tested);
            btnTestUrl.setEnabled(false);
        } else {
            btnTestUrl.setText(R.string.test_url);
            btnTestUrl.setEnabled(true);
        }
    }
    
    /**
     * Save URL test status
     */
    private void saveUrlTestedStatus(boolean tested) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_URL_TESTED, tested)
            .apply();
    }
    
    /**
     * Show success message to user
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show error message to user
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}