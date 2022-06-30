package org.wikipedia.usercontrib

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewUserContribStatsBinding
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil

class UserContribStatsView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewUserContribStatsBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val padding = DimenUtil.roundedDpToPx(16f)
        setPadding(padding, 0, padding, 0)
    }

    fun setup(userName: String, stats: UserContribListViewModel.UserContribStats) {
        binding.userNameView.text = StringUtil.fromHtml(context.getString(R.string.user_contrib_activity_title,
                "<a href=\"#\">$userName</a>"))
        RichTextUtil.removeUnderlinesFromLinks(binding.userNameView)

        val regYear = DateUtil.getYearOnlyDateString(stats.registrationDate)
        binding.editCountsView.text = context.resources.getQuantityString(R.plurals.page_edit_history_article_edits_since_year,
                stats.totalEdits, stats.totalEdits, regYear)

        binding.userNameView.movementMethod = LinkMovementMethodExt { _ ->
            // context.startActivity(PageActivity.newIntentForNewTab(context, HistoryEntry(pageTitle, HistoryEntry.SOURCE_EDIT_HISTORY), pageTitle))
        }
    }
}
