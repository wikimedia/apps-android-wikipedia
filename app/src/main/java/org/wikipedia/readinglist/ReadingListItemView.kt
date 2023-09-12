package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.*
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.metricsplatform.BreadcrumbLogEvent
import org.wikipedia.databinding.ItemReadingListBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.*
import org.wikipedia.views.ViewUtil

class ReadingListItemView : ConstraintLayout {
    interface Callback {
        fun onClick(readingList: ReadingList)
        fun onRename(readingList: ReadingList)
        fun onDelete(readingList: ReadingList)
        fun onSaveAllOffline(readingList: ReadingList)
        fun onRemoveAllOffline(readingList: ReadingList)
        fun onSelectList(readingList: ReadingList)
        fun onChecked(readingList: ReadingList)
        fun onShare(readingList: ReadingList)
    }

    enum class Description {
        DETAIL, SUMMARY
    }

    private val binding = ItemReadingListBinding.inflate(LayoutInflater.from(context), this)
    private var readingList: ReadingList? = null
    private val imageViews = listOf(binding.itemImage1, binding.itemImage2, binding.itemImage3, binding.itemImage4)
    var callback: Callback? = null
    val shareButton get() = binding.itemShareButton
    val listTitle get() = binding.itemTitle
    val previewSaveButton get() = binding.itemPreviewSaveButton

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(0, DimenUtil.roundedDpToPx(16f), 0, DimenUtil.roundedDpToPx(16f))
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
                    menu.menu.findItem(R.id.menu_reading_list_share).isVisible = ReadingListsShareHelper.shareEnabled()
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

        FeedbackUtil.setButtonLongPressToast(binding.itemShareButton, binding.itemOverflowMenu)
    }

    fun setReadingList(readingList: ReadingList, description: Description, selectMode: Boolean = false, newImport: Boolean = false) {
        this.readingList = readingList
        val isDetailView = description == Description.DETAIL
        binding.itemDescription.maxLines = if (isDetailView) Int.MAX_VALUE else resources.getInteger(R.integer.reading_list_description_summary_view_max_lines)
        val text: CharSequence = if (isDetailView) buildStatisticalDetailText(readingList) else buildStatisticalSummaryText(readingList)
        binding.itemReadingListStatisticalDescription.text = text
        binding.itemTitleIndicator.isVisible = newImport
        updateDetails(selectMode)
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

    fun setPreviewMode(isPreview: Boolean) {
        binding.itemPreviewSaveButton.isVisible = isPreview
        binding.itemOverflowMenu.isVisible = !isPreview
        binding.itemReadingListStatisticalDescription.visibility = if (isPreview) View.GONE else View.VISIBLE
        setOnLongClickListener {
            // Ignore onLongClick action
            false
        }
    }

    private fun updateDetails(showCheckBoxes: Boolean) {
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
            binding.itemSelectCheckbox.visibility = if (showCheckBoxes) VISIBLE else GONE
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

    private inner class OverflowMenuClickListener constructor(private val list: ReadingList?) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            BreadCrumbLogEvent.logClick(context, item)
            BreadcrumbLogEvent().logClick(context, item)
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
                R.id.menu_reading_list_export -> {
                    list?.let { ReadingListsExportImportHelper
                        .exportLists(context as BaseActivity, listOf(it)) }
                    return true
                }
                R.id.menu_reading_list_select -> {
                    list?.let { callback?.onSelectList(it) }
                    return true
                }
                R.id.menu_reading_list_share -> {
                    list?.let { callback?.onShare(it) }
                    return true
                }
                else -> return false
            }
        }
    }
}
