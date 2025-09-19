package org.wikipedia.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaSize
import com.hcaptcha.sdk.HCaptchaTokenResponse
import kotlinx.coroutines.launch
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.LicenseLinkText
import org.wikipedia.compose.components.LinkTextData
import org.wikipedia.compose.components.Snackbar
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L

class AboutActivity : BaseActivity() {
    private var hCaptcha: HCaptcha? = null
    private var tokenResponse: HCaptchaTokenResponse? = null


    private val credits = listOf(
        LinkTextData(
            text = "Balloon",
            url = "https://github.com/skydoves/Balloon/blob/main/LICENSE/"
        ),
        LinkTextData(
            text = "(license),",
            asset = "licenses/Balloon"
        ),
        LinkTextData(
            text = "Commons Lang",
            url = "https://www.apache.org/licenses/"
        ),
        LinkTextData(
            text = "(license),",
            asset = "licenses/CommonsLang3"
        ),
        LinkTextData(
            text = "jsoup",
            url = "https://github.com/jhy/jsoup"
        ),
        LinkTextData(
            text = "(license),",
            asset = "licenses/jsoup"
        ),
        LinkTextData(
            text = "OkHttp",
            url = "https://square.github.io/okhttp/"
        ),
        LinkTextData(
            text = "(license),",
            asset = "licenses/OkHttp"
        ),
        LinkTextData(
            text = "Retrofit",
            url = "https://square.github.io/retrofit/"
        ),
        LinkTextData(
            text = "(license)",
            asset = "licenses/Retrofit"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtil.setEdgeToEdge(this)
        setContent {
            BaseTheme {
                AboutWikipediaScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    versionName = BuildConfig.VERSION_NAME,
                    credits = credits,
                    onBackButtonClick = {
                        //onBackPressedDispatcher.onBackPressed()



                        verifyHCaptcha()
                        //hCaptcha = HCaptcha.getClient(this).setup(getHCaptchaConfig())
                        //setupHCaptchaClient(hCaptcha)



                    }
                )
            }
        }
    }

    private fun setupHCaptchaClient(captcha: HCaptcha?) {
        captcha?.addOnSuccessListener { response ->
            tokenResponse = response
            val userResponseToken = response.tokenResult
            L.d("hCaptcha token: $userResponseToken")
            finish()
        }?.addOnFailureListener { e ->
            L.e("hCaptcha failed: ${e.message} (${e.statusCode})")
            tokenResponse = null
            FeedbackUtil.showMessage(this, "hCaptcha failed: ${e.message} (${e.statusCode})")
        }?.addOnOpenListener {
            FeedbackUtil.showMessage(this, "hCaptcha shown")
        }
    }

    private fun getHCaptchaConfig(): HCaptchaConfig {
        val size = HCaptchaSize.NORMAL
        return HCaptchaConfig.builder()
            .siteKey("f1f21d64-6384-4114-b7d0-d9d23e203b4a") // << TODO: use our site key
            .size(size)
            .loading(true)
            .hideDialog(false)
            .tokenExpiration(10)
            .diagnosticLog(true)
            .retryPredicate { config, exception ->
                exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT
            }
            .build()
    }

    private fun verifyHCaptcha() {
        if (hCaptcha != null) {
            hCaptcha?.verifyWithHCaptcha()
        } else {
            hCaptcha = HCaptcha.getClient(this).verifyWithHCaptcha(getHCaptchaConfig())
            setupHCaptchaClient(hCaptcha)
        }
    }

    private fun resetHCaptcha() {
        hCaptcha?.reset()
        hCaptcha = null
    }

    private fun markHCaptchaUsed() {
        tokenResponse?.markUsed()
    }

    companion object {
        const val SECRET_CLICK_LIMIT = 7
    }
}

@Composable
fun AboutWikipediaScreen(
    modifier: Modifier = Modifier,
    versionName: String,
    credits: List<LinkTextData>,
    onBackButtonClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        message = it.visuals.message
                    )
                }
            )
        },
        topBar = {
            WikiTopAppBar(
                title = stringResource(R.string.about_activity_title),
                onNavigationClick = onBackButtonClick
            )
        },
        containerColor = WikipediaTheme.colors.paperColor,
        content = { paddingValues ->
           AboutScreenContent(
               modifier = Modifier
                   .padding(paddingValues),
               versionName = versionName,
               credits = credits,
               snackbarHostState = snackbarHostState
           )
        }
    )
}

@Composable
fun AboutScreenContent(
    modifier: Modifier = Modifier,
    versionName: String,
    credits: List<LinkTextData>,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AboutWikipediaHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, bottom = 16.dp),
            versionName = versionName,
            snackbarHostState = snackbarHostState
        )
        SelectionContainer {
            AboutScreenBody(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
                    .padding(horizontal = 16.dp),
                credits = credits
            )
        }
        AboutScreenFooter(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun AboutWikipediaHeader(
    modifier: Modifier = Modifier,
    versionName: String,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
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
        SelectionContainer {
            Text(
                modifier = Modifier
                    .padding(vertical = 16.dp),
                text = versionName,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    }
}

@Composable
fun AboutWikipediaImage(
    modifier: Modifier = Modifier,
    onSecretCountClick: (isEnabled: Boolean) -> Unit,
) {
    var secretClickCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
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
            contentDescription = stringResource(R.string.about_logo_content_description),
        )
        Image(
            modifier = Modifier
                .padding(top = 4.dp)
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
fun AboutScreenBody(
    modifier: Modifier = Modifier,
    credits: List<LinkTextData>
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        LinkTextWithHeader(
            header = stringResource(R.string.about_contributors_heading),
            html = stringResource(R.string.about_contributors)
        )

        LinkTextWithHeader(
            header = stringResource(R.string.about_translators_heading),
            html = stringResource(R.string.about_translators_translatewiki)
        )

        LicenseTextWithHeader(
            header = stringResource(R.string.about_libraries_heading),
            credits = credits
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(R.drawable.ic_wmf_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor)
        )
        HtmlText(
            text = stringResource(R.string.about_wmf),
            style = MaterialTheme.typography.bodySmall,
            color = WikipediaTheme.colors.secondaryColor,
            linkStyle = TextLinkStyles(
                style = SpanStyle(color = WikipediaTheme.colors.progressiveColor)
            )
        )
    }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
        HtmlText(
            text = html,
            linkStyle = linkStyles
        )
    }
}

@Composable
fun LicenseTextWithHeader(
    modifier: Modifier = Modifier,
    header: String,
    credits: List<LinkTextData>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.primaryColor
        )
        LicenseLinkText(
            links = credits
        )
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        AboutWikipediaScreen(
            modifier = Modifier
                .fillMaxSize(),
            versionName = "version name",
            credits = listOf(),
            onBackButtonClick = {}
        )
    }
}
