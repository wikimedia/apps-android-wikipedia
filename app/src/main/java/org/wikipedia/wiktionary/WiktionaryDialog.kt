package org.wikipedia.wiktionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.compose.components.AnnotatedHtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

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
                    WiktionaryDialogScreen()
                }
            }
        }
    }

    @Composable
    fun WiktionaryDialogScreen() {
        val uiState = viewModel.uiState.collectAsState().value
        WiktionaryDialogContent(
            title = sanitizeForDialogTitle(viewModel.selectedText),
            showNoDefinitions = uiState is Resource.Error,
            showProgress = uiState is Resource.Loading
        ) {
            if (uiState is Resource.Success) {
                BuildUsageItems(uiState.data)
            }
        }
    }

    @Composable
    fun WiktionaryDialogContent(
        title: String,
        showNoDefinitions: Boolean = false,
        showProgress: Boolean = false,
        definitionsContent: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .heightIn(min = dimensionResource(R.dimen.bottomSheetPeekHeight))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_define),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 12.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = title,
                    color = WikipediaTheme.colors.primaryColor,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                )
            }

            HorizontalDivider(
                color = WikipediaTheme.colors.borderColor,
                thickness = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            )

            if (showNoDefinitions) {
                Text(
                    text = stringResource(R.string.wiktionary_no_definitions_found),
                    color = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                definitionsContent()
            }
        }

        if (showProgress) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 128.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor,
                )
            }
        }
    }

    @Composable
    fun BuildUsageItems(usageList: List<RbDefinition.Usage>) {
        Column {
            usageList.forEach {
                DefinitionList(it)
            }
        }
    }

    @Composable
    fun DefinitionList(usage: RbDefinition.Usage) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = usage.partOfSpeech,
                fontSize = 14.sp,
                color = WikipediaTheme.colors.placeholderColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            var index = 0
            usage.definitions.forEach {
                if (it.definition.isNotEmpty()) {
                    DefinitionWithExamples(
                        definition = it,
                        count = ++index
                    )
                }
            }
        }
    }

    @Composable
    fun DefinitionWithExamples(definition: RbDefinition.Definition, count: Int) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            AnnotatedHtmlText(
                html = StringUtil.fromHtml("$count. ${definition.definition}"),
                onLinkClick = { url ->
                    maybeShowNewDialogForLink(url)
                }
            ) {
                SelectionContainer {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = WikipediaTheme.colors.primaryColor,
                        lineHeight = 14.sp * 1.3f,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            definition.examples?.forEach { example ->
                AnnotatedHtmlText(
                    html = StringUtil.fromHtml(example),
                    onLinkClick = { url ->
                        maybeShowNewDialogForLink(url)
                    }
                ) {
                    SelectionContainer {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = WikipediaTheme.colors.primaryColor,
                            lineHeight = 14.sp * 1.3f,
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }

    @Preview
    @Composable
    fun WiktionaryDialogPreview() {
        WiktionaryDialogContent(
            title = "Lorem ipsum",
            showNoDefinitions = false,
            showProgress = false
        ) {
            Text(stringResource(R.string.wiktionary_no_definitions_found))
        }
    }

    private fun maybeShowNewDialogForLink(url: String) {
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
