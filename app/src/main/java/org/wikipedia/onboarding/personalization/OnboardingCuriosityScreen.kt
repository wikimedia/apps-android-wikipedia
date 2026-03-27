package org.wikipedia.onboarding.personalization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.LoadingIndicator

@Composable
fun OnboardingCuriosityScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(1f))

        SubcomposeAsyncImage(
            modifier = Modifier
                .size(125.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.yir_puzzle_browser)
                .allowHardware(false)
                .build(),
            loading = { LoadingIndicator() },
            success = {
                SubcomposeAsyncImageContent()
            },
            contentDescription = stringResource(R.string.explore_feed_onboarding_curiosity_title),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            text = stringResource(R.string.explore_feed_onboarding_curiosity_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.explore_feed_onboarding_curiosity_description),
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

data class InterestOnboardingUiState(
    val isLoading: Boolean = true,
    val items: List<String> = emptyList(),
    val selectedItems: Set<String> = emptySet()
)

@Preview
@Composable
private fun OnboardingCuriosityScreenPreview() {
    BaseTheme (
        currentTheme = Theme.LIGHT
    ) {
        OnboardingCuriosityScreen()
    }
}
