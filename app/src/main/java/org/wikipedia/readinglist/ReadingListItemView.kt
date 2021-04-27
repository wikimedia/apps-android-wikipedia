package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ItemReadingListBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class ReadingListItemView : ConstraintLayout {
    interface Callback {
        fun onClick(readingList: ReadingList)
        fun onRename(readingList: ReadingList)
        fun onDelete(readingList: ReadingList)
        fun onSaveAllOffline(readingList: ReadingList)
        fun onRemoveAllOffline(readingList: ReadingList)
    }

    enum class Description {
        DETAIL, SUMMARY
    }

    private val binding = ItemReadingListBinding.inflate(LayoutInflater.from(context), this)
    private var readingList: ReadingList? = null
    private val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3, binding.itemImage4)
    var callback: Callback? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(0, DimenUtil.roundedDpToPx(16f), 0, DimenUtil.roundedDpToPx(16f))
        background = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        isClickable = true
        isFocusable = true
        clearThumbnails()
        DeviceUtil.setContextClickAsLongClick(this)

        setOnClickListener {
            readingList?.let {
                callback?.onClick(it)
            }
        }

        setOnLongClickListener { view ->
            readingList?.let {
                PopupMenu(context, view, Gravity.END).let { menu ->
                    menu.menuInflater.inflate(R.menu.menu_reading_list_item, menu.menu)
                    if (it.isDefault) {
                        menu.menu.findItem(R.id.menu_reading_list_rename).isVisible = false
                        menu.menu.findItem(R.id.menu_reading_list_delete).isVisible = false
                    }
                    menu.setOnMenuItemClickListener(OverflowMenuClickListener(it))
                    menu.show()
                }
            }
            false
        }

        binding.itemOverflowMenu.setOnClickListener { anchorView ->
            readingList?.let {
                PopupMenu(context, anchorView, Gravity.END).let { menu ->
                    menu.menuInflater.inflate(R.menu.menu_reading_list_item, menu.menu)
                    if (it.isDefault) {
                        menu.menu.findItem(R.id.menu_reading_list_rename).isVisible = false
                        menu.menu.findItem(R.id.menu_reading_list_delete).isVisible = false
                    }
                    menu.setOnMenuItemClickListener(OverflowMenuClickListener(it))
                    menu.show()
                }
            }
        }
    }

    fun setReadingList(readingList: ReadingList, description: Description) {
        this.readingList = readingList
        val isDetailView = description == Description.DETAIL
        binding.itemDescription.maxLines = if (isDetailView) Int.MAX_VALUE else resources.getInteger(R.integer.reading_list_description_summary_view_max_lines)
        val text: CharSequence = if (isDetailView) buildStatisticalDetailText(readingList) else buildStatisticalSummaryText(readingList)
        binding.itemReadingListStatisticalDescription.text = text
        updateDetails()
        if (binding.itemImage1.visibility == VISIBLE) {
            updateThumbnails()
        }
    }

    fun setThumbnailVisible(visible: Boolean) {
        imageViews.forEach {
            it.visibility = if (visible) VISIBLE else GONE
        }
        binding.defaultListEmptyImage.visibility = if (visible) VISIBLE else GONE
    }

    fun setTitleTextAppearance(@StyleRes id: Int) {
        TextViewCompat.setTextAppearance(binding.itemTitle, id)
    }

    fun setSearchQuery(searchQuery: String?) {
        // highlight search term within the text
        StringUtil.boldenKeywordText(binding.itemTitle, binding.itemTitle.text.toString(), searchQuery)
    }

    fun setOverflowViewVisibility(visibility: Int) {
        binding.itemOverflowMenu.visibility = visibility
    }

    private fun updateDetails() {
        readingList?.let {
            binding.defaultListEmptyImage.visibility = if (it.isDefault && it.pages.size == 0 && binding.itemImage1.visibility == VISIBLE) VISIBLE else GONE
            binding.itemTitle.text = it.title
            if (it.isDefault) {
                binding.itemDescription.text = context.getString(R.string.default_reading_list_description)
                binding.itemDescription.visibility = VISIBLE
            } else {
                binding.itemDescription.text = it.description
                binding.itemDescription.visibility = if (it.description.isNullOrEmpty()) GONE else VISIBLE
            }
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

    private fun buildStatisticalSummaryText(readingList: ReadingList): String {
        return resources.getQuantityString(R.plurals.format_reading_list_statistical_summary,
                readingList.pages.size, readingList.pages.size, statsTextListSize(readingList))
    }

    private fun buildStatisticalDetailText(readingList: ReadingList): String {
        return resources.getQuantityString(R.plurals.format_reading_list_statistical_detail,
                readingList.pages.size, readingList.numPagesOffline, readingList.pages.size, statsTextListSize(readingList))
    }

    private fun statsTextListSize(readingList: ReadingList): Float {
        return readingList.sizeBytes / 1.coerceAtLeast(resources.getInteger(R.integer.reading_list_item_size_bytes_per_unit)).toFloat()
    }

    private inner class OverflowMenuClickListener constructor(private val list: ReadingList?) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_reading_list_rename -> {
                    list?.let { callback?.onRename(it) }
                    return true
                }
                R.id.menu_reading_list_delete -> {
                    list?.let { callback?.onDelete(it) }
                    return true
                }
                R.id.menu_reading_list_save_all_offline -> {
                    list?.let { callback?.onSaveAllOffline(it) }
                    return true
                }
                R.id.menu_reading_list_remove_all_offline -> {
                    list?.let { callback?.onRemoveAllOffline(it) }
                    return true
                }
                else -> return false
            }
        }
    }
}
