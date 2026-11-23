package com.mishkatapps.themes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import java.io.InputStream;
import java.util.Calendar;

/** Draws the three lock-screen clock designs. Used by the live wallpaper and by the in-app preview. */
public final class ClockRenderer {

    public static final int GOLD = 0xFFD9B15C;
    public static final int NAVY = 0xFF0F2350;
    public static final int DEEP = 0xFF081433;
    public static final int BLUE = 0xFF17336E;
    public static final int CREAM = 0xFFEFE3C2;

    public static final String PREFS = "clock";
    public static final String KEY_DESIGN = "design";
    public static final String KEY_BG = "bg";
    public static final String KEY_SECONDS = "seconds";
    public static final String KEY_VERSE = "verse";

    static final String VERSE = "﴿ اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ﴾";
    static final String[] DAYS = {"الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
    static final String[] G_MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو",
            "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
    static final String[] H_MONTHS = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى",
            "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};

    private static Typeface amiri;

    private ClockRenderer() {}

    public static Typeface font(Context c) {
        if (amiri == null) {
            try {
                amiri = Typeface.createFromAsset(c.getAssets(), "fonts/Amiri-Regular.ttf");
            } catch (Exception e) {
                amiri = Typeface.SERIF;
            }
        }
        return amiri;
    }

    /** Default background for a design when the user picked "auto". */
    public static String defaultBg(int design) {
        if (design == 0) return "01-mishkat-home.jpg";
        if (design == 1) return "05-mishkat-star.jpg";
        return null;
    }

    /** Loads a wallpaper asset scaled down to roughly the target size. */
    public static Bitmap loadBg(Context c, String file, int targetW, int targetH) {
        if (file == null || file.isEmpty()) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = c.getAssets().open("wallpapers/" + file);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int sample = 1;
            while (o.outWidth / (sample * 2) >= targetW && o.outHeight / (sample * 2) >= targetH) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = c.getAssets().open("wallpapers/" + file);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            in.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    public static String ar(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') b.append((char) ('٠' + (ch - '0')));
            else b.append(ch);
        }
        return b.toString();
    }

    public static String ar(int n) {
        return ar(Integer.toString(n));
    }

    public static String dateLine(Calendar now) {
        String g = DAYS[now.get(Calendar.DAY_OF_WEEK) - 1] + " " + ar(now.get(Calendar.DAY_OF_MONTH))
                + " " + G_MONTHS[now.get(Calendar.MONTH)];
        try {
            android.icu.util.IslamicCalendar h = new android.icu.util.IslamicCalendar();
            h.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            h.setTimeInMillis(now.getTimeInMillis());
            int d = h.get(android.icu.util.Calendar.DAY_OF_MONTH);
            int m = h.get(android.icu.util.Calendar.MONTH);
            return g + "، " + ar(d) + " " + H_MONTHS[m];
        } catch (Throwable t) {
            return g;
        }
    }

    // ------------------------------------------------------------------ main entry

    public static void draw(Context c, Canvas cv, int w, int h, int design, Bitmap bg,
                            int level, boolean charging, Calendar now,
                            boolean seconds, boolean verse) {
        Typeface tf = font(c);
        if (design == 2) {
            cv.drawColor(Color.BLACK);
        } else if (bg != null) {
            drawCover(cv, bg, w, h);
        } else {
            Paint p = new Paint();
            p.setShader(new LinearGradient(0, 0, 0, h, new int[]{BLUE, NAVY, DEEP}, null, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
        }
        // soft vignette so gold reads on any photo
        Paint v = new Paint();
        v.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0x66081433, 0x00081433, 0x00081433, 0x80081433}, new float[]{0f, 0.25f, 0.7f, 1f},
                Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, v);

        switch (design) {
            case 1: drawLamp(cv, w, h, tf, level, charging, now, seconds, verse); break;
            case 2: drawAmoled(cv, w, h, tf, level, charging, now, seconds, verse); break;
            default: drawStar(cv, w, h, tf, level, charging, now, seconds, verse); break;
        }
    }

    static void drawCover(Canvas cv, Bitmap b, int w, int h) {
        float s = Math.max(w / (float) b.getWidth(), h / (float) b.getHeight());
        int sw = Math.round(w / s), sh = Math.round(h / s);
        int sx = (b.getWidth() - sw) / 2, sy = (b.getHeight() - sh) / 2;
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(b, new Rect(sx, sy, sx + sw, sy + sh), new Rect(0, 0, w, h), p);
    }

    // ------------------------------------------------------------------ design A: star dial + battery ring

    static void drawStar(Canvas cv, int w, int h, Typeface tf, int level, boolean charging,
                         Calendar now, boolean seconds, boolean verse) {
        float cx = w / 2f, cy = h * 0.36f;
        float ring = w * 0.355f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.017f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(GOLD);
        p.setAlpha(45);
        cv.drawCircle(cx, cy, ring, p);
        p.setAlpha(255);
        p.setShadowLayer(w * 0.012f, 0, 0, 0xB3D9B15C);
        RectF ro = new RectF(cx - ring, cy - ring, cx + ring, cy + ring);
        cv.drawArc(ro, -90, 360f * Math.max(1, level) / 100f, false, p);
        p.clearShadowLayer();

        float r = w * 0.31f;
        Path star = star(cx, cy, r, r * 0.786f);
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setShader(new RadialGradient(cx, cy - r * 0.2f, r * 1.1f, 0xFF1D3D80, DEEP, Shader.TileMode.CLAMP));
        cv.drawPath(star, f);
        p.setStrokeWidth(w * 0.008f);
        p.setStrokeJoin(Paint.Join.ROUND);
        cv.drawPath(star, p);

        ticks(cv, cx, cy, w * 0.233f, w, GOLD);
        numerals(cv, cx, cy, w * 0.178f, w * 0.062f, tf, false, GOLD);
        Paint t = text(tf, w * 0.034f, GOLD);
        t.setAlpha(200);
        cv.drawText("مشكاة", cx, cy + w * 0.12f, t);
        hands(cv, cx, cy, w * 0.222f, w, now, seconds);

        float y = cy + ring + w * 0.07f;
        y = batteryPill(cv, w, cx, y, tf, level, charging, true);
        y += w * 0.02f;
        rtl(cv, dateLine(now), cx, y, w, text(tf, w * 0.05f, CREAM));
        if (verse) rtl(cv, VERSE, cx, h * 0.70f, w, text(tf, w * 0.061f, GOLD));
    }

    // ------------------------------------------------------------------ design B: round dial + lamp battery

    static void drawLamp(Canvas cv, int w, int h, Typeface tf, int level, boolean charging,
                         Calendar now, boolean seconds, boolean verse) {
        float cx = w / 2f, cy = h * 0.30f, rad = w * 0.34f;
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(0x8C081433);
        cv.drawCircle(cx, cy, rad, f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(GOLD);
        p.setStrokeWidth(w * 0.007f);
        cv.drawCircle(cx, cy, rad, p);
        p.setStrokeWidth(w * 0.0025f);
        p.setAlpha(150);
        cv.drawCircle(cx, cy, rad * 0.934f, p);
        p.setAlpha(90);
        cv.drawPath(star(cx, cy, rad * 0.475f, rad * 0.377f), p);
        ticks(cv, cx, cy, rad * 0.90f, w, GOLD);
        numerals(cv, cx, cy, rad * 0.72f, w * 0.058f, tf, true, GOLD);
        hands(cv, cx, cy, rad * 0.82f, w, now, seconds);

        float y = cy + rad + w * 0.035f;
        rtl(cv, dateLine(now), cx, y, w, text(tf, w * 0.05f, CREAM));

        // lamp from the Mishkat logo, filled to the battery level
        float lampH = w * 0.40f;
        float s = lampH / 415f;
        float top = cy + rad + w * 0.15f;
        cv.save();
        cv.translate(cx - 500 * s, top - 225 * s);
        cv.scale(s, s);
        Path body = lampBody();
        Paint stem = new Paint(Paint.ANTI_ALIAS_FLAG);
        stem.setColor(GOLD);
        stem.setStrokeWidth(12);
        cv.drawLine(500, 320, 500, 420, stem);
        Paint glass = new Paint(Paint.ANTI_ALIAS_FLAG);
        glass.setColor(BLUE);
        cv.drawPath(body, glass);
        float fy = 622 - 200 * level / 100f;
        cv.save();
        cv.clipPath(body);
        Paint oil = new Paint(Paint.ANTI_ALIAS_FLAG);
        oil.setShader(new LinearGradient(0, fy, 0, 622, 0xFFF1D58E, 0xFFB8903F, Shader.TileMode.CLAMP));
        cv.drawRect(400, fy, 600, 640, oil);
        cv.restore();
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setColor(GOLD);
        rim.setStrokeWidth(12);
        rim.setStrokeJoin(Paint.Join.ROUND);
        cv.drawPath(body, rim);
        // flame dims as the battery runs low
        Path flame = new Path();
        flame.moveTo(500, 334);
        flame.quadTo(530, 294, 500, 244);
        flame.quadTo(470, 294, 500, 334);
        flame.close();
        Paint fl = new Paint(Paint.ANTI_ALIAS_FLAG);
        fl.setColor(0xFFFFD98A);
        fl.setAlpha(70 + 185 * level / 100);
        fl.setShadowLayer(18, 0, 0, 0xFFFFCF6A);
        cv.drawPath(flame, fl);
        cv.restore();

        float py = top + lampH + w * 0.03f;
        batteryPill(cv, w, cx, py, tf, level, charging, false);
        if (verse) rtl(cv, VERSE, cx, h * 0.80f, w, text(tf, w * 0.061f, GOLD));
    }

    // ------------------------------------------------------------------ design C: AMOLED minimal

    static void drawAmoled(Canvas cv, int w, int h, Typeface tf, int level, boolean charging,
                           Calendar now, boolean seconds, boolean verse) {
        float cx = w / 2f, cy = h * 0.34f, rad = w * 0.33f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(GOLD);
        p.setAlpha(180);
        p.setStrokeWidth(w * 0.0035f);
        cv.drawCircle(cx, cy, rad, p);
        ticks(cv, cx, cy, rad * 0.95f, w, GOLD);
        numerals(cv, cx, cy, rad * 0.76f, w * 0.055f, tf, false, GOLD);
        hands(cv, cx, cy, rad * 0.85f, w, now, seconds);

        float y = cy + rad + w * 0.05f;
        rtl(cv, dateLine(now), cx, y, w, text(tf, w * 0.05f, CREAM));

        // battery bar
        float bw = w * 0.42f, bh = w * 0.033f, by = y + w * 0.13f, bx = cx - bw / 2;
        Paint o = new Paint(Paint.ANTI_ALIAS_FLAG);
        o.setStyle(Paint.Style.STROKE);
        o.setColor(GOLD);
        o.setStrokeWidth(w * 0.004f);
        cv.drawRoundRect(new RectF(bx, by, bx + bw, by + bh), bh / 2, bh / 2, o);
        Paint nub = new Paint(Paint.ANTI_ALIAS_FLAG);
        nub.setColor(GOLD);
        cv.drawRoundRect(new RectF(bx + bw + w * 0.006f, by + bh * 0.3f, bx + bw + w * 0.014f, by + bh * 0.7f), 3, 3, nub);
        Paint in = new Paint(Paint.ANTI_ALIAS_FLAG);
        float pad = w * 0.006f;
        float fw = (bw - 2 * pad) * level / 100f;
        in.setShader(new LinearGradient(bx, 0, bx + bw, 0, 0xFFB8903F, 0xFFF1D58E, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(bx + pad, by + pad, bx + pad + fw, by + bh - pad), bh / 2, bh / 2, in);
        batteryPill(cv, w, cx, by + bh + w * 0.02f, tf, level, charging, false);

        // outline logo
        float ls = w * 0.17f;
        cv.save();
        cv.translate(cx - ls / 2, h * 0.74f);
        cv.scale(ls / 920f, ls / 920f);
        cv.translate(-40, -40);
        drawLogoOutline(cv);
        cv.restore();
        if (verse) rtl(cv, VERSE, cx, h * 0.86f, w, text(tf, w * 0.052f, 0xFF9E8A5C));
    }

    // ------------------------------------------------------------------ helpers

    static Paint text(Typeface tf, float size, int color) {
        TextPaint t = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(tf);
        t.setTextSize(size);
        t.setColor(color);
        t.setTextAlign(Paint.Align.CENTER);
        t.setShadowLayer(size * 0.15f, 0, size * 0.04f, 0xCC000000);
        return t;
    }

    /** Draws centered RTL text with its top at y; returns the y below it. */
    static float rtl(Canvas cv, String s, float cx, float y, int w, Paint paint) {
        TextPaint tp = new TextPaint(paint);
        tp.setTextAlign(Paint.Align.LEFT);
        int width = Math.round(w * 0.9f);
        StaticLayout l = StaticLayout.Builder.obtain(s, 0, s.length(), tp, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .build();
        cv.save();
        cv.translate(cx - width / 2f, y);
        l.draw(cv);
        cv.restore();
        return y + l.getHeight();
    }

    static float batteryPill(Canvas cv, int w, float cx, float y, Typeface tf, int level,
                             boolean charging, boolean withLabel) {
        String s = ar(level) + "٪";
        if (charging) s = "⚡ " + s + (withLabel ? " – جارٍ الشحن" : "");
        Paint t = text(tf, w * 0.052f, GOLD);
        float tw = t.measureText(s);
        float ph = w * 0.085f;
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0x8C081433);
        RectF r = new RectF(cx - tw / 2 - w * 0.04f, y, cx + tw / 2 + w * 0.04f, y + ph);
        cv.drawRoundRect(r, ph / 2, ph / 2, bg);
        rtl(cv, s, cx, y + (ph - t.getFontSpacing()) / 2f, w, t);
        return y + ph;
    }

    static Path star(float cx, float cy, float r, float ri) {
        Path p = new Path();
        for (int i = 0; i < 16; i++) {
            double a = Math.PI / 8 * i - Math.PI / 2;
            float rr = (i % 2 == 0) ? r : ri;
            float x = cx + (float) (rr * Math.cos(a)), y = cy + (float) (rr * Math.sin(a));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    static void ticks(Canvas cv, float cx, float cy, float r, int w, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        for (int i = 0; i < 60; i++) {
            boolean big = i % 5 == 0;
            p.setStrokeWidth(big ? w * 0.0085f : w * 0.0035f);
            p.setAlpha(big ? 255 : 140);
            double a = Math.toRadians(i * 6);
            float r1 = r - (big ? w * 0.043f : w * 0.021f);
            cv.drawLine(cx + (float) (r1 * Math.sin(a)), cy - (float) (r1 * Math.cos(a)),
                    cx + (float) (r * Math.sin(a)), cy - (float) (r * Math.cos(a)), p);
        }
    }

    static void numerals(Canvas cv, float cx, float cy, float r, float size, Typeface tf, boolean all, int color) {
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(tf);
        t.setTextSize(size);
        t.setColor(color);
        t.setTextAlign(Paint.Align.CENTER);
        float off = -(t.descent() + t.ascent()) / 2f;
        for (int i = 1; i <= 12; i++) {
            if (!all && i % 3 != 0) continue;
            double a = Math.toRadians(i * 30 - 90);
            cv.drawText(ar(i), cx + (float) (r * Math.cos(a)), cy + (float) (r * Math.sin(a)) + off, t);
        }
    }

    static void hands(Canvas cv, float cx, float cy, float L, int w, Calendar now, boolean seconds) {
        int hr = now.get(Calendar.HOUR), mn = now.get(Calendar.MINUTE), sc = now.get(Calendar.SECOND);
        float ha = (hr + mn / 60f) * 30f, ma = (mn + sc / 60f) * 6f, sa = sc * 6f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(GOLD);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setShadowLayer(w * 0.008f, 0, w * 0.003f, 0x99000000);
        hand(cv, cx, cy, ha, L * 0.5f, 0, w * 0.022f, p);
        hand(cv, cx, cy, ma, L * 0.75f, 0, w * 0.014f, p);
        if (seconds) {
            Paint s = new Paint(p);
            s.setColor(0xFFE8D29A);
            hand(cv, cx, cy, sa, L * 0.84f, w * 0.07f, w * 0.005f, s);
        }
        Paint d = new Paint(Paint.ANTI_ALIAS_FLAG);
        d.setColor(GOLD);
        cv.drawCircle(cx, cy, w * 0.022f, d);
        d.setColor(NAVY);
        cv.drawCircle(cx, cy, w * 0.008f, d);
    }

    static void hand(Canvas cv, float cx, float cy, float angle, float len, float tail, float width, Paint p) {
        p.setStrokeWidth(width);
        cv.save();
        cv.rotate(angle, cx, cy);
        cv.drawLine(cx, cy + tail, cx, cy - len, p);
        cv.restore();
    }

    static Path lampBody() {
        // M448 422 H552 L528 458 Q578 482 578 532 Q578 598 500 622 Q422 598 422 532 Q422 482 472 458 Z
        Path b = new Path();
        b.moveTo(448, 422);
        b.lineTo(552, 422);
        b.lineTo(528, 458);
        b.quadTo(578, 482, 578, 532);
        b.quadTo(578, 598, 500, 622);
        b.quadTo(422, 598, 422, 532);
        b.quadTo(422, 482, 472, 458);
        b.close();
        return b;
    }

    /** Mishkat logo as gold outlines, in the SVG's 1000-unit coordinates. */
    static void drawLogoOutline(Canvas cv) {
        float[] pts = {500, 76, 624.4f, 199.7f, 799.8f, 200.2f, 800.3f, 375.6f, 924, 500, 800.3f, 624.4f,
                799.8f, 799.8f, 624.4f, 800.3f, 500, 924, 375.6f, 800.3f, 200.2f, 799.8f, 199.7f, 624.4f,
                76, 500, 199.7f, 375.6f, 200.2f, 200.2f, 375.6f, 199.7f};
        Path oct = new Path();
        oct.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) oct.lineTo(pts[i], pts[i + 1]);
        oct.close();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(GOLD);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(18);
        cv.drawPath(oct, p);
        Path arch = new Path();
        arch.moveTo(379, 670);
        arch.lineTo(379, 480);
        arch.quadTo(379, 360, 500, 297);
        arch.quadTo(621, 360, 621, 480);
        arch.lineTo(621, 670);
        p.setStrokeWidth(16);
        cv.drawPath(arch, p);
        cv.drawLine(335, 677, 665, 677, p);
        p.setStrokeWidth(12);
        cv.drawLine(500, 340, 500, 420, p);
        cv.drawPath(lampBody(), p);
        Path drop = new Path();
        drop.moveTo(500, 488);
        drop.quadTo(528, 525, 500, 580);
        drop.quadTo(472, 525, 500, 488);
        drop.close();
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(GOLD);
        cv.drawPath(drop, fp);
    }
}
