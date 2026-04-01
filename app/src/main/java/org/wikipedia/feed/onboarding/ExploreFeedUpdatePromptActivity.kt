package org.wikipedia.feed.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.TwoButtonBottomBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme

private val onboardingItems = listOf(
    OnboardingItem(
        icon = R.drawable.ic_home_24dp,
        title = R.string.explore_feed_new_update_prompt_new_name_title,
        subTitle = R.string.explore_feed_new_update_prompt_new_name_description
    ),
    OnboardingItem(
        icon = R.drawable.ic_split_scene_24dp,
        title = R.string.explore_feed_new_update_prompt_two_ways_title,
        subTitle = R.string.explore_feed_new_update_prompt_two_ways_description
    ),
    OnboardingItem(
        icon = R.drawable.ic_baseline_tune_24,
        title = R.string.explore_feed_new_update_prompt_control_title,
        subTitle = R.string.explore_feed_new_update_prompt_control_description
    ),
    OnboardingItem(
        icon = R.drawable.ic_public_24dp,
        title = R.string.explore_feed_new_update_prompt_language_title,
        subTitle = R.string.explore_feed_new_update_prompt_language_description
    )
)

class ExploreFeedUpdatePromptActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.isExploreFeedUpdatePromptShown = true
        setContent {
            BaseTheme {
                ExploreFeedUpdatePromptScreen(
                    onSetItUpForMeClick = {
                        finish()
                        // TODO: navigate directly to the feed.
                    },
                    onCustomizeFeedClick = {
                        finish()
                        // TODO: navigate to condensed onboarding flow (Interests Selection, Feed Preference and Feed Loading screens)
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ExploreFeedUpdatePromptActivity::class.java)
        }
    }
}

@Composable
fun ExploreFeedUpdatePromptScreen(
    modifier: Modifier = Modifier,
    onSetItUpForMeClick: () -> Unit,
    onCustomizeFeedClick: () -> Unit
) {
    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            TwoButtonBottomBar(
                primaryButtonText = stringResource(R.string.explore_feed_new_update_prompt_primary_button_text),
                secondaryButtonText = stringResource(R.string.explore_feed_new_update_prompt_secondary_button_text),
                onPrimaryOnClick = onCustomizeFeedClick,
                onSecondaryOnClick = onSetItUpForMeClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 58.dp, bottom = 32.dp),
                text = stringResource(R.string.explore_feed_new_update_prompt_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                color = WikipediaTheme.colors.primaryColor
            )

            onboardingItems.forEach { onboardingItem ->
                OnboardingListItem(
                    modifier = Modifier
                        .padding(bottom = 24.dp),
                    item = onboardingItem
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreFeedUpdatePromptScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ExploreFeedUpdatePromptScreen(
            onSetItUpForMeClick = {},
            onCustomizeFeedClick = {}
        )
    }
}
