package org.wikipedia.page

import android.view.View
import androidx.core.view.isVisible

/**
 * Covers the article while a new page is loading, and reveals it once the WebView has rendered
 * the new content and the page summary has updated the lead image. Without this, on slow
 * connections the new lead image can appear on top of the previous article's content, or on top
 * of new content whose margins don't match it yet.
 *
 * Once the new content is rendered, the cover waits at most [MAX_WAIT_AFTER_CONTENT_MILLIS] for
 * the page summary and the restored scroll position, so that a slow request can never leave the
 * page blank.
 */
class PageLoadMaskHandler(private val maskView: View, private val onUncovered: () -> Unit) {
    var isCovering = false
        private set
    private var isWebContentReady = false
    private var isMetadataReady = false
    private var isWaitingForScrollRestore = false
    private var hasWaitedLongEnough = false
    private val revealDurationMillis = maskView.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    private val stopWaiting = Runnable {
        hasWaitedLongEnough = true
        revealIfReady()
    }

    fun onLoadStarted(waitForScrollRestore: Boolean) {
        isCovering = true
        isWebContentReady = false
        isMetadataReady = false
        isWaitingForScrollRestore = waitForScrollRestore
        hasWaitedLongEnough = false
        maskView.removeCallbacks(stopWaiting)
        maskView.animate().cancel()
        maskView.alpha = 1f
        maskView.isVisible = true
    }

    fun onWebContentReady() {
        if (isCovering && !isWebContentReady) {
            maskView.postDelayed(stopWaiting, MAX_WAIT_AFTER_CONTENT_MILLIS)
        }
        isWebContentReady = true
        revealIfReady()
    }

    fun onMetadataReady() {
        isMetadataReady = true
        revealIfReady()
    }

    fun onScrollRestored() {
        isWaitingForScrollRestore = false
        revealIfReady()
    }

    fun onLoadFailed() {
        maskView.removeCallbacks(stopWaiting)
        maskView.animate().cancel()
        maskView.isVisible = false
        uncover()
    }

    private fun revealIfReady() {
        if (!isCovering || !isWebContentReady) {
            return
        }
        if (!hasWaitedLongEnough && (!isMetadataReady || isWaitingForScrollRestore)) {
            return
        }
        maskView.removeCallbacks(stopWaiting)
        maskView.animate()
            .alpha(0f)
            .setDuration(revealDurationMillis)
            .withEndAction { maskView.isVisible = false }
            .start()
        uncover()
    }

    private fun uncover() {
        if (isCovering) {
            isCovering = false
            onUncovered()
        }
    }

    companion object {
        private const val MAX_WAIT_AFTER_CONTENT_MILLIS = 1000L
    }
}
