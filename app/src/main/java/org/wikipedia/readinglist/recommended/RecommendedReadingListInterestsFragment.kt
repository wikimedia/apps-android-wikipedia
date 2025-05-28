package org.wikipedia.readinglist.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.R
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.Resource

class RecommendedReadingListInterestsFragment : Fragment() {

    private val viewModel: RecommendedReadingListInterestsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    RecommendedReadingListInterestsScreen(
                        uiState = viewModel.uiState.collectAsState().value,
                        onCloseClick = {

                        },
                        onNextClick = {

                        },
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            backClickListener = {
                                requireActivity().finish()
                            },
                            retryClickListener = {

                            }
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(): RecommendedReadingListInterestsFragment {
            return RecommendedReadingListInterestsFragment()
        }
    }
}

@Composable
fun RecommendedReadingListInterestsScreen(
    uiState: Resource<RecommendedReadingListInterestsViewModel.UiState>,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    when (uiState) {
        is Resource.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor,
                )
            }
        }

        is Resource.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
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
            RecommendedReadingListInterestsContent(
                fromSettings = uiState.data.fromSettings,
                onCloseClick = onCloseClick,
                onNextClick = onNextClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendedReadingListInterestsContent(
    fromSettings: Boolean = false,
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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

            }

            if (!fromSettings) {
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
}
