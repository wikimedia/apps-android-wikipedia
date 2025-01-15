package org.wikipedia.compose.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.WikipediaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiTopAppBar(
    title: String,
    onNavigationClick: (() -> Unit),
    titleStyle: TextStyle = WikipediaTheme.typography.h1,
    elevation: Int = 0,
    modifier: Modifier = Modifier
) {
    val navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
    val backgroundColor = WikipediaTheme.colors.paperColor
    val elevation = elevation.dp
    val titleStyle = titleStyle.copy(
        lineHeight = 24.sp
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                style = titleStyle,
                color = WikipediaTheme.colors.primaryColor
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = backgroundColor,
            titleContentColor = WikipediaTheme.colors.primaryColor
        ),
        modifier = modifier.shadow(elevation = elevation)
    )
}
