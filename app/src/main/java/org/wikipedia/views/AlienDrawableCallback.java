package org.wikipedia.views;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * A {@link Drawable.Callback} for any {@link Drawable} on any {@link View}.
 * {@link View#verifyDrawable} does not permit callbacks on unknown {@link Drawable}s so this
 * wrapper invokes the {@link View}'s {@link Handler} directly when available, or its own handler
 * otherwise.
 */
public class AlienDrawableCallback implements Drawable.Callback {
    @NonNull
    private final Handler handler;

    @NonNull
    private final View view;

    public AlienDrawableCallback(@NonNull View view) {
        handler = view.getHandler() == null ? new Handler() : view.getHandler();
        this.view = view;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        // view.invalidate(who.getDirtyBounds()) would be more efficient but it doesn't seem to
        // be practical to obtain the relative coordinates of the Drawable.
        view.invalidate();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        handler.postAtTime(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        handler.removeCallbacks(what);
    }
}
