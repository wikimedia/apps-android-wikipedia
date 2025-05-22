package org.wikipedia.readinglist.recommended

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionScreen(
    onCloseClick: () -> Unit = {},
    onInterestsClick: () -> Unit = {},
    onSavedClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(WikipediaTheme.colors.paperColor),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.table_close),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onCloseClick)
                            .padding(12.dp),
                        tint = WikipediaTheme.colors.primaryColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WikipediaTheme.colors.paperColor,
                    scrolledContainerColor = WikipediaTheme.colors.paperColor
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(WikipediaTheme.colors.paperColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Text(
                text = stringResource(id = R.string.recommended_reading_list_interest_source_message),
                color = WikipediaTheme.colors.primaryColor,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            SourceOptionCard(
                modifier = Modifier
                    .clickable(onClick = onInterestsClick),
                iconRes = R.drawable.outline_interests_24,
                textRes = R.string.recommended_reading_list_interest_source_interests,
            )

            SourceOptionCard(
                modifier = Modifier
                    .clickable(onClick = onSavedClick),
                iconRes = R.drawable.ic_bookmark_border_white_24dp,
                textRes = R.string.recommended_reading_list_interest_source_saved,
            )

            SourceOptionCard(
                modifier = Modifier
                    .clickable(onClick = onHistoryClick),
                iconRes = R.drawable.ic_history_24,
                textRes = R.string.recommended_reading_list_interest_source_history,
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WikipediaTheme.colors.borderColor)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(id = R.string.nav_item_forward),
                    tint = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onNextClick)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SourceOptionCard(
    modifier: Modifier,
    iconRes: Int,
    textRes: Int,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ListItem(
                modifier = modifier,
                colors = ListItemDefaults.colors(
                    containerColor = WikipediaTheme.colors.paperColor
                ),
                leadingContent = {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = painterResource(iconRes),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = stringResource(textRes)
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(textRes),
                        style = WikipediaTheme.typography.p,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewSourceSelectionScreen() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SourceSelectionScreen()
    }
}
