package org.wikipedia.compose.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun WikiCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 8.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val isDarkTheme = WikipediaApp.instance.currentTheme.isDark
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
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        border = border,
    ) {
        content()
    }
}
