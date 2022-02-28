package org.wikipedia.page.edithistory

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
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
        binding.userNameText.setOnClickListener {
            // TODO
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
        binding.userNameText.setIconResource(if (itemRevision.isAnon) R.drawable.ic_anonymous_ooui else R.drawable.ic_user_avatar)

        if (itemRevision.comment.isEmpty()) {
            binding.editHistoryTitle.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
            binding.editHistoryTitle.setTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color))
            binding.editHistoryTitle.text = context.getString(R.string.page_edit_history_comment_placeholder)
        } else {
            binding.editHistoryTitle.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            binding.editHistoryTitle.setTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_primary_color))
            binding.editHistoryTitle.text = if (itemRevision.minor) StringUtil.fromHtml(context.getString(R.string.page_edit_history_minor_edit, itemRevision.comment))
            else itemRevision.comment
        }
        binding.userNameText.text = itemRevision.user
        binding.editHistoryTimeText.text = DateUtil.getTimeString(DateUtil.iso8601DateParse(itemRevision.timeStamp))
    }

    fun setSelectedState(selectedState: Int) {
        val colorDefault = ResourceUtil.getThemedColor(context, R.attr.paper_color)
        val colorButtonDefault = ResourceUtil.getThemedColor(context, R.attr.color_group_22)
        val colorSecondary = ResourceUtil.getThemedColor(context, R.attr.material_theme_secondary_color)
        val colorFrom = ResourceUtil.getThemedColor(context, R.attr.colorAccent)
        val colorTo = ResourceUtil.getThemedColor(context, R.attr.color_group_68)
        binding.selectButton.isVisible = selectedState != EditHistoryListViewModel.SELECT_INACTIVE

        if (selectedState == EditHistoryListViewModel.SELECT_INACTIVE ||
                selectedState == EditHistoryListViewModel.SELECT_NONE) {
            binding.selectButton.setImageResource(R.drawable.ic_check_empty_24)
            ImageViewCompat.setImageTintList(binding.selectButton, ColorStateList.valueOf(colorSecondary))
            binding.cardView.setDefaultBorder()
            binding.cardView.setCardBackgroundColor(colorDefault)
            binding.diffText.backgroundTintList = ColorStateList.valueOf(colorButtonDefault)
            binding.userNameText.backgroundTintList = ColorStateList.valueOf(colorButtonDefault)
            binding.userNameText.setTextColor(colorSecondary)
            binding.userNameText.iconTint = ColorStateList.valueOf(colorSecondary)
            binding.editHistoryTimeText.setTextColor(colorSecondary)
        } else if (selectedState == EditHistoryListViewModel.SELECT_FROM) {
            binding.selectButton.setImageResource(R.drawable.ic_check_circle_black_24dp)
            ImageViewCompat.setImageTintList(binding.selectButton, ColorStateList.valueOf(colorFrom))
            binding.cardView.strokeColor = colorFrom
            val cardBackground = ColorUtils.blendARGB(colorDefault, colorFrom, 0.05f)
            binding.cardView.setCardBackgroundColor(cardBackground)
            val buttonBackground = ColorUtils.blendARGB(cardBackground, colorFrom, 0.05f)
            binding.diffText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
            binding.userNameText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
            binding.userNameText.setTextColor(colorFrom)
            binding.userNameText.iconTint = ColorStateList.valueOf(colorFrom)
            binding.editHistoryTimeText.setTextColor(colorFrom)
        } else if (selectedState == EditHistoryListViewModel.SELECT_TO) {
            binding.selectButton.setImageResource(R.drawable.ic_check_circle_black_24dp)
            ImageViewCompat.setImageTintList(binding.selectButton, ColorStateList.valueOf(colorTo))
            binding.cardView.strokeColor = colorTo
            val cardBackground = ColorUtils.blendARGB(colorDefault, colorTo, 0.05f)
            binding.cardView.setCardBackgroundColor(cardBackground)
            val buttonBackground = ColorUtils.blendARGB(cardBackground, colorTo, 0.05f)
            binding.diffText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
            binding.userNameText.backgroundTintList = ColorStateList.valueOf(buttonBackground)
            binding.userNameText.setTextColor(colorTo)
            binding.userNameText.iconTint = ColorStateList.valueOf(colorTo)
            binding.editHistoryTimeText.setTextColor(colorTo)
        }
    }
}
