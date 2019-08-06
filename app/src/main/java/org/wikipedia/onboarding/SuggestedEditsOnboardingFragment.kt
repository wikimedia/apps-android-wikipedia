package org.wikipedia.onboarding


import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_suggested_edits_onboarding.*
import org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.SuggestedEditsTasksActivity


class SuggestedEditsOnboardingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_suggested_edits_onboarding, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val invokeSource = arguments?.getSerializable(INTENT_EXTRA_INVOKE_SOURCE)
        Prefs.setShowEditTasksOnboarding(false)
        getStartedButton.setOnClickListener {
            if (invokeSource == FEED_CARD_SUGGESTED_EDITS_ADD_DESC || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC) {
                requireActivity().setResult(Activity.RESULT_OK)
            } else {
                startActivity(SuggestedEditsTasksActivity.newIntent(requireActivity(), SUGGESTED_EDITS_ONBOARDING))
            }
            requireActivity().finish()
        }
    }

    companion object {
        fun newInstance(invokeSource: InvokeSource): SuggestedEditsOnboardingFragment {
            val suggestedEditsOnboardingFragment = SuggestedEditsOnboardingFragment()
            val args = Bundle()
            args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            suggestedEditsOnboardingFragment.arguments = args
            return suggestedEditsOnboardingFragment
        }
    }
}
