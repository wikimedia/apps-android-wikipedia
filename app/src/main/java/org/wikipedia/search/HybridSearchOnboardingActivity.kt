package org.wikipedia.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import org.wikipedia.settings.Prefs
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

private val defaultSearchQueries = listOf("How to pass time?", "first olympics", "RNA vs DNA", "pizza hall of fame", "What are the biggest cities in Europe?")

class HybridSearchOnboardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        Prefs.isHybridSearchEnabled = true
        setContent {
            BaseTheme {
                HybridSearchOnboardingScreen(
                    onGetStarted = { exampleQuery ->
                        if (exampleQuery == null) {
                            setResult(RESULT)
                            finish()
                            return@HybridSearchOnboardingScreen
                        } else {
                            // TODO: open deep search screen with exampleQuery
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val RESULT = 1000
        fun newIntent(context: Context): Intent {
            return Intent(context, HybridSearchOnboardingActivity::class.java)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridSearchOnboardingScreen(
    modifier: Modifier = Modifier,
    onGetStarted: (String?) -> Unit,
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(OnboardingStep.FEATURE_OVERVIEW) }
    Scaffold(
        modifier = modifier
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
                primaryButtonText = if (currentStep == OnboardingStep.FEATURE_OVERVIEW) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started),
                secondaryButtonText = stringResource(R.string.hybrid_search_onboarding_learn_more),
                onPrimaryOnClick = {
                    if (currentStep == OnboardingStep.FEATURE_OVERVIEW) {
                        currentStep = OnboardingStep.SEARCH_EXAMPLES
                    } else {
                        onGetStarted(null)
                    }
                },
                onSecondaryOnClick = {
                    // TODO: add URL
                    UriUtil.visitInExternalBrowser(context, "".toUri())
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            label = "HybridSearchOnboardingAnimation"
        ) { targetStep ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
            ) {
                when (targetStep) {
                    OnboardingStep.FEATURE_OVERVIEW -> {
                        onboardingItems.forEach { onboardingItem ->
                            OnboardingListItem(item = onboardingItem)
                        }
                        ExperimentalFeatureToggleView()
                    }
                    OnboardingStep.SEARCH_EXAMPLES -> {
                        SearchExamplesView(
                            modifier = Modifier
                                .background(WikipediaTheme.colors.paperColor),
                            onClick = { exampleQuery ->
                                onGetStarted(exampleQuery)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExperimentalFeatureToggleView() {
    var isChecked by remember { mutableStateOf(Prefs.isHybridSearchEnabled) }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(36.dp)) // (Icon size + spacer width) which aligns with the OnboardingList item
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(R.string.hybrid_search_experimental_feature_switch_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WikipediaTheme.colors.primaryColor
        )

        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = !isChecked
                Prefs.isHybridSearchEnabled = isChecked
            },
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

@Composable
fun SearchExamplesView(
    modifier: Modifier = Modifier,
    searchExamples: List<String> = defaultSearchQueries,
    onClick: (String) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        OnboardingListItem(
            item = OnboardingItem(
                icon = R.drawable.ic_light_bulb,
                title = R.string.hybrid_search_onboarding_search_example_title,
                subTitle = R.string.hybrid_search_onboarding_search_example_description
            )
        )
        FlowRow(
            modifier = Modifier
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searchExamples.forEach { exampleQuery ->
                SuggestionChip(
                    onClick = { onClick(exampleQuery) },
                    label = {
                        Text(
                            text = exampleQuery
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = WikipediaTheme.colors.paperColor,
                        labelColor = WikipediaTheme.colors.secondaryColor,
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = WikipediaTheme.colors.borderColor
                    )
                )
            }
        }
    }
}

enum class OnboardingStep {
    FEATURE_OVERVIEW,
    SEARCH_EXAMPLES
}

@Preview(showBackground = true)
@Composable
private fun HybridSearchOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HybridSearchOnboardingScreen(
            onGetStarted = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchExamplesPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        SearchExamplesView(
            modifier = Modifier
                .background(WikipediaTheme.colors.paperColor),
            onClick = {}
        )
    }
}
