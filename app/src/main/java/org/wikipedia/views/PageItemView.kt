package org.wikipedia.views

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.google.android.material.chip.Chip
import org.wikipedia.R
import org.wikipedia.databinding.ItemPageListEntryBinding
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DeviceUtil.setContextClickAsLongClick
import org.wikipedia.util.DimenUtil.roundedDpToPx
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil.getThemedAttributeId
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.StringUtil.boldenKeywordText
import org.wikipedia.util.StringUtil.fromHtml

/*
 * TODO: Use this for future RecyclerView updates where we show a list of pages
 * (e.g. History, Search, Disambiguation)
 */
class PageItemView<T>(context: Context) : ConstraintLayout(context) {
    interface Callback<T> {
        fun onClick(item: T?)
        fun onLongClick(item: T?): Boolean
        fun onThumbClick(item: T?)
        fun onActionClick(item: T?, view: View)
        fun onListChipClick(readingList: ReadingList)
    }

    private val binding = ItemPageListEntryBinding.inflate(LayoutInflater.from(context), this)
    private var imageUrl: String? = null
    private var selected = false
    var callback: Callback<T?>? = null
    var item: T? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(0, roundedDpToPx(16f), 0, roundedDpToPx(16f))
        background = AppCompatResources.getDrawable(context, getThemedAttributeId(context, R.attr.selectableItemBackground))
        isFocusable = true
        setOnClickListeners()
        setContextClickAsLongClick(this)
        FeedbackUtil.setButtonLongPressToast(binding.pageListItemAction)
    }

    override fun setSelected(selected: Boolean) {
        if (this.selected != selected) {
            this.selected = selected
            updateSelectedState()
        }
    }

    private fun setOnClickListeners() {
        setOnClickListener {
            callback?.onClick(item)
        }
        setOnLongClickListener {
            callback?.onLongClick(item)
            false
        }
        binding.pageListItemImage.setOnClickListener {
            callback?.onThumbClick(item)
        }
        binding.pageListItemAction.setOnClickListener {
            callback?.onActionClick(item, this)
        }
    }

    private fun updateSelectedState() {
        if (selected) {
            binding.pageListItemSelectedImage.visibility = VISIBLE
            binding.pageListItemImage.visibility = GONE
            setBackgroundColor(getThemedColor(context, R.attr.multi_select_background_color))
        } else {
            binding.pageListItemImage.visibility = if (imageUrl.isNullOrEmpty()) GONE else VISIBLE
            ViewUtil.loadImageWithRoundedCorners(binding.pageListItemImage, imageUrl)
            binding.pageListItemSelectedImage.visibility = GONE
            setBackground(AppCompatResources.getDrawable(context, getThemedAttributeId(context, R.attr.selectableItemBackground)))
        }
    }

    fun setTitle(text: String?) {
        binding.pageListItemTitle.text = fromHtml(text)
    }

    fun setTitleMaxLines(linesCount: Int) {
        binding.pageListItemTitle.maxLines = linesCount
    }

    fun setTitleEllipsis() {
        binding.pageListItemTitle.ellipsize = TextUtils.TruncateAt.END
    }

    fun setDescription(text: CharSequence?) {
        binding.pageListItemDescription.text = text
    }

    fun setDescriptionMaxLines(linesCount: Int) {
        binding.pageListItemDescription.maxLines = linesCount
    }

    fun setDescriptionEllipsis() {
        binding.pageListItemDescription.ellipsize = TextUtils.TruncateAt.END
    }

    fun setImageUrl(url: String?) {
        imageUrl = url
        updateSelectedState()
    }

    fun setSecondaryActionIcon(@DrawableRes id: Int, show: Boolean) {
        binding.pageListItemAction.setImageResource(id)
        binding.pageListItemAction.visibility = if (show) VISIBLE else GONE
        binding.pageListItemActionContainer.visibility = if (show) VISIBLE else GONE
    }

    fun setProgress(progress: Int) {
        binding.pageListItemCircularProgressBar.setCurrentProgress(progress.toDouble())
    }

    fun setCircularProgressVisibility(visible: Boolean) {
        binding.pageListItemCircularProgressBar.visibility = if (visible) VISIBLE else GONE
    }

    fun setActionHint(@StringRes id: Int) {
        binding.pageListItemAction.contentDescription = context.getString(id)
    }

    fun setListItemImageDimensions(width: Int, height: Int) {
        binding.pageListItemImage.layoutParams.width = width
        binding.pageListItemImage.layoutParams.height = height
        requestLayout()
    }

    fun setUpChipGroup(readingLists: List<ReadingList>) {
        binding.chipsScrollview.visibility = VISIBLE
        binding.chipsScrollview.setFadingEdgeLength(0)
        binding.readingListsChipGroup.removeAllViews()
        readingLists.forEach { readingList ->
            val chip = Chip(binding.readingListsChipGroup.context)
            TextViewCompat.setTextAppearance(chip, R.style.CustomChipStyle)
            chip.text = readingList.title
            chip.isClickable = true
            chip.setChipBackgroundColorResource(getThemedAttributeId(context, R.attr.chip_background_color))
            chip.setOnClickListener {
                callback?.onListChipClick(readingList)
            }
            binding.readingListsChipGroup.addView(chip)
        }
    }

    fun hideChipGroup() {
        binding.chipsScrollview.visibility = GONE
    }

    fun setSearchQuery(searchQuery: String?) {
        // highlight search term within the text
        boldenKeywordText(binding.pageListItemTitle, binding.pageListItemTitle.text.toString(), searchQuery)
    }

    fun setViewsGreyedOut(greyedOut: Boolean) {
        val alpha = if (greyedOut) 0.5f else 1.0f
        binding.pageListItemTitle.alpha = alpha
        binding.pageListItemDescription.alpha = alpha
        binding.pageListItemImage.alpha = alpha
    }
}
