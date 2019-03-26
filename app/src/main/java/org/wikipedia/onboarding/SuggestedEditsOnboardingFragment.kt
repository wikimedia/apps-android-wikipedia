package org.wikipedia.onboarding


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_suggested_edits.*
import org.wikipedia.R
import org.wikipedia.editactionfeed.EditTasksActivity
import org.wikipedia.settings.Prefs


class SuggestedEditsOnboardingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_suggested_edits, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Prefs.setShowEditTasksOnboarding(false)
        getStartedButton.setOnClickListener {
            startActivity(EditTasksActivity.newIntent(requireActivity()));
        }
    }

    companion object {
        fun newInstance(): SuggestedEditsOnboardingFragment {
            return SuggestedEditsOnboardingFragment()
        }
    }
}
