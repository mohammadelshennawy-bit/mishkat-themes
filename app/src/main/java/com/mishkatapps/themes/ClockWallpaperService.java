package com.mishkatapps.themes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.Calendar;

/** Live wallpaper: analog clock + battery state, in one of three Mishkat designs. */
public class ClockWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    class ClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable tick = new Runnable() {
            @Override public void run() {
                drawFrame();
                schedule();
            }
        };
        private SharedPreferences prefs;
        private boolean visible;
        private int width, height;
        private Bitmap bg;
        private String bgName;
        private int level = 100;
        private boolean charging;

        private final BroadcastReceiver battery = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                readBattery(i);
                if (visible) drawFrame();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            prefs = getSharedPreferences(ClockRenderer.PREFS, MODE_PRIVATE);
            prefs.registerOnSharedPreferenceChangeListener(this);
            Intent sticky = registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) readBattery(sticky);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(tick);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            try { unregisterReceiver(battery); } catch (Exception ignored) {}
            if (bg != null) { bg.recycle(); bg = null; }
        }

        private void readBattery(Intent i) {
            int l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (l >= 0 && s > 0) level = Math.round(l * 100f / s);
            int st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences p, String key) {
            loadBackground();
            if (visible) drawFrame();
        }

        @Override
        public void onVisibilityChanged(boolean v) {
            visible = v;
            handler.removeCallbacks(tick);
            if (v) {
                Intent sticky = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (sticky != null) readBattery(sticky);
                drawFrame();
                schedule();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            super.onSurfaceChanged(holder, format, w, h);
            width = w;
            height = h;
            bgName = null;
            loadBackground();
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(tick);
        }

        private int design() {
            return prefs.getInt(ClockRenderer.KEY_DESIGN, 0);
        }

        private void loadBackground() {
            if (width == 0) return;
            int d = design();
            String name = prefs.getString(ClockRenderer.KEY_BG, "");
            if (name.isEmpty()) name = ClockRenderer.defaultBg(d);
            if (d == 2) name = null;
            if (name == null ? bgName == null && bg == null : name.equals(bgName)) return;
            Bitmap old = bg;
            bg = ClockRenderer.loadBg(ClockWallpaperService.this, name, width, height);
            bgName = name;
            if (old != null) old.recycle();
        }

        private void schedule() {
            if (!visible) return;
            long now = System.currentTimeMillis();
            boolean sec = prefs.getBoolean(ClockRenderer.KEY_SECONDS, true);
            long delay = sec ? 1000 - (now % 1000) : 60000 - (now % 60000);
            handler.postDelayed(tick, delay + 5);
        }

        private void drawFrame() {
            if (width == 0 || height == 0) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    ClockRenderer.draw(ClockWallpaperService.this, c, width, height, design(), bg,
                            level, charging, Calendar.getInstance(),
                            prefs.getBoolean(ClockRenderer.KEY_SECONDS, true),
                            prefs.getBoolean(ClockRenderer.KEY_VERSE, true));
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) {
                    try { holder.unlockCanvasAndPost(c); } catch (Exception ignored) {}
                }
            }
        }
    }
}
