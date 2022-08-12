package org.wikipedia.edit

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.wikipedia.databinding.ViewWikitextFloatingPreviewBinding
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class WikiTextFloatingPreviewView : FrameLayout {
    private val binding = ViewWikitextFloatingPreviewBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun showImagePreview(thumbUrl: String) {
        binding.previewArticleContainer.isVisible = false
        binding.brokenLinkImage.isVisible = false
        binding.previewImage.isVisible = true
        ViewUtil.loadImage(binding.previewImage, thumbUrl)
    }

    fun showArticlePreview(title: String, summary: String) {
        binding.previewImage.isVisible = false
        binding.brokenLinkImage.isVisible = false
        binding.previewArticleContainer.isVisible = true
        binding.previewArticleTitle.text = StringUtil.fromHtml(title)
        binding.previewArticleSummary.text = StringUtil.fromHtml(summary)
    }

    fun showBrokenLink() {
        binding.previewImage.isVisible = false
        binding.previewArticleContainer.isVisible = false
        binding.brokenLinkImage.isVisible = true
    }
}
