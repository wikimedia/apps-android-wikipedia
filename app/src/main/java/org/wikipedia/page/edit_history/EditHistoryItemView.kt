package org.wikipedia.page.edit_history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import org.wikipedia.R
import org.wikipedia.databinding.ItemEditHistoryBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil

class EditHistoryItemView(context: Context) : FrameLayout(context) {

    private val binding = ItemEditHistoryBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var revision: Revision

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setContents(itemRevision: Revision) {
        this.revision = itemRevision
        binding.diffText.text = context.getString(R.string.page_edit_history_item_size_text, itemRevision.size)
        binding.editHistoryTitle.text = itemRevision.comment.ifEmpty { context.getString(R.string.page_edit_history_comment_placeholder) }
        binding.editHistoryTitle.text = if (itemRevision.minor) StringUtil.fromHtml(context.getString(R.string.page_edit_history_minor_edit, binding.editHistoryTitle.text))
        else binding.editHistoryTitle.text
        binding.userNameText.text = itemRevision.user
        binding.editHistoryTimeText.text = DateUtil.getTimeString(DateUtil.iso8601DateParse(itemRevision.timeStamp))
    }
}
