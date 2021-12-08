package org.wikipedia.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import org.wikipedia.WikipediaApp
import org.wikipedia.events.WebViewInvalidateEvent
import org.wikipedia.util.DimenUtil.densityScalar
import org.wikipedia.views.FrameLayoutNavMenuTriggerer.Companion.setChildViewScrolled
import java.util.*
import kotlin.math.abs

class ObservableWebView : WebView {

    fun interface OnClickListener {
        fun onClick(x: Float, y: Float): Boolean
    }

    fun interface OnScrollChangeListener {
        fun onScrollChanged(oldScrollY: Int, scrollY: Int, isHumanScroll: Boolean)
    }

    fun interface OnDownMotionEventListener {
        fun onDownMotionEvent()
    }

    fun interface OnUpOrCancelMotionEventListener {
        fun onUpOrCancelMotionEvent()
    }

    fun interface OnContentHeightChangedListener {
        fun onContentHeightChanged(contentHeight: Int)
    }
    fun interface OnFastScrollListener {
        fun onFastScroll()
    }

    private var onClickListeners: MutableList<OnClickListener> = ArrayList()
    private var onScrollChangeListeners: MutableList<OnScrollChangeListener> = ArrayList()
    private var onDownMotionEventListeners: MutableList<OnDownMotionEventListener> = ArrayList()
    private var onUpOrCancelMotionEventListeners: MutableList<OnUpOrCancelMotionEventListener> = ArrayList()
    private var onContentHeightChangedListeners: MutableList<OnContentHeightChangedListener> = ArrayList()
    private var onFastScrollListener: OnFastScrollListener? = null
    private var currentContentHeight = 0
    private var lastScrollTime: Long = 0
    private var totalAmountScrolled = 0
    private var drawEventsWhileSwiping = 0
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var touchStartX = 0f
        private set
    var touchStartY = 0f
        private set
    var lastTouchX = 0f
        private set
    var lastTouchY = 0f
        private set

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int) : super(context, attrs, defStyle)

    fun addOnClickListener(onClickListener: OnClickListener) {
        onClickListeners.add(onClickListener)
    }

    fun addOnScrollChangeListener(onScrollChangeListener: OnScrollChangeListener) {
        onScrollChangeListeners.add(onScrollChangeListener)
    }

    fun addOnDownMotionEventListener(onDownMotionEventListener: OnDownMotionEventListener) {
        onDownMotionEventListeners.add(onDownMotionEventListener)
    }

    fun addOnUpOrCancelMotionEventListener(onUpOrCancelMotionEventListener: OnUpOrCancelMotionEventListener) {
        onUpOrCancelMotionEventListeners.add(onUpOrCancelMotionEventListener)
    }

    fun addOnContentHeightChangedListener(onContentHeightChangedListener: OnContentHeightChangedListener) {
        onContentHeightChangedListeners.add(onContentHeightChangedListener)
    }

    fun clearAllListeners() {
        onClickListeners.clear()
        onScrollChangeListeners.clear()
        onDownMotionEventListeners.clear()
        onUpOrCancelMotionEventListeners.clear()
        onContentHeightChangedListeners.clear()
        onFastScrollListener = null
    }

    override fun onScrollChanged(left: Int, top: Int, oldLeft: Int, oldTop: Int) {
        super.onScrollChanged(left, top, oldLeft, oldTop)
        val isHumanScroll = abs(top - oldTop) < MAX_HUMAN_SCROLL
        onScrollChangeListeners.forEach {
            it.onScrollChanged(oldTop, top, isHumanScroll)
        }
        if (!isHumanScroll) {
            return
        }
        totalAmountScrolled += top - oldTop
        if (abs(totalAmountScrolled) > FAST_SCROLL_THRESHOLD &&
                onFastScrollListener != null) {
            onFastScrollListener!!.onFastScroll()
            totalAmountScrolled = 0
        }
        lastScrollTime = System.currentTimeMillis()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.buttonState == MotionEvent.BUTTON_SECONDARY &&
                event.actionMasked == MotionEvent.ACTION_DOWN) {
            handleMouseRightClick(event.x, event.y)
            return true
        }
        lastTouchX = event.x
        lastTouchY = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDownMotionEventListeners.forEach {
                    it.onDownMotionEvent()
                }
                if (System.currentTimeMillis() - lastScrollTime > MAX_MILLIS_BETWEEN_SCROLLS) {
                    totalAmountScrolled = 0
                }
                touchStartX = event.x
                touchStartY = event.y
                drawEventsWhileSwiping = 0
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.x - touchStartX) <= touchSlop &&
                        abs(event.y - touchStartY) <= touchSlop) {
                    if (hitTestResult.type == HitTestResult.UNKNOWN_TYPE) {
                        if (onClickListeners.any { it.onClick(event.x, event.y) }) {
                            return true
                        }
                    }
                }
                drawEventsWhileSwiping = 0
                onUpOrCancelMotionEventListeners.forEach {
                    it.onUpOrCancelMotionEvent()
                }
                drawEventsWhileSwiping = 0
            }
            MotionEvent.ACTION_CANCEL -> {
                onUpOrCancelMotionEventListeners.forEach {
                    it.onUpOrCancelMotionEvent()
                }
                drawEventsWhileSwiping = 0
            }
            else -> { }
        }
        return super.onTouchEvent(event)
    }

    private fun handleMouseRightClick(x: Float, y: Float) {
        val eventTimeTravelMillis = 1000
        post {
            dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis() + eventTimeTravelMillis,
                    MotionEvent.ACTION_DOWN, x, y, 0))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isInEditMode) {
            return
        }
        drawEventsWhileSwiping++
        if (drawEventsWhileSwiping > SWIPE_DRAW_TOLERANCE) {
            setChildViewScrolled()
        }
        if (currentContentHeight != contentHeight) {
            currentContentHeight = contentHeight
            onContentHeightChangedListeners.forEach {
                it.onContentHeightChanged(currentContentHeight)
            }
        }
        WikipediaApp.getInstance().bus.post(INVALIDATE_EVENT)
    }

    companion object {
        private val INVALIDATE_EVENT = WebViewInvalidateEvent()
        private val FAST_SCROLL_THRESHOLD = (1000 * densityScalar).toInt()
        private val MAX_HUMAN_SCROLL = (500 * densityScalar).toInt()
        private const val MAX_MILLIS_BETWEEN_SCROLLS = 500
        private const val SWIPE_DRAW_TOLERANCE = 4
    }
}
