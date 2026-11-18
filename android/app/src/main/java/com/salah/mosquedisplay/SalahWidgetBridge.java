package com.salah.mosquedisplay;

import android.content.Context;
import android.webkit.JavascriptInterface;

/** Receives prayer times from the web page and stores them for the home-screen widget. */
public class SalahWidgetBridge {
    private final Context appContext;

    public SalahWidgetBridge(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @JavascriptInterface
    public void save(String json) {
        if (json == null || json.length() == 0 || json.length() > 200000) return;
        appContext
            .getSharedPreferences(SalahWidgetProvider.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SalahWidgetProvider.KEY, json)
            .apply();
        SalahWidgetProvider.updateAll(appContext);
    }
}
