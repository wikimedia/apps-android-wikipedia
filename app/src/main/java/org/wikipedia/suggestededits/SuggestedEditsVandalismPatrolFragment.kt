package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wikipedia.databinding.FragmentSuggestedEditsVandalismItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.diff.ArticleEditDetailsFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs

class SuggestedEditsVandalismPatrolFragment : SuggestedEditsItemFragment(), ArticleEditDetailsFragment.Callback {
    private var _binding: FragmentSuggestedEditsVandalismItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsVandalismItemBinding.inflate(inflater, container, false)

        val targetWikiLangCode = Prefs.recentEditsWikiCode

        childFragmentManager.beginTransaction()
            .add(binding.suggestedEditsItemRootView.id, ArticleEditDetailsFragment
                .newInstance(PageTitle("", WikiSite.forLanguageCode(targetWikiLangCode)), -1, -1, -1, fromRecentEdits = true))
            .commit()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onUndoSuccess() {
        publish()
    }

    override fun onRollbackSuccess() {
        publish()
    }

    override fun publish() {
        parent().nextPage(this)
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsVandalismPatrolFragment()
        }
    }
}
