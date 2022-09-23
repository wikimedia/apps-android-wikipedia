package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.R
import org.wikipedia.databinding.DialogImportReadingListBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.views.ViewUtil

class ReadingListImportDialogView : FrameLayout {

    private val binding = DialogImportReadingListBinding.inflate(LayoutInflater.from(context), this, true)
    private val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3, binding.itemImage4)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setReadingList(readingList: ReadingList) {

        binding.readingListTitle.text = readingList.listTitle
        binding.readingListNumberOfPages.text = resources.getQuantityString(R.plurals.shareable_reading_lists_import_dialog_content_articles, readingList.pages.size, readingList.pages.size)

        // Set default images
        imageViews.forEach {
            ViewUtil.loadImage(it, null)
        }

        readingList.let {
            val thumbUrls = it.pages.mapNotNull { page -> page.thumbUrl }
                .filterNot { url -> url.isEmpty() }
            (imageViews zip thumbUrls).forEach { (imageView, url) ->
                ViewUtil.loadImage(imageView, url)
            }
        }
    }
}
