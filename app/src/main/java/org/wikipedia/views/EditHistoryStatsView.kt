package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewEditHistoryStatsBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.edithistory.EditHistoryListViewModel
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import java.util.*

class EditHistoryStatsView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewEditHistoryStatsBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val padding = DimenUtil.roundedDpToPx(16f)
        setPadding(padding, 0, padding, 0)
    }

    fun setup(pageTitle: PageTitle, editHistoryStats: EditHistoryListViewModel.EditHistoryStats) {
        binding.articleTitleView.text = StringUtil.fromHtml(context.getString(R.string.page_edit_history_activity_title,
                "<a href=\"#\">${pageTitle.displayText}</a>"))
        RichTextUtil.removeUnderlinesFromLinks(binding.articleTitleView)
        val timestamp = editHistoryStats.revision.timeStamp
        if (timestamp.isNotBlank()) {
            val createdYear = DateUtil.getYearOnlyDateString(DateUtil.iso8601DateParse(timestamp))
            val calendar = Calendar.getInstance()
            val today = DateUtil.getMDYDateString(calendar.time)
            calendar.add(Calendar.YEAR, -1)
            val lastYear = DateUtil.getMDYDateString(calendar.time)
            binding.editCountsView.text = context.resources.getQuantityString(R.plurals.page_edit_history_article_edits_since_year,
                    editHistoryStats.editCount.count, editHistoryStats.editCount.count, createdYear)
            binding.statsGraphView.setData(editHistoryStats.metrics.map { it.edits.toFloat() })
            binding.statsGraphView.contentDescription = context.getString(R.string.page_edit_history_metrics_content_description, today, lastYear)
            FeedbackUtil.setButtonLongPressToast(binding.statsGraphView)

            binding.articleTitleView.movementMethod = LinkMovementMethodExt { _ ->
                context.startActivity(PageActivity.newIntentForNewTab(context, HistoryEntry(pageTitle, HistoryEntry.SOURCE_EDIT_HISTORY), pageTitle))
            }
        }
    }
}
