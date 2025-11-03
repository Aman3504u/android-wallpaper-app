package com.example.wallpaper.settings;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.wallpaper.R;

/**
 * Preview Activity for testing wallpaper preview before applying
 * 
 * This activity provides a way to preview the wallpaper configuration
 * without actually setting it as the device wallpaper.
 */
public class PreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        
        setupToolbar();
        showPreviewInfo();
    }
    
    /**
     * Setup the toolbar with back navigation
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.preview_title);
            getSupportActionBar().setSubtitle(R.string.preview_description);
        }
    }
    
    /**
     * Show preview information
     */
    private void showPreviewInfo() {
        Toast.makeText(this, "Preview functionality coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
