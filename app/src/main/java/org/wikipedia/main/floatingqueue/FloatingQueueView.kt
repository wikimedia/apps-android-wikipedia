package org.wikipedia.main.floatingqueue

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_floating_queue.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.FaceAndColorDetectImageView

class FloatingQueueView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_floating_queue, this)

        // This fix the invisible issue when returning back from the PageActivity
        floatingQueueThumbnail.setLegacyVisibilityHandlingEnabled(true)

        // TODO: remove as soon as we drop support for API 19, and replace with CardView with elevation.
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, R.attr.shadow_background_drawable))
    }

    var callback: Callback? = null
    var imageView: FaceAndColorDetectImageView = floatingQueueThumbnail
        internal set
    private var openPageFromFloatingQueue: Boolean = false
    private var shouldShowFloatingQueue: Boolean = false

    interface Callback {
        fun onFloatingQueueClicked(title: PageTitle)
    }

    fun update() {
        openPageFromFloatingQueue = false

        val tabList = WikipediaApp.getInstance().tabList
        shouldShowFloatingQueue = tabList.size > 0

        if (shouldShowFloatingQueue) {
            val title = tabList[tabList.size - 1].backStackPositionTitle
            if (title != null) {
                floatingQueueArticle!!.text = title.displayText

                // Prevent blink
                val imageUrl = getProtocolRelativeUrl(title.thumbUrl)
                if (floatingQueueThumbnail.tag == null || floatingQueueThumbnail.tag != imageUrl) {
                    floatingQueueThumbnail.loadImage(if (!TextUtils.isEmpty(imageUrl)) Uri.parse(imageUrl) else null)
                    floatingQueueThumbnail.tag = imageUrl
                }

                floatingQueueCount!!.setTabCount(tabList.size)

                setOnClickListener {
                    openPageFromFloatingQueue = true
                    callback!!.onFloatingQueueClicked(title)
                }
                animation(false)
            } else {
                shouldShowFloatingQueue = false
            }
        }

        show()
    }

    fun show() {
        visibility = if (shouldShowFloatingQueue) VISIBLE else INVISIBLE
    }

    fun hide() {
        visibility = INVISIBLE
    }

    fun animation(isOnPause: Boolean) {
        if (isOnPause) {
            floatingQueueArticle!!.animate().translationX((-floatingQueueArticle!!.width).toFloat())
            if (!openPageFromFloatingQueue) {
                floatingQueueThumbnail.animate().alpha(0.0f).startDelay = ANIMATION_DELAY_MILLIS.toLong()
            }
        } else {
            floatingQueueArticle!!.animate().translationX(0f)
            if (!openPageFromFloatingQueue) {
                floatingQueueThumbnail.animate().alpha(1.0f).duration = ANIMATION_DELAY_MILLIS.toLong()
            }
        }
    }

    private fun getProtocolRelativeUrl(url: String?): String? {
        return if (url != null) UriUtil.resolveProtocolRelativeUrl(url) else null
    }

    companion object {
        const val ANIMATION_DELAY_MILLIS = 300
    }
}

