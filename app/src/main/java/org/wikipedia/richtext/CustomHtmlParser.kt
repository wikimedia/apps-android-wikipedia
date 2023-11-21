package org.wikipedia.richtext

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Html.ImageGetter
import android.text.Html.TagHandler
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.core.graphics.applyCanvas
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.core.text.toSpanned
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.WhiteBackgroundTransformation
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

    class CustomTagHandler(private val view: TextView?,
                           private val noSmallSizeImage: Boolean = true) : TagHandler {
        private var lastAClass = ""
        private var lastDivClass = ""
        private var lastDivStyle = ""
        private var lastSpannedDivString = ""
        private var listItemCount = 0
        private val listParents = mutableListOf<String>()

        override fun handleTag(opening: Boolean, tag: String?, output: Editable?, attributes: Attributes?): Boolean {
            if (tag == "img" && view == null) {
                return true
            } else if (tag == "img" && opening && view != null) {
                var imgWidthStr = getValue(attributes, "width").orEmpty()
                var imgHeightStr = getValue(attributes, "height").orEmpty()
                val styleStr = getValue(attributes, "style").orEmpty()
                val widthRegex = "width:\\s*([\\d.]+)\\w{2}".toRegex()
                if (imgWidthStr.isEmpty()) {
                    imgWidthStr = widthRegex.find(styleStr)?.value.orEmpty().replace("width:", "")
                }
                val heightRegex = "height:\\s*([\\d.]+)\\w{2}".toRegex()
                if (imgHeightStr.isEmpty()) {
                    imgHeightStr = heightRegex.find(styleStr)?.value.orEmpty().replace("height:", "")
                }
                var imgWidth = DimenUtil.htmlUnitToPxInt(imgWidthStr) ?: MIN_IMAGE_SIZE
                var imgHeight = DimenUtil.htmlUnitToPxInt(imgHeightStr) ?: MIN_IMAGE_SIZE
                val imgSrc = getValue(attributes, "src").orEmpty()

                if (noSmallSizeImage && (imgWidth < MIN_IMAGE_SIZE || imgHeight < MIN_IMAGE_SIZE)) {
                    return true
                }

                imgWidth = DimenUtil.roundedDpToPx(imgWidth.toFloat())
                imgHeight = DimenUtil.roundedDpToPx(imgHeight.toFloat())

                if (imgWidth > 0 && imgHeight > 0 && imgSrc.isNotEmpty()) {
                    val uri = if (imgSrc.startsWith("//")) {
                        WikiSite.DEFAULT_SCHEME + ":" + imgSrc
                    } else if (imgSrc.startsWith("./")) {
                        Service.COMMONS_URL + imgSrc.replace("./", "")
                    } else {
                        UriUtil.resolveProtocolRelativeUrl(WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode), imgSrc)
                    }

                    val extension = MimeTypeMap.getFileExtensionFromUrl(uri)
                    if (!MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty().contains("image", true)) {
                        return true
                    }

                    val bmpMap = contextBmpMap.getOrPut(view.context) { mutableMapOf() }
                    var drawable = bmpMap[imgSrc]

                    if (drawable == null || drawable.bitmap.isRecycled) {
                        // give it a placeholder drawable of the appropriate size
                        drawable = BitmapDrawable(view.context.resources,
                            Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.RGB_565))
                        bmpMap[imgSrc] = drawable

                        drawable.setBounds(0, 0, imgWidth, imgHeight)

                        Glide.with(view)
                            .asBitmap()
                            .load(uri)
                            .transform(WhiteBackgroundTransformation())
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    if (!drawable.bitmap.isRecycled) {
                                        drawable.bitmap.applyCanvas {
                                            drawBitmap(resource, Rect(0, 0, resource.width, resource.height), drawable.bounds, null)
                                        }
                                        WhiteBackgroundTransformation.maybeDimImage(drawable.bitmap)
                                        view.postInvalidate()
                                    }
                                }
                                override fun onLoadCleared(placeholder: Drawable?) { }
                            })
                    }
                }
            } else if (tag == "a") {
                if (opening) {
                    lastAClass = getValue(attributes, "class").orEmpty()
                } else if (!output.isNullOrEmpty()) {
                    val spans = output.getSpans<URLSpan>(output.length - 1)
                    if (spans.isNotEmpty()) {
                        val span = spans.last()
                        val start = output.getSpanStart(span)
                        val end = output.getSpanEnd(span)
                        output.removeSpan(span)
                        val color = if (lastAClass == "new" && view != null) ResourceUtil.getThemedColor(view.context, androidx.appcompat.R.attr.colorError) else -1
                        output.setSpan(URLSpanNoUnderline(span.url, color), start, end, 0)
                    }
                }
            } else if (tag == "code" && output != null) {
                if (opening) {
                    output.setSpan(TypefaceSpan("monospace"), output.length, output.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                } else {
                    val spans = output.getSpans<TypefaceSpan>(output.length)
                    if (spans.isNotEmpty()) {
                        val span = spans.last()
                        val start = output.getSpanStart(span)
                        output.removeSpan(span)
                        output.setSpan(TypefaceSpan("monospace"), start, output.length, 0)
                    }
                }
            } else if (tag == "ol") {
                if (opening) {
                    listParents.add(tag)
                } else {
                    listParents.remove(tag)
                }
                listItemCount = 0
            } else if (tag == "li" && listParents.isNotEmpty() && !opening && output != null) {
                handleListTag(output)
            } else if (tag == "div" && output != null) {
                if (opening) {
                    lastDivStyle = getValue(attributes, "style").orEmpty()
                    lastDivClass = getValue(attributes, "class").orEmpty()
                    lastSpannedDivString = output.toString()
                } else {
                    val alignmentSpan = if (lastDivClass == "center" || lastDivStyle.contains("margin-left: auto", true) && lastDivStyle.contains("margin-right: auto", true)) {
                        Layout.Alignment.ALIGN_CENTER
                    } else if (lastDivClass == "floatright" || lastDivStyle.contains("text-align: right", true)) {
                        Layout.Alignment.ALIGN_OPPOSITE
                    } else {
                        Layout.Alignment.ALIGN_NORMAL
                    }
                    val start = lastSpannedDivString.length
                    val end = output.length
                    val spans = output.getSpans<AlignmentSpan>(end)
                    if (start < end && spans.isEmpty()) {
                        // TODO: fix unexpected error that cannot be escaped.
                        output.setSpan(AlignmentSpan.Standard(alignmentSpan), start, end, 0)
                    }
                }
            } else if ((tag == "dd" || tag == "dl") && output != null) {
                if (opening) {
                    // TODO: maybe replace with LeadingMarginSpan
                    output.append("\n")
                    output.append("     ")
                }
            } else if (tag == "dt" && output != null) {
                if (opening) {
                    output.setSpan(StyleSpan(Typeface.BOLD), output.length, output.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                } else {
                    val spans = output.getSpans<StyleSpan>(output.length)
                    if (spans.isNotEmpty()) {
                        val span = spans.last()
                        val start = output.getSpanStart(span)
                        output.removeSpan(span)
                        output.setSpan(StyleSpan(Typeface.BOLD), start, output.length, 0)
                    }
                }
            }
            return false
        }

        private fun handleListTag(output: Editable) {
            if (listParents.last() == "ol") {
                listItemCount++
                val split = output.split("\n").filter { it.isNotEmpty() }
                val start = output.length - split.last().length - 1
                val replaceStr = "$listItemCount. ${split.last()}"
                output.replace(start - 1, output.length - 1, replaceStr)

                val spans = output.getSpans<LeadingMarginSpan>(output.length)
                if (spans.isNotEmpty()) {
                    val span = spans.last()
                    val startSpan = output.getSpanStart(span)
                    output.removeSpan(span)
                    output.setSpan(LeadingMarginSpan.Standard(50 * listParents.size), startSpan, output.length, 0)
                }
            }
        }
    }

    class CustomImageGetter(private val context: Context) : ImageGetter {
        override fun getDrawable(source: String?): Drawable {
            var bmp: BitmapDrawable? = null
            try {
                if (contextBmpMap.containsKey(context)) {
                    if (contextBmpMap[context]!!.containsKey(source)) {
                        bmp = contextBmpMap[context]!![source]
                    }
                }
            } catch (e: Exception) {
                L.e(e)
            }
            if (bmp == null) {
                bmp = BlankBitmapPlaceholder(context.resources, null)
            }
            return bmp
        }
    }

    internal class BlankBitmapPlaceholder(res: Resources, bitmap: Bitmap?) : BitmapDrawable(res, bitmap) {
        override fun draw(canvas: Canvas) {
        }
    }

    companion object {
        private const val MIN_IMAGE_SIZE = 64
        private val contextBmpMap = mutableMapOf<Context, MutableMap<String, BitmapDrawable>>()

        fun fromHtml(html: String?, view: TextView? = null, hasMinImageSize: Boolean = true): Spanned {
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
            return sourceStr.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY,
                if (view == null) null else CustomImageGetter(view.context),
                CustomHtmlParser(CustomTagHandler(view, hasMinImageSize)))
        }

        fun pruneBitmaps(context: Context) {
            contextBmpMap[context]?.let { map ->
                map.values.forEach {
                    try {
                        it.bitmap.recycle()
                    } catch (e: Exception) {
                        L.e(e)
                    }
                }
                map.clear()
                contextBmpMap.remove(context)
            }
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
