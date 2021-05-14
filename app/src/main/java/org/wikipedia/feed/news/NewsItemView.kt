package org.wikipedia.feed.news

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import org.wikipedia.databinding.ViewHorizontalScrollListItemCardBinding
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.WikiCardView

class NewsItemView(context: Context) : WikiCardView(context) {

    private val binding = ViewHorizontalScrollListItemCardBinding.inflate(LayoutInflater.from(context), this, true)
    var callback: FeedAdapter.Callback? = null
    var newsItem: NewsItem? = null

    init {
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layoutParams.setMargins(DimenUtil.roundedDpToPx(8f))
        setLayoutParams(layoutParams)
    }

    /* Remove the in-Wikitext thumbnail caption, which will almost certainly not apply here */
    private fun removeImageCaption(text: Spanned): CharSequence {
        val spans = RichTextUtil.getSpans(text, 0, text.length)
        val span = spans.find { it is StyleSpan && it.style == Typeface.ITALIC }
        span?.let {
            val start = text.getSpanStart(it)
            val end = text.getSpanEnd(it)
            if (text[start] == '(' && text[end - 1] == ')') {
                L.v("Removing spanned text: " + text.subSequence(start, end))
                return RichTextUtil.remove(text, start, end)
            }
        }
        return text
    }

    fun setContents(newsItem: NewsItem) {
        this.newsItem = newsItem
        binding.horizontalScrollListItemText.text = removeImageCaption(StringUtil.fromHtml(newsItem.story()))
        RichTextUtil.removeUnderlinesFromLinksAndMakeBold(binding.horizontalScrollListItemText)
        val thumb = newsItem.thumb()
        binding.horizontalScrollListItemImage.isVisible = thumb != null
        if (thumb != null) {
            binding.horizontalScrollListItemImage.loadImage(thumb)
        } else {
            binding.horizontalScrollListItemText.maxLines = 10
        }
    }

    val imageView = binding.horizontalScrollListItemImage
}
