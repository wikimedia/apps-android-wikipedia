package org.wikipedia.page

import android.view.Gravity
import android.view.View
import org.wikipedia.R
import org.wikipedia.util.DimenUtil.dpToPx
import org.wikipedia.util.DimenUtil.getDimension
import org.wikipedia.views.ObservableWebView
import org.wikipedia.views.ObservableWebView.OnDownMotionEventListener
import org.wikipedia.views.ObservableWebView.OnUpOrCancelMotionEventListener
import org.wikipedia.views.ViewAnimations.ensureTranslationY

class ViewHideHandler(private val hideableView: View,
                      private val anchoredView: View?,
                      private val gravity: Int,
                      private val updateElevation: Boolean = true) :
        ObservableWebView.OnScrollChangeListener, OnUpOrCancelMotionEventListener, OnDownMotionEventListener, ObservableWebView.OnClickListener {

    private var webView: ObservableWebView? = null
    var enabled = true
        set(value) {
            field = value
            if (!enabled) {
                ensureDisplayed()
            }
        }

    fun setScrollView(webView: ObservableWebView?) {
        this.webView = webView
        webView?.let {
            it.addOnScrollChangeListener(this)
            it.addOnDownMotionEventListener(this)
            it.addOnUpOrCancelMotionEventListener(this)
            it.addOnClickListener(this)
        }
    }

    override fun onScrollChanged(oldScrollY: Int, scrollY: Int, isHumanScroll: Boolean) {
        if (webView == null || !enabled) {
            return
        }
        var animMargin = 0
        val scrollDelta = scrollY - oldScrollY
        if (gravity == Gravity.BOTTOM) {
            animMargin = if (oldScrollY < scrollY) {
                // Scroll down
                hideableView.height.coerceAtMost(hideableView.translationY.toInt() + scrollDelta)
            } else {
                // Scroll up
                0.coerceAtLeast(hideableView.translationY.toInt() + scrollDelta)
            }
        } else if (gravity == Gravity.TOP) {
            animMargin = if (oldScrollY > scrollY) {
                // scroll up
                0.coerceAtMost(hideableView.translationY.toInt() - scrollDelta)
            } else {
                // scroll down
                (-hideableView.height).coerceAtLeast(hideableView.translationY.toInt() - scrollDelta)
            }
        }
        hideableView.translationY = animMargin.toFloat()
        anchoredView?.translationY = animMargin.toFloat()

        if (updateElevation) {
            val elevation = if (scrollY == 0 && oldScrollY != 0) 0F else dpToPx(getDimension(R.dimen.toolbar_default_elevation))
            if (elevation != hideableView.elevation) {
                hideableView.elevation = elevation
            }
        }
    }

    override fun onUpOrCancelMotionEvent() {
        if (!enabled) {
            return
        }
        val transY = hideableView.translationY.toInt()
        val height = hideableView.height
        if (gravity == Gravity.BOTTOM && transY != 0 && transY < height) {
            if (transY > height / 2) {
                ensureHidden()
            } else {
                ensureDisplayed()
            }
        } else if (gravity == Gravity.TOP && transY != 0 && transY > -height) {
            if (transY > -height / 2) {
                ensureDisplayed()
            } else {
                ensureHidden()
            }
        }
    }

    override fun onDownMotionEvent() {
        // Don't do anything for now
    }

    override fun onClick(x: Float, y: Float): Boolean {
        if (enabled) {
            if (hideableView.translationY != 0f) {
                ensureDisplayed()
            } else {
                ensureHidden()
            }
        }
        return false
    }

    fun ensureDisplayed() {
        ensureTranslationY(hideableView, 0)
        anchoredView?.run {
            ensureTranslationY(this, 0)
        }
    }

    private fun ensureHidden() {
        val translation = if (gravity == Gravity.BOTTOM) hideableView.height else -hideableView.height
        ensureTranslationY(hideableView, translation)
        anchoredView?.run {
            ensureTranslationY(this, translation)
        }
    }
}
