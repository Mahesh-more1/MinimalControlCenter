package com.mydevice.controlcenter;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> checkPermissionAndStart());
    }

    private void checkPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            Toast.makeText(this, "Grant 'Appear on top' to display panel", Toast.LENGTH_LONG).show();
        } else {
            startService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Control Center Enabled!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startService(new Intent(this, OverlayService.class));
                Toast.makeText(this, "Control Center Enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
