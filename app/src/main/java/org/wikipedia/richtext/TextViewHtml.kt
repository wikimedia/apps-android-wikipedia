package org.wikipedia.richtext

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.style.URLSpan
import android.widget.TextView
import androidx.core.graphics.applyCanvas
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.wikipedia.R
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.xml.sax.Attributes

fun TextView.setHtml(source: String?) {
    var sourceStr = source.orEmpty()

    if ("<" !in sourceStr && "&" !in sourceStr) {
        // If the string doesn't contain any hints of HTML entities, then skip the expensive
        // processing that we need to perform.
        this.text = sourceStr
    }

    sourceStr = sourceStr.replace("&#8206;", "\u200E")
        .replace("&#8207;", "\u200F")
        .replace("&amp;", "&")

    // TODO: Investigate if it's necessary to inject a dummy tag at the beginning of the
    // text, since there are reports that XmlReader ignores the first tag by default?
    // This would become something like "<inject/>$html".parseAsHtml(...)

    this.text = sourceStr.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY, { url ->
        try {
            var bmp: BitmapDrawable? = null
            if (CustomHtmlParser.viewBmpMap.containsKey(this.context)) {
                if (CustomHtmlParser.viewBmpMap[this.context]!!.containsKey(url)) {
                    bmp = CustomHtmlParser.viewBmpMap[this.context]!![url]
                }
            }
            if (bmp == null) {
                bmp = CustomHtmlParser.BlankBitmapPlaceholder(this.context.resources, null)
            }
            bmp
        } catch (e: Exception) {
            L.e(e)
            null
        }
    }, CustomHtmlParser(object : CustomHtmlParser.TagHandler {
        var lastAClass = ""

        override fun handleTag(opening: Boolean, tag: String?, output: Editable?, attributes: Attributes?): Boolean {
            if (tag == "img" && opening) {
                var imgWidth = DimenUtil.htmlPxToInt(CustomHtmlParser.getValue(attributes, "width").orEmpty())
                var imgHeight = DimenUtil.htmlPxToInt(CustomHtmlParser.getValue(attributes, "height").orEmpty())
                val imgSrc = CustomHtmlParser.getValue(attributes, "src").orEmpty()

                if (imgWidth < CustomHtmlParser.MIN_IMAGE_SIZE || imgHeight < CustomHtmlParser.MIN_IMAGE_SIZE) {
                    return true
                }

                imgWidth = DimenUtil.roundedDpToPx(imgWidth.toFloat())
                imgHeight = DimenUtil.roundedDpToPx(imgHeight.toFloat())

                if (imgWidth > 0 && imgHeight > 0 && imgSrc.isNotEmpty()) {
                    val bmpMap = CustomHtmlParser.viewBmpMap.getOrPut(this@setHtml.context) { mutableMapOf() }
                    var drawable = bmpMap[imgSrc]

                    if (drawable == null || drawable.bitmap.isRecycled) {
                        // give it a placeholder drawable of the appropriate size

                        if (this@setHtml.measuredWidth in 1 until imgWidth) {
                            val ratio = this@setHtml.width.toFloat() / imgHeight.toFloat()
                            imgWidth = this@setHtml.width
                            imgHeight = (imgHeight.toFloat() * ratio).toInt()
                        }

                        drawable = BitmapDrawable(this@setHtml.context.resources,
                            Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.RGB_565))
                        bmpMap[imgSrc] = drawable

                        drawable.setBounds(0, 0, imgWidth, imgHeight)

                        var uri = imgSrc
                        if (uri.startsWith("//")) {
                            uri = WikiSite.DEFAULT_SCHEME + ":" + uri
                        } else if (uri.startsWith("./")) {
                            uri = Service.COMMONS_URL + uri.replace("./", "")
                        }

                        Glide.with(this@setHtml)
                            .asBitmap()
                            .load(uri)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    if (isAttachedToWindow) {
                                        drawable.bitmap.applyCanvas {
                                            drawBitmap(resource, Rect(0, 0, resource.width, resource.height), drawable.bounds, null)
                                        }
                                        this@setHtml.postInvalidate()
                                    }
                                }
                                override fun onLoadCleared(placeholder: Drawable?) { }
                            })
                    }
                }
            } else if (tag == "a") {
                if (opening) {
                    lastAClass = CustomHtmlParser.getValue(attributes, "class").orEmpty()
                } else if (output != null && output.isNotEmpty()) {
                    val spans = output.getSpans<URLSpan>(output.length - 1)
                    if (spans.isNotEmpty()) {
                        val span = spans.last()
                        val start = output.getSpanStart(span)
                        val end = output.getSpanEnd(span)
                        output.removeSpan(span)
                        val color = if (lastAClass == "new") ResourceUtil.getThemedColor(this@setHtml.context, R.attr.colorError) else -1
                        output.setSpan(URLSpanNoUnderline(span.url, color), start, end, 0)
                    }
                }
            }
            return false
        }
    }))
}
