package org.wikipedia.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun SearchEmptyView(
    modifier: Modifier = Modifier,
    emptyTexTitle: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .requiredSize(96.dp)
                .clip(CircleShape)
                .background(ComposeColors.White)
                .padding(20.dp),
            imageVector = Icons.Outlined.Search,
            tint = WikipediaTheme.colors.placeholderColor,
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .padding(top = 24.dp),
            text = emptyTexTitle,
            style = WikipediaTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.placeholderColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchEmptyViewPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SearchEmptyView(
                emptyTexTitle = "No languages found"
            )
        }
    }
}
