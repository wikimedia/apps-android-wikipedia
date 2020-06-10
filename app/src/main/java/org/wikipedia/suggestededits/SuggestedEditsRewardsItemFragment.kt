package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_suggested_edits_rewards_item.*
import org.wikipedia.R

class SuggestedEditsRewardsItemFragment : SuggestedEditsItemFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_rewards_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rewardImage.setImageResource(requireArguments().getInt(ARG_IMAGE_RESOURCE))
        rewardText.text = requireArguments().getString(ARG_TEXT)
    }

    companion object {
        private const val ARG_IMAGE_RESOURCE = "imageResource"
        private const val ARG_TEXT = "text"
        const val EDIT_STREAK_MAX_REVERT_SEVERITY = 3

        fun newInstance(imageResource: Int, text: String): SuggestedEditsItemFragment {
            val fragment = SuggestedEditsRewardsItemFragment()
            val arguments = Bundle()
            arguments.putInt(ARG_IMAGE_RESOURCE, imageResource)
            arguments.putString(ARG_TEXT, text)
            fragment.arguments = arguments
            return fragment
        }
    }
}
