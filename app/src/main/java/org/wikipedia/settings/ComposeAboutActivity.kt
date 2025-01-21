package org.wikipedia.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

class ComposeAboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                AboutWikipediaScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WikipediaTheme.colors.paperColor),
                    versionName = BuildConfig.VERSION_NAME
                )
            }
        }
    }
}

@Composable
fun AboutWikipediaScreen(
    modifier: Modifier = Modifier,
    versionName: String,
) {
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = "About",
                onNavigationClick = {}
            )
        },
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AboutWikipediaImage()
                Text(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    text = versionName,
                    fontSize = 14.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
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
        }
    )
}

@Composable
fun AboutWikipediaImage(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .size(88.dp),
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
fun LinkTextWithHeader(
    modifier: Modifier = Modifier,
    header: String,
    html: String,
    linkStyles: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = WikipediaTheme.colors.progressiveColor
        )
    ),
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
            linkStyles = linkStyles
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
            versionName = BuildConfig.VERSION_NAME
        )
    }
}
