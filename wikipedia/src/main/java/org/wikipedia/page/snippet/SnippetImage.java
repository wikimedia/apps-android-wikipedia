package org.wikipedia.page.snippet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import org.wikipedia.R;

/**
 * Creator and holder of a Bitmap which is comprised of an optional lead image, a title,
 * optional description, text, the Wikipedia wordmark, and some license icons.
 */
public final class SnippetImage {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int HORIZONTAL_PADDING = 30;
    private static final int TEXT_WIDTH = WIDTH - 2 * HORIZONTAL_PADDING;
    private static final int DESCRIPTION_WIDTH = 360;
    private static final float SPACING_MULTIPLIER = 1.0f;
    private static final Typeface SERIF = Typeface.create("serif", Typeface.NORMAL);

    private final Context context;
    private final Bitmap leadImageBitmap;
    private final int faceYOffset;
    private final String title;
    private final String description;
    private final CharSequence textSnippet;
    private boolean isArticleRTL;

    public SnippetImage(Context context, Bitmap leadImageBitmap,
                        int faceYOffset, String title, String description,
                        CharSequence textSnippet) {
        this.context = context;
        this.leadImageBitmap = leadImageBitmap;
        this.faceYOffset = faceYOffset;
        this.title = title;
        this.description = description;
        this.textSnippet = textSnippet;
    }

    /**
     * Creates a card image usable for sharing and the preview of the same.
     * If we have a leadImageBitmap the use that as the background. If not then
     * just use a black background.
     */
    public Bitmap createImage() {
        Bitmap resultBitmap = drawBackground(leadImageBitmap, faceYOffset);
        Canvas canvas = new Canvas(resultBitmap);

        Layout textLayout = drawTextSnippet(canvas, textSnippet);
        isArticleRTL = textLayout.getParagraphDirection(0) == Layout.DIR_RIGHT_TO_LEFT;

        drawLicenseIcons(canvas, context);
        if (!TextUtils.isEmpty(description)) {
            drawDescription(canvas, description);
        }
        drawTitle(canvas, title);

        drawWordmark(canvas, context);

        return resultBitmap;
    }

    private Bitmap drawBackground(Bitmap leadImageBitmap, int faceYOffset) {
        Bitmap resultBitmap;
        if (leadImageBitmap != null) {
            // use lead image
            resultBitmap = scaleCropToFitFace(leadImageBitmap, WIDTH, HEIGHT, faceYOffset);
        } else {
            resultBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            // final int backgroundColor = Color.parseColor("#242438");
            final int backgroundColor = -14408648;
            resultBitmap.eraseColor(backgroundColor);
        }
        return resultBitmap;
    }

