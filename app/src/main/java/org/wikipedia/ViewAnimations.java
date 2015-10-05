package org.wikipedia;

import android.content.res.Resources;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.view.View;

import static android.support.v4.view.ViewCompat.animate;

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
    public static void crossFade(final View curView, final View newView) {
        fadeIn(newView);
        fadeOut(curView);
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    public static void crossFade(final View curView, final View newView, final Runnable runOnComplete) {
        fadeIn(newView);
        fadeOut(curView, runOnComplete);
    }

    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     */
    public static void fadeIn(final View view) {
        fadeIn(view, null);
    }

    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    public static void fadeIn(View view, final Runnable runOnComplete) {
        ViewCompat.setAlpha(view, 0f);
        view.setVisibility(View.VISIBLE);
        animate(view)
                .alpha(1.0f)
                .setDuration(MEDIUM_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    private boolean wasCanceled = false;

                    @Override
                    public void onAnimationCancel(View view) {
                        wasCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        if (!wasCanceled) {
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
        animate(view)
                .alpha(0f)
                .setDuration(MEDIUM_ANIMATION_DURATION)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    private boolean wasCanceled = false;

                    @Override
                    public void onAnimationCancel(View view) {
                        wasCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        if (!wasCanceled) {
                            // Detect if we got canceled, and if so DON'T hide...
                            // There's another animation now pushing the alpha back up
                            view.setVisibility(View.GONE);
                            ViewCompat.setAlpha(view, 1.0f);
                            if (runOnComplete != null) {
                                runOnComplete.run();
                            }
                        }
                    }
                })
                .start();
    }

    /**
     * Slides a view back in (after being slid to the left or right), and makes it VISIBLE again.
     * @param view The view to slide in
     * @param listener Listener for receiving animation events (may be null)
     */
    public static void slideIn(View view, ViewPropertyAnimatorListener listener) {
        animate(view)
                .translationX(0)
                .setDuration(SHORT_ANIMATION_DURATION)
                .setListener(listener != null ? listener : new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(View view) {
                        view.setVisibility(View.VISIBLE);
                    }
                })
                .start();
    }

    /**
     * Slides a view out to the left (and makes it GONE).
     * @param view The view to slide out
     * @param listener Listener for receiving animation events (may be null)
     */
    public static void slideOutLeft(View view, ViewPropertyAnimatorListener listener) {
        animate(view)
                .translationX(-view.getWidth())
                .setDuration(SHORT_ANIMATION_DURATION)
                .setListener(listener != null ? listener : new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(View view) {
                        view.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    /**
     * Slides a view out to the right (and makes it GONE).
     * @param view The view to slide out
     * @param listener Listener for receiving animation events (may be null)
     */
    public static void slideOutRight(View view, ViewPropertyAnimatorListener listener) {
        animate(view)
                .translationX(view.getWidth())
                .setDuration(SHORT_ANIMATION_DURATION)
                .setListener(listener != null ? listener : new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(View view) {
                        view.setVisibility(View.GONE);
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
            animate(view).translationY(translation).setDuration(SHORT_ANIMATION_DURATION).start();
        }
    }

    /**
     * Ensures that the translationX of a particular view is the given value.
     *
     * If it isn't the current value, then it performs a short animation to make it so.
     *
     * @param view The view to translate
     * @param translation The value to ensure it is translated by
     */
    public static void ensureTranslationX(View view, int translation) {
        if (view.getTranslationX() != translation) {
            animate(view).translationX(translation).setDuration(SHORT_ANIMATION_DURATION).start();
        }
    }
}
