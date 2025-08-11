package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState

@Composable
fun TopCategoriesView(
    modifier: Modifier = Modifier,
    uiState: UiState<List<Category>>
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        when (uiState) {
            is UiState.Error -> {}
            UiState.Loading -> {}
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Row {
                        Icon(
                            modifier = Modifier
                                .size(16.dp),
                            painter = painterResource(R.drawable.outline_interests_24),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = "Top categories read this month",
                            style = MaterialTheme.typography.labelMedium,
                            color = WikipediaTheme.colors.primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    uiState.data.forEachIndexed { index, value ->
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            text = value.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor
                        )
                        if (index < uiState.data.size - 1) {
                            HorizontalDivider(
                                color = WikipediaTheme.colors.borderColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TopCategoriesViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        TopCategoriesView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            uiState = UiState.Success(listOf())
        )
    }
}
