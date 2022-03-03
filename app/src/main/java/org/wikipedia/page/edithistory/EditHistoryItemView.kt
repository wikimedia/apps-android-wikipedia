package org.wikipedia.page.edithistory

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.databinding.ItemEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class EditHistoryItemView(context: Context) : FrameLayout(context) {

    private val binding = ItemEditHistoryBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        binding.editHistoryTitle.text = itemRevision.comment.ifEmpty { context.getString(R.string.page_edit_history_comment_placeholder) }
        binding.editHistoryTitle.text = if (itemRevision.minor) StringUtil.fromHtml(context.getString(R.string.page_edit_history_minor_edit, binding.editHistoryTitle.text))
        else binding.editHistoryTitle.text
        binding.userNameText.text = itemRevision.user
        binding.editHistoryTimeText.text = DateUtil.getTimeString(DateUtil.iso8601DateParse(itemRevision.timeStamp))
    }
}
