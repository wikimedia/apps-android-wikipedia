package org.wikipedia.readinglist.recommended

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionScreen(
    uiState: Resource<RecommendedReadingListSourceViewModel.SourceSelectionUiState>,
    fromSettings: Boolean,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
    onSourceClick: (RecommendedReadingListSource) -> Unit
) {
    val activeInterface = if (fromSettings) "settings_hub_select" else "rrl_hub_select"
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BackHandler {
        RecommendedReadingListEvent.submit("close_click", activeInterface)
        onCloseClick()
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(WikipediaTheme.colors.paperColor),
        topBar = {
            TopAppBar(
                title = {
                    if (fromSettings) {
                        Text(
                            text = stringResource(id = R.string.recommended_reading_list_settings_updates_base_title),
                            color = WikipediaTheme.colors.primaryColor,
                            style = WikipediaTheme.typography.h1.copy(lineHeight = 24.sp)
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = if (fromSettings) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                        contentDescription = stringResource(id = if (fromSettings) R.string.search_back_button_content_description else R.string.table_close),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = {
                                RecommendedReadingListEvent.submit("close_click", activeInterface)
                                onCloseClick()
                            })
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
        when (uiState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = WikipediaTheme.colors.progressiveColor,
                        trackColor = WikipediaTheme.colors.borderColor
                    )
                }
            }

            is Resource.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    WikiErrorView(
                        modifier = Modifier
                            .fillMaxWidth(),
                        caught = uiState.throwable,
                        errorClickEvents = wikiErrorClickEvents
                    )
                }
            }

            is Resource.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    SourceSelectionContent(
                        selectedSource = uiState.data.selectedSource,
                        isSavedOptionEnabled = uiState.data.isSavedOptionEnabled,
                        isHistoryOptionEnabled = uiState.data.isHistoryOptionEnabled,
                        fromSettings = fromSettings,
                        onSourceClick = onSourceClick,
                        onNextClick = onNextClick
                    )
                }
            }
        }
    }
}

@Composable
fun SourceSelectionContent(
    onSourceClick: (RecommendedReadingListSource) -> Unit,
    onNextClick: () -> Unit,
    selectedSource: RecommendedReadingListSource,
    isSavedOptionEnabled: Boolean,
    isHistoryOptionEnabled: Boolean,
    fromSettings: Boolean
) {
    val sourceOptionsForEvent = mutableListOf<String>()
    var selectedSourceForEvent = "interests"
    val activeInterface = if (fromSettings) "settings_hub_select" else "rrl_hub_select"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!fromSettings) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!fromSettings) {
                Text(
                    text = stringResource(id = R.string.recommended_reading_list_interest_source_message),
                    color = WikipediaTheme.colors.primaryColor,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }

            SourceOptionCard(
                modifier = Modifier
                    .clickable(onClick = {
                        onSourceClick(RecommendedReadingListSource.INTERESTS)
                        if (!fromSettings) {
                            selectedSourceForEvent = "interests"
                            RecommendedReadingListEvent.submit("interests_click", activeInterface)
                        }
                    }),
                iconRes = R.drawable.outline_interests_24,
                textRes = R.string.recommended_reading_list_interest_source_interests,
                isSelected = selectedSource == RecommendedReadingListSource.INTERESTS
            )
            sourceOptionsForEvent.add("interests")

            if (isSavedOptionEnabled) {
                SourceOptionCard(
                    modifier = Modifier
                        .clickable(onClick = {
                            onSourceClick(RecommendedReadingListSource.READING_LIST)
                            if (!fromSettings) {
                                selectedSourceForEvent = "saved"
                                RecommendedReadingListEvent.submit("saved_click", activeInterface)
                            }
                        }),
                    iconRes = R.drawable.ic_bookmark_border_white_24dp,
                    textRes = R.string.recommended_reading_list_interest_source_saved,
                    isSelected = selectedSource == RecommendedReadingListSource.READING_LIST
                )
                sourceOptionsForEvent.add("saved")
            }

            if (isHistoryOptionEnabled) {
                SourceOptionCard(
                    modifier = Modifier
                        .clickable(onClick = {
                            onSourceClick(RecommendedReadingListSource.HISTORY)
                            if (!fromSettings) {
                                selectedSourceForEvent = "history"
                                RecommendedReadingListEvent.submit("history_click", activeInterface)
                            }
                        }),
                    iconRes = R.drawable.ic_history_24,
                    textRes = R.string.recommended_reading_list_interest_source_history,
                    isSelected = selectedSource == RecommendedReadingListSource.HISTORY
                )
                sourceOptionsForEvent.add("history")
            }
        }

        if (!fromSettings) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WikipediaTheme.colors.borderColor)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.nav_item_forward),
                    tint = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = {
                            onNextClick()
                            RecommendedReadingListEvent.submit(
                                action = "submit_click",
                                activeInterface = "rrl_hub_select",
                                optionsShown = sourceOptionsForEvent.toString(),
                                selected = selectedSourceForEvent
                            )
                        })
                        .padding(12.dp)
                )
            }
        }
    }

    RecommendedReadingListEvent.submit("impression", activeInterface, optionsShown = sourceOptionsForEvent.toString())
}

@Composable
fun SourceOptionCard(
    isSelected: Boolean = false,
    modifier: Modifier,
    iconRes: Int,
    textRes: Int
) {
    WikiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = 0.dp,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) WikipediaTheme.colors.additionColor else WikipediaTheme.colors.paperColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                painter = painterResource(iconRes),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = stringResource(textRes)
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewSourceSelectionScreen() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        SourceSelectionScreen(
            uiState = Resource.Success(
                RecommendedReadingListSourceViewModel.SourceSelectionUiState(
                    isSavedOptionEnabled = true,
                    isHistoryOptionEnabled = true,
                    selectedSource = RecommendedReadingListSource.INTERESTS
                )
            ),
            fromSettings = false,
            onSourceClick = {},
            onCloseClick = {},
            onNextClick = {}
        )
    }
}
