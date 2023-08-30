package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import org.wikipedia.databinding.FragmentSuggestedEditsVandalismItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.diff.ArticleEditDetailsFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource

class SuggestedEditsVandalismPatrolFragment : SuggestedEditsItemFragment() {
    private var _binding: FragmentSuggestedEditsVandalismItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SuggestedEditsVandalismPatrolViewModel by viewModels { SuggestedEditsVandalismPatrolViewModel.Factory(parent().langFromCode) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsVandalismItemBinding.inflate(inflater, container, false)

        childFragmentManager.beginTransaction()
            .add(binding.suggestedEditsItemRootView.id, ArticleEditDetailsFragment
                .newInstance(PageTitle("", WikiSite.forLanguageCode(parent().langFromCode)), -1, -1, -1, fromRecentEdits = true))
            .commit()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*
        binding.voteGoodButton.setOnClickListener {
            parent().nextPage(this)
        }

        binding.voteNotSureButton.setOnClickListener {
            parent().nextPage(this)
        }
         */

        viewModel.candidateLiveData.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
            } else if (it is Resource.Error) {
            }
        }

        viewModel.rollbackResponse.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
            } else if (it is Resource.Error) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        parent().updateActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun publish() {
        parent().nextPage(this)
    }

    private fun onSuccess() {
        parent().nextPage(this)
    }

    private fun onError(caught: Throwable) {
        FeedbackUtil.showError(requireActivity(), caught)
    }

    override fun publishOutlined(): Boolean {
        return true
    }

    override fun publishEnabled(): Boolean {
        return true
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsVandalismPatrolFragment()
        }
    }
}
