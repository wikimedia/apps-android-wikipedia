package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.nonEnglishCollectiveEditCountData

@Composable
fun YearInReviewScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = WikipediaTheme.colors.paperColor,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = containerColor,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewTopBar(
    onNavigationBackButtonClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = WikipediaTheme.colors.paperColor),
        title = {
            Icon(
                painter = painterResource(R.drawable.ic_w_transparent),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(R.string.year_in_review_topbar_w_icon)
            )
        },
        navigationIcon = {
            IconButton(onClick = { onNavigationBackButtonClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = stringResource(R.string.year_in_review_navigate_left)
                )
            }
        },
        actions = actions
    )
}

@Preview
@Composable
fun PreviewContent() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewScreenDeck(
            contentData = listOf(nonEnglishCollectiveEditCountData),
            onDonateClick = {},
            onNavigationBackButtonClick = {},
            onNavigationRightClick = {}
        )
    }
}
