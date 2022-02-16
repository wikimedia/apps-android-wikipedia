package org.wikipedia.page

import android.net.Uri
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.text.getSpans
import org.wikipedia.WikipediaApp
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

class LinkMovementMethodExt : LinkMovementMethod {

    fun interface UrlHandler {
        fun onUrlClick(url: String)
    }

    fun interface UrlHandlerWithText {
        fun onUrlClick(url: String, titleString: String?, linkText: String)
    }

    fun interface UrlHandlerWithTextAndCoords {
        fun onUrlClick(url: String, titleString: String?, linkText: String, x: Int, y: Int)
    }

    private var handler: UrlHandler? = null
    private var handlerWithText: UrlHandlerWithText? = null
    private var handlerWithTextAndCoords: UrlHandlerWithTextAndCoords? = null

    constructor(handler: UrlHandler?) {
        this.handler = handler
    }

    constructor(handler: UrlHandlerWithText?) {
        handlerWithText = handler
    }

    constructor(handler: UrlHandlerWithTextAndCoords?) {
        handlerWithTextAndCoords = handler
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val links = buffer.getSpans<URLSpan>(off, off)
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

                handlerWithTextAndCoords?.run {
                    onUrlClick(url, UriUtil.getTitleFromUrl(url), linkText, event.rawX.toInt(), event.rawY.toInt())
                }

                return true
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }

    internal class ErrorLinkHandler internal constructor() : LinkHandler(WikipediaApp.getInstance()) {
        override var wikiSite = WikipediaApp.getInstance().wikiSite
        override fun onMediaLinkClicked(title: PageTitle) {}
        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {}
        override fun onPageLinkClicked(anchor: String, linkText: String) {}
        override fun onInternalLinkClicked(title: PageTitle) {
            // Explicitly send everything to an external browser, since the error might be shown in
            // a child activity of PageActivity, and we don't want to lose our place.
            UriUtil.visitInExternalBrowser(WikipediaApp.getInstance(),
                    Uri.parse(UriUtil.resolveProtocolRelativeUrl(title.wikiSite, title.mobileUri)))
        }
    }

    companion object {
        fun getExternalLinkMovementMethod(): LinkMovementMethodExt {
            return LinkMovementMethodExt(ErrorLinkHandler())
        }
    }
}
