package org.wikipedia.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.Snackbar
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                AboutWikipediaScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    versionName = BuildConfig.VERSION_NAME,
                    onBackButtonClick = {
                        onBackPressed()
                    }
                )
            }
        }
    }

    companion object {
        const val SECRET_CLICK_LIMIT = 7
    }
}

@Composable
fun AboutWikipediaScreen(
    modifier: Modifier = Modifier,
    versionName: String,
    onBackButtonClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        message = it.visuals.message
                    )
                }
            )
        },
        topBar = {
            WikiTopAppBar(
                title = "About",
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AboutWikipediaImage(
                    onSecretCountClick = { isEnabled ->
                        scope.launch {
                            when (isEnabled) {
                                true -> {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.show_developer_settings_already_enabled),
                                        duration = SnackbarDuration.Short
                                    )
                                }

                                false -> {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.show_developer_settings_enabled),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                )
                Text(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    text = versionName,
                    fontSize = 14.sp,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(Modifier.height(20.dp))
                AboutScreenBody(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
                AboutScreenFooter()
            }
        }
    )
}

@Composable
fun AboutWikipediaImage(
    modifier: Modifier = Modifier,
    onSecretCountClick: (isEnabled: Boolean) -> Unit,
) {
    var secretClickCount by remember { mutableStateOf(0) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .size(88.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        secretClickCount++
                        if (secretClickCount == AboutActivity.SECRET_CLICK_LIMIT) {
                            if (Prefs.isShowDeveloperSettingsEnabled) {
                                onSecretCountClick(true)
                            } else {
                                Prefs.isShowDeveloperSettingsEnabled = true
                                onSecretCountClick(false)
                            }
                        }
                    },
                ),
            painter = painterResource(R.drawable.w_nav_mark),
            contentDescription = null,
        )
        Image(
            modifier = Modifier
                .size(
                    height = 22.dp,
                    width = 114.dp
                ),
            painter = painterResource(R.drawable.wp_wordmark),
            colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.primaryColor),
            contentDescription = null,
        )
    }
}

@Composable
fun AboutScreenBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinkTextWithHeader(
            header = stringResource(R.string.about_contributors_heading),
            html = stringResource(R.string.about_contributors)
        )

        LinkTextWithHeader(
            header = stringResource(R.string.about_translators_heading),
            html = stringResource(R.string.about_translators_translatewiki)
        )

        LinkTextWithHeader(
            header = stringResource(R.string.about_libraries_heading),
            html = stringResource(R.string.libraries_list)
        )

        LinkTextWithHeader(
            header = stringResource(R.string.about_app_license_heading),
            html = stringResource(R.string.about_app_license)
        )
    }
}

@Composable
fun AboutScreenFooter(
    modifier: Modifier = Modifier
) {
    Image(
        modifier = Modifier
            .size(24.dp),
        painter = painterResource(R.drawable.ic_wmf_logo),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor)
    )
    Spacer(
        modifier = Modifier
            .padding(
                bottom = 16.dp
            )
    )
    HtmlText(
        html = stringResource(R.string.about_wmf),
        linkStyle = TextLinkStyles(
            style = SpanStyle(
                color = WikipediaTheme.colors.progressiveColor,
                fontSize = 14.sp,
            )
        )
    )
}

@Composable
fun LinkTextWithHeader(
    modifier: Modifier = Modifier,
    header: String,
    html: String,
    linkStyles: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = WikipediaTheme.colors.progressiveColor
        )
    )
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = header,
            fontSize = 16.sp,
            color = WikipediaTheme.colors.primaryColor
        )
        HtmlText(
            html = html,
            linkStyle = linkStyles
        )
    }
}

@Preview
@Composable
private fun AboutWikipediaImagePreview() {
    BaseTheme {
        AboutWikipediaScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            versionName = BuildConfig.VERSION_NAME,
            onBackButtonClick = {}
        )
    }
}
