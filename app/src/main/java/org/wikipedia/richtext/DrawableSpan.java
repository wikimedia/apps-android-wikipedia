package org.wikipedia.richtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ImageSpan;

/** A more mutable ImageSpan better suited to Drawables. */
public class DrawableSpan extends ImageSpan {
    @Nullable
    private Drawable drawable;

    public DrawableSpan(@NonNull Context context, Bitmap bitmap) {
        super(context, bitmap);
        init();
    }

    public DrawableSpan(@NonNull Context context, Bitmap bitmap, int verticalAlignment) {
        super(context, bitmap, verticalAlignment);
        init();
    }

    public DrawableSpan(Drawable drawable) {
        super(drawable);
        init();
    }

    public DrawableSpan(Drawable drawable, int verticalAlignment) {
        super(drawable, verticalAlignment);
        init();
    }

    public DrawableSpan(Drawable drawable, String source) {
        super(drawable, source);
        init();
    }

    public DrawableSpan(Drawable drawable, String source, int verticalAlignment) {
        super(drawable, source, verticalAlignment);
        init();
    }

    public DrawableSpan(@NonNull Context context, Uri uri) {
        super(context, uri);
        init();
    }

    public DrawableSpan(@NonNull Context context, Uri uri, int verticalAlignment) {
        super(context, uri, verticalAlignment);
        init();
    }

    public DrawableSpan(@NonNull Context context, @DrawableRes int resourceId) {
        super(context, resourceId);
        init();
    }

    public DrawableSpan(@NonNull Context context, @DrawableRes int resourceId, int verticalAlignment) {
        super(context, resourceId, verticalAlignment);
        init();
    }

    @Override
    @Nullable
    public Drawable getDrawable() {
        return drawable;
    }

    // Make vertical alignment consistent across APIs. See https://code.google.com/p/android/issues/detail?id=21397
    @Override
    @SuppressWarnings("checkstyle:parameternumber")
    public void draw(Canvas canvas,
                     CharSequence text,
                     int start,
                     int end,
                     float x,
                     int top,
                     int y,
                     int bottom,
                     Paint paint) {
        if (drawable == null) {
            return;
        }

        canvas.save();

        canvas.translate(x, drawY(y, bottom));
        drawable.draw(canvas);
        canvas.restore();
    }

    public void setDrawable(@Nullable Drawable drawable) {
        this.drawable = drawable;
    }

    public void setIntrinsicBounds() {
        if (drawable != null && drawable.getBounds().isEmpty()) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
    }

    protected int drawY(int y, int bottom) {
        int ret;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            ret = y;
        } else {
            ret = bottom;
        }
        ret -= drawable == null ? 0 : drawable.getBounds().bottom;
        return ret;
    }

    private void init() {
        // super.getDrawable() is convoluted:
        // * May return an original or a new Drawable; does not keep a reference in the latter
        //   case.
        // * May set the bounds or not.
        // * May set the bounds differently.
        //
        // This is the only seam to change the Drawable state.
        drawable = super.getDrawable();

        setIntrinsicBounds();
    }
}
