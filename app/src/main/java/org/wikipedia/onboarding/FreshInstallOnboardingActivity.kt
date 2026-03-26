package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.addCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.yearinreview.LoadingIndicator

class FreshInstallOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, FreshInstallOnboardingActivity::class.java)
        }
    }
}

@Composable
fun FreshInstallOnboardingScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .safeDrawingPadding(),
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = {
                    // TODO: go to next
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                        tint = WikipediaTheme.colors.progressiveColor,
                        contentDescription = stringResource(R.string.nav_item_forward)
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
            FreshInstallOnboardingKnowledgeContent(
                onLearnMoreClick = {
                    // TODO: implement this
                }
            )
        }
    }
}

@Composable
fun FreshInstallOnboardingKnowledgeContent(
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
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
fun FreshInstallOnboardingDataPrivacyContent(
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.weight(1f))

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.yir_puzzle_cloud)
                .allowHardware(false)
                .build(),
            loading = { LoadingIndicator() },
            success = {
                SubcomposeAsyncImageContent()
            },
            contentDescription = stringResource(R.string.onboarding_data_and_privacy_title),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            text = stringResource(R.string.onboarding_data_and_privacy_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.onboarding_data_and_privacy_text),
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(onClick = onLearnMoreClick),
            text = stringResource(R.string.onboarding_data_and_privacy_learn_more),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = WikipediaTheme.colors.progressiveColor
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun FreshInstallOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FreshInstallOnboardingScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun FreshInstallOnboardingKnowledgeContentPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        FreshInstallOnboardingKnowledgeContent(
            onLearnMoreClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FreshInstallOnboardingDataPrivacyContentPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        FreshInstallOnboardingDataPrivacyContent(
            onLearnMoreClick = {}
        )
    }
}