    private Layout drawTextSnippet(Canvas canvas, CharSequence textSnippet) {
        final int top = 10;
        final int maxHeight = 200;
        final int maxLines = 5;
        final float maxFontSize = 96.0f;
        final float minFontSize = 32.0f;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(maxFontSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(1.0f, 1.0f, 1.0f, Color.GRAY);

        StaticLayout textLayout = optimizeTextSize(
                new TextLayoutParams(textSnippet, textPaint, TEXT_WIDTH, SPACING_MULTIPLIER),
                maxHeight, maxLines, maxFontSize, minFontSize);

        canvas.save();
        canvas.translate(HORIZONTAL_PADDING, top);
        textLayout.draw(canvas);
        canvas.restore();

        return textLayout;
    }

    private void drawDescription(Canvas canvas, String description) {
        final int descriptionY = 287;
        final int maxHeight = 24;
        final int maxLines = 2;
        final float maxFontSize = 15.0f;
        final float minFontSize = 10.0f;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(maxFontSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(1.0f, 0.0f, 0.0f, Color.GRAY);

        StaticLayout textLayout = optimizeTextSize(
                new TextLayoutParams(description, textPaint, DESCRIPTION_WIDTH, SPACING_MULTIPLIER),
                maxHeight, maxLines, maxFontSize, minFontSize);
        int left = HORIZONTAL_PADDING;
        if (isArticleRTL) {
            left = WIDTH - HORIZONTAL_PADDING - textLayout.getWidth();
        }

        canvas.save();
        canvas.translate(left, descriptionY);
        textLayout.draw(canvas);
        canvas.restore();
    }

    private void drawTitle(Canvas canvas, String title) {
        final int top = 242;
        final int height = 44;
        final int maxLines = 2;
        final float maxFontSize = 30.0f;
        final float minFontSize = 19.0f;
        final float spacingMultiplier = 0.7f;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(maxFontSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(SERIF);
        textPaint.setShadowLayer(1.0f, 0.0f, 1.0f, Color.GRAY);

        StaticLayout textLayout = optimizeTextSize(
                new TextLayoutParams(title, textPaint, DESCRIPTION_WIDTH, spacingMultiplier),
                height, maxLines, maxFontSize, minFontSize);
        int left = HORIZONTAL_PADDING;
        if (isArticleRTL) {
            left = WIDTH - HORIZONTAL_PADDING - textLayout.getWidth();
        }

        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();
    }

    private void drawLicenseIcons(Canvas canvas, Context context) {
        final int iconsWidth = 52;
        final int iconsHeight = 16;
        final int top = 319;
        final int bottom = top + iconsHeight;

        int left = HORIZONTAL_PADDING;
        int right = left + iconsWidth;
        if (isArticleRTL) {
            right = WIDTH - HORIZONTAL_PADDING;
            left = right - iconsWidth;
        }

        Drawable d = context.getResources().getDrawable(R.drawable.cc_by_sa_gray);
        d.setBounds(left, top, right, bottom);
        d.draw(canvas);
    }

    private void drawWordmark(Canvas canvas, Context context) {
        final int top = 293;
        final float fontSize = 24.0f;
        final int maxWidth = WIDTH - DESCRIPTION_WIDTH - 2 * HORIZONTAL_PADDING;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(SERIF);

        Spanned wikipedia = Html.fromHtml(context.getString(R.string.wp_stylized));
        StaticLayout wordmarkLayout = buildLayout(
                new TextLayoutParams(wikipedia, textPaint, maxWidth, 1.0f));
        final int wordMarkWidth = (int) wordmarkLayout.getLineWidth(0);

        // (R) as part of wordmark -- add registered trademark symbol
        final float rFontSize = 12.0f;
        final int rMaxWidth = (int) rFontSize;
        TextPaint rPaint = new TextPaint(textPaint);
        rPaint.setTextSize(rFontSize);
        StaticLayout rLayout = buildLayout(new TextLayoutParams("\u24C7", rPaint, rMaxWidth, 1.0f));
        final int rWidth = (int) rLayout.getLineWidth(0);

        int left = WIDTH - HORIZONTAL_PADDING - wordMarkWidth - rWidth;
        if (isArticleRTL) {
            left = HORIZONTAL_PADDING;
        }

        canvas.save(); // --
        // Combined wordmark and registered trademark drawing
        // Note that internally, the directionality is based on the wordmark string
        // and not the article language.
        if (wordmarkLayout.getParagraphDirection(0) != Layout.DIR_RIGHT_TO_LEFT) {
            // LTR for wordmark string resource
            final int rYOffset = 4;
            // draw wordmark
            canvas.translate(left, top);
            wordmarkLayout.draw(canvas);
            // draw (R)
            canvas.translate(wordMarkWidth, rYOffset);
            rLayout.draw(canvas);
        } else {
            // RTL for wordmark string resource
            // draw (R)
            canvas.translate(left, top);
            rLayout.draw(canvas);
            // draw wordmark
            canvas.translate(rWidth - maxWidth + wordMarkWidth, 0);
            wordmarkLayout.draw(canvas);
        }
        canvas.restore(); // --
    }

    /**
     * If the title or text is too long we first reduce the font size.
     * If that is not enough it gets ellipsized.
     */
    private StaticLayout optimizeTextSize(TextLayoutParams params, int maxHeight, int maxLines,
                                          float maxFontSize, float minFontSize) {
        final float threshold1 = 60.0f;
        final float threshold2 = 40.0f;
        final float extraStep1 = 3.0f;
        final float extraStep2 = 1.0f;
        boolean fits = false;
        StaticLayout textLayout = null;

        // Try decreasing font size first
        for (float fontSize = maxFontSize; fontSize >= minFontSize; fontSize -= 1.0f) {
            params.textPaint.setTextSize(fontSize);
            textLayout = buildLayout(params);
            if (textLayout.getHeight() <= maxHeight) {
                fits = true;
                break;
            }

            // make it go faster at the beginning...
            if (fontSize > threshold1) {
                fontSize -= extraStep1;
            } else if (fontSize > threshold2) {
                fontSize -= extraStep2;
            }
        }

        // Then do own ellipsize: cut text off after last fitting space and add "..."
        // Didn't want to cut off randomly in the middle of a line or word.
        if (!fits) {
            final String textStr = params.text.toString();
            final int ellipsisLength = 3;
            final int ellipsisStart = textLayout != null
                    ? textLayout.getLineStart(maxLines) - ellipsisLength
                    : textStr.length();
            final int end = textStr.lastIndexOf(' ', ellipsisStart) + 1;
            if (end > 0) {
                textLayout = buildLayout(
                        new TextLayoutParams(params, textStr.substring(0, end) + "..."));
                if (textLayout.getLineCount() <= maxLines) {
                    fits = true;
                }
            }
        }

        // last resort: use TextUtils.ellipsize()
        if (!fits) {
            final float textRatio = .87f;
            final float maxWidth = textRatio * maxLines * params.lineWidth;
            textLayout = buildLayout(new TextLayoutParams(params,
                    TextUtils.ellipsize(params.text, params.textPaint, maxWidth,
                            TextUtils.TruncateAt.END)));
        }

        return textLayout;
    }

    private StaticLayout buildLayout(TextLayoutParams params) {
        return new StaticLayout(
                params.text,
                params.textPaint,
                params.lineWidth,
                Layout.Alignment.ALIGN_NORMAL,
                params.spacingMultiplier,
                0.0f,
                false);
    }

    // Borrowed from http://stackoverflow.com/questions/5226922/crop-to-fit-image-in-android
    // Modified to allow for face detection adjustment, startY
    private Bitmap scaleCropToFitFace(Bitmap original, int targetWidth, int targetHeight,
                                      int yOffset) {
        // Need to scale the image, keeping the aspect ratio first
        int width = original.getWidth();
        int height = original.getHeight();

        float widthScale = (float) targetWidth / (float) width;
        float heightScale = (float) targetHeight / (float) height;
        float scaledWidth;
        float scaledHeight;

        int startX = 0;
        int startY = 0;

        if (widthScale > heightScale) {
            scaledWidth = targetWidth;
            scaledHeight = height * widthScale;
            // crop height by...
//            startY = (int) ((scaledHeight - targetHeight) / 2);
        } else {
            scaledHeight = targetHeight;
            scaledWidth = width * heightScale;
            // crop width by..
//            startX = (int) ((scaledWidth - targetWidth) / 2);
        }

        Bitmap scaledBitmap
                = Bitmap.createScaledBitmap(original, (int) scaledWidth, (int) scaledHeight, true);
        Bitmap bitmap = Bitmap.createBitmap(scaledBitmap, startX, startY, targetWidth, targetHeight);
        scaledBitmap.recycle();
        return bitmap;
    }

    /**
     * Parameter object for #buildLayout and #optimizeTextSize.
     */
    private static class TextLayoutParams {
        private final CharSequence text;
        private final TextPaint textPaint;
        private final int lineWidth;
        private final float spacingMultiplier;

        private TextLayoutParams(CharSequence text, TextPaint textPaint, int lineWidth,
                                 float spacingMultiplier) {
            this.text = text;
            this.textPaint = textPaint;
            this.lineWidth = lineWidth;
            this.spacingMultiplier = spacingMultiplier;
        }

        public TextLayoutParams(TextLayoutParams other, CharSequence text) {
            this.text = text;
            this.textPaint = other.textPaint;
            this.lineWidth = other.lineWidth;
            this.spacingMultiplier = other.spacingMultiplier;
        }
    }
}
