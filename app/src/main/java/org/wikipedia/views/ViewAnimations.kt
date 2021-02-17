package org.wikipedia.views

import android.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Resources
import android.view.View

/**
 * Contains convenient methods for performing various animations on Views.
 */
object ViewAnimations {
    private var SHORT_ANIMATION_DURATION: Long = 0
    private var MEDIUM_ANIMATION_DURATION: Long = 0
    fun init(resources: Resources) {
        SHORT_ANIMATION_DURATION = resources.getInteger(R.integer.config_shortAnimTime).toLong()
        MEDIUM_ANIMATION_DURATION = resources.getInteger(R.integer.config_mediumAnimTime).toLong()
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     */
    fun crossFade(curView: View, newView: View) {
        fadeIn(newView)
        fadeOut(curView)
    }

    /**
     * Crossfades two views, one of which is assumed to be currently visible
     * @param curView The view that is currently visible
     * @param newView The new view that should be faded in
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    fun crossFade(curView: View, newView: View, runOnComplete: Runnable?) {
        fadeIn(newView)
        fadeOut(curView, runOnComplete)
    }
    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    /**
     * Fades in a view.
     * @param view The currently invisible view to be faded in
     */
    @JvmOverloads
    fun fadeIn(view: View, runOnComplete: Runnable? = null) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
                .alpha(1f)
                .setDuration(MEDIUM_ANIMATION_DURATION)
                .setListener(object : CancelStateAnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!canceled()) {
                            runOnComplete?.run()
                        }
                    }
                })
                .start()
    }
    /**
     * Fades out a view.
     * @param view The currently visible view to be faded out
     * @param runOnComplete Optional Runnable to be run when the animation is complete (may be null).
     */
    /**
     * Fades out a view.
     * @param view The currently visible view to be faded out
     */
    @JvmOverloads
    fun fadeOut(view: View, runOnComplete: Runnable? = null) {
        view.animate().cancel()
        view.animate()
                .alpha(0f)
                .setDuration(MEDIUM_ANIMATION_DURATION)
                .setListener(object : CancelStateAnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!canceled()) {
                            // Detect if we got canceled, and if so DON'T hide...
                            // There's another animation now pushing the alpha back up
                            view.visibility = View.GONE
                            view.alpha = 1f
                            runOnComplete?.run()
                        }
                    }
                })
                .start()
    }

    /**
     * Ensures that the translationY of a particular view is the given value.
     *
     * If it isn't the current value, then it performs a short animation to make it so.
     *
     * @param view The view to translate
     * @param translation The value to ensure it is translated by
     */
    fun ensureTranslationY(view: View, translation: Int) {
        if (view.translationY != translation.toFloat()) {
            view.animate().translationY(translation.toFloat()).setDuration(SHORT_ANIMATION_DURATION).start()
        }
    }

    private open class CancelStateAnimatorListenerAdapter : AnimatorListenerAdapter() {
        private var canceled = false
        override fun onAnimationCancel(animation: Animator) {
            canceled = true
        }

        protected fun canceled(): Boolean {
            return canceled
        }
    }
}