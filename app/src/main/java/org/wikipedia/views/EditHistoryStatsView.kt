package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewEditHistoryStatsBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil

class EditHistoryStatsView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewEditHistoryStatsBinding.inflate(LayoutInflater.from(context), this)

    init {
        val padding = DimenUtil.roundedDpToPx(16f)
        setPadding(padding, 0, padding, 0)
    }

    fun setup(title: String, pair: Pair<MwQueryPage.Revision, EditCount>) {
        binding.articleTitleView.text = title
        if (pair.first.timeStamp.isNotBlank()) {
            val createdYear = DateUtil.getYearOnlyDateString(DateUtil.iso8601DateParse(pair.first.timeStamp))
            binding.editCountsView.text = context.getString(R.string.page_edit_history_article_created_date, pair.second.count.toString(), createdYear)
        }
    }
}
