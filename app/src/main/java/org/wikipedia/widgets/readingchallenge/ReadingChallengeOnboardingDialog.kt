package org.wikipedia.widgets.readingchallenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.TwoButtonBottomBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.theme.Theme

class ReadingChallengeOnboardingDialog : ExtendedBottomSheetDialogFragment() {
    private val onboardingItems = listOf(
        OnboardingItem(
            icon = R.drawable.ic_contract_24dp,
            title = R.string.reading_challenge_onboarding_read_title,
            subTitle = R.string.reading_challenge_onboarding_read_description
        ),
        OnboardingItem(
            icon = R.drawable.ic_featured_seasonal_and_gifts_24dp,
            title = R.string.reading_challenge_onboarding_win_title,
            subTitle = R.string.reading_challenge_onboarding_win_description
        ),
        OnboardingItem(
            icon = R.drawable.dashboard_customize_24dp,
            title = R.string.reading_challenge_onboarding_install_title,
            subTitle = R.string.reading_challenge_onboarding_install_description
        )
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    OnboardingScreen(
                        modifier = Modifier.fillMaxSize(),
                        onboardingItems = onboardingItems,
                        onCloseClick = {
                            dismiss()
                        },
                        onLearnMoreClick = {
                            // TODO: add link
                        },
                        onJoinClick = {
                            // TODO: opt in
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun OnboardingScreen(
        modifier: Modifier = Modifier,
        onboardingItems: List<OnboardingItem>,
        onCloseClick: () -> Unit,
        onLearnMoreClick: () -> Unit,
        onJoinClick: () -> Unit
    ) {
        Scaffold(
            modifier = modifier
                .safeDrawingPadding(),
            containerColor = WikipediaTheme.colors.paperColor,
            bottomBar = {
                Column {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(R.string.reading_challenge_onboarding_note),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.placeholderColor
                    )
                    TwoButtonBottomBar(
                        primaryButtonText = stringResource(R.string.reading_challenge_onboarding_join_button),
                        secondaryButtonText = stringResource(R.string.reading_challenge_onboarding_learn_more_button),
                        onPrimaryOnClick = onJoinClick,
                        onSecondaryOnClick = onLearnMoreClick
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .offset(x = 12.dp),
                    onClick = {
                        onCloseClick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_close_black_24dp),
                        contentDescription = stringResource(R.string.search_clear_query_content_description),
                        tint = WikipediaTheme.colors.primaryColor
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.reading_challenge_onboarding_header),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                    color = WikipediaTheme.colors.primaryColor
                )

                onboardingItems.forEach { onboardingItem ->
                    OnboardingListItem(
                        modifier = Modifier.padding(bottom = 16.dp),
                        item = onboardingItem
                    )
                }
            }
        }
    }

    @Preview
    @Composable
    private fun OnboardingScreenPreview() {
        BaseTheme(
            currentTheme = Theme.LIGHT
        ) {
            OnboardingScreen(
                onboardingItems = onboardingItems,
                onCloseClick = {},
                onLearnMoreClick = {},
                onJoinClick = {}
            )
        }
    }

    companion object {
        fun newInstance(): ReadingChallengeOnboardingDialog {
            return ReadingChallengeOnboardingDialog()
        }
    }
}
