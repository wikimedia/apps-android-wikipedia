package org.wikipedia.onboarding

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_suggested_edits_onboarding.*
import org.wikipedia.R
import org.wikipedia.settings.Prefs

class SuggestedEditsOnboardingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_suggested_edits_onboarding, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Prefs.setShowEditTasksOnboarding(false)
        getStartedButton.setOnClickListener {
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsOnboardingFragment {
            return SuggestedEditsOnboardingFragment()
        }
    }
}
