package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun TopCategoriesView(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    onClick: (Category) -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp),
                    painter = painterResource(R.drawable.outline_category_24),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
                Text(
                    text = "Top categories read this month",
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            categories.forEachIndexed { index, value ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onClick(value) })
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        text = value.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }

                if (index < categories.size - 1) {
                    HorizontalDivider(
                        color = WikipediaTheme.colors.borderColor
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverSomethingNewView(
    title: String,
    modifier: Modifier = Modifier,
    onDiscoverBtnCLick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            color = WikipediaTheme.colors.primaryColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier
                .height(32.dp),
            onClick = onDiscoverBtnCLick,
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor,
                contentColor = WikipediaTheme.colors.paperColor,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_discover_globe),
                        contentDescription = null
                    )
                    Text(
                        text = "Discover Articles ",
                    )
                }
            }
        )
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
            categories = listOf(
                Category(2025, 1, "Ancient History", "en", 1),
                Category(2025, 1, "World knowledge literature", "en", 1),
                Category(2025, 1, "Random stories of the Ancient Civilization", "en", 1),
            ),
            onClick = {}
        )
    }
}
