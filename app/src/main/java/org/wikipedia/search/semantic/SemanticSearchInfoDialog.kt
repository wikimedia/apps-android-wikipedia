package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.InfoActionScreen
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResult.SearchResultType
import org.wikipedia.util.UriUtil

class SemanticSearchInfoDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SemanticSearchInfoDialogContent(
                    onCloseClick = { dismiss() },
                    onLearnMoreClick = {
                        UriUtil.visitInExternalBrowser(requireContext(), getString(R.string.semantic_search_info_learn_more_url).toUri())
                        dismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SemanticSearchInfoDialogContent(
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {

    val snippet = stringResource(id = R.string.semantic_search_info_dialog_sample_snippet)

    val searchResult = SearchResult(
        pageTitle = PageTitle("Cats", WikiSite.preview()).apply {
            description = "Cats"
            thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/2/25/Siam_lilacpoint.jpg?utm_source=en.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail_unscaled"
        },
        redirectFrom = null,
        type = SearchResultType.SEMANTIC,
        coordinates = null,
        snippet = snippet,
        indexInApiCall = 0,
        sectionTitle = "Vision",
        editCounts = 2348,
        referenceCounts = 35
    )

    BaseTheme {
        InfoActionScreen(
            title = stringResource(id = R.string.semantic_search_info_dialog_title),
            message = stringResource(id = R.string.semantic_search_info_dialog_message),
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
                            text = stringResource(id = R.string.semantic_search_info_dialog_search_string),
                            color = WikipediaTheme.colors.primaryColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight(600),
                        )
                    }
                    SemanticSearchResultCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        prefixQuotationMark = "«",
                        searchResult = searchResult,
                        onItemClick = {},
                        onLinkClick = {}
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
                                text = stringResource(id = R.string.semantic_search_info_dialog_button_text),
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

@Preview(showBackground = true)
@Composable
fun SemanticSearchInfoDialogPreview() {
    SemanticSearchInfoDialogContent(
        onCloseClick = {},
        onLearnMoreClick = { }
    )
}
