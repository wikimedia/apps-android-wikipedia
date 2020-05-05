package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wikipedia.R

class SuggestedEditsRewardsItemFragment : SuggestedEditsItemFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_rewards_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUserContribution()
    }

    private fun fetchUserContribution() {
        // add endpoint loading
    }
//    enum class TYPE(val image: Int, val text: Int) {
//        CONTRIBUTION(R.drawable.ic_illustration_heart, R.string.suggested_edits_rewards_contribution),
//        PAGEVIEW(R.drawable.ic_illustration_views, R.string.suggested_edits_rewards_pageviews),
//        EDIT_STREAK(R.drawable.ic_illustration_calendar, R.string.suggested_edits_rewards_edit_streak),
//        EDIT_QUALITY_PERFECT(R.drawable.ic_illustration_quality_perfect, R.string.suggested_edits_rewards_edit_quality),
//        EDIT_QUALITY_EXCELLENT(R.drawable.ic_illustration_quality_excellent, R.string.suggested_edits_rewards_edit_quality),
//        EDIT_QUALITY_VERY_GOOD(R.drawable.ic_illustration_quality_very_good, R.string.suggested_edits_rewards_edit_quality),
//        EDIT_QUALITY_GOOD(R.drawable.ic_illustration_quality_good, R.string.suggested_edits_rewards_edit_quality)
//    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsRewardsItemFragment()
        }
    }
}
