package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.databinding.ItemReadingListBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class ReadingListItemView : ConstraintLayout {
    interface Callback {
        fun onClick(readingList: ReadingList) {}
        fun onRename(readingList: ReadingList) {}
        fun onDelete(readingList: ReadingList) {}
        fun onSaveAllOffline(readingList: ReadingList) {}
        fun onRemoveAllOffline(readingList: ReadingList) {}
        fun onSelectList(readingList: ReadingList) {}
        fun onChecked(readingList: ReadingList) {}
        fun onShare(readingList: ReadingList) {}
    }

    enum class Description {
        DETAIL, SUMMARY
    }

    private val binding = ItemReadingListBinding.inflate(LayoutInflater.from(context), this)
    private var readingList: ReadingList? = null
    private val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3, binding.itemImage4)
    var callback: Callback? = null
    var saveClickListener: OnClickListener? = null
    val shareButton get() = binding.itemShareButton
    val listTitle get() = binding.itemTitle

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(0, DimenUtil.roundedDpToPx(16f), 0, DimenUtil.roundedDpToPx(16f))
        clipToPadding = false
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackground))
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
                    menu.menu.findItem(R.id.menu_reading_list_select).title =
                        context.getString(if (it.selected) R.string.reading_list_menu_unselect else R.string.reading_list_menu_select)
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
                    menu.menu.findItem(R.id.menu_reading_list_select).isVisible = false
                    if (it.isDefault) {
                        menu.menu.findItem(R.id.menu_reading_list_rename).isVisible = false
                        menu.menu.findItem(R.id.menu_reading_list_delete).isVisible = false
                    }
                    menu.menu.findItem(R.id.menu_reading_list_share).isVisible = false
                    menu.setOnMenuItemClickListener(OverflowMenuClickListener(it))
                    menu.show()
                }
            }
        }

        binding.itemSelectCheckbox.setOnClickListener {
            readingList?.let { callback?.onChecked(it) }
        }

        binding.itemShareButton.setOnClickListener {
            readingList?.let {
                callback?.onShare(it)
            }
        }

        binding.itemPreviewSaveButton.setOnClickListener {
            saveClickListener?.onClick(it)
        }

        FeedbackUtil.setButtonTooltip(binding.itemShareButton, binding.itemOverflowMenu)
    }

    fun setReadingList(readingList: ReadingList, description: Description,
                       selectMode: Boolean = false, newImport: Boolean = false) {
        this.readingList = readingList
        val isDetailView = description == Description.DETAIL
        binding.itemDescription.maxLines = if (isDetailView) Int.MAX_VALUE else resources.getInteger(R.integer.reading_list_description_summary_view_max_lines)
        val text: CharSequence = if (isDetailView) buildStatisticalDetailText(readingList) else buildStatisticalSummaryText(readingList)
        binding.itemReadingListStatisticalDescription.text = text
        binding.itemTitleIndicator.isVisible = newImport
        updateDetails(selectMode)
        if (binding.itemImage1.isVisible) {
            updateThumbnails()
        }
    }

    fun setThumbnailVisible(visible: Boolean) {
        imageViews.forEach {
            it.isVisible = visible
        }
        binding.defaultListEmptyImage.isVisible = visible
    }

    fun setTitleTextAppearance(@StyleRes id: Int) {
        TextViewCompat.setTextAppearance(binding.itemTitle, id)
    }

    fun setSearchQuery(searchQuery: String?) {
        // highlight search term within the text
        StringUtil.boldenKeywordText(binding.itemTitle, binding.itemTitle.text.toString(), searchQuery)
    }

    fun setOverflowViewVisibility(isVisible: Boolean) {
        binding.itemOverflowMenu.isVisible = isVisible
    }

    fun setPreviewMode(isPreview: Boolean) {
        binding.itemPreviewSaveButton.isVisible = isPreview
        binding.itemOverflowMenu.isVisible = !isPreview
        binding.itemReadingListStatisticalDescription.isVisible = !isPreview
        setOnLongClickListener {
            // Ignore onLongClick action
            false
        }
    }

    fun setRecommendedListMode(isRecommendedList: Boolean) {
        // TODO
    }

    private fun updateDetails(showCheckBoxes: Boolean) {
        readingList?.let {
            binding.defaultListEmptyImage.isVisible = it.isDefault && it.pages.isEmpty() && binding.itemImage1.isVisible
            binding.itemTitle.text = it.title
            if (it.isDefault) {
                binding.itemDescription.text = context.getString(R.string.default_reading_list_description)
                binding.itemDescription.isVisible = true
            } else {
                binding.itemDescription.text = it.description
                binding.itemDescription.isVisible = !it.description.isNullOrEmpty()
            }
            binding.itemSelectCheckbox.isVisible = showCheckBoxes
            binding.itemSelectCheckbox.isChecked = it.selected
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
            val thumbUrls = it.pages.mapNotNull { page -> page.thumbUrl }
                .filterNot { url -> url.isEmpty() }
            (imageViews zip thumbUrls).forEach { (imageView, url) ->
                ViewUtil.loadImage(imageView, url)
            }
        }
    }

    private fun buildStatisticalSummaryText(readingList: ReadingList): String {
        val totalListSize = statsTextListSize(readingList)
        return if (totalListSize > 0) resources.getQuantityString(R.plurals.format_reading_list_statistical_summary,
            readingList.pages.size, readingList.pages.size, statsTextListSize(readingList))
        else resources.getQuantityString(R.plurals.format_reading_list_statistical_summary_without_size,
            readingList.pages.size, readingList.pages.size)
    }

    private fun buildStatisticalDetailText(readingList: ReadingList): String {
        val totalListSize = statsTextListSize(readingList)
        return if (totalListSize > 0) resources.getQuantityString(R.plurals.format_reading_list_statistical_detail,
            readingList.pages.size, readingList.numPagesOffline, readingList.pages.size, totalListSize)
        else resources.getQuantityString(R.plurals.format_reading_list_statistical_detail_without_size,
            readingList.pages.size, readingList.numPagesOffline, readingList.pages.size)
    }

    private fun statsTextListSize(readingList: ReadingList): Float {
        return readingList.sizeBytesFromPages / 1.coerceAtLeast(resources.getInteger(R.integer.reading_list_item_size_bytes_per_unit)).toFloat()
    }

    private inner class OverflowMenuClickListener(private val list: ReadingList?) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            BreadCrumbLogEvent.logClick(context, item)
            return list?.let {
                when (item.itemId) {
                    R.id.menu_reading_list_rename -> {
                        callback?.onRename(it)
                        true
                    }

                    R.id.menu_reading_list_delete -> {
                        callback?.onDelete(it)
                        true
                    }

                    R.id.menu_reading_list_save_all_offline -> {
                        callback?.onSaveAllOffline(it)
                        true
                    }

                    R.id.menu_reading_list_remove_all_offline -> {
                        callback?.onRemoveAllOffline(it)
                        true
                    }

                    R.id.menu_reading_list_export -> {
                        ReadingListsExportImportHelper.exportLists(context as BaseActivity, listOf(it))
                        true
                    }

                    R.id.menu_reading_list_select -> {
                        callback?.onSelectList(it)
                        true
                    }

                    R.id.menu_reading_list_share -> {
                        callback?.onShare(it)
                        return true
                    }

                    else -> false
                }
            } == true
        }
    }
}
