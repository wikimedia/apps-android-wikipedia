package org.wikipedia.activitytab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil

// TODO: MARK_ACTIVITY_TAB add actual strings from design and update accordingly
private val onboardingItems = listOf(
    OnboardingItem(
        icon = R.drawable.ic_newsstand_24,
        title = "Reading history",
        subTitle = "Activity tab insights are based on the primary language set in settings and is leveraging local data with the exception of edits which are public."
    ),
    OnboardingItem(
        icon = R.drawable.ic_icon_user_contributions_ooui,
        title = "Your Impact",
        subTitle = "Activity tab insights are based on the primary language set in settings and is leveraging local data with the exception of edits which are public."
    ),
    OnboardingItem(
        icon = R.drawable.ic_star_24,
        title = "Highlights",
        subTitle = "Activity tab insights are based on the primary language set in settings and is leveraging local data with the exception of edits which are public."
    ),
    OnboardingItem(
        icon = R.drawable.ic_link_black_24dp,
        title = "Timeline",
        subTitle = "Activity tab insights are based on the primary language set in settings and is leveraging local data with the exception of edits which are public."
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
                    onLearnMoreClick = {},
                    onContinueClick = {}
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp, bottom = 32.dp),
                    textAlign = TextAlign.Center,
                    text = "Introducing Activity",
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
                                text = onboardingItem.title,
                                style = MaterialTheme.typography.titleMedium,

                                )
                        },
                        supportingContent = {
                            Text(
                                text = onboardingItem.subTitle
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(onboardingItem.icon),
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 24.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Button(
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
                        text = "Learn more",
                        style = MaterialTheme.typography.labelLarge,
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onContinueClick
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.labelLarge,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            }
        }
    }
}

data class OnboardingItem(
    val icon: Int,
    val title: String,
    val subTitle: String
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
