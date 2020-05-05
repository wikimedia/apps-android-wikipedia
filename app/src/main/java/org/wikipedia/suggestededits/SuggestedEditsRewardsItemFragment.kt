package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import org.wikipedia.R
import org.wikipedia.util.DateUtil
import org.wikipedia.util.log.L

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
        // TODO: add progressbar?
        disposables.add(SuggestedEditsUserStats.getEditCountsObservable()
                .subscribe({ response ->
                    val editorTaskCounts = response.query()!!.editorTaskCounts()!!

                    // TODO: see setGoodnessState() for edit quality rates
                    SuggestedEditsUserStats.getRevertSeverity()


//                    Contributions:
//                    Starting at the 5th contribution, shown after every 50th additional contribution, (5, 55, 105 and so forth)
//                    Edit streak:
//                    Shown when on an edit streak on every 5th day (5, 10, 15 and so forth)
//                    Edit quality:
//                    Shown every 14 days when revert rate is “Perfect“, “Excellent“, “Very good“ or “Good“, when user has actively contributed in the past 14 days (at least one edit)
//                    Page views:
//                    Shown once a month when user has actively contributed in the past 30 days (at least one edit)

                }, { t ->
                    L.e(t)
                    // TODO: add errorView?
                }))
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsRewardsItemFragment()
        }
    }
}
