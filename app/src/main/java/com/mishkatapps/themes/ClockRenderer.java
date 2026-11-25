package com.mishkatapps.themes;

import android.content.Context;
import android.content.SharedPreferences;
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

/** Draws the configurable lock-screen clock. Used by the live wallpaper and by the in-app preview. */
public final class ClockRenderer {

    public static final int GOLD = 0xFFD9B15C;
    public static final int NAVY = 0xFF0F2350;
    public static final int DEEP = 0xFF081433;
    public static final int BLUE = 0xFF17336E;
    public static final int CREAM = 0xFFEFE3C2;

    public static final String PREFS = "clock";
    public static final String KEY_SHAPE = "shape";
    public static final String KEY_PALETTE = "palette";
    public static final String KEY_MARKS = "marks";
    public static final String KEY_HANDS = "hands";
    public static final String KEY_BATTERY = "battery";
    public static final String KEY_TICKS = "ticks";
    public static final String KEY_DATEWIN = "datewin";
    public static final String KEY_BG = "bg";
    public static final String KEY_SECONDS = "seconds";
    public static final String KEY_VERSE = "verse";

    public static final String BG_BLACK = "#black";
    public static final String DEFAULT_BG = "01-mishkat-home.jpg";

    public static final String[] SHAPE_NAMES = {"دائرية", "نجمة مشكاة", "مربعة", "ثُمانية"};
    public static final String[] PALETTE_NAMES = {"أزرق ملكي وذهبي", "زمردي وذهبي", "عاجي وذهبي", "أسود وذهبي", "أبيض كلاسيكي"};
    public static final String[] MARK_NAMES = {"أرقام كاملة", "أرقام رئيسية", "عِصيّ", "نقاط", "أرقام رومانية", "بدون"};
    public static final String[] HAND_NAMES = {"مدببة", "مستقيمة", "رفيعة"};
    public static final String[] BATTERY_NAMES = {"حلقة حول الساعة", "قنديل", "شريط", "دائرة داخل الساعة", "نسبة فقط", "إخفاء"};

