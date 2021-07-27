package org.wikipedia.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import org.wikipedia.WikipediaApp

object ViewAnimations {
    private val SHORT_ANIMATION_DURATION = WikipediaApp.instance.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    private val MEDIUM_ANIMATION_DURATION = WikipediaApp.instance.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

    @JvmStatic
    fun crossFade(curView: View, newView: View) {
        fadeIn(newView)
        fadeOut(curView)
    }

    @JvmStatic
    fun crossFade(curView: View, newView: View, runOnComplete: Runnable?) {
        fadeIn(newView)
        fadeOut(curView, runOnComplete)
    }

    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
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
