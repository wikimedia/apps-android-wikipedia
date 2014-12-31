package org.wikipedia.page.snippet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import org.wikipedia.R;

/**
 * Creator of Bitmap objects that include an optional lead image, a title, and text.
 */
public final class SnippetImage {
    private SnippetImage() {
    }

    /**
     * Initially we create a a fixed size Bitmap that is a bit taller than what we need.
     * We manually keep track of the vertical space (y) we draw on.
     * Once we're done drawing we adjust the height of the bitmap.
     *
     * If we have a lead image, it goes on top, followed by the title, an opening double quote sign,
     * then the text, and lastly the Wikipedia wordmark.
     *
     * The lead image portion gets clipped and scaled to fit the width of the screen. It's reusing
     * the faceYOffset for the face detection adjustment and the Bitmap from LeadImageHandler.
     */
    public static Bitmap createImage(Context context, Bitmap leadImageBitmap, int faceYOffset,
                                     String title, CharSequence textSnippet) {
        final int width = 560;
        final int initialHeight = 2000; // will be cut down later
        final int maxImageHeight = 330;
        final int horizontalPadding = 24;
        final int textIndent = 52;
        final int belowTitle = 8; // when title inside image: bottom padding
        final int aboveTitle = 32; // when title under image: top padding
        final int aboveQuotationMark = 16; // between bottom of title or image and top of quotation mark
        final int aboveText = 40; // between top of quotation mark and top of text
        final int aboveWordmark = 40; // between end of text and top of wordmark
        final int maxTitleLines = 3;
        final int maxTextLines = 10;

        final int titleLineWidth = width - 2 * horizontalPadding;
        final int textLineWidth = titleLineWidth - textIndent;

        final float titleFontSize = 40.0f;
        final float minTitleFontSize = 32.0f;
        final float textFontSize = 32.0f;
        final float minTextFontSize = 24.0f;
        final float wordmarkFontSize = 32.0f;
        final float quoteFontSize = 96.0f;
        final float titleSpacingMultiplier = 0.8f;
        final float spacingMultiplier = 1.4f;
        final Typeface serif = Typeface.create("serif", Typeface.NORMAL);

        Bitmap resultBitmap = Bitmap.createBitmap(width, initialHeight, Bitmap.Config.ARGB_8888);
        // final int backgroundColor = Color.parseColor("#242438");
        final int backgroundColor = -14408648;
        resultBitmap.eraseColor(backgroundColor);

        int y = 0, dy = 0;

        // prepare title layout
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(context.getResources().getColor(R.color.lead_text_color));
        textPaint.setTextSize(titleFontSize);
        textPaint.setTypeface(serif);
        // and give it a nice drop shadow!
        textPaint.setShadowLayer(2, 1, 1, context.getResources().getColor(R.color.lead_text_shadow));

        StaticLayout textLayout = optimizeTextSize(title,
                titleLineWidth, maxTitleLines, textPaint,
                titleSpacingMultiplier, titleFontSize, minTitleFontSize);

        Canvas canvas = new Canvas(resultBitmap);
        if (leadImageBitmap != null) {
            // draw lead image
            Bitmap tmpLeadImageBitmap
                    = scaleCropToFitFace(leadImageBitmap, width, maxImageHeight, faceYOffset);

            y = tmpLeadImageBitmap.getHeight();
            canvas.drawBitmap(tmpLeadImageBitmap,
                    null,
                    new Rect(0, 0, tmpLeadImageBitmap.getWidth(), tmpLeadImageBitmap.getHeight()),
                    null);

            tmpLeadImageBitmap.recycle();

            dy = tmpLeadImageBitmap.getHeight() - textLayout.getHeight() - belowTitle;
            canvas.translate(horizontalPadding, dy);

            // draw title inside lead image
            textLayout.draw(canvas);

            dy = textLayout.getHeight() + aboveQuotationMark;
            canvas.translate(0, dy);
        } else {
            dy += aboveTitle;
            canvas.translate(horizontalPadding, dy);
            y += dy;

            // draw title under lead image
            textLayout.draw(canvas);

            dy = textLayout.getHeight() + aboveQuotationMark;
            canvas.translate(0, dy);
            y += dy;
        }

        // draw quotation mark
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(quoteFontSize);
        textPaint.setTypeface(serif);

        textLayout = buildLayout("\u201C", textPaint, textIndent, spacingMultiplier);
        textLayout.draw(canvas);

        dy = aboveText;
        canvas.translate(textIndent, dy);
        y += dy;

        // draw textSnippet
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textFontSize);
        textPaint.setStyle(Paint.Style.FILL);

