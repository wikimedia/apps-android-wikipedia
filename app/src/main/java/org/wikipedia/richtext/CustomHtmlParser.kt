package org.wikipedia.richtext

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.text.Editable
import android.text.Html.TagHandler
import android.text.Spanned
import android.text.style.URLSpan
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import org.wikipedia.util.log.L
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

    internal class BlankBitmapPlaceholder(res: Resources, bitmap: Bitmap?) : BitmapDrawable(res, bitmap) {
        override fun draw(canvas: Canvas) {
        }
    }

    companion object {
        const val MIN_IMAGE_SIZE = 64
        val viewBmpMap = mutableMapOf<Context, MutableMap<String, BitmapDrawable>>()

        fun pruneBitmaps(context: Context) {
            if (viewBmpMap.containsKey(context)) {
                val bmpMap = viewBmpMap[context]!!
                bmpMap.values.forEach {
                    try {
                        it.bitmap.recycle()
                    } catch (e: Exception) {
                        L.e(e)
                    }
                }
                bmpMap.clear()
                viewBmpMap.remove(context)
            }
        }

        fun fromHtml(html: String): Spanned {
            // TODO: Investigate if it's necessary to inject a dummy tag at the beginning of the
            // text, since there are reports that XmlReader ignores the first tag by default?
            // This would become something like "<inject/>$html".parseAsHtml(...)
            return html.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY, null, CustomHtmlParser(object : TagHandler {
                var lastAClass = ""

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
                                // TODO: if we need to override the color (e.g. for showing red links):
                                val color = -1 // if (lastAClass == "new") ResourcesCompat.getColor(WikipediaApp.instance.resources, R.color.red50, WikipediaApp.instance.theme) else -1
                                output.setSpan(URLSpanNoUnderline(span.url, color), start, end, 0)
                            }
                        }
                    }
                    return false
                }
            }))
        }

        fun getValue(attributes: Attributes?, name: String): String? {
            if (attributes != null) {
                var i = 0
                val n: Int = attributes.length
                while (i < n) {
                    if (name == attributes.getLocalName(i)) return attributes.getValue(i)
                    i++
                }
            }
            return null
        }
    }
}
