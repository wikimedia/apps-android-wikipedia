package org.wikipedia.suggestededits

import android.media.RingtoneManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.fragment_suggested_edits_rewards_item.*
import org.wikipedia.R

class SuggestedEditsRewardsItemFragment : SuggestedEditsItemFragment() {

    private var shouldPlaySound = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_rewards_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rewardImage.setImageResource(requireArguments().getInt(ARG_IMAGE_RESOURCE))
        rewardText.text = requireArguments().getString(ARG_TEXT)
    }

    override fun onResume() {
        playVibrationAndSound()
        super.onResume()
    }

    private fun playVibrationAndSound() {
        if (shouldPlaySound) {
            rewardImage.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            try {
                RingtoneManager.getRingtone(requireContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play()
            } catch (exception: Exception) {
                // ignore
            }
            shouldPlaySound = false
        }
    }

    companion object {
        private const val ARG_IMAGE_RESOURCE = "imageResource"
        private const val ARG_TEXT = "text"
        const val EDIT_STREAK_MAX_REVERT_SEVERITY = 3

        fun newInstance(imageResource: Int, text: String): SuggestedEditsItemFragment {
            val fragment = SuggestedEditsRewardsItemFragment()
            fragment.arguments = bundleOf(ARG_IMAGE_RESOURCE to imageResource, ARG_TEXT to text)
            return fragment
        }
    }
}