        textLayout = optimizeTextSize(textSnippet,
                textLineWidth, maxTextLines, textPaint,
                spacingMultiplier, textFontSize, minTextFontSize);
        textLayout.draw(canvas);

        dy = textLayout.getHeight() + aboveWordmark;
        canvas.translate(-textIndent, dy);
        y += dy;

        // draw wordmark
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(wordmarkFontSize);
        textPaint.setTypeface(serif);

        textLayout = buildLayout(Html.fromHtml(context.getString(R.string.wp_stylized)),
                textPaint, width, 1.0f);
        textLayout.draw(canvas);

        dy = textLayout.getHeight() + horizontalPadding;
        y += dy;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            resultBitmap.setHeight(y);
        } else {
            resultBitmap = setImageHeight(resultBitmap, y);
        }

        return resultBitmap;
    }

    private static Bitmap setImageHeight(Bitmap bmp, int y) {
        Bitmap croppedImage = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), y);
        bmp.recycle();
        return croppedImage;
    }

    /**
     * If the title or text is too long we first reduce the font size.
     * If that is not enough it gets ellipsized.
     */
    private static StaticLayout optimizeTextSize(CharSequence text, int lineWidth, int maxLines,
                                                 TextPaint textPaint, float spacingMultiplier,
                                                 float maxFontSize, float minFontSize) {
        boolean fits = false;
        StaticLayout textLayout = null;

        // Try decreasing font size first
        for (float fontSize = maxFontSize; fontSize >= minFontSize; fontSize -= 2.0f) {
            textPaint.setTextSize(fontSize);
            textLayout = buildLayout(text, textPaint, lineWidth, spacingMultiplier);
            if (textLayout.getLineCount() <= maxLines) {
                fits = true;
                break;
            }
        }

        // Then do own ellipsize: cut text off after last fitting space and add "..."
        // Didn't want to cut off randomly in the middle of a line or word.
        if (!fits) {
            final String textStr = text.toString();
            final int ellipsisLength = 3;
            final int ellipsisStart = textLayout != null
                    ? textLayout.getLineStart(maxLines) - ellipsisLength
                    : textStr.length();
            final int end = textStr.lastIndexOf(' ', ellipsisStart) + 1;
            if (end > 0) {
                textLayout = buildLayout(textStr.substring(0, end) + "...",
                        textPaint, lineWidth, spacingMultiplier);
                if (textLayout.getLineCount() <= maxLines) {
                    fits = true;
                }
            }
        }

        // last resort: use TextUtils.ellipsize()
        if (!fits) {
            final float textRatio = .87f;
            float maxWidth = textRatio * maxLines * lineWidth;
            text = TextUtils.ellipsize(text, textPaint, maxWidth, TextUtils.TruncateAt.END);
            textLayout = buildLayout(text, textPaint, lineWidth, spacingMultiplier);
        }

        return textLayout;
    }

    private static StaticLayout buildLayout(CharSequence text, TextPaint textPaint, int lineWidth,
                                            float spacingMultiplier) {
        StaticLayout textLayout;
        textLayout = new StaticLayout(
                text,
                textPaint,
                lineWidth,
                Layout.Alignment.ALIGN_NORMAL,
                spacingMultiplier,
                0.0f,
                false);
        return textLayout;
    }

    // Borrowed from http://stackoverflow.com/questions/5226922/crop-to-fit-image-in-android
    // Modified to allow for face detection adjustment, startY
    private static Bitmap scaleCropToFitFace(Bitmap original, int targetWidth, int targetHeight,
                                             int startY) {
        // Need to scale the image, keeping the aspect ratio first
        int width = original.getWidth();
        int height = original.getHeight();

        float widthScale = (float) targetWidth / (float) width;
        float heightScale = (float) targetHeight / (float) height;
        float scaledWidth;
        float scaledHeight;

        int startX = 0;

        if (widthScale > heightScale) {
            scaledWidth = targetWidth;
            scaledHeight = height * widthScale;
            // crop height by...
            // not needed here since we already have startY passed in (=face detection adjustment)
            // startY = (int) ((scaledHeight - targetHeight) / 2);

            final int minY = 8;
            startY = (int) (startY * heightScale);
            if (startY < minY) {
                startY = minY;
            }
        } else {
            scaledHeight = targetHeight;
            scaledWidth = width * heightScale;
            // crop width by..
            startX = (int) ((scaledWidth - targetWidth) / 2);
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, (int) scaledWidth, (int) scaledHeight, true);
        Bitmap bitmap = Bitmap.createBitmap(scaledBitmap, startX, startY, targetWidth, targetHeight);
        scaledBitmap.recycle();
        return bitmap;
    }
}
