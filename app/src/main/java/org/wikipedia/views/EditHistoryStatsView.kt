package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewEditHistoryStatsBinding
import org.wikipedia.page.edit_history.EditHistoryListViewModel
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil

class EditHistoryStatsView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewEditHistoryStatsBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val padding = DimenUtil.roundedDpToPx(16f)
        setPadding(padding, 0, padding, 0)
    }

    fun setup(title: String, editStats: EditHistoryListViewModel.EditStats) {
        binding.articleTitleView.text = StringUtil.fromHtml(title)
        val timestamp = editStats.revision.timeStamp
        if (timestamp.isNotBlank()) {
            val createdYear = DateUtil.getYearOnlyDateString(DateUtil.iso8601DateParse(timestamp))
            binding.editCountsView.text = context.getString(R.string.page_edit_history_article_created_date, editStats.editCount.count, createdYear)
            binding.statsGraphView.setData(editStats.metrics.map { it.edits.toFloat() })
        }
    }
}
