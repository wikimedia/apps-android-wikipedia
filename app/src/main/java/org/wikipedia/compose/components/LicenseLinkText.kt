package org.wikipedia.compose.components

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.settings.LicenseActivity

data class LinkTextData(
    val text: String,
    val url: String? = null,
    val asset: String? = null,
)

@Composable
fun LicenseLinkText(
    links: List<LinkTextData>,
    textStyle: TextStyle = WikipediaTheme.typography.small,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val annotatedString = buildAnnotatedString {
        links.forEach { linkTextData ->
            withLink(
                link = LinkAnnotation.Url(
                    url = linkTextData.url ?: "",
                    styles = TextLinkStyles(
                        style = SpanStyle(color = WikipediaTheme.colors.progressiveColor)
                    ),
                    linkInteractionListener = {
                        if (linkTextData.asset != null) {
                            println("orange: content ${linkTextData.text}")
                            val intent = Intent(context, LicenseActivity::class.java)
                            intent.putExtra(LicenseActivity.ASSET, linkTextData.asset)
                            context.startActivity(intent)
                        } else {
                            println("orange: url ${linkTextData.text}")
                           uriHandler.openUri(linkTextData.url ?: "")
                        }
                    }
                )
            ) {
                println("orange: ${linkTextData.text}")
                append(linkTextData.text)
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = textStyle
    )
}
