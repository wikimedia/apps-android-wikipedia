package org.wikimedia.wikipedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

/**
 * Contains utility methods that Java doesn't have because we can't make code look too good, can we?
 */
public class Utils {
    /**
     * Compares two strings properly, even when one of them is null - without throwing up
     *
     * @param str1 The first string
     * @param str2 Guess?
     * @return true if they are both equal (even if both are null)
     */
    public static boolean compareStrings(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     */
    public static void crossFade(final View curView, final View newView) {
        newView.setAlpha(0f);
        newView.setVisibility(View.VISIBLE);
        newView.animate()
                .alpha(1.0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(null);

        curView.animate()
                .alpha(0f)
                .setDuration(WikipediaApp.MEDIUM_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        curView.setVisibility(View.GONE);
                    }
                });
    }
}
