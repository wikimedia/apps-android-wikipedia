package org.wikipedia.usercontrib

import android.content.Context
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewUserContribStatsBinding
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil

class UserContribStatsView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewUserContribStatsBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val padding = DimenUtil.roundedDpToPx(16f)
        setPadding(padding, 0, padding, 0)
    }

    fun setup(userName: String, stats: UserContribListViewModel.UserContribStats, movementMethod: MovementMethod, userPageTitle: PageTitle) {
        binding.userNameView.text = StringUtil.fromHtml(context.getString(R.string.user_contrib_activity_title,
                "<a href='" + userPageTitle.uri + "'>$userName</a>"))
        binding.userNameView.movementMethod = movementMethod

        if (stats.totalEdits >= 0) {
            val regYear = stats.registrationDate.year.toString()
            binding.editCountsView.isVisible = true
            binding.editCountsView.text = context.resources.getQuantityString(R.plurals.edits_since_year_per_wiki,
                    stats.totalEdits, stats.totalEdits, regYear, stats.projectName)
        } else {
            binding.editCountsView.isVisible = false
        }
    }
}
