package org.wikipedia.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.TwoButtonBottomBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.UriUtil

private val onboardingItems = listOf(
    OnboardingItem(
        icon = R.drawable.ic_chat_bubble_24,
        title = R.string.hybrid_search_onboarding_search_title,
        subTitle = R.string.hybrid_search_onboarding_search_description
    ),
    OnboardingItem(
        icon = R.drawable.ic_baseline_person_24,
        title = R.string.hybrid_search_onboarding_opt_in_choice_title,
        subTitle = R.string.hybrid_search_onboarding_opt_in_choice_description
    )
)

class HybridSearchOnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                HybridSearchOnboardingScreen()
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HybridSearchOnboardingActivity::class.java)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridSearchOnboardingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier
                .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .padding(top = 58.dp, bottom = 32.dp)
                            .padding(horizontal = 12.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .height(16.dp)
                                .width(47.dp),
                            shape = RoundedCornerShape(50),
                            color = WikipediaTheme.colors.progressiveColor
                        ) {
                            Text(
                                text = stringResource(R.string.hybrid_search_beta_tag),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                color = WikipediaTheme.colors.paperColor,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Title text
                        Text(
                            text = "Introducing deep search",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WikipediaTheme.colors.paperColor
                )
            )
        },
        bottomBar = {
            TwoButtonBottomBar(
                primaryButtonText = stringResource(R.string.onboarding_next),
                secondaryButtonText = stringResource(R.string.hybrid_search_onboarding_learn_more),
                onPrimaryOnClick = {},
                onSecondaryOnClick = {
                    // TODO: add URL
                    UriUtil.visitInExternalBrowser(context, "".toUri())
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            onboardingItems.forEach { onboardingItem ->
                OnboardingListItem(item = onboardingItem)
            }
            ExperimentalFeatureToggleView()
        }
    }
}

@Composable
fun ExperimentalFeatureToggleView() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(40.dp)) // (Icon size + spacer width) which aligns with the OnboardingList item
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(R.string.hybrid_search_experimental_feature_switch_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Switch(
            checked = true,
            onCheckedChange = {},
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = WikipediaTheme.colors.paperColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                checkedTrackColor = WikipediaTheme.colors.progressiveColor,
                checkedThumbColor = WikipediaTheme.colors.paperColor
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HybridSearchOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HybridSearchOnboardingScreen()
    }
}
