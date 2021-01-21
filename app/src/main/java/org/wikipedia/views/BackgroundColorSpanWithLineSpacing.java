package org.wikipedia.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class BackgroundColorSpanWithLineSpacing extends ReplacementSpan {

    private int backgroundColor = 0;
    private int textColor = 0;
    private float lineSpacingAdded = 0;


    public BackgroundColorSpanWithLineSpacing(int backgroundColor, int textColor, float lineSpacingAdded) {
        super();
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.lineSpacingAdded = lineSpacingAdded;
    }

    @Override
    @SuppressWarnings("checkstyle:parameternumber")
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        float newBottom = bottom - lineSpacingAdded;
        RectF rect = new RectF(x, top, x + measureText(paint, text, start, end), newBottom);
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(rect, 0, 0, paint);
        paint.setColor(textColor);
        canvas.drawText(text, start, end, x, y, paint);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}
