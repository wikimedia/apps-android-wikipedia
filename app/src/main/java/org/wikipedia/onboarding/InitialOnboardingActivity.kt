package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.onboarding.personalization.PersonalizationScreen
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.yearinreview.LoadingIndicator
import org.wikipedia.util.DeviceUtil

class InitialOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            var currentTheme by remember { mutableStateOf(Theme.BLACK) }
            var currentNavigationBarColor by remember { mutableIntStateOf(ContextCompat.getColor(window.context, android.R.color.black)) }
            DeviceUtil.setLightSystemUiVisibility(this, currentTheme = currentTheme)
            setNavigationBarColor(currentNavigationBarColor)
            BaseTheme(
                currentTheme = currentTheme
            ) {
                AppOnboardingScreen(
                    isNewUser = Prefs.isInitialOnboardingEnabled,
                    onUpdateTheme = {
                        currentTheme = WikipediaApp.instance.currentTheme
                        currentNavigationBarColor = ResourceUtil.getThemedColor(this, R.attr.paper_color)
                    },
                    onFinish = {
                        Prefs.isInitialOnboardingEnabled = false
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val RESULT_LANGUAGE_CHANGED = 1
        fun newIntent(context: Context): Intent {
            return Intent(context, InitialOnboardingActivity::class.java)
        }
    }
}

@Composable
fun AppOnboardingScreen(
    modifier: Modifier = Modifier,
    isNewUser: Boolean,
    onUpdateTheme: () -> Unit,
    onFinish: () -> Unit
) {
    var showIntroScreen by remember { mutableStateOf(isNewUser) }
    if (showIntroScreen) {
        InitialOnboardingScreen(
            modifier = modifier,
            onNextClick = {
                onUpdateTheme()
            },
            onFinishClick = {
                showIntroScreen = false
            }
        )
    } else {
        // TODO: interest selection
        onFinish()
    }
}

@Composable
fun InitialOnboardingScreen(
    modifier: Modifier = Modifier,
    onNextClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    val context = LocalContext.current
    var isFirstScreen by remember { mutableStateOf(true) }
    Scaffold(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (!isFirstScreen) {
                    Spacer(
                        modifier = Modifier.height(1.dp)
                            .fillMaxWidth()
                            .background(WikipediaTheme.colors.borderColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(onClick = {
                        if (isFirstScreen) {
                            onNextClick()
                            isFirstScreen = false
                        } else {
                            // TODO: navigate to interest selection screen
                            onFinishClick()
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                            tint = WikipediaTheme.colors.progressiveColor,
                            contentDescription = stringResource(R.string.nav_item_forward)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isFirstScreen) {
                InitialOnboardingIntroContent(
                    onLearnMoreClick = {
                        FeedbackUtil.showAboutWikipedia(context)
                    }
                )
            } else {
                InitialOnboardingDataPrivacyContent(
                    onPrivacyClick = {
                        FeedbackUtil.showPrivacyPolicy(context)
                    },
                    onTermsClick = {
                        FeedbackUtil.showTermsOfUse(context)
                    }
                )
            }
        }
    }
}

@Composable
fun InitialOnboardingIntroContent(
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
                .height(20.dp),
            painter = painterResource(R.drawable.feed_header_wordmark),
            contentDescription = stringResource(R.string.app_name_prod),
            colorFilter = ColorFilter.tint(WikipediaTheme.colors.primaryColor),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            text = stringResource(R.string.onboarding_fresh_install_knowledge_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.onboarding_fresh_install_knowledge_text),
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(onClick = onLearnMoreClick),
            text = stringResource(R.string.onboarding_fresh_install_knowledge_learn_more),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = WikipediaTheme.colors.progressiveColor
        )

        Spacer(modifier = Modifier.weight(1f))

        Image(
            modifier = Modifier
                .fillMaxWidth(),
            painter = painterResource(R.drawable.ic_onboarding_knowledge),
            contentDescription = null,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun InitialOnboardingDataPrivacyContent(
    modifier: Modifier = Modifier,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        SubcomposeAsyncImage(
            modifier = Modifier
                .size(124.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_onboarding_puzzle)
                .allowHardware(false)
                .build(),
            loading = { LoadingIndicator() },
            success = {
                SubcomposeAsyncImageContent()
            },
            contentDescription = stringResource(R.string.onboarding_data_privacy_title),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            text = stringResource(R.string.onboarding_data_privacy_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )
        HtmlText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.onboarding_data_privacy_text),
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor,
            linkStyle = TextLinkStyles(
                style = SpanStyle(
                    color = WikipediaTheme.colors.progressiveColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            ),
            linkInteractionListener = {
                val url = (it as LinkAnnotation.Url).url
                when {
                    url.contains("#privacy") -> onPrivacyClick()
                    url.contains("#termsOfUse") -> onTermsClick()
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InitialOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InitialOnboardingScreen(
            modifier = Modifier.fillMaxSize(),
            onNextClick = {},
            onFinishClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun InitialOnboardingIntroScreenPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        InitialOnboardingIntroContent(
            onLearnMoreClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InitialOnboardingDataPrivacyContentPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InitialOnboardingDataPrivacyContent(
            onPrivacyClick = {},
            onTermsClick = {}
        )
    }
}
