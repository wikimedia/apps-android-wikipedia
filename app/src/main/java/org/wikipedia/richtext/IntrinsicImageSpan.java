package org.wikipedia.richtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ImageSpan;

public class IntrinsicImageSpan extends ImageSpan {
    @Nullable private Drawable drawable;

    public IntrinsicImageSpan(@NonNull Context context, Bitmap bitmap) {
        super(context, bitmap);
        init();
    }

    public IntrinsicImageSpan(@NonNull Context context, Bitmap bitmap, int verticalAlignment) {
        super(context, bitmap, verticalAlignment);
        init();
    }

    public IntrinsicImageSpan(Drawable d) {
        super(d);
        init();
    }

    public IntrinsicImageSpan(Drawable d, int verticalAlignment) {
        super(d, verticalAlignment);
        init();
    }

    public IntrinsicImageSpan(Drawable d, String source) {
        super(d, source);
        init();
    }

    public IntrinsicImageSpan(Drawable d, String source, int verticalAlignment) {
        super(d, source, verticalAlignment);
        init();
    }

    public IntrinsicImageSpan(@NonNull Context context, Uri uri) {
        super(context, uri);
        init();
    }

    public IntrinsicImageSpan(@NonNull Context context, Uri uri, int verticalAlignment) {
        super(context, uri, verticalAlignment);
        init();
    }

    public IntrinsicImageSpan(@NonNull Context context, @DrawableRes int resourceId) {
        super(context, resourceId);
        init();
    }

    public IntrinsicImageSpan(@NonNull Context context, @DrawableRes int resourceId, int verticalAlignment) {
        super(context, resourceId, verticalAlignment);
        init();
    }

    @Override @Nullable public Drawable getDrawable() {
        return drawable;
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

        setIntrinsicSize();
    }

    private void setIntrinsicSize() {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
    }
}