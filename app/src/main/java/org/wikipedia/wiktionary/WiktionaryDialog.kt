package org.wikipedia.wiktionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil

class WiktionaryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun wiktionaryShowDialogForTerm(term: String)
    }

    private val viewModel: WiktionaryViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            L10nUtil.setConditionalLayoutDirection(this, viewModel.pageTitle.wikiSite.languageCode)
            setContent {
                BaseTheme {
                    WiktionaryDialogScreen(viewModel) {
                        maybeShowNewDialogForLink(it)
                    }
                }
            }
        }
    }

    private fun maybeShowNewDialogForLink(url: String) {
        if (url.startsWith(PATH_WIKI) || url.startsWith(PATH_CURRENT)) {
            dismiss()
            showNewDialogForLink(url)
        }
    }

    private fun showNewDialogForLink(url: String) {
        callback()?.wiktionaryShowDialogForTerm(viewModel.getTermFromWikiLink(url))
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val PATH_WIKI = "/wiki/"
        private const val PATH_CURRENT = "./"
        const val WIKTIONARY_DOMAIN = ".wiktionary.org"

        val enabledLanguages = listOf("en")

        fun newInstance(title: PageTitle, selectedText: String): WiktionaryDialog {
            return WiktionaryDialog().apply {
                arguments = bundleOf(Constants.ARG_TITLE to title, Constants.ARG_TEXT to selectedText)
            }
        }
    }
}
