package org.wikipedia.random

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentRandomItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L

class RandomItemFragment : Fragment() {

    private var _binding: FragmentRandomItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomItemViewModel by viewModels { RandomItemViewModel.Factory(requireArguments()) }

    val isLoadComplete: Boolean get() = viewModel.summary != null
    val title: PageTitle? get() = viewModel.summary?.getPageTitle(viewModel.wikiSite)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRandomItemBinding.inflate(inflater, container, false)

        binding.randomItemWikiArticleCardView.setOnClickListener {
            title?.let { title ->
                parent().onSelectPage(title, binding.randomItemWikiArticleCardView.getSharedElements())
            }
        }

        binding.randomItemErrorView.backClickListener = View.OnClickListener {
            requireActivity().finish()
        }

        binding.randomItemErrorView.retryClickListener = View.OnClickListener {
            binding.randomItemProgress.visibility = View.VISIBLE
            viewModel.getRandomPage()
        }

        L10nUtil.setConditionalLayoutDirection(binding.root, viewModel.wikiSite.languageCode)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is Resource.Loading -> {
                            binding.randomItemProgress.isVisible = true
                        }
                        is Resource.Success -> updateContents(it.data)
                        is Resource.Error -> setErrorState(it.throwable)
                    }
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.randomItemErrorView.setError(t)
        binding.randomItemErrorView.isVisible = true
        binding.randomItemProgress.isVisible = false
        binding.randomItemWikiArticleCardView.isVisible = false
    }

    private fun updateContents(summary: PageSummary?) {
        binding.randomItemErrorView.isVisible = false
        binding.randomItemProgress.isVisible = false
        binding.randomItemWikiArticleCardView.isVisible = summary != null
        summary?.run {
            binding.randomItemWikiArticleCardView.setTitle(displayTitle)
            binding.randomItemWikiArticleCardView.setDescription(description)
            binding.randomItemWikiArticleCardView.setExtract(extract, EXTRACT_MAX_LINES)

            var imageUri: Uri? = null

            thumbnailUrl.takeUnless { it.isNullOrBlank() }?.let { thumbnailUrl ->
                imageUri = Uri.parse(getUrlForPreferredSize(thumbnailUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE))
            }
            binding.randomItemWikiArticleCardView.setImageUri(imageUri, false)
        }
        parent().onChildLoaded()
    }

    private fun parent(): RandomFragment {
        return requireActivity().supportFragmentManager.fragments[0] as RandomFragment
    }

    companion object {
        private const val EXTRACT_MAX_LINES = 4

        fun newInstance(wikiSite: WikiSite) = RandomItemFragment().apply {
            arguments = bundleOf(Constants.ARG_WIKISITE to wikiSite)
        }
    }
}
