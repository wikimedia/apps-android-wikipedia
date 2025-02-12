package org.wikipedia.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.components.WikiTopAppBar
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.FileUtil.readFile

class LicenseActivity : BaseActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val asset = intent.getStringExtra(ASSET) ?: ""
        val text = readFile(assets.open(asset))
        val licenseText = text.replace("\n\n", "<br/><br/>")
        val strings = asset.split("/")
        val title = getString(R.string.license_title, strings[strings.size - 1].trim())

        setContent {
            BaseTheme {
                LicenseScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    title = title,
                    licenseText = licenseText,
                    onBackButtonClick = {
                        onBackPressed()
                    }
                )
            }
        }
    }

    @Composable
    fun LicenseScreen(
        modifier: Modifier = Modifier,
        title: String,
        licenseText: String?,
        onBackButtonClick: () -> Unit
    ) {
        Scaffold(
            topBar = {
                WikiTopAppBar(
                    title = title,
                    onNavigationClick = onBackButtonClick
                )
            },
            containerColor = WikipediaTheme.colors.paperColor
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .verticalScroll(state = rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = AnnotatedString.fromHtml(
                        htmlString = licenseText ?: ""
                    ),
                    color = WikipediaTheme.colors.inactiveColor
                )
            }
        }
    }

    companion object {
        const val ASSET = "asset"
    }
}
