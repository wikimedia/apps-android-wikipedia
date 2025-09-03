package org.wikipedia.activitytab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil

private val onboardingItems = listOf(
    OnboardingItem(
        icon = R.drawable.ic_newsstand_24,
        title = R.string.activity_tab_onboarding_reading_patterns_title,
        subTitle = R.string.activity_tab_onboarding_reading_patterns_message
    ),
    OnboardingItem(
        icon = R.drawable.ic_mode_edit_white_24dp,
        title = R.string.activity_tab_onboarding_impact_title,
        subTitle = R.string.activity_tab_onboarding_impact_message
    ),
    OnboardingItem(
        icon = R.drawable.ic_outline_stadia_controller_24,
        title = R.string.activity_tab_onboarding_engage_title,
        subTitle = R.string.activity_tab_onboarding_engage_message
    ),
    OnboardingItem(
        icon = R.drawable.ic_outline_lock_24,
        title = R.string.activity_tab_onboarding_stay_in_control_title,
        subTitle = R.string.activity_tab_onboarding_stay_in_control_message
    )
)

class ActivityTabOnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                OnboardingScreen(
                    onboardingItems = onboardingItems,
                    onLearnMoreClick = {
                        // TODO: MARK_ACTIVITY_TAB waiting for mediawiki page link
                        Prefs.isActivityTabOnboardingShown = true
                    },
                    onContinueClick = {
                        Prefs.isActivityTabOnboardingShown = true
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ActivityTabOnboardingActivity::class.java)
        }
    }
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onboardingItems: List<OnboardingItem>,
    onLearnMoreClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 24.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = WikipediaTheme.colors.borderColor
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.paperColor
                    ),
                    onClick = onLearnMoreClick
                ) {
                    Text(
                        text = stringResource(R.string.activity_tab_menu_info),
                        style = MaterialTheme.typography.labelLarge,
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }

                Button(
                    modifier = Modifier
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onContinueClick
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_continue),
                        style = MaterialTheme.typography.labelLarge,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, bottom = 32.dp),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.activity_tab_onboarding_screen_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                color = WikipediaTheme.colors.primaryColor
            )

            onboardingItems.forEach { onboardingItem ->
                ListItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 16.dp),
                    colors = ListItemDefaults.colors(
                        containerColor = WikipediaTheme.colors.paperColor
                    ),
                    headlineContent = {
                        Text(
                            text = stringResource(onboardingItem.title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = WikipediaTheme.colors.primaryColor
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(onboardingItem.subTitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    },
                    leadingContent = {
                        Icon(
                            modifier = Modifier
                                .padding(top = 2.dp),
                            painter = painterResource(onboardingItem.icon),
                            tint = WikipediaTheme.colors.progressiveColor,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

data class OnboardingItem(
    val icon: Int,
    val title: Int,
    val subTitle: Int
)

@Preview
@Composable
private fun OnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnboardingScreen(
            onboardingItems = onboardingItems,
            onLearnMoreClick = {},
            onContinueClick = {}
        )
    }
}