    static final String VERSE = "﴿ اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ﴾";
    static final String[] DAYS = {"الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
    static final String[] DAYS_EN = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
    static final String[] G_MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو",
            "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
    static final String[] H_MONTHS = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى",
            "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};
    static final String[] ROMAN = {"XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"};

    /** Colours of one dial theme. */
    static final class Palette {
        final int faceTop, faceBottom, rim, mark, num, hand, sec;
        Palette(int faceTop, int faceBottom, int rim, int mark, int num, int hand, int sec) {
            this.faceTop = faceTop; this.faceBottom = faceBottom; this.rim = rim;
            this.mark = mark; this.num = num; this.hand = hand; this.sec = sec;
        }
    }

    static final Palette[] PALETTES = {
            new Palette(0xFF1D3D80, 0xFF081433, GOLD, GOLD, GOLD, GOLD, 0xFFE8D29A),
            new Palette(0xFF13805C, 0xFF03261B, GOLD, GOLD, GOLD, GOLD, 0xFFE8D29A),
            new Palette(0xFFFFFCF2, 0xFFE6D8B6, 0xFFC9A04E, 0xFFA8822E, 0xFF6E521C, 0xFF8C6A26, 0xFFB23A2E),
            new Palette(0xFF2A2A2A, 0xFF050505, GOLD, GOLD, GOLD, GOLD, 0xFFE8D29A),
            new Palette(0xFFFFFFFF, 0xFFEDEDED, 0xFF2E5E78, 0xFF1A1A1A, 0xFF111111, 0xFF111111, 0xFFE8A33D),
    };

    /** All user choices for the clock. */
    public static final class Style {
        public int shape = 1, palette = 0, marks = 1, hands = 0, battery = 0;
        public boolean ticks = true, dateWin = false, seconds = true, verse = true;
        public String bg = "";

        public static Style from(SharedPreferences p) {
            Style s = new Style();
            s.shape = clamp(p.getInt(KEY_SHAPE, 1), SHAPE_NAMES.length);
            s.palette = clamp(p.getInt(KEY_PALETTE, 0), PALETTE_NAMES.length);
            s.marks = clamp(p.getInt(KEY_MARKS, 1), MARK_NAMES.length);
            s.hands = clamp(p.getInt(KEY_HANDS, 0), HAND_NAMES.length);
            s.battery = clamp(p.getInt(KEY_BATTERY, 0), BATTERY_NAMES.length);
            s.ticks = p.getBoolean(KEY_TICKS, true);
            s.dateWin = p.getBoolean(KEY_DATEWIN, false);
            s.seconds = p.getBoolean(KEY_SECONDS, true);
            s.verse = p.getBoolean(KEY_VERSE, true);
            s.bg = p.getString(KEY_BG, "");
            return s;
        }

        private static int clamp(int v, int n) { return v < 0 || v >= n ? 0 : v; }

        /** Wallpaper asset to draw behind the clock, or null for plain black. */
        public String bgFile() {
            if (BG_BLACK.equals(bg)) return null;
            return bg == null || bg.isEmpty() ? DEFAULT_BG : bg;
        }
    }

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

    // ------------------------------------------------------------------ main entry

    public static void draw(Context c, Canvas cv, int w, int h, Style st, Bitmap bg,
                            int level, boolean charging, Calendar now) {
        Typeface tf = font(c);
        Palette pal = PALETTES[st.palette];
        level = Math.max(0, Math.min(100, level));

        if (st.bgFile() == null) {
            cv.drawColor(Color.BLACK);
        } else if (bg != null) {
            drawCover(cv, bg, w, h);
        } else {
            Paint p = new Paint();
            p.setShader(new LinearGradient(0, 0, 0, h, new int[]{BLUE, NAVY, DEEP}, null, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
        }
        Paint v = new Paint();
        v.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0x66081433, 0x00081433, 0x00081433, 0x80081433}, new float[]{0f, 0.25f, 0.7f, 1f},
                Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, v);

        float R = w * 0.31f;
        float cx = w / 2f, cy = h * 0.45f;
        float ringR = R * 1.14f;
        int accent = (st.palette == 2 || st.palette == 4) ? pal.rim : GOLD;

        if (st.battery == 0) drawRing(cv, w, cx, cy, ringR, level, accent);
        drawFace(cv, w, cx, cy, R, st.shape, pal);

        float markR = R * markScale(st.shape);
        boolean skip3 = st.dateWin, skip6 = st.battery == 3;
        if (st.ticks) minuteTicks(cv, w, cx, cy, markR, pal.mark);
        drawMarks(cv, w, cx, cy, markR, st.marks, pal, tf, skip3, skip6);

        Paint brand = plain(tf, w * 0.032f, pal.num);
        brand.setAlpha(190);
        cv.drawText("مشكاة", cx, cy - R * 0.30f, brand);

        if (st.dateWin) drawDateWindow(cv, w, cx + markR * 0.58f, cy, now, pal);
        if (st.battery == 3) drawSubdial(cv, w, cx, cy + R * 0.42f, R * 0.19f, level, charging, pal, tf);
        drawHands(cv, w, cx, cy, markR, now, st, pal);

        // ---- below the dial
        float y = cy + (st.battery == 0 ? ringR : R) + w * 0.05f;
        switch (st.battery) {
            case 0:
            case 4:
                y = batteryPill(cv, w, cx, y, tf, level, charging) + w * 0.03f;
                break;
            case 1:
                y = lampRow(cv, w, cx, y, tf, level, charging) + w * 0.03f;
                break;
            case 2:
                y = batteryBar(cv, w, cx, y, tf, level, charging) + w * 0.03f;
                break;
            default:
                break;
        }
        y = drawDate(cv, w, cx, y, tf, now);
        if (st.verse) {
            float vy = Math.min(y + w * 0.04f, h * 0.875f);
            rtl(cv, VERSE, cx, vy, w, text(tf, w * 0.056f, GOLD));
        }
    }

    static void drawCover(Canvas cv, Bitmap b, int w, int h) {
        float s = Math.max(w / (float) b.getWidth(), h / (float) b.getHeight());
        int sw = Math.round(w / s), sh = Math.round(h / s);
        int sx = (b.getWidth() - sw) / 2, sy = (b.getHeight() - sh) / 2;
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(b, new Rect(sx, sy, sx + sw, sy + sh), new Rect(0, 0, w, h), p);
    }

    // ------------------------------------------------------------------ dial

    static float markScale(int shape) {
        switch (shape) {
            case 1: return 0.70f;   // star: stay inside the inner points
            case 2: return 0.80f;   // rounded square
            case 3: return 0.84f;   // octagon
            default: return 0.90f;  // circle
        }
    }

    static Path shapePath(int shape, float cx, float cy, float R) {
        Path p = new Path();
        switch (shape) {
            case 1:
                return star(cx, cy, R, R * 0.786f);
            case 2: {
                float a = R * 0.88f;
                p.addRoundRect(new RectF(cx - a, cy - a, cx + a, cy + a), R * 0.30f, R * 0.30f, Path.Direction.CW);
                return p;
            }
            case 3: {
                for (int i = 0; i < 8; i++) {
                    double ang = Math.toRadians(-90 + 22.5 + 45 * i);
                    float x = cx + (float) (R * Math.cos(ang)), yy = cy + (float) (R * Math.sin(ang));
                    if (i == 0) p.moveTo(x, yy); else p.lineTo(x, yy);
                }
                p.close();
                return p;
            }
            default:
                p.addCircle(cx, cy, R, Path.Direction.CW);
                return p;
        }
    }

    static void drawFace(Canvas cv, int w, float cx, float cy, float R, int shape, Palette pal) {
        Path face = shapePath(shape, cx, cy, R);
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0x00000000);
        shadow.setShadowLayer(w * 0.03f, 0, w * 0.01f, 0x99000000);
        shadow.setColor(pal.faceBottom);
        cv.drawPath(face, shadow);

        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setShader(new RadialGradient(cx, cy - R * 0.3f, R * 1.25f, pal.faceTop, pal.faceBottom, Shader.TileMode.CLAMP));
        cv.drawPath(face, f);

        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeJoin(Paint.Join.ROUND);
        rim.setColor(pal.rim);
        rim.setStrokeWidth(w * 0.011f);
        cv.drawPath(face, rim);
        if (shape == 0) {
            rim.setStrokeWidth(w * 0.0025f);
            rim.setAlpha(160);
            cv.drawCircle(cx, cy, R * 0.955f, rim);
        }
    }

