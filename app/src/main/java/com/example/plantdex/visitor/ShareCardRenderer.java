package com.example.plantdex.visitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Draws the picture a visitor shares: their discovery's photo with its name, rarity, a short
 * message and their progress on one image (1080 x 1350, Instagram's portrait size).
 *
 * The message is drawn on the picture because Instagram (and some other apps) drop any text
 * sent alongside an image, so a separate caption would never show there.
 */
final class ShareCardRenderer {

    private static final int W = 1080, H = 1350, PAD = 72;
    private static final int GREEN = Color.parseColor("#2D6A4F");
    private static final int TILE = Color.parseColor("#D8F3DC");
    private static final int DARK = Color.parseColor("#1B1B1B");
    private static final int MUTED = Color.parseColor("#6B7280");

    private ShareCardRenderer() {}

    /** Runs off the UI thread: may download the picture. {@code image} may be null. */
    static Bitmap render(Context context, String image, String name, String rarity,
                         String message, String progress) {
        Bitmap out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(GREEN);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // White card
        RectF card = new RectF(PAD, PAD, W - PAD, H - PAD - 70);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(card, 48, 48, paint);

        // Photo (cropped to fill), or a plain tile when there's no picture
        RectF photo = new RectF(card.left + 36, card.top + 36, card.right - 36, card.top + 36 + 620);
        Bitmap picture = loadPicture(context, image);
        canvas.save();
        android.graphics.Path clip = new android.graphics.Path();
        clip.addRoundRect(photo, 32, 32, android.graphics.Path.Direction.CW);
        canvas.clipPath(clip);
        if (picture != null) {
            canvas.drawBitmap(picture, centreCrop(picture, photo), photo, new Paint(Paint.FILTER_BITMAP_FLAG));
        } else {
            paint.setColor(TILE);
            canvas.drawRect(photo, paint);
        }
        canvas.restore();

        float x = photo.left, width = photo.width();
        float y = photo.bottom + 56;

        // "NEW DISCOVERY" label and rarity pill on one line
        TextPaint label = text(30, MUTED, true);
        label.setLetterSpacing(0.12f);
        canvas.drawText("NEW DISCOVERY", x, y + 30, label);
        if (rarity != null && !rarity.isEmpty()) {
            TextPaint pill = text(30, GREEN, true);
            float pw = pill.measureText(rarity) + 48;
            RectF pr = new RectF(photo.right - pw, y - 8, photo.right, y + 44);
            paint.setColor(TILE);
            canvas.drawRoundRect(pr, 26, 26, paint);
            canvas.drawText(rarity, pr.left + 24, y + 30, pill);
        }
        y += 70;

        y = draw(canvas, name, text(64, DARK, true), x, y, width, 2) + 18;
        y = draw(canvas, message, text(36, DARK, false), x, y, width, 4) + 24;

        if (progress != null && !progress.isEmpty()) {
            draw(canvas, progress, text(32, GREEN, true), x, y, width, 1);
        }

        // Footer under the card
        TextPaint footer = text(34, Color.WHITE, true);
        String brand = "PlantDex · Singapore Botanic Gardens";
        canvas.drawText(brand, (W - footer.measureText(brand)) / 2f, H - PAD + 6, footer);
        return out;
    }

    private static TextPaint text(float size, int color, boolean bold) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    /** Draws wrapped text (at most {@code maxLines}, ending in "…") and returns the y below it. */
    private static float draw(Canvas canvas, String s, TextPaint paint, float x, float y, float width, int maxLines) {
        if (s == null || s.isEmpty()) return y;
        StaticLayout layout = StaticLayout.Builder.obtain(s, 0, s.length(), paint, (int) width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, 1.1f)
                .setMaxLines(maxLines)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build();
        canvas.save();
        canvas.translate(x, y);
        layout.draw(canvas);
        canvas.restore();
        return y + layout.getHeight();
    }

    private static Rect centreCrop(Bitmap b, RectF target) {
        float scale = Math.max(target.width() / b.getWidth(), target.height() / b.getHeight());
        int w = Math.round(target.width() / scale), h = Math.round(target.height() / scale);
        int left = (b.getWidth() - w) / 2, top = (b.getHeight() - h) / 2;
        return new Rect(left, top, left + w, top + h);
    }

    /** The visitor's own photo (content://) or the card's picture from the portal (http), or null. */
    private static Bitmap loadPicture(Context context, String image) {
        if (image == null || image.isEmpty()) return null;
        try {
            if (image.startsWith("content://") && android.os.Build.VERSION.SDK_INT >= 28) {
                // ImageDecoder applies the photo's EXIF rotation; software so we can draw it.
                return android.graphics.ImageDecoder.decodeBitmap(
                        android.graphics.ImageDecoder.createSource(context.getContentResolver(), Uri.parse(image)),
                        (decoder, info, src) -> {
                            decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                            int longest = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                            decoder.setTargetSampleSize(Math.max(1, longest / 1200));
                        });
            }
            if (image.startsWith("content://")) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;   // camera photos are far bigger than the card needs
                try (InputStream in = context.getContentResolver().openInputStream(Uri.parse(image))) {
                    return BitmapFactory.decodeStream(in, null, options);
                }
            }
            if (image.startsWith("http://") || image.startsWith("https://")) {
                HttpURLConnection conn = (HttpURLConnection) new URL(image).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                try (InputStream in = conn.getInputStream()) {
                    return BitmapFactory.decodeStream(in);
                } finally {
                    conn.disconnect();
                }
            }
        } catch (IOException | SecurityException e) {
            android.util.Log.w("ShareCard", "Could not load the picture", e);
        }
        return null;
    }
}
