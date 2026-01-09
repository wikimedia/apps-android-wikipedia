package org.wikipedia.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.OnboardingScreen
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
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

class HybridSearchOnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                OnboardingScreen(
                    headerContent = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 58.dp, bottom = 24.dp)
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(47.dp),
                                shape = RoundedCornerShape(50), // This creates the pill shape
                                color = WikipediaTheme.colors.progressiveColor
                            ) {
                                Text(
                                    text = "BETA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    color = WikipediaTheme.colors.paperColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "Introducing deep search",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                                color = WikipediaTheme.colors.primaryColor
                            )
                        }
                    },
                    content = {
                        onboardingItems.forEach { onboardingItem ->
                            OnboardingListItem(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp),
                                item = onboardingItem
                            )
                        }
                    },
                    primaryButtonText = stringResource(R.string.onboarding_next),
                    secondaryButtonText = stringResource(R.string.hybrid_search_onboarding_learn_more),
                    onPrimaryOnClick = {},
                    onSecondaryOnClick = {
                        // TODO: add URL
                        UriUtil.visitInExternalBrowser(this, "".toUri())
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HybridSearchOnboardingActivity::class.java)
        }
    }
}