    static void minuteTicks(Canvas cv, int w, float cx, float cy, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setAlpha(150);
        p.setStrokeWidth(w * 0.0032f);
        float len = w * 0.016f;
        for (int i = 0; i < 60; i++) {
            if (i % 5 == 0) continue;
            double a = Math.toRadians(i * 6);
            float s = (float) Math.sin(a), co = (float) Math.cos(a);
            cv.drawLine(cx + (r - len) * s, cy - (r - len) * co, cx + r * s, cy - r * co, p);
        }
    }

    static void drawMarks(Canvas cv, int w, float cx, float cy, float r, int style, Palette pal,
                          Typeface tf, boolean skip3, boolean skip6) {
        Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        bar.setColor(pal.mark);
        bar.setStrokeCap(Paint.Cap.ROUND);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(pal.mark);
        float inset = w * 0.022f;
        for (int i = 0; i < 12; i++) {
            int hour = i == 0 ? 12 : i;
            if ((hour == 3 && skip3) || (hour == 6 && skip6)) continue;
            boolean main = hour % 3 == 0;
            double a = Math.toRadians(i * 30);
            float s = (float) Math.sin(a), co = (float) Math.cos(a);
            switch (style) {
                case 0:
                    numeral(cv, Integer.toString(hour), cx, cy, r - w * 0.065f, s, co, w * 0.060f, pal.num, tf);
                    break;
                case 1:
                    if (main) numeral(cv, Integer.toString(hour), cx, cy, r - w * 0.068f, s, co, w * 0.072f, pal.num, tf);
                    else baton(cv, bar, cx, cy, r - inset, r - inset - w * 0.045f, s, co, w * 0.011f);
                    break;
                case 2: {
                    float len = main ? w * 0.075f : w * 0.050f;
                    float wd = main ? w * 0.016f : w * 0.011f;
                    if (hour == 12) {
                        float off = w * 0.014f;
                        baton(cv, bar, cx - off, cy, r - inset, r - inset - len, s, co, wd);
                        baton(cv, bar, cx + off, cy, r - inset, r - inset - len, s, co, wd);
                    } else {
                        baton(cv, bar, cx, cy, r - inset, r - inset - len, s, co, wd);
                    }
                    break;
                }
                case 3: {
                    float rr = r - inset - w * 0.012f;
                    cv.drawCircle(cx + rr * s, cy - rr * co, main ? w * 0.016f : w * 0.008f, dot);
                    break;
                }
                case 4:
                    numeral(cv, ROMAN[i], cx, cy, r - w * 0.07f, s, co, main ? w * 0.052f : w * 0.040f, pal.num, tf);
                    break;
                default:
                    break;
            }
        }
    }

