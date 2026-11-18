package com.salah.mosquedisplay;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the screen on - this is a prayer-times display
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Allow the Adhan audio to play without a tap
        WebSettings settings = getBridge().getWebView().getSettings();
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Lets the page send prayer times to the home-screen widget
        getBridge().getWebView().addJavascriptInterface(new SalahWidgetBridge(this), "SalahWidget");

        enterImmersiveMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    // Hide status + navigation bars (swipe from the edge to reveal them temporarily)
    private void enterImmersiveMode() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        View decor = window.getDecorView();
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(window, decor);
        if (c != null) {
            c.hide(WindowInsetsCompat.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
}
