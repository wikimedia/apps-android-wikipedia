package org.wikipedia.page.edithistory

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ItemEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class EditHistoryItemView(context: Context) : FrameLayout(context) {
    interface Listener {
        fun onClick()
        fun onToggleSelect()
    }

    var listener: Listener? = null
    private val binding = ItemEditHistoryBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.clickTargetView.setOnClickListener {
            listener?.onClick()
        }
        binding.selectButton.setOnClickListener {
            listener?.onToggleSelect()
        }
    }

    fun setContents(itemRevision: Revision) {
        val diffSize = itemRevision.diffSize
        binding.diffText.text = String.format(if (diffSize != 0) "%+d" else "%d", diffSize)
        if (diffSize >= 0) {
            binding.diffText.setTextColor(if (diffSize > 0) ContextCompat.getColor(context, R.color.green50)
            else ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color))
        } else {
            binding.diffText.setTextColor(ContextCompat.getColor(context, R.color.red50))
        }
        binding.editHistoryTitle.text = itemRevision.comment.ifEmpty { context.getString(R.string.page_edit_history_comment_placeholder) }
        binding.editHistoryTitle.text = if (itemRevision.minor) StringUtil.fromHtml(context.getString(R.string.page_edit_history_minor_edit, binding.editHistoryTitle.text))
        else binding.editHistoryTitle.text
        binding.userNameText.text = itemRevision.user
        binding.editHistoryTimeText.text = DateUtil.getTimeString(DateUtil.iso8601DateParse(itemRevision.timeStamp))
    }

    fun setSelectedState(selectedState: Int) {
        if (selectedState == EditHistoryListViewModel.SELECT_INACTIVE) {
            binding.selectButton.isVisible = false
            binding.cardView.setDefaultBorder()
            return
        }

        binding.selectButton.isVisible = true

        val colorDefault = ResourceUtil.getThemedColor(context, R.attr.paper_color)
        val colorFrom = ResourceUtil.getThemedColor(context, R.attr.colorAccent)
        val colorTo = ResourceUtil.getThemedColor(context, R.attr.color_group_68)

        when (selectedState) {
            EditHistoryListViewModel.SELECT_FROM -> {
                binding.selectButton.setImageResource(R.drawable.ic_check_circle_black_24dp)
                ImageViewCompat.setImageTintList(binding.selectButton, ColorStateList.valueOf(colorFrom))
                binding.cardView.strokeColor = colorFrom
                val cardBackground = ColorUtils.blendARGB(colorDefault, colorFrom, 0.05f)
                binding.cardView.setCardBackgroundColor(cardBackground)
                val buttonBackground = ColorUtils.blendARGB(cardBackground, colorFrom, 0.05f)
                binding.diffText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
                binding.userNameText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
            }
            EditHistoryListViewModel.SELECT_TO -> {
                binding.selectButton.setImageResource(R.drawable.ic_check_circle_black_24dp)
                ImageViewCompat.setImageTintList(binding.selectButton, ColorStateList.valueOf(colorTo))
                binding.cardView.strokeColor = colorTo
                val cardBackground = ColorUtils.blendARGB(colorDefault, colorTo, 0.05f)
                binding.cardView.setCardBackgroundColor(cardBackground)
                val buttonBackground = ColorUtils.blendARGB(cardBackground, colorTo, 0.05f)
                binding.diffText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
                binding.userNameText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
            }
            EditHistoryListViewModel.SELECT_NONE -> {
                binding.selectButton.setImageResource(R.drawable.ic_check_empty_24)
                ImageViewCompat.setImageTintList(binding.selectButton, ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color)))
                binding.cardView.setDefaultBorder()
                binding.cardView.setCardBackgroundColor(colorDefault)
            }
        }
    }
}