    static void baton(Canvas cv, Paint p, float cx, float cy, float r1, float r2, float s, float co, float width) {
        p.setStrokeWidth(width);
        cv.drawLine(cx + r1 * s, cy - r1 * co, cx + r2 * s, cy - r2 * co, p);
    }

    static void numeral(Canvas cv, String t, float cx, float cy, float r, float s, float co, float size, int color, Typeface tf) {
        Paint p = plain(tf, size, color);
        Rect b = new Rect();
        p.getTextBounds(t, 0, t.length(), b);
        float x = cx + r * s, y = cy - r * co;
        cv.drawText(t, x, y - (b.top + b.bottom) / 2f, p);
    }

    static void drawDateWindow(Canvas cv, int w, float x, float y, Calendar now, Palette pal) {
        String t = DAYS_EN[now.get(Calendar.DAY_OF_WEEK) - 1] + " " + now.get(Calendar.DAY_OF_MONTH);
        Paint p = plain(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), w * 0.034f, pal.num);
        Rect b = new Rect();
        p.getTextBounds(t, 0, t.length(), b);
        float tw = p.measureText(t), padX = w * 0.016f, padY = w * 0.012f;
        RectF r = new RectF(x - tw / 2 - padX, y - b.height() / 2f - padY, x + tw / 2 + padX, y + b.height() / 2f + padY);
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(pal.faceBottom);
        cv.drawRoundRect(r, w * 0.01f, w * 0.01f, f);
        Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeWidth(w * 0.004f);
        s.setColor(pal.rim);
        cv.drawRoundRect(r, w * 0.01f, w * 0.01f, s);
        cv.drawText(t, x, y - (b.top + b.bottom) / 2f, p);
    }

    // ------------------------------------------------------------------ hands

    static void drawHands(Canvas cv, int w, float cx, float cy, float L, Calendar now, Style st, Palette pal) {
        int hr = now.get(Calendar.HOUR), mn = now.get(Calendar.MINUTE), sc = now.get(Calendar.SECOND);
        float ha = (hr + mn / 60f) * 30f, ma = (mn + sc / 60f) * 6f, sa = sc * 6f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(pal.hand);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setShadowLayer(w * 0.008f, 0, w * 0.004f, 0x88000000);
        float hw, mw;
        switch (st.hands) {
            case 0:
                dauphine(cv, cx, cy, ha, L * 0.58f, w * 0.026f, p);
                dauphine(cv, cx, cy, ma, L * 0.88f, w * 0.022f, p);
                break;
            default:
                hw = st.hands == 1 ? w * 0.022f : w * 0.011f;
                mw = st.hands == 1 ? w * 0.014f : w * 0.008f;
                line(cv, cx, cy, ha, L * 0.55f, w * 0.03f, hw, p);
                line(cv, cx, cy, ma, L * 0.85f, w * 0.03f, mw, p);
                break;
        }
        if (st.seconds) {
            Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
            s.setColor(pal.sec);
            s.setStrokeCap(Paint.Cap.ROUND);
            s.setShadowLayer(w * 0.006f, 0, w * 0.003f, 0x77000000);
            line(cv, cx, cy, sa, L * 0.95f, L * 0.22f, w * 0.005f, s);
            cv.save();
            cv.rotate(sa, cx, cy);
            s.setStyle(Paint.Style.STROKE);
            s.setStrokeWidth(w * 0.004f);
            cv.drawCircle(cx, cy + L * 0.16f, w * 0.012f, s);
            cv.restore();
        }
        Paint d = new Paint(Paint.ANTI_ALIAS_FLAG);
        d.setColor(st.seconds ? pal.sec : pal.hand);
        cv.drawCircle(cx, cy, w * 0.020f, d);
        d.setColor(pal.faceBottom);
        cv.drawCircle(cx, cy, w * 0.008f, d);
    }

    static void line(Canvas cv, float cx, float cy, float angle, float len, float tail, float width, Paint p) {
        p.setStrokeWidth(width);
        p.setStyle(Paint.Style.STROKE);
        cv.save();
        cv.rotate(angle, cx, cy);
        cv.drawLine(cx, cy + tail, cx, cy - len, p);
        cv.restore();
    }

    static void dauphine(Canvas cv, float cx, float cy, float angle, float len, float half, Paint p) {
        cv.save();
        cv.rotate(angle, cx, cy);
        Path left = new Path();
        left.moveTo(cx, cy - len);
        left.lineTo(cx - half, cy - len * 0.10f);
        left.lineTo(cx, cy + len * 0.12f);
        left.close();
        Path right = new Path();
        right.moveTo(cx, cy - len);
        right.lineTo(cx + half, cy - len * 0.10f);
        right.lineTo(cx, cy + len * 0.12f);
        right.close();
        p.setStyle(Paint.Style.FILL);
        cv.drawPath(left, p);
        Paint dark = new Paint(p);
        dark.clearShadowLayer();
        dark.setColor(darken(p.getColor()));
        cv.drawPath(right, dark);
        cv.restore();
    }

    static int darken(int c) {
        return Color.argb(Color.alpha(c), (int) (Color.red(c) * 0.72f), (int) (Color.green(c) * 0.72f), (int) (Color.blue(c) * 0.72f));
    }

    // ------------------------------------------------------------------ battery styles

    static void drawRing(Canvas cv, int w, float cx, float cy, float r, int level, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.014f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(color);
        p.setAlpha(55);
        cv.drawCircle(cx, cy, r, p);
        p.setAlpha(255);
        p.setShadowLayer(w * 0.012f, 0, 0, 0xAAD9B15C);
        cv.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -90, 360f * Math.max(1, level) / 100f, false, p);
    }

    static void drawSubdial(Canvas cv, int w, float x, float y, float r, int level, boolean charging, Palette pal, Typeface tf) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w * 0.008f);
        p.setColor(pal.mark);
        p.setAlpha(60);
        cv.drawCircle(x, y, r, p);
        p.setAlpha(255);
        cv.drawArc(new RectF(x - r, y - r, x + r, y + r), -90, 360f * Math.max(1, level) / 100f, false, p);
        String t = level + "%";
        Paint tp = plain(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), r * 0.62f, pal.num);
        Rect b = new Rect();
        tp.getTextBounds(t, 0, t.length(), b);
        cv.drawText(t, x, y - (b.top + b.bottom) / 2f, tp);
        if (charging) drawBolt(cv, x, y + r * 0.62f, r * 0.30f, pal.mark);
    }

    /** "63%" with a bolt while charging, on a dark pill; returns the bottom y. */
    static float batteryPill(Canvas cv, int w, float cx, float top, Typeface tf, int level, boolean charging) {
        String t = level + "%";
        Paint tp = plain(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), w * 0.045f, GOLD);
        Rect b = new Rect();
        tp.getTextBounds(t, 0, t.length(), b);
        float tw = tp.measureText(t);
        float bolt = charging ? b.height() * 0.9f : 0, gap = charging ? w * 0.015f : 0;
        float total = tw + bolt + gap;
        float padX = w * 0.045f, padY = w * 0.020f;
        RectF r = new RectF(cx - total / 2 - padX, top, cx + total / 2 + padX, top + b.height() + 2 * padY);
        pillBg(cv, w, r);
        float base = top + padY - b.top;
        cv.drawText(t, cx + (bolt + gap) / 2f, base, tp);
        if (charging) drawBolt(cv, cx - total / 2 + bolt / 2, top + padY + b.height() / 2f, b.height() * 0.55f, GOLD);
        return r.bottom;
    }

    static float batteryBar(Canvas cv, int w, float cx, float top, Typeface tf, int level, boolean charging) {
        float bw = w * 0.42f, bh = w * 0.034f, bx = cx - bw / 2;
        Paint o = new Paint(Paint.ANTI_ALIAS_FLAG);
        o.setStyle(Paint.Style.STROKE);
        o.setColor(GOLD);
        o.setStrokeWidth(w * 0.004f);
        cv.drawRoundRect(new RectF(bx, top, bx + bw, top + bh), bh / 2, bh / 2, o);
        Paint nub = new Paint(Paint.ANTI_ALIAS_FLAG);
        nub.setColor(GOLD);
        cv.drawRoundRect(new RectF(bx + bw + w * 0.006f, top + bh * 0.3f, bx + bw + w * 0.014f, top + bh * 0.7f), 3, 3, nub);
        Paint in = new Paint(Paint.ANTI_ALIAS_FLAG);
        float pad = w * 0.006f;
        float fw = (bw - 2 * pad) * level / 100f;
        in.setShader(new LinearGradient(bx, 0, bx + bw, 0, 0xFFB8903F, 0xFFF1D58E, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(bx + pad, top + pad, bx + pad + fw, top + bh - pad), bh / 2, bh / 2, in);
        return batteryPill(cv, w, cx, top + bh + w * 0.02f, tf, level, charging);
    }

    static float lampRow(Canvas cv, int w, float cx, float top, Typeface tf, int level, boolean charging) {
        float lampH = w * 0.19f;
        float s = lampH / 415f;
        float lampW = 200 * s;
        float lampX = cx + w * 0.10f;               // lamp to the right, percentage to the left
        cv.save();
        cv.translate(lampX - 500 * s, top - 225 * s);
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
        // percentage pill, vertically centred on the lamp, to its left
        float pillCx = cx - w * 0.10f;
        Paint tp = plain(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), w * 0.045f, GOLD);
        Rect b = new Rect();
        tp.getTextBounds("0%", 0, 2, b);
        float pillH = b.height() + 2 * w * 0.020f;
        batteryPill(cv, w, pillCx, top + lampH / 2f - pillH / 2f, tf, level, charging);
        return top + Math.max(lampH, lampW);
    }

    static void drawBolt(Canvas cv, float x, float y, float size, int color) {
        Path p = new Path();
        p.moveTo(x + size * 0.15f, y - size);
        p.lineTo(x - size * 0.55f, y + size * 0.12f);
        p.lineTo(x - size * 0.02f, y + size * 0.12f);
        p.lineTo(x - size * 0.15f, y + size);
        p.lineTo(x + size * 0.55f, y - size * 0.12f);
        p.lineTo(x + size * 0.02f, y - size * 0.12f);
        p.close();
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(color);
        cv.drawPath(p, f);
    }

    static void pillBg(Canvas cv, int w, RectF r) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0x99081433);
        float rad = r.height() / 2f;
        cv.drawRoundRect(r, rad, rad, bg);
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(w * 0.0025f);
        rim.setColor(0x66D9B15C);
        cv.drawRoundRect(r, rad, rad, rim);
    }

    // ------------------------------------------------------------------ date

    /** Gregorian + Hijri dates as two centred lines inside a box sized from the real text bounds. */
    static float drawDate(Canvas cv, int w, float cx, float top, Typeface tf, Calendar now) {
        String g = DAYS[now.get(Calendar.DAY_OF_WEEK) - 1] + "  " + now.get(Calendar.DAY_OF_MONTH)
                + " " + G_MONTHS[now.get(Calendar.MONTH)] + " " + now.get(Calendar.YEAR);
        String hj = null;
        try {
            android.icu.util.IslamicCalendar h = new android.icu.util.IslamicCalendar();
            h.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            h.setTimeInMillis(now.getTimeInMillis());
            hj = h.get(android.icu.util.Calendar.DAY_OF_MONTH) + " " + H_MONTHS[h.get(android.icu.util.Calendar.MONTH)]
                    + " " + h.get(android.icu.util.Calendar.EXTENDED_YEAR) + " هـ";
        } catch (Throwable ignored) {}

        Paint p1 = plain(tf, w * 0.055f, CREAM);
        Paint p2 = plain(tf, w * 0.047f, GOLD);
        Rect b1 = new Rect(), b2 = new Rect();
        p1.getTextBounds(g, 0, g.length(), b1);
        float w1 = Math.max(p1.measureText(g), b1.width());
        float w2 = 0;
        if (hj != null) {
            p2.getTextBounds(hj, 0, hj.length(), b2);
            w2 = Math.max(p2.measureText(hj), b2.width());
        }
        float gap = hj == null ? 0 : w * 0.022f;
        float padX = w * 0.05f, padY = w * 0.028f;
        float boxW = Math.max(w1, w2) + 2 * padX;
        float boxH = b1.height() + (hj == null ? 0 : b2.height()) + gap + 2 * padY;
        RectF r = new RectF(cx - boxW / 2, top, cx + boxW / 2, top + boxH);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0x99081433);
        cv.drawRoundRect(r, w * 0.04f, w * 0.04f, bg);
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(w * 0.0025f);
        rim.setColor(0x66D9B15C);
        cv.drawRoundRect(r, w * 0.04f, w * 0.04f, rim);

        float yy = top + padY;
        cv.drawText(g, cx, yy - b1.top, p1);
        yy += b1.height() + gap;
        if (hj != null) cv.drawText(hj, cx, yy - b2.top, p2);
        return r.bottom;
    }

    // ------------------------------------------------------------------ helpers

    /** Centred text paint with a soft shadow; for drawText. */
    static Paint plain(Typeface tf, float size, int color) {
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(tf);
        t.setTextSize(size);
        t.setColor(color);
        t.setTextAlign(Paint.Align.CENTER);
        t.setShadowLayer(size * 0.12f, 0, size * 0.04f, 0x99000000);
        return t;
    }

    static Paint text(Typeface tf, float size, int color) {
        TextPaint t = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(tf);
        t.setTextSize(size);
        t.setColor(color);
        t.setTextAlign(Paint.Align.CENTER);
        t.setShadowLayer(size * 0.15f, 0, size * 0.04f, 0xCC000000);
        return t;
    }

    /** Draws centred RTL text (may wrap) with its top at y; returns the y below it. */
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

    static Path lampBody() {
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
}
