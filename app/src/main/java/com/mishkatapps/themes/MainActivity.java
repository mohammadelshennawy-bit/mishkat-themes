package com.mishkatapps.themes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    static final String[] WALLS = {
            "01-mishkat-home.jpg", "02-mishkat-lock.jpg", "03-mishkat-amoled.jpg", "04-mishkat-light.jpg",
            "05-mishkat-star.jpg", "06-forest-mist.jpg", "07-forest-autumn-rays.jpg", "08-forest-green.jpg",
            "09-forest-sunbeams.jpg", "10-kuang-si-falls.jpg", "11-tropical-stream.jpg",
            "12-amazon-lake-dawn.jpg", "13-lake-reflection.jpg"};
    static final String[] WALL_NAMES = {
            "مشكاة · الرئيسية", "مشكاة · القفل", "مشكاة · أسود", "مشكاة · فاتح", "مشكاة · النجمة",
            "غابة الضباب", "أشعة الخريف", "الغابة الخضراء", "أشعة الصباح",
            "شلالات كوانغ سي", "جدول استوائي", "فجر الأمازون", "بحيرة الانعكاس"};

    static final String[] ICONS = {"phone", "messages", "contacts", "camera", "gallery", "settings", "clock",
            "calendar", "calculator", "browser", "files", "music", "mail", "maps", "store", "notes",
            "weather", "quran", "azkar", "qibla", "flashlight", "recorder"};
    static final String[] ICON_NAMES = {"الهاتف", "الرسائل", "جهات الاتصال", "الكاميرا", "المعرض",
            "الإعدادات", "الساعة", "التقويم", "الحاسبة", "المتصفح", "الملفات", "الصوتيات", "البريد",
            "الخرائط", "المتجر", "الملاحظات", "الطقس", "المصحف", "الأذكار", "القبلة", "الكشاف", "المسجل"};

    static final String[] DESIGN_NAMES = {"الساعة النجمية", "قنديل المشكاة", "AMOLED البسيطة"};

    private static final int REQ_STORAGE = 7;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    private Typeface font;
    private FrameLayout content;
    private final TextView[] tabs = new TextView[4];
    private SharedPreferences prefs;
    private float dp;

    // clock tab state
    private ImageView preview;
    private LinearLayout designRow, bgRow;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        font = ClockRenderer.font(this);
        prefs = getSharedPreferences(ClockRenderer.PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(ClockRenderer.DEEP);

        // header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(px(16), px(14), px(16), px(10));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        head.addView(logo, new LinearLayout.LayoutParams(px(46), px(46)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(px(12), 0, px(12), 0);
        titles.addView(label("مشكاة ثيمز", 26, ClockRenderer.GOLD));
        titles.addView(label("مشكاة · برمجيات نافعة للهاتف والحاسب", 14, 0xFFB9AD8C));
        head.addView(titles);
        root.addView(head);

        // tabs
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(px(8), 0, px(8), px(6));
        String[] names = {"الخلفيات", "ساعة القفل", "الأيقونات", "حقوق الصور"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView t = label(names[i], 17, ClockRenderer.CREAM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, px(8), 0, px(8));
            t.setOnClickListener(v -> showTab(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(px(3), 0, px(3), 0);
            tabRow.addView(t, lp);
            tabs[i] = t;
        }
        root.addView(tabRow);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        showTab(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pool.shutdownNow();
    }

    // ------------------------------------------------------------------ tabs

    private void showTab(int i) {
        for (int k = 0; k < tabs.length; k++) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(px(18));
            if (k == i) {
                g.setColor(ClockRenderer.GOLD);
                tabs[k].setTextColor(ClockRenderer.DEEP);
            } else {
                g.setColor(0x00000000);
                g.setStroke(px(1), 0x66D9B15C);
                tabs[k].setTextColor(ClockRenderer.CREAM);
            }
            tabs[k].setBackground(g);
        }
        content.removeAllViews();
        preview = null;
        switch (i) {
            case 0: content.addView(wallpapersTab()); break;
            case 1: content.addView(clockTab()); break;
            case 2: content.addView(iconsTab()); break;
            default: content.addView(creditsTab()); break;
        }
    }

    // ------------------------------------------------------------------ wallpapers

    private View wallpapersTab() {
        GridView g = new GridView(this);
        g.setNumColumns(3);
        g.setPadding(px(8), px(6), px(8), px(16));
        g.setClipToPadding(false);
        g.setVerticalSpacing(px(10));
        g.setHorizontalSpacing(px(8));
        g.setAdapter(new BaseAdapter() {
            public int getCount() { return WALLS.length; }
            public Object getItem(int p) { return WALLS[p]; }
            public long getItemId(int p) { return p; }
            public View getView(int p, View v, ViewGroup parent) {
                LinearLayout cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                AspectImage img = new AspectImage(MainActivity.this, 2400f / 1080f);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setBackground(round(0xFF17336E, 10, 0));
                img.setClipToOutline(true);
                loadThumb("thumbs/" + WALLS[p], img);
                cell.addView(img);
                TextView t = label(WALL_NAMES[p], 13, ClockRenderer.CREAM);
                t.setGravity(Gravity.CENTER);
                cell.addView(t);
                return cell;
            }
        });
        g.setOnItemClickListener((parent, view, pos, id) -> chooseTarget(pos));
        return g;
    }

    private void chooseTarget(int pos) {
        String[] opts = {"الشاشة الرئيسية", "شاشة القفل", "الشاشتان معًا"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(WALL_NAMES[pos])
                .setItems(opts, (d, which) -> {
                    int flags = which == 0 ? WallpaperManager.FLAG_SYSTEM
                            : which == 1 ? WallpaperManager.FLAG_LOCK
                            : WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                    applyWallpaper(WALLS[pos], flags);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void applyWallpaper(String file, int flags) {
        toast("جارٍ تعيين الخلفية…");
        pool.execute(() -> {
            try (InputStream in = getAssets().open("wallpapers/" + file)) {
                WallpaperManager.getInstance(this).setStream(in, null, true, flags);
                runOnUiThread(() -> toast("تم تعيين الخلفية ✓"));
            } catch (Exception e) {
                runOnUiThread(() -> toast("تعذّر تعيين الخلفية"));
            }
        });
    }

    // ------------------------------------------------------------------ clock

    private View clockTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(px(14), px(6), px(14), px(28));
        sv.addView(col);

        preview = new ImageView(this);
        preview.setBackground(round(0xFF05070C, 22, 0));
        preview.setClipToOutline(true);
        preview.setScaleType(ImageView.ScaleType.FIT_XY);
        int pw = Math.round(getResources().getDisplayMetrics().widthPixels * 0.56f);
        col.addView(preview, new LinearLayout.LayoutParams(pw, Math.round(pw * 2400f / 1080f)));

        col.addView(section("تصميم الساعة"));
        designRow = new LinearLayout(this);
        designRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 3; i++) {
            final int d = i;
            TextView t = label(DESIGN_NAMES[i], 15, ClockRenderer.CREAM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(px(4), px(10), px(4), px(10));
            t.setOnClickListener(v -> {
                prefs.edit().putInt(ClockRenderer.KEY_DESIGN, d).apply();
                refreshClockUi();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(px(4), 0, px(4), 0);
            designRow.addView(t, lp);
        }
        col.addView(designRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(section("خلفية الساعة"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        bgRow = new LinearLayout(this);
        bgRow.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(bgRow);
        addBgChoice("", "تلقائي");
        for (int i = 0; i < WALLS.length; i++) {
            if (WALLS[i].startsWith("02-") || WALLS[i].startsWith("03-")) continue;
            addBgChoice(WALLS[i], WALL_NAMES[i]);
        }
        col.addView(hs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        CheckBox sec = check("إظهار عقرب الثواني", ClockRenderer.KEY_SECONDS);
        CheckBox verse = check("إظهار الآية الكريمة", ClockRenderer.KEY_VERSE);
        col.addView(sec);
        col.addView(verse);

        TextView go = label("تفعيل ساعة شاشة القفل", 20, ClockRenderer.DEEP);
        go.setGravity(Gravity.CENTER);
        go.setBackground(round(ClockRenderer.GOLD, 26, 0));
        go.setPadding(px(10), px(12), px(10), px(12));
        go.setOnClickListener(v -> activateLive());
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, px(14), 0, px(8));
        col.addView(go, glp);

        TextView note = label("ستفتح شاشة معاينة النظام: اضغط «تعيين خلفية» واختر «شاشة القفل» إن ظهر لك الاختيار. "
                + "ساعة الهاتف الأصلية ستظل ظاهرة أعلى شاشة القفل، ويمكنك تصغيرها من إعدادات شاشة القفل. "
                + "الخلفية الحية تستهلك قدرًا بسيطًا من البطارية؛ إخفاء عقرب الثواني يقلّل ذلك.", 14, 0xFFB9AD8C);
        col.addView(note);

        refreshClockUi();
        return sv;
    }

    private void addBgChoice(String file, String name) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setTag(file);
        cell.setPadding(px(3), px(3), px(3), px(3));
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setClipToOutline(true);
        img.setBackground(round(ClockRenderer.NAVY, 8, 0));
        if (!file.isEmpty()) loadThumb("thumbs/" + file, img);
        else img.setImageResource(R.drawable.logo);
        cell.addView(img, new LinearLayout.LayoutParams(px(64), px(110)));
        TextView t = label(name, 11, ClockRenderer.CREAM);
        t.setGravity(Gravity.CENTER);
        t.setMaxWidth(px(64));
        t.setSingleLine(true);
        cell.addView(t, new LinearLayout.LayoutParams(px(64), ViewGroup.LayoutParams.WRAP_CONTENT));
        cell.setOnClickListener(v -> {
            prefs.edit().putString(ClockRenderer.KEY_BG, file).apply();
            refreshClockUi();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(px(3), 0, px(3), 0);
        bgRow.addView(cell, lp);
    }

    private CheckBox check(String text, String key) {
        CheckBox c = new CheckBox(this);
        c.setText(text);
        c.setTypeface(font);
        c.setTextSize(16);
        c.setTextColor(ClockRenderer.CREAM);
        c.setChecked(prefs.getBoolean(key, true));
        c.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            refreshClockUi();
        });
        return c;
    }

    private void refreshClockUi() {
        if (preview == null) return;
        int design = prefs.getInt(ClockRenderer.KEY_DESIGN, 0);
        String bg = prefs.getString(ClockRenderer.KEY_BG, "");
        for (int i = 0; i < designRow.getChildCount(); i++) {
            TextView t = (TextView) designRow.getChildAt(i);
            boolean on = i == design;
            t.setBackground(round(on ? ClockRenderer.GOLD : 0x00000000, 14, on ? 0 : 0x66D9B15C));
            t.setTextColor(on ? ClockRenderer.DEEP : ClockRenderer.CREAM);
        }
        for (int i = 0; i < bgRow.getChildCount(); i++) {
            View c = bgRow.getChildAt(i);
            boolean on = bg.equals(c.getTag());
            c.setBackground(on ? round(0x00000000, 10, ClockRenderer.GOLD) : null);
            c.setAlpha(design == 2 ? 0.35f : 1f);
        }
        final ImageView target = preview;
        final boolean seconds = prefs.getBoolean(ClockRenderer.KEY_SECONDS, true);
        final boolean verse = prefs.getBoolean(ClockRenderer.KEY_VERSE, true);
        pool.execute(() -> {
            int w = 540, h = 1200;
            String name = bg.isEmpty() ? ClockRenderer.defaultBg(design) : bg;
            Bitmap bgb = design == 2 ? null : ClockRenderer.loadBg(this, name, w, h);
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            ClockRenderer.draw(this, c, w, h, design, bgb, 85, true, Calendar.getInstance(), seconds, verse);
            if (bgb != null) bgb.recycle();
            runOnUiThread(() -> target.setImageBitmap(out));
        });
    }

    private void activateLive() {
        ComponentName cn = new ComponentName(this, ClockWallpaperService.class);
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, cn);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
                toast("اختر «ساعة مشكاة» من القائمة");
            } catch (ActivityNotFoundException e2) {
                toast("هاتفك لا يدعم الخلفيات الحية");
            }
        }
    }

    // ------------------------------------------------------------------ icons

    private View iconsTab() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(14), px(6), px(14), 0);
        col.addView(label("احفظ الأيقونات في معرض الصور، ثم غيّر أيقونات تطبيقاتك من مشغّل يدعم ذلك مثل Nova Launcher "
                + "(اضغط مطوّلًا على التطبيق ← تعديل ← الصورة)، أو من تطبيق لتغيير الأيقونات.", 14, 0xFFB9AD8C));

        TextView save = label("حفظ الأيقونات في المعرض", 18, ClockRenderer.DEEP);
        save.setGravity(Gravity.CENTER);
        save.setBackground(round(ClockRenderer.GOLD, 24, 0));
        save.setPadding(px(10), px(10), px(10), px(10));
        save.setOnClickListener(v -> saveIconsChecked());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, px(10), 0, px(10));
        col.addView(save, slp);

        GridView g = new GridView(this);
        g.setNumColumns(4);
        g.setVerticalSpacing(px(12));
        g.setPadding(0, 0, 0, px(16));
        g.setClipToPadding(false);
        g.setAdapter(new BaseAdapter() {
            public int getCount() { return ICONS.length; }
            public Object getItem(int p) { return ICONS[p]; }
            public long getItemId(int p) { return p; }
            public View getView(int p, View v, ViewGroup parent) {
                LinearLayout cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                ImageView img = new ImageView(MainActivity.this);
                loadThumb("icons/" + ICONS[p] + ".png", img);
                cell.addView(img, new LinearLayout.LayoutParams(px(58), px(58)));
                TextView t = label(ICON_NAMES[p], 12, ClockRenderer.CREAM);
                t.setGravity(Gravity.CENTER);
                cell.addView(t);
                return cell;
            }
        });
        col.addView(g, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return col;
    }

    private void saveIconsChecked() {
        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        saveIcons();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == REQ_STORAGE && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) saveIcons();
        else if (req == REQ_STORAGE) toast("لم يُسمح بالحفظ في الذاكرة");
    }

    private void saveIcons() {
        toast("جارٍ الحفظ…");
        pool.execute(() -> {
            int ok = 0;
            for (String n : ICONS) {
                String fileName = "mishkat_" + n + ".png";
                try (InputStream in = getAssets().open("icons/" + n + ".png")) {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                        cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MishkatThemes");
                        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                        if (uri == null) continue;
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            copy(in, out);
                        }
                    } else {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MishkatThemes");
                        if (!dir.exists() && !dir.mkdirs()) continue;
                        File f = new File(dir, fileName);
                        try (OutputStream out = new FileOutputStream(f)) {
                            copy(in, out);
                        }
                        MediaScannerConnection.scanFile(this, new String[]{f.getAbsolutePath()}, null, null);
                    }
                    ok++;
                } catch (Exception ignored) {
                }
            }
            final int saved = ok;
            runOnUiThread(() -> toast(saved > 0
                    ? "حُفظت " + ClockRenderer.ar(saved) + " أيقونة في الصور ← مجلد MishkatThemes"
                    : "تعذّر حفظ الأيقونات"));
        });
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buf = new byte[16384];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
    }

    // ------------------------------------------------------------------ credits

    private View creditsTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(16), px(8), px(16), px(28));
        sv.addView(col);
        col.addView(section("حقوق الصور"));
        col.addView(label("صور الطبيعة من موقع ويكيميديا كومنز، منشورة بترخيص المشاع الإبداعي "
                + "«نسب المصنَّف – الترخيص بالمثل 4.0» (CC BY-SA 4.0). قُصَّت الصور لمقاس الهاتف وأضيف إليها تظليل خفيف، "
                + "والصور المعدّلة متاحة بالترخيص نفسه.\nhttps://creativecommons.org/licenses/by-sa/4.0/", 14, 0xFFB9AD8C));
        try (InputStream in = getAssets().open("credits.txt")) {
            byte[] all = new byte[in.available()];
            int n = in.read(all);
            String[] lines = new String(all, 0, Math.max(0, n), "UTF-8").split("\n");
            for (String line : lines) {
                String[] p = line.split("\t");
                if (p.length < 4) continue;
                TextView t = label(p[0] + "\nالمصوّر: " + p[1] + " · " + p[2] + "\n" + p[3], 14, ClockRenderer.CREAM);
                t.setPadding(0, px(10), 0, px(4));
                Linkify.addLinks(t, Linkify.WEB_URLS);
                t.setLinkTextColor(ClockRenderer.GOLD);
                t.setMovementMethod(LinkMovementMethod.getInstance());
                col.addView(t);
            }
        } catch (Exception ignored) {
        }
        col.addView(section("الخط"));
        col.addView(label("خط «أميري» – Amiri، ترخيص SIL Open Font License 1.1.", 14, 0xFFB9AD8C));
        col.addView(section("تصميم وإعداد"));
        TextView m = label("خلفيات مشكاة والأيقونات والساعات من تصميم «مشكاة».\nمشكاة · برمجيات نافعة للهاتف والحاسب\n"
                + "https://mishkatapps.github.io\nmishkatapps@gmail.com", 14, ClockRenderer.CREAM);
        Linkify.addLinks(m, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        m.setLinkTextColor(ClockRenderer.GOLD);
        m.setMovementMethod(LinkMovementMethod.getInstance());
        col.addView(m);
        return sv;
    }

    // ------------------------------------------------------------------ ui helpers

    private int px(float v) { return Math.round(v * dp); }

    private TextView label(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTypeface(font);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private TextView section(String s) {
        TextView t = label(s, 19, ClockRenderer.GOLD);
        t.setPadding(0, px(14), 0, px(6));
        return t;
    }

    private GradientDrawable round(int fill, float radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(radiusDp));
        if (stroke != 0) g.setStroke(px(2), stroke);
        return g;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void loadThumb(String asset, ImageView into) {
        Bitmap hit = cache.get(asset);
        into.setTag(asset);
        if (hit != null) { into.setImageBitmap(hit); return; }
        into.setImageDrawable(null);
        pool.execute(() -> {
            try (InputStream in = getAssets().open(asset)) {
                Bitmap b = BitmapFactory.decodeStream(in);
                if (b == null) return;
                cache.put(asset, b);
                runOnUiThread(() -> { if (asset.equals(into.getTag())) into.setImageBitmap(b); });
            } catch (Exception ignored) {
            }
        });
    }

    /** ImageView that keeps a fixed height/width ratio. */
    static class AspectImage extends ImageView {
        private final float ratio;
        AspectImage(android.content.Context c, float ratio) { super(c); this.ratio = ratio; }
        @Override protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            setMeasuredDimension(w, Math.round(w * ratio));
        }
    }
}
