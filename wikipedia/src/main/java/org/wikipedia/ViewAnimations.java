package org.wikipedia;

import android.view.View;
import com.nineoldandroids.animation.*;
import com.nineoldandroids.view.*;

import static com.nineoldandroids.view.ViewPropertyAnimator.*;

/**
 * Contains convenient methods for performing various animations on Views.
 */
public final class ViewAnimations {

    private ViewAnimations() { }

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
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     */
    public static void fadeIn(final View view) {
        ViewHelper.setAlpha(view, 0f);
        view.setVisibility(View.VISIBLE);
        animate(view)
                .alpha(1.0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(null)
                .start();
    }

    /**
     * Fades out a view.
     * @param view The currently visible view to be faded out
     */
    public static void fadeOut(final View view) {
        animate(view)
                .alpha(0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean wasCanceled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        wasCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!wasCanceled) {
                            // Detect if we got canceled, and if so DON'T hide...
                            // There's another animation now pushing the alpha back up
                            view.setVisibility(View.GONE);
                            ViewHelper.setAlpha(view, 1.0f);
                        }
                    }
                });
    }

    /**
     * Slides a view back in (after being slid to the left or right), and makes it VISIBLE again.
     * @param view The view to slide in
     * @param listener Listener for receiving animation events (may be null)
     */
    public static void slideIn(final View view, AnimatorListenerAdapter listener) {
        animate(view)
                .translationX(0)
                .setDuration(WikipediaApp.SHORT_ANIMATION_DURATION)
                .setListener(listener != null ? listener : new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
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
    public static void slideOutLeft(final View view, AnimatorListenerAdapter listener) {
        animate(view)
                .translationX(-view.getWidth())
                .setDuration(WikipediaApp.SHORT_ANIMATION_DURATION)
                .setListener(listener != null ? listener : new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
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
    public static void slideOutRight(final View view, AnimatorListenerAdapter listener) {
        animate(view)
                .translationX(view.getWidth())
                .setDuration(WikipediaApp.SHORT_ANIMATION_DURATION)
                .setListener(listener != null ? listener : new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
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
        if (ViewHelper.getTranslationY(view) != translation) {
            animate(view).translationY(translation).setDuration(WikipediaApp.SHORT_ANIMATION_DURATION).start();
        }
    }

}
