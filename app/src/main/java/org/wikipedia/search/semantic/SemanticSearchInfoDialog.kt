package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.InfoActionScreen
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResult.SearchResultType
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.UriUtil

class SemanticSearchInfoDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    private val languageCode: String
        get() = arguments?.getString(ARG_LANGUAGE_CODE).orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SemanticSearchInfoDialogContent(
                    onCloseClick = { dismiss() },
                    onLearnMoreClick = {
                        UriUtil.visitInExternalBrowser(requireContext(), getString(R.string.semantic_search_info_learn_more_link).toUri())
                        dismiss()
                    },
                    quotationMark = SemanticSearchHelper.getQuotationMark(languageCode),
                    languageCode = languageCode
                )
            }
        }
    }

    companion object {
        private const val ARG_LANGUAGE_CODE = "language_code"

        fun newInstance(languageCode: String): SemanticSearchInfoDialog {
            return SemanticSearchInfoDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_LANGUAGE_CODE, languageCode)
                }
            }
        }
    }
}

@Composable
private fun SemanticSearchInfoDialogContent(
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    quotationMark: String,
    languageCode: String
) {
    val context = LocalContext.current
    val dialogTextData = getSemanticSearchInfoDialogText(languageCode)
    val layoutDirection =
        if (L10nUtil.isLangRTL(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr

    val searchResult = SearchResult(
        pageTitle = PageTitle(
            stringResource(id = dialogTextData.title),
            WikiSite.preview()).apply {
                thumbUrl = stringResource(id = dialogTextData.thumbnailUrl)
            },
        redirectFrom = null,
        type = SearchResultType.SEMANTIC,
        coordinates = null,
        snippet = stringResource(id = dialogTextData.snippet),
        indexInApiCall = 0,
        sectionTitle = stringResource(id = dialogTextData.sectionTitle),
        editCounts = 2348,
        referenceCounts = 35
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BaseTheme {
            InfoActionScreen(
                title = context.getString(languageCode, R.string.semantic_search_info_dialog_title),
                message = context.getString(languageCode, R.string.semantic_search_info_dialog_message),
                onCloseClick = onCloseClick,
                bottomContent = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_search_24),
                                tint = WikipediaTheme.colors.primaryColor,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = dialogTextData.searchString),
                                color = WikipediaTheme.colors.primaryColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight(600),
                            )
                        }
                        SemanticSearchResultCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            prefixQuotationMark = quotationMark,
                            searchResult = searchResult,
                            onItemClick = null,
                            onLinkClick = { url ->
                                // ignore in-article links
                            },
                            showLastUpdatedTime = false,
                            showReadInArticleText = false,
                            languageCode = languageCode
                        )

                        AppButton(
                            onClick = onLearnMoreClick,
                            backgroundColor = WikipediaTheme.colors.backgroundColor,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = context.getString(languageCode, R.string.on_this_day_game_menu_info),
                                    color = WikipediaTheme.colors.progressiveColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_open_in_new_black_24px),
                                    tint = WikipediaTheme.colors.progressiveColor,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class InfoDialogTextConfig(
    @StringRes val title: Int,
    @StringRes val sectionTitle: Int,
    @StringRes val snippet: Int,
    @StringRes val thumbnailUrl: Int,
    @StringRes val searchString: Int
)

private fun getSemanticSearchInfoDialogText(languageCode: String): InfoDialogTextConfig {

    return when (languageCode) {
        "ja" -> InfoDialogTextConfig(
                    title = R.string.semantic_search_info_dialog_article_title_ja,
                    sectionTitle = R.string.semantic_search_info_dialog_article_section_title_ja,
                    snippet = R.string.semantic_search_info_dialog_sample_snippet_ja,
                    thumbnailUrl = R.string.semantic_search_info_dialog_article_thumbnail_url_ja,
                    searchString = R.string.semantic_search_info_dialog_search_string_ja)
        "fr" -> InfoDialogTextConfig(
                    title = R.string.semantic_search_info_dialog_article_title_fr,
                    sectionTitle = R.string.semantic_search_info_dialog_article_section_title_fr,
                    snippet = R.string.semantic_search_info_dialog_sample_snippet_fr,
                    thumbnailUrl = R.string.semantic_search_info_dialog_article_thumbnail_url_fr,
                    searchString = R.string.semantic_search_info_dialog_search_string_fr)
        "ar" -> InfoDialogTextConfig(
                    title = R.string.semantic_search_info_dialog_article_title_ar,
                    sectionTitle = R.string.semantic_search_info_dialog_article_section_title_ar,
                    snippet = R.string.semantic_search_info_dialog_sample_snippet_ar,
                    thumbnailUrl = R.string.semantic_search_info_dialog_article_thumbnail_url_ar,
                    searchString = R.string.semantic_search_info_dialog_search_string_ar)
        else -> InfoDialogTextConfig(
                    title = R.string.semantic_search_info_dialog_article_title_en,
                    sectionTitle = R.string.semantic_search_info_dialog_article_section_title_en,
                    snippet = R.string.semantic_search_info_dialog_sample_snippet_en,
                    thumbnailUrl = R.string.semantic_search_info_dialog_article_thumbnail_url_en,
                    searchString = R.string.semantic_search_info_dialog_search_string_en)
    }
}

@Preview(showBackground = true)
@Composable
fun SemanticSearchInfoDialogPreview() {
    SemanticSearchInfoDialogContent(
        onCloseClick = {},
        onLearnMoreClick = { },
        quotationMark = SemanticSearchHelper.getQuotationMark("ja"),
        languageCode = "ja"
    )
}
