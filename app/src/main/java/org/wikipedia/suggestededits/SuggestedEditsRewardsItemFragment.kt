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
        // TODO: add logic here
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsRewardsItemFragment()
        }
    }
}
