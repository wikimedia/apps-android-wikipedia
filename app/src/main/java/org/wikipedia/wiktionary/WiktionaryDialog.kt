package org.wikipedia.wiktionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogWiktionaryBinding
import org.wikipedia.databinding.ItemWiktionaryDefinitionWithExamplesBinding
import org.wikipedia.databinding.ItemWiktionaryDefinitionsListBinding
import org.wikipedia.databinding.ItemWiktionaryExampleBinding
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.dataclient.restbase.RbDefinition.Usage
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class WiktionaryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun wiktionaryShowDialogForTerm(term: String)
    }

    private var _binding: DialogWiktionaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WiktionaryViewModel by viewModels { WiktionaryViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogWiktionaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.wiktionaryDefinitionDialogTitle.text = sanitizeForDialogTitle(viewModel.selectedText)
        L10nUtil.setConditionalLayoutDirection(binding.root, viewModel.pageTitle.wikiSite.languageCode)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> onSuccess(it.data)
                            is Resource.Error -> onError()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun onLoading() {
        binding.wiktionaryNoDefinitionsFound.visibility = View.GONE
        binding.dialogWiktionaryProgress.visibility = View.VISIBLE
    }

    private fun onError() {
        binding.wiktionaryNoDefinitionsFound.visibility = View.VISIBLE
        binding.dialogWiktionaryProgress.visibility = View.GONE
    }

    private fun onSuccess(usageList: List<Usage>) {
        binding.wiktionaryNoDefinitionsFound.visibility = View.GONE
        binding.dialogWiktionaryProgress.visibility = View.GONE
        usageList.forEach {
            binding.wiktionaryDefinitionsByPartOfSpeech.addView(layOutUsage(it))
        }
    }

    private fun layOutUsage(currentUsage: Usage): View {
        val usageBinding = ItemWiktionaryDefinitionsListBinding.inflate(layoutInflater, binding.root, false)
        usageBinding.wiktionaryPartOfSpeech.text = currentUsage.partOfSpeech
        for (i in currentUsage.definitions.indices) {
            usageBinding.listWiktionaryDefinitionsWithExamples.addView(layOutDefinitionWithExamples(currentUsage.definitions[i], i + 1))
        }
        return usageBinding.root
    }

    private fun layOutDefinitionWithExamples(currentDefinition: RbDefinition.Definition, count: Int): View {
        val definitionBinding = ItemWiktionaryDefinitionWithExamplesBinding.inflate(layoutInflater, binding.root, false)
        val definitionWithCount = "$count. ${currentDefinition.definition}"
        definitionBinding.wiktionaryDefinition.text = StringUtil.fromHtml(definitionWithCount)
        definitionBinding.wiktionaryDefinition.movementMethod = linkMovementMethod
        currentDefinition.examples?.forEach {
            definitionBinding.wiktionaryExamples.addView(layoutExamples(it))
        }
        return definitionBinding.root
    }

    private fun layoutExamples(example: String): View {
        val exampleBinding = ItemWiktionaryExampleBinding.inflate(layoutInflater, binding.root, false)
        exampleBinding.itemWiktionaryExample.text = StringUtil.fromHtml(example)
        exampleBinding.itemWiktionaryExample.movementMethod = linkMovementMethod
        return exampleBinding.root
    }

    private val linkMovementMethod = LinkMovementMethodExt { url: String ->
        if (url.startsWith(PATH_WIKI) || url.startsWith(PATH_CURRENT)) {
            dismiss()
            showNewDialogForLink(url)
        }
    }

    private fun getTermFromWikiLink(url: String): String {
        return removeLinkFragment(url.substringAfterLast('/'))
    }

    private fun removeLinkFragment(url: String): String {
        val splitUrl = url.split('#')
        return if (splitUrl[0].endsWith(GLOSSARY_OF_TERMS) && splitUrl.size > 1) splitUrl[1] else splitUrl[0]
    }

    private fun showNewDialogForLink(url: String) {
        callback()?.wiktionaryShowDialogForTerm(getTermFromWikiLink(url))
    }

    private fun sanitizeForDialogTitle(text: String?): String {
        return StringUtil.removeUnderscores(StringUtil.removeSectionAnchor(text))
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val PATH_WIKI = "/wiki/"
        private const val PATH_CURRENT = "./"
        const val WIKTIONARY_DOMAIN = ".wiktionary.org"

        // Try to get the correct definition from glossary terms: https://en.wiktionary.org/wiki/Appendix:Glossary
        private const val GLOSSARY_OF_TERMS = ":Glossary"

        val enabledLanguages = listOf("en")

        fun newInstance(title: PageTitle, selectedText: String): WiktionaryDialog {
            return WiktionaryDialog().apply {
                arguments = bundleOf(Constants.ARG_TITLE to title, Constants.ARG_TEXT to selectedText)
            }
        }
    }
}
