package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentSuggestedEditsVandalismItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.diff.ArticleEditDetailsActivity
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

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                add<ArticleEditDetailsFragment>(binding.suggestedEditsItemRootView.id, args = bundleOf(
                    ArticleEditDetailsActivity.EXTRA_ARTICLE_TITLE to PageTitle("", WikiSite.forLanguageCode(targetWikiLangCode)),
                    Constants.INTENT_EXTRA_INVOKE_SOURCE to Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS,
                ))
            }
        }
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
