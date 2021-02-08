package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import org.wikipedia.util.DimenUtil.densityScalar
import kotlin.math.abs

open class LinearLayoutOverWebView : LinearLayout {
    private lateinit var webView: ObservableWebView
    private var touchSlop = 0
    private var viewPressed = false
    private var amountScrolled = 0
    private var startY = 0f
    private var slopReached = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setWebView(webView: ObservableWebView) {
        this.webView = webView
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                viewPressed = true
                slopReached = false
                startY = event.y
                amountScrolled = 0
            }
            MotionEvent.ACTION_MOVE -> if (viewPressed) {
                val contentHeight = (webView.contentHeight * densityScalar).toInt()
                val minScroll = -webView.scrollY
                val maxScroll = contentHeight - webView.scrollY - webView.height
                var scrollAmount = (startY - event.y).toInt().coerceAtMost(maxScroll)
                scrollAmount = minScroll.coerceAtLeast(scrollAmount)
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
                if (slopReached) {
                    // manually scroll the WebView that's underneath us...
                    webView.scrollBy(0, scrollAmount)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewPressed = false
                slopReached = false
            }
            else -> { }
        }
        return slopReached || super.dispatchTouchEvent(event)
    }
}
