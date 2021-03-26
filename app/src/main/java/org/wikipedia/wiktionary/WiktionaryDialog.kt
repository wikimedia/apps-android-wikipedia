package org.wikipedia.wiktionary

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.WiktionaryDialogFunnel
import org.wikipedia.databinding.DialogWiktionaryBinding
import org.wikipedia.databinding.ItemWiktionaryDefinitionWithExamplesBinding
import org.wikipedia.databinding.ItemWiktionaryDefinitionsListBinding
import org.wikipedia.databinding.ItemWiktionaryExampleBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.dataclient.restbase.RbDefinition.Usage
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class WiktionaryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun wiktionaryShowDialogForTerm(term: String)
    }

    private var _binding: DialogWiktionaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageTitle: PageTitle
    private lateinit var selectedText: String
    private var currentDefinition: RbDefinition? = null
    private var funnel: WiktionaryDialogFunnel? = null
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = requireArguments().getParcelable(TITLE)!!
        selectedText = requireArguments().getString(SELECTED_TEXT)!!
    }

    override fun onDestroyView() {
        disposables.clear()
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogWiktionaryBinding.inflate(inflater, container, false)
        binding.wiktionaryDefinitionDialogTitle.text = sanitizeForDialogTitle(selectedText)
        L10nUtil.setConditionalLayoutDirection(binding.root, pageTitle.wikiSite.languageCode())
        loadDefinitions()
        funnel = WiktionaryDialogFunnel(WikipediaApp.getInstance(), selectedText)
        return binding.root
    }

    override fun onDismiss(dialogInterface: DialogInterface) {
        super.onDismiss(dialogInterface)
        funnel?.logClose()
    }

    private fun loadDefinitions() {
        if (selectedText.trim().isEmpty()) {
            displayNoDefinitionsFound()
            return
        }

        // TODO: centralize the Wiktionary domain better. Maybe a SharedPreference that defaults to
        disposables.add(ServiceFactory.getRest(WikiSite(pageTitle.wikiSite.subdomain() + WIKTIONARY_DOMAIN)).getDefinition(StringUtil.addUnderscores(selectedText))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { usages -> RbDefinition(usages) }
                .subscribe({ definition ->
                    binding.dialogWiktionaryProgress.visibility = View.GONE
                    currentDefinition = definition
                    layOutDefinitionsByUsage()
                }) { throwable ->
                    displayNoDefinitionsFound()
                    L.e(throwable)
                })
    }

    private fun displayNoDefinitionsFound() {
        binding.wiktionaryNoDefinitionsFound.visibility = View.VISIBLE
        binding.dialogWiktionaryProgress.visibility = View.GONE
    }

    private fun layOutDefinitionsByUsage() {
        currentDefinition?.getUsagesForLang("en").let { usageList ->
            if (usageList.isNullOrEmpty()) {
                displayNoDefinitionsFound()
                return
            }
            usageList.forEach {
                binding.wiktionaryDefinitionsByPartOfSpeech.addView(layOutUsage(it))
            }
        }
    }

    private fun layOutUsage(currentUsage: Usage): View {
        val usageBinding = ItemWiktionaryDefinitionsListBinding.inflate(LayoutInflater.from(context), binding.root, false)
        usageBinding.wiktionaryPartOfSpeech.text = currentUsage.partOfSpeech
        for (i in currentUsage.definitions.indices) {
            usageBinding.listWiktionaryDefinitionsWithExamples.addView(layOutDefinitionWithExamples(currentUsage.definitions[i], i + 1))
        }
        return usageBinding.root
    }

    private fun layOutDefinitionWithExamples(currentDefinition: RbDefinition.Definition, count: Int): View {
        val definitionBinding = ItemWiktionaryDefinitionWithExamplesBinding.inflate(LayoutInflater.from(context), binding.root, false)
        val definitionWithCount = "$count. ${currentDefinition.definition}"
        definitionBinding.wiktionaryDefinition.text = StringUtil.fromHtml(definitionWithCount)
        definitionBinding.wiktionaryDefinition.movementMethod = linkMovementMethod
        currentDefinition.examples?.forEach {
            definitionBinding.wiktionaryExamples.addView(layoutExamples(it))
        }
        return definitionBinding.root
    }

    private fun layoutExamples(example: String): View {
        val exampleBinding = ItemWiktionaryExampleBinding.inflate(LayoutInflater.from(context), binding.root, false)
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
        return removeLinkFragment(url.substring(url.lastIndexOf("/") + 1))
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
        private const val WIKTIONARY_DOMAIN = ".wiktionary.org"
        private const val TITLE = "title"
        private const val SELECTED_TEXT = "selected_text"
        private const val PATH_WIKI = "/wiki/"
        private const val PATH_CURRENT = "./"

        // Try to get the correct definition from glossary terms: https://en.wiktionary.org/wiki/Appendix:Glossary
        private const val GLOSSARY_OF_TERMS = ":Glossary"

        val enabledLanguages = listOf("en")

        fun newInstance(title: PageTitle, selectedText: String): WiktionaryDialog {
            return WiktionaryDialog().apply {
                arguments = bundleOf(TITLE to title, SELECTED_TEXT to selectedText)
            }
        }
    }
}
