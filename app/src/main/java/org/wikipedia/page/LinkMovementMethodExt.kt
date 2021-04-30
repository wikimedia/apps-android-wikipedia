package org.wikipedia.page

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

class LinkMovementMethodExt : LinkMovementMethod {

    fun interface UrlHandler {
        fun onUrlClick(url: String)
    }

    fun interface UrlHandlerWithText {
        fun onUrlClick(url: String, titleString: String?, linkText: String)
    }

    private var handler: UrlHandler? = null
    private var handlerWithText: UrlHandlerWithText? = null

    constructor(handler: UrlHandler?) {
        this.handler = handler
    }

    constructor(handler: UrlHandlerWithText?) {
        handlerWithText = handler
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val links = buffer.getSpans(off, off, URLSpan::class.java)
            if (links.isNotEmpty()) {
                val linkText = try {
                    buffer.subSequence(buffer.getSpanStart(links[0]), buffer.getSpanEnd(links[0])).toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
                L.d(linkText)
                val url = UriUtil.decodeURL(links[0].url)

                handler?.run {
                    onUrlClick(url)
                }

                handlerWithText?.run {
                    onUrlClick(url, UriUtil.getTitleFromUrl(url), linkText)
                }

                return true
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }
}
