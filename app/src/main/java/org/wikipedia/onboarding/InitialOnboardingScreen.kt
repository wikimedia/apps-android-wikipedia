package org.wikipedia.onboarding

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.extensions.lazyColumnScrollbar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.instrument
import org.wikipedia.language.AppLanguageState
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.yearinreview.LoadingIndicator

@Composable
fun InitialOnboardingScreen(
    modifier: Modifier = Modifier,
    onboardingScreens: List<OnboardingScreen>,
    languageState: AppLanguageState? = null,
    appLanguageCodes: List<String>,
    onAddLanguageClick: () -> Unit,
    onNextClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    val context = LocalContext.current
    var currentScreenIndex by remember { mutableIntStateOf(0) }
    Scaffold(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (onboardingScreens[currentScreenIndex] != OnboardingScreen.INTRO) {
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
                        context.instrument?.submitInteraction("click", elementId = "next_button", actionSubtype = onboardingScreens[currentScreenIndex].name)
                        if (currentScreenIndex == onboardingScreens.size - 1) {
                            onFinishClick()
                        } else {
                            currentScreenIndex++
                        }
                        onNextClick()
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
            when (onboardingScreens[currentScreenIndex]) {
                OnboardingScreen.INTRO -> {
                    InitialOnboardingIntroContent(
                        onLearnMoreClick = {
                            context.instrument?.submitInteraction("click", elementId = "about_link", actionSubtype = onboardingScreens[currentScreenIndex].name)
                            FeedbackUtil.showAboutWikipedia(context)
                        }
                    )
                }
                OnboardingScreen.DATA_PRIVACY -> {
                    InitialOnboardingDataPrivacyContent(
                        onPrivacyClick = {
                            context.instrument?.submitInteraction("click", elementId = "privacy_link", actionSubtype = onboardingScreens[currentScreenIndex].name)
                            FeedbackUtil.showPrivacyPolicy(context)
                        },
                        onTermsClick = {
                            context.instrument?.submitInteraction("click", elementId = "terms_link", actionSubtype = onboardingScreens[currentScreenIndex].name)
                            FeedbackUtil.showTermsOfUse(context)
                        }
                    )
                }
                OnboardingScreen.LANGUAGES -> {
                    InitialOnboardingLanguagesScreen(
                        languageState = languageState,
                        appLanguageCodes = appLanguageCodes,
                        onAddLanguageClick = {
                            context.instrument?.submitInteraction("click", elementId = "add_language", actionSubtype = onboardingScreens[currentScreenIndex].name)
                            onAddLanguageClick()
                        }
                    )
                }
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

@Composable
fun InitialOnboardingLanguagesScreen(
    modifier: Modifier = Modifier,
    languageState: AppLanguageState?,
    appLanguageCodes: List<String>,
    onAddLanguageClick: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .size(124.dp),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.yir_puzzle_stone)
                    .allowHardware(false)
                    .build(),
                loading = { LoadingIndicator() },
                success = {
                    SubcomposeAsyncImageContent()
                },
                contentDescription = stringResource(R.string.onboarding_app_languages_title),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            text = stringResource(R.string.onboarding_app_languages_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.onboarding_app_languages_text),
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 24.dp)
                .lazyColumnScrollbar(
                    state = lazyListState,
                    color = WikipediaTheme.colors.inactiveColor
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(count = appLanguageCodes.size) {
                val isPrimary = it == 0
                if (isPrimary) {
                    Spacer(
                        modifier = Modifier.height(0.5.dp)
                            .fillMaxWidth()
                            .background(WikipediaTheme.colors.borderColor)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                InitialOnboardingLanguageItem(
                    languageState = languageState,
                    languageCode = appLanguageCodes[it],
                    isPrimary = isPrimary
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clickable(onClick = onAddLanguageClick)
        ) {
            Text(
                modifier = Modifier
                    .padding(vertical = 24.dp),
                text = stringResource(R.string.onboarding_app_languages_add_button),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = WikipediaTheme.colors.progressiveColor,
            )
        }
    }
}

@Composable
fun InitialOnboardingLanguageItem(
    languageState: AppLanguageState?,
    languageCode: String,
    isPrimary: Boolean
) {
    val localizedName = languageState?.getAppLanguageLocalizedName(languageCode) ?: languageCode
    Text(
        modifier = Modifier
            .fillMaxWidth(),
        text = localizedName,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = WikipediaTheme.colors.primaryColor
    )
    if (isPrimary) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.onboarding_app_languages_primary),
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.secondaryColor
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Spacer(
        modifier = Modifier
            .height(0.5.dp)
            .fillMaxWidth()
            .background(WikipediaTheme.colors.borderColor)
    )
}

@Preview(showBackground = true)
@Composable
fun InitialOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InitialOnboardingScreen(
            modifier = Modifier.fillMaxSize(),
            onboardingScreens = listOf(OnboardingScreen.INTRO, OnboardingScreen.DATA_PRIVACY, OnboardingScreen.LANGUAGES),
            languageState = null,
            appLanguageCodes = listOf("en", "es", "de"),
            onAddLanguageClick = {},
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

@Preview(showBackground = true)
@Composable
fun InitialOnboardingLanguagesLanguagesScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InitialOnboardingLanguagesScreen(
            languageState = null,
            appLanguageCodes = listOf("en", "es", "de"),
            onAddLanguageClick = {}
        )
    }
}
