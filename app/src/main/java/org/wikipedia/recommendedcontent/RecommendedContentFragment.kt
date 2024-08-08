package org.wikipedia.recommendedcontent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentRecommendedContentBinding
import org.wikipedia.dataclient.WikiSite

class RecommendedContentFragment : Fragment() {
    private var _binding: FragmentRecommendedContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecommendedContentViewModel by viewModels { RecommendedContentViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRecommendedContentBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            // TODO
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_IN_HISTORY = "inHistory"
        const val ARG_SHOW_TABS = "showTabs"

        fun newInstance(wikiSite: WikiSite, inHistory: Boolean, showTabs: Boolean) = RecommendedContentFragment().apply {
            arguments = bundleOf(
                Constants.ARG_WIKISITE to wikiSite,
                ARG_IN_HISTORY to inHistory,
                ARG_SHOW_TABS to showTabs
            )
        }
    }
}
