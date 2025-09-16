package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme

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
