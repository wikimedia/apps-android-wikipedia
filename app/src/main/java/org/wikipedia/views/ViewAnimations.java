package org.wikipedia.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Contains convenient methods for performing various animations on Views.
 */
public final class ViewAnimations {
    private static long SHORT_ANIMATION_DURATION;
    private static long MEDIUM_ANIMATION_DURATION;

    private ViewAnimations() { }

    public static void init(Resources resources) {
        SHORT_ANIMATION_DURATION = resources.getInteger(android.R.integer.config_shortAnimTime);
        MEDIUM_ANIMATION_DURATION = resources.getInteger(android.R.integer.config_mediumAnimTime);
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     */
    public static void crossFade(View curView, View newView) {
        fadeIn(newView);
        fadeOut(curView);
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    public static void crossFade(View curView, View newView, Runnable runOnComplete) {
        fadeIn(newView);
        fadeOut(curView, runOnComplete);
    }

    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     */
    public static void fadeIn(View view) {
        fadeIn(view, null);
    }

    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    public static void fadeIn(View view, @Nullable final Runnable runOnComplete) {
        view.setAlpha(0);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1)
                .setDuration(MEDIUM_ANIMATION_DURATION)
                .setListener(new CancelStateAnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        if (!canceled()) {
                            if (runOnComplete != null) {
                                runOnComplete.run();
                            }
                        }
                    }
                })
                .start();
    }

    /**
     * Fades out a view.
     * @param view The currently visible view to be faded out
     */
    public static void fadeOut(final View view) {
        fadeOut(view, null);
    }

    /**
     * Fades out a view.
     * @param view The currently visible view to be faded out
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    public static void fadeOut(final View view, final Runnable runOnComplete) {
        view.animate().cancel();
        view.animate()
                .alpha(0)
                .setDuration(MEDIUM_ANIMATION_DURATION)
                .setListener(new CancelStateAnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        if (!canceled()) {
                            // Detect if we got canceled, and if so DON'T hide...
                            // There's another animation now pushing the alpha back up
                            view.setVisibility(View.GONE);
                            view.setAlpha(1);
                            if (runOnComplete != null) {
                                runOnComplete.run();
                            }
                        }
                    }
                })
                .start();
    }

    /**
     * Ensures that the translationY of a particular view is the given value.
     *
     * If it isn't the current value, then it performs a short animation to make it so.
     *
     * @param view The view to translate
     * @param translation The value to ensure it is translated by
     */
    public static void ensureTranslationY(View view, int translation) {
        if (view.getTranslationY() != translation) {
            view.animate().translationY(translation).setDuration(SHORT_ANIMATION_DURATION).start();
        }
    }

    private static class CancelStateAnimatorListenerAdapter extends AnimatorListenerAdapter {
        private boolean canceled;

        @Override public void onAnimationCancel(Animator animation) {
            canceled = true;
        }

        protected boolean canceled() {
            return canceled;
        }
    }
}
