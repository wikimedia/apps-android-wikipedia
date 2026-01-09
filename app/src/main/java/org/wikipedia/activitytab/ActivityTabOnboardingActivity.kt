package org.wikipedia.activitytab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.ActivityTabEvent
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.OnboardingScreen
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.UriUtil

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
        ActivityTabEvent.submit(activeInterface = "activity_tab_start", action = "impression")
        setContent {
            BaseTheme {
                OnboardingScreen(
                    headerContent = {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 58.dp, bottom = 32.dp),
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.activity_tab_onboarding_screen_title),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                            color = WikipediaTheme.colors.primaryColor
                        )
                    },
                    content = {
                        onboardingItems.forEach { onboardingItem ->
                            OnboardingListItem(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .padding(bottom = 16.dp),
                                item = onboardingItem
                            )
                        }
                    },
                    primaryButtonText = stringResource(R.string.onboarding_continue),
                    secondaryButtonText = stringResource(R.string.activity_tab_menu_info),
                    onPrimaryOnClick = {
                        ActivityTabEvent.submit(
                            activeInterface = "activity_tab_start",
                            action = "continue_click"
                        )
                        Prefs.isActivityTabOnboardingShown = true
                        setResult(RESULT_OK)
                        finish()
                    },
                    onSecondaryOnClick = {
                        UriUtil.visitInExternalBrowser(
                            this,
                            getString(R.string.activity_tab_url).toUri()
                        )
                        Prefs.isActivityTabOnboardingShown = true
                        ActivityTabEvent.submit(
                            activeInterface = "activity_tab_start",
                            action = "learn_click"
                        )
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
