package com.mydevice.controlcenter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
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
    private View triggerPill;
    private boolean isTorchOn = false;
    private boolean isPanelOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 1. Inflate the full One UI control center layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.view_control_center, null);

        setupPanelControls();

        // 2. Create a clearly visible floating pull-tab / pill at the top center
        createVisiblePullHandle();
    }

    private void createVisiblePullHandle() {
        triggerPill = new View(this);
        
        // Styled as a clean, visible rounded pill (One UI Edge Tab)
        GradientDrawable pillShape = new GradientDrawable();
        pillShape.setShape(GradientDrawable.RECTANGLE);
        pillShape.setCornerRadius(16);
        pillShape.setColor(Color.parseColor("#80FFFFFF")); // 50% semi-transparent white bar
        triggerPill.setBackground(pillShape);

        WindowManager.LayoutParams handleParams = new WindowManager.LayoutParams(
            220, // 220px wide
            30,  // 30px high bar
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        handleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        handleParams.y = 10; // 10px below top camera notch

        triggerPill.setOnClickListener(v -> {
            if (isPanelOpen) {
                hidePanel();
            } else {
                showPanel();
            }
        });

        windowManager.addView(triggerPill, handleParams);
    }

    private void showPanel() {
        if (overlayView.getParent() != null) return;

        WindowManager.LayoutParams panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.TOP;
        windowManager.addView(overlayView, panelParams);
        isPanelOpen = true;
    }

    private void hidePanel() {
        if (overlayView.getParent() != null) {
            windowManager.removeView(overlayView);
        }
        isPanelOpen = false;
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
        if (triggerPill != null && triggerPill.getParent() != null) {
            windowManager.removeView(triggerPill);
        }
        hidePanel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
