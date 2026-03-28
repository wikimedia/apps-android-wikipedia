package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class AppLanguagesOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                AppLanguagesOnboardingScreen(
                    onAddLanguageClick = {
                    },
                    onNextClick = {
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, AppLanguagesOnboardingActivity::class.java)
        }
    }
}

@Composable
fun AppLanguagesOnboardingScreen(
    modifier: Modifier = Modifier,
    onAddLanguageClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Spacer(
                    modifier = Modifier.height(1.dp)
                        .fillMaxWidth()
                        .background(WikipediaTheme.colors.borderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(onClick = onNextClick) {
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
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues), // TODO: think about scrollable state
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                contentDescription = stringResource(R.string.onboarding_data_privacy_title),
            )

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Spacer(
                    modifier = Modifier.height(1.dp)
                        .fillMaxWidth()
                        .background(WikipediaTheme.colors.borderColor)
                )

                // TODO: finish this
                LazyColumn(
                    modifier = Modifier,
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(count = 3) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = "English",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = WikipediaTheme.colors.primaryColor
                        )
                        if (it == 0) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                text = stringResource(R.string.onboarding_app_languages_primary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WikipediaTheme.colors.secondaryColor
                            )
                        }
                        Spacer(
                            modifier = Modifier.height(1.dp)
                                .fillMaxWidth()
                                .background(WikipediaTheme.colors.borderColor)
                        )
                    }
                }
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(onClick = onAddLanguageClick),
                text = stringResource(R.string.onboarding_app_languages_add_button),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppLanguagesOnboardingScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        AppLanguagesOnboardingScreen(
            onAddLanguageClick = {},
            onNextClick = {}
        )
    }
}
