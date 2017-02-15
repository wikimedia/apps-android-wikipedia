package org.wikipedia.richtext;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.views.AlienDrawableCallback;

public class AnimatedImageSpan extends DrawableSpan {
    private Drawable.Callback animateCallback;

    public AnimatedImageSpan(@NonNull View view, Bitmap bitmap) {
        super(view.getContext(), bitmap);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Bitmap bitmap, int verticalAlignment) {
        super(view.getContext(), bitmap, verticalAlignment);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Drawable drawable) {
        super(drawable);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Drawable drawable, int verticalAlignment) {
        super(drawable, verticalAlignment);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Drawable drawable, String source) {
        super(drawable, source);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Drawable drawable, String source, int verticalAlignment) {
        super(drawable, source, verticalAlignment);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Uri uri) {
        super(view.getContext(), uri);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, Uri uri, int verticalAlignment) {
        super(view.getContext(), uri, verticalAlignment);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, @DrawableRes int resourceId) {
        super(view.getContext(), resourceId);
        init(view);
    }

    public AnimatedImageSpan(@NonNull View view, @DrawableRes int resourceId, int verticalAlignment) {
        super(view.getContext(), resourceId, verticalAlignment);
        init(view);
    }

    public void start() {
        AnimationDrawable drawable = getAnimationDrawable();
        if (drawable != null) {
            drawable.start();
        }
    }

    public void stop() {
        AnimationDrawable drawable = getAnimationDrawable();
        if (drawable != null) {
            drawable.stop();
        }
    }

    public void toggle() {
        if (isRunning()) {
            stop();
        } else {
            start();
        }
    }

    public boolean isRunning() {
        AnimationDrawable drawable = getAnimationDrawable();
        return drawable != null && drawable.isRunning();
    }

    @Override
    public void setDrawable(@Nullable Drawable drawable) {
        clearCallback(drawable);
        super.setDrawable(drawable);
        setCallback(drawable);
    }

    @Nullable
    protected AnimationDrawable getAnimationDrawable() {
        return getDrawable() instanceof AnimationDrawable
                ? (AnimationDrawable) getDrawable()
                : null;
    }

    private void init(@NonNull View view) {
        // Drawable.setCallback() keeps a weak reference so hold a strong reference here.
        animateCallback = new AlienDrawableCallback(view);
        setCallback(getDrawable());
    }

    private void setCallback(@Nullable Drawable drawable) {
        if (drawable != null) {
            drawable.setCallback(animateCallback);
        }
    }

    private void clearCallback(@Nullable Drawable drawable) {
        if (drawable != null) {
            stop();
            drawable.setCallback(null);
        }
    }
}
