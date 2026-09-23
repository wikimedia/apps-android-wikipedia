package org.wikipedia.search.semantic

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun SemanticSearchEntryCard(
    searchTerm: String?,
    isFirstUse: Boolean,
    onInfoBtnClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSemanticSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .wrapContentHeight()
            .clickable(onClick = { onSemanticSearchClick() })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = WikipediaTheme.colors.borderColor,
                            shape = RoundedCornerShape(size = 16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        modifier = Modifier,
                        painter = painterResource(R.drawable.ic_experiment_24dp),
                        tint = WikipediaTheme.colors.secondaryColor,
                        contentDescription = null
                    )

                    Text(
                        text = stringResource(R.string.donation_reminders_beta_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }

                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = { onInfoBtnClick() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                        tint = WikipediaTheme.colors.placeholderColor,
                        contentDescription = stringResource(R.string.semantic_search_info_btn_content_description)
                    )
                }
            }

            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = { onCloseClick() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_black_24dp),
                    tint = WikipediaTheme.colors.progressiveColor,
                    contentDescription = stringResource(R.string.semantic_search_close_btn_content_description)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                searchTerm?.let { searchText ->
                    Text(
                        text = searchText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }

                Text(
                    text = stringResource(R.string.semantic_search_entry_point_card_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WikipediaTheme.colors.secondaryColor
                )
                if (isFirstUse) {
                    Text(
                        text = stringResource(R.string.semantic_search_entry_point_card_text_button),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            }
            Box(
                modifier = Modifier
                    .height(96.dp)
                    .aspectRatio(38f / 54f)
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.semantic_search_intro_card_icon),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SemanticSearchEntryCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            SemanticSearchEntryCard(
                searchTerm = "what is communication",
                onCloseClick = {},
                onInfoBtnClick = {},
                onSemanticSearchClick = {},
                isFirstUse = false
            )
        }
    }
}
