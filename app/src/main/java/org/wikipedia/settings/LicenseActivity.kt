package org.wikipedia.settings

import android.os.Bundle
import android.text.Spanned
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.toSpanned
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.util.FileUtil.readFile
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import java.io.IOException

class LicenseActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground))

        val libraryNameStart = 24
        val path = (intent.data ?: return).path ?: return
        if (path.length > libraryNameStart) {
            // Example string: "/android_asset/licenses/Otto"
            title = getString(R.string.license_title, path.substring(libraryNameStart))
            var htmlText: Spanned = "".toSpanned()
            htmlText = try {
                val assetPathStart = 15
                val textFromFile = readFile(assets.open(path.substring(assetPathStart)))
                StringUtil.fromHtml(textFromFile.replace("\n\n", "<br/><br/>"))
            } catch (e: IOException) {
                e.printStackTrace()
                e.toString().toSpanned()
            } finally {
                setContent {
                    LicenseContent(htmlText)
                }
            }
        }
    }

    @Preview
    @Composable
    private fun LicenseContentPreview() {
        // You cannot preview the AndroidView in Compose.
        LicenseContent("Preview Content".toSpanned())
    }

    @Composable
    private fun LicenseContent(licenseText: Spanned) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // The Compose Text cannot render HTML, so we use AndroidView to render the HTML text.
            AndroidView(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                factory = { context -> TextView(context) },
                update = { textView ->
                    textView.setTextColor(ResourceUtil.getThemedColor(this@LicenseActivity, R.attr.placeholder_color))
                    textView.text = licenseText
                    textView.textSize = 14f
                }
            )
        }
    }
}
