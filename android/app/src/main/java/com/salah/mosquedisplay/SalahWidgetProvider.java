package com.salah.mosquedisplay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/** Home-screen widget: next prayer, live countdown and today's five prayer times. */
public class SalahWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TICK = "com.salah.mosquedisplay.WIDGET_TICK";
    public static final String PREFS = "salah_widget";
    public static final String KEY = "data";

    private static final String[] ORDER = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private static final int[] NAME_IDS = {R.id.w_n0, R.id.w_n1, R.id.w_n2, R.id.w_n3, R.id.w_n4};
    private static final int[] TIME_IDS = {R.id.w_t0, R.id.w_t1, R.id.w_t2, R.id.w_t3, R.id.w_t4};
    private static final String[] AR_NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

    private static final int GOLD = 0xFFF3D7A0;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM = 0xB3FFFFFF;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        render(context, manager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_TICK.equals(intent.getAction())) {
            updateAll(context);
        }
    }

    @Override
    public void onDisabled(Context context) {
        cancelAlarm(context);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, SalahWidgetProvider.class));
        if (ids == null || ids.length == 0) {
            cancelAlarm(context);
            return;
        }
        render(context, manager, ids);
    }

    private static void render(Context context, AppWidgetManager manager, int[] ids) {
        long now = System.currentTimeMillis();
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);

        String loc = "";
        JSONArray items = null;
        if (json != null) {
            try {
                JSONObject root = new JSONObject(json);
                loc = root.optString("loc", "");
                items = root.optJSONArray("items");
            } catch (Exception ignored) {
                items = null;
            }
        }

        // Find the next prayer
        JSONObject next = null;
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.optJSONObject(i);
                if (it == null) continue;
                long ms = it.optLong("ms", 0);
                if (ms > now && (next == null || ms < next.optLong("ms", Long.MAX_VALUE))) {
                    next = it;
                }
            }
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_salah);

        // Tap anywhere to open the app
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            views.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(context, 0, launch, flags));
        }

        if (next == null) {
            views.setTextViewText(R.id.w_loc, "افتح التطبيق لتحميل المواقيت");
            views.setTextViewText(R.id.w_name, "—");
            views.setTextViewText(R.id.w_time, "--:--");
            views.setViewVisibility(R.id.w_chrono, View.GONE);
            for (int i = 0; i < 5; i++) {
                views.setTextViewText(NAME_IDS[i], AR_NAMES[i]);
                views.setTextViewText(TIME_IDS[i], "--:--");
                views.setTextColor(NAME_IDS[i], DIM);
                views.setTextColor(TIME_IDS[i], WHITE);
            }
            for (int id : ids) manager.updateAppWidget(id, views);
            schedule(context, now + 30L * 60L * 1000L);
            return;
        }

        long nextMs = next.optLong("ms", now);
        String nextId = next.optString("id", "");
        String nextDay = next.optString("d", "");

        views.setTextViewText(R.id.w_loc, loc);
        views.setTextViewText(R.id.w_name, next.optString("n", ""));
        views.setTextViewText(R.id.w_time, next.optString("t", "--:--"));

        long base = SystemClock.elapsedRealtime() + (nextMs - now);
        views.setViewVisibility(R.id.w_chrono, View.VISIBLE);
        views.setChronometer(R.id.w_chrono, base, null, true);
        if (Build.VERSION.SDK_INT >= 24) {
            views.setChronometerCountDown(R.id.w_chrono, true);
        }

        // Five prayers of the day that contains the next prayer
        for (int p = 0; p < 5; p++) {
            String name = AR_NAMES[p];
            String time = "--:--";
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject it = items.optJSONObject(i);
                    if (it == null) continue;
                    if (ORDER[p].equals(it.optString("id")) && nextDay.equals(it.optString("d"))) {
                        name = it.optString("n", name);
                        time = it.optString("t", time);
                        break;
                    }
                }
            }
            boolean isNext = ORDER[p].equals(nextId);
            views.setTextViewText(NAME_IDS[p], name);
            views.setTextViewText(TIME_IDS[p], time);
            views.setTextColor(NAME_IDS[p], isNext ? GOLD : DIM);
            views.setTextColor(TIME_IDS[p], isNext ? GOLD : WHITE);
        }

        for (int id : ids) manager.updateAppWidget(id, views);

        // Refresh right after the prayer time so the next one is highlighted
        schedule(context, nextMs + 1500L);
    }

    private static PendingIntent tickIntent(Context context) {
        Intent intent = new Intent(context, SalahWidgetProvider.class);
        intent.setAction(ACTION_TICK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 1, intent, flags);
    }

    private static void schedule(Context context, long atMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = tickIntent(context);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, atMillis, pi);
        }
    }

    private static void cancelAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(tickIntent(context));
    }
}
