package org.wikipedia.compose.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun WikiCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = WikipediaApp.instance.currentTheme.isDark,
    elevation: Dp = 8.dp,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = WikipediaTheme.colors.paperColor,
        contentColor = WikipediaTheme.colors.paperColor
    ),
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val cardElevation = remember(elevation, isDarkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isDarkTheme) {
            0.dp
        } else {
            elevation
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = colors,
        border = border,
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun WikiCardSimpleWikiTextPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiCard(
            modifier = Modifier
                .padding(20.dp),
            isDarkTheme = false
        ) {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = "Text example in a WikiCard",
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BorderAndElevationWikiTextPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        WikiCard(
            modifier = Modifier
                .padding(20.dp),
            border = BorderStroke(width = 0.5.dp, color = WikipediaTheme.colors.progressiveColor),
            elevation = 4.dp,
            isDarkTheme = true
        ) {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = "Text example in a WikiCard",
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}
