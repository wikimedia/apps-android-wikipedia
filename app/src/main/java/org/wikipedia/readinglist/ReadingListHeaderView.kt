package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewReadingListHeaderBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.GradientUtil
import org.wikipedia.views.ViewUtil

class ReadingListHeaderView : FrameLayout {

    private val binding = ViewReadingListHeaderBinding.inflate(LayoutInflater.from(context), this)
    private var imageViews = listOf(binding.readingListHeaderImage0, binding.readingListHeaderImage1, binding.readingListHeaderImage2,
            binding.readingListHeaderImage3, binding.readingListHeaderImage4, binding.readingListHeaderImage5)
    private var readingList: ReadingList? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding.readingListHeaderImageGradient.background = GradientUtil.getPowerGradient(R.color.black54, Gravity.TOP)
        if (!isInEditMode) {
            clearThumbnails()
        }
    }

    fun setReadingList(readingList: ReadingList) {
        this.readingList = readingList
        if (readingList.pages.isEmpty()) {
            binding.readingListHeaderImageContainer.visibility = GONE
            binding.readingListHeaderEmptyImage.visibility = VISIBLE
        } else {
            binding.readingListHeaderImageContainer.visibility = VISIBLE
            binding.readingListHeaderEmptyImage.visibility = GONE
            updateThumbnails()
        }
    }

    private fun clearThumbnails() {
        imageViews.forEach {
            ViewUtil.loadImage(it, null)
        }
    }

    private fun updateThumbnails() {
        readingList?.let {
            clearThumbnails()
            val thumbUrls = arrayOfNulls<String>(imageViews.size)
            var thumbUrlsIndex = 0
            it.pages.forEach { page ->
                if (!page.thumbUrl.isNullOrEmpty() && thumbUrlsIndex < imageViews.size) {
                    thumbUrls[thumbUrlsIndex++] = page.thumbUrl
                }
            }
            thumbUrls.forEachIndexed { i, url ->
                ViewUtil.loadImage(imageViews[i], url)
            }
        }
    }
}
