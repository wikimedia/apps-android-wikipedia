package org.wikipedia.activitytab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.util.UiState

class ActivityTabFragment : Fragment() {

    private val viewModel: ActivityTabViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Prefs.activityTabRedDotShown = true

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ActivityTabScreen(
                        uiState = viewModel.uiState.collectAsState().value,
                        donationUiState = viewModel.donationUiState.collectAsState().value,
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            retryClickListener = {
                                viewModel.load()
                            }
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun ActivityTabScreen(
        uiState: UiState<Unit>,
        donationUiState: UiState<String?>,
        wikiErrorClickEvents: WikiErrorClickEvents? = null
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            when (uiState) {
                is UiState.Loading -> {
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
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        WikiErrorView(
                            modifier = Modifier
                                .fillMaxWidth(),
                            caught = uiState.error,
                            errorClickEvents = wikiErrorClickEvents
                        )
                    }
                }
                is UiState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Text(
                            text = "TODO!",
                            modifier = Modifier.align(Alignment.Center),
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                }
            }
            if (donationUiState is UiState.Success) {
                // TODO: default is off. Make sure to handle
                DonationView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
                    uiState = donationUiState,
                    wikiErrorClickEvents = wikiErrorClickEvents,
                    onClick = {
                        // TODO: implement this
                    }
                )
            }
        }
    }

    companion object {
        fun newInstance(): ActivityTabFragment {
            return ActivityTabFragment().apply {
                arguments = Bundle().apply {
                    // TODO
                }
            }
        }
    }
}
