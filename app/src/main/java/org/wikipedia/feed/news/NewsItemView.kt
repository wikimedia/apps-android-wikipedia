package org.wikipedia.feed.news

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
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
        val margin = DimenUtil.roundedDpToPx(8f)
        layoutParams.setMargins(margin, margin, margin, margin)
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
        binding.horizontalScrollListItemText.text = removeImageCaption(StringUtil.fromHtml(newsItem.story))
        RichTextUtil.removeUnderlinesFromLinksAndMakeBold(binding.horizontalScrollListItemText)
        newsItem.thumb()?.let {
            binding.horizontalScrollListItemImage.visibility = VISIBLE
            binding.horizontalScrollListItemImage.loadImage(it)
        } ?: run {
            binding.horizontalScrollListItemImage.visibility = GONE
            binding.horizontalScrollListItemText.maxLines = 10
        }
    }

    val imageView = binding.horizontalScrollListItemImage
}
