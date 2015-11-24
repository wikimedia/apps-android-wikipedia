package org.wikipedia.richtext;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.widget.TextView;

public class DefaultClickSpan implements ClickSpan {
    // The click collision area relative the host View with origin at centerpoint.
    @NonNull
    private final RectF bounds = new RectF();

    /** @param x Icon origin x coordinate relative the host View. */
    public void setOriginX(float x) {
        setOrigin(x, bounds.centerY());
    }

    /** @param y Icon origin y coordinate relative the host View. */
    public void setOriginY(float y) {
        setOrigin(bounds.centerX(), y);
    }

    public void setOrigin(float x, float y) {
        bounds.offset(x - bounds.centerX(), y - bounds.centerY());
    }

    public void setWidth(float width) {
        setRect(width, bounds.height());
    }

    public void setHeight(float height) {
        setRect(bounds.width(), height);
    }

    public void setRect(float width, float height) {
        float x = bounds.centerX();
        float y = bounds.centerY();

        float a = width / 2;
        float b = height / 2;
        bounds.set(-a, -b, a, b);

        setOrigin(x, y);
    }

    public void draw(Canvas canvas) {
        Paint debugPaint = new Paint();
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(Color.RED);

        canvas.drawRect(bounds, debugPaint);
    }

    @Override
    public void onClick(@NonNull TextView textView) {
    }

    @Override
    public boolean contains(@NonNull PointF point) {
        return bounds.contains(point.x, point.y);
    }
}