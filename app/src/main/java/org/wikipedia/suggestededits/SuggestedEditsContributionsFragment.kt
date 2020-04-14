package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wikipedia.R

class SuggestedEditsContributionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView: View = inflater.inflate(R.layout.fragment_contributions_suggested_edits, container, false)
        return rootView
    }

    companion object {
        fun newInstance(): SuggestedEditsContributionsFragment {
            return SuggestedEditsContributionsFragment()
        }
    }
}
