package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import org.wikipedia.util.DimenUtil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class LinearLayoutOverWebView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    private var webView: ObservableWebView? = null
    private var touchSlop = 0
    private var isViewPressed = false
    private var amountScrolled = 0
    private var startY = 0f
    private var slopReached = false

    fun setWebView(webView: ObservableWebView) {
        this.webView = webView
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isViewPressed = true
                slopReached = false
                startY = event.y
                amountScrolled = 0
            }
            MotionEvent.ACTION_MOVE -> {
                webView?.takeIf { isViewPressed }?.let {
                    val contentHeight = (it.contentHeight * DimenUtil.getDensityScalar()).toInt()
                    val minScroll = -it.scrollY
                    val maxScroll = contentHeight - it.scrollY - it.height
                    var scrollAmount = min((startY - event.y).toInt(), maxScroll)
                    scrollAmount = max(minScroll, scrollAmount)
                    // manually scroll the WebView that's underneath us...
                    it.scrollBy(0, scrollAmount)
                    amountScrolled += scrollAmount

                    if (abs(amountScrolled) > touchSlop && !slopReached) {
                        // if we go outside the slop radius, then dispatch a Cancel event to
                        // our children, and no longer dispatch any other events until we're
                        // finished with the current gesture.
                        slopReached = true
                        val moveEvent = MotionEvent.obtain(event)
                        moveEvent.action = MotionEvent.ACTION_CANCEL
                        super.dispatchTouchEvent(moveEvent)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isViewPressed = false
                slopReached = false
            }
        }
        return slopReached || super.dispatchTouchEvent(event)
    }
}
