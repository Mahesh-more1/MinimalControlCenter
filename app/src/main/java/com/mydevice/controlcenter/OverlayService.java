package com.mydevice.controlcenter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private View overlayView;
    private View triggerHandle;
    private boolean isTorchOn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 1. Create a tiny, invisible top-edge swipe/tap trigger (consumes 0 RAM)
        createTriggerHandle();

        // 2. Inflate the full One UI control center layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.view_control_center, null);

        setupPanelControls();
    }

    private void createTriggerHandle() {
        triggerHandle = new View(this);
        triggerHandle.setBackgroundColor(0x01000000); // 99.9% transparent

        WindowManager.LayoutParams handleParams = new WindowManager.LayoutParams(
            200, // Width: 200px at center top
            40,  // Height: 40px touch zone
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        handleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        triggerHandle.setOnClickListener(v -> showPanel());
        windowManager.addView(triggerHandle, handleParams);
    }

    private void showPanel() {
        if (overlayView.getParent() != null) return;

        WindowManager.LayoutParams panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.TOP;
        windowManager.addView(overlayView, panelParams);
    }

    private void hidePanel() {
        if (overlayView.getParent() != null) {
            windowManager.removeView(overlayView);
        }
    }

    private void setupPanelControls() {
        // Close Button
        Button btnClose = overlayView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> hidePanel());

        // Wi-Fi Quick Settings Launcher
        Button btnWifi = overlayView.findViewById(R.id.btn_wifi);
        btnWifi.setOnClickListener(v -> {
            Intent wifiIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(wifiIntent);
            hidePanel();
        });

        // Torch / Flashlight Toggle
        Button btnTorch = overlayView.findViewById(R.id.btn_torch);
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        btnTorch.setOnClickListener(v -> {
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                isTorchOn = !isTorchOn;
                cameraManager.setTorchMode(cameraId, isTorchOn);
                btnTorch.setBackgroundColor(isTorchOn ? 0xFF007AFF : 0xFF2B2B2E);
            } catch (Exception e) {
                Toast.makeText(this, "Torch error", Toast.LENGTH_SHORT).show();
            }
        });

        // System Settings Shortcut
        Button btnSettings = overlayView.findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            hidePanel();
        });

        // Brightness Slider
        SeekBar sliderBrightness = overlayView.findViewById(R.id.slider_brightness);
        try {
            int curBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            sliderBrightness.setProgress(curBrightness);
        } catch (Exception ignored) {}

        sliderBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && Settings.System.canWrite(OverlayService.this)) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (triggerHandle != null && triggerHandle.getParent() != null) {
            windowManager.removeView(triggerHandle);
        }
        hidePanel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
