package org.wikipedia.richtext

import android.text.Editable
import android.text.Html.TagHandler
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.core.text.toSpanned
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.Locator
import org.xml.sax.XMLReader

class CustomHtmlParser constructor(private val handler: TagHandler) : TagHandler, ContentHandler {
    interface TagHandler {
        fun handleTag(opening: Boolean, tag: String?, output: Editable?, attributes: Attributes?): Boolean
    }

    private var wrapped: ContentHandler? = null
    private var text: Editable? = null
    private val tagStatus = ArrayDeque<Boolean>()

    override fun handleTag(opening: Boolean, tag: String?, output: Editable?, xmlReader: XMLReader) {
        if (wrapped == null) {
            text = output
            wrapped = xmlReader.contentHandler
            xmlReader.contentHandler = this

            // Note: If it becomes necessary to inject a starting tag (see comments below), then
            // explicitly add a status to our queue to account for this tag:
            // tagStatus.addLast(false)
        }
    }

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        val isHandled = handler.handleTag(true, localName, text, attributes)
        tagStatus.addLast(isHandled)
        if (!isHandled) {
            wrapped?.startElement(uri, localName, qName, attributes)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (tagStatus.isNotEmpty() && !tagStatus.removeLast()) {
            wrapped?.endElement(uri, localName, qName)
        }
        handler.handleTag(false, localName, text, null)
    }

    override fun setDocumentLocator(locator: Locator?) {
        wrapped?.setDocumentLocator(locator)
    }

    override fun startDocument() {
        wrapped?.startDocument()
    }

    override fun endDocument() {
        wrapped?.endDocument()
    }

    override fun startPrefixMapping(prefix: String?, uri: String?) {
        wrapped?.startPrefixMapping(prefix, uri)
    }

    override fun endPrefixMapping(prefix: String?) {
        wrapped?.endPrefixMapping(prefix)
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        wrapped?.characters(ch, start, length)
    }

    override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {
        wrapped?.ignorableWhitespace(ch, start, length)
    }

    override fun processingInstruction(target: String?, data: String?) {
        wrapped?.processingInstruction(target, data)
    }

    override fun skippedEntity(name: String?) {
        wrapped?.skippedEntity(name)
    }

    class CustomTagHandler(private val view: TextView?) : TagHandler {
        private var lastAClass = ""

        override fun handleTag(opening: Boolean, tag: String?, output: Editable?, attributes: Attributes?): Boolean {
            if (tag == "img") {
                return true
            } else if (tag == "a") {
                if (opening) {
                    lastAClass = getValue(attributes, "class").orEmpty()
                } else if (output != null && output.isNotEmpty()) {
                    val spans = output.getSpans<URLSpan>(output.length - 1)
                    if (spans.isNotEmpty()) {
                        val span = spans.last()
                        val start = output.getSpanStart(span)
                        val end = output.getSpanEnd(span)
                        output.removeSpan(span)
                        val color = if (lastAClass == "new" && view != null) ResourceUtil.getThemedColor(view.context, R.attr.colorError) else -1
                        output.setSpan(URLSpanNoUnderline(span.url, color), start, end, 0)
                    }
                }
            }
            return false
        }
    }

    companion object {

        fun fromHtml(html: String?, view: TextView? = null): Spanned {
            var sourceStr = html.orEmpty()

            if ("<" !in sourceStr && "&" !in sourceStr) {
                // If the string doesn't contain any hints of HTML entities, then skip the expensive
                // processing that fromHtml() performs.
                return sourceStr.toSpanned()
            }

            // Replace a few HTML entities that are not handled automatically by the parser.
            sourceStr = sourceStr.replace("&#8206;", "\u200E")
                .replace("&#8207;", "\u200F")
                .replace("&amp;", "&")

            // TODO: Investigate if it's necessary to inject a dummy tag at the beginning of the
            // text, since there are reports that XmlReader ignores the first tag by default?
            // This would become something like "<inject/>$sourceStr".parseAsHtml(...)
            return sourceStr.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY, null,
                CustomHtmlParser(CustomTagHandler(view)))
        }

        private fun getValue(attributes: Attributes?, name: String): String? {
            if (attributes != null) {
                for (i in 0 until attributes.length) {
                    if (name == attributes.getLocalName(i)) return attributes.getValue(i)
                }
            }
            return null
        }
    }
}
