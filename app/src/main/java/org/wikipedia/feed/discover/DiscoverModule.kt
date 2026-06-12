package org.wikipedia.feed.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.FeedCtaPromptModule
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.theme.Theme
private val discoverPromptImageUrls = listOf(
    "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Red_eyed_tree_frog_edit2.jpg/960px-Red_eyed_tree_frog_edit2.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Palais_de_l%27Industrie_-_%C3%89douard_Baldus.jpg/1920px-Palais_de_l%27Industrie_-_%C3%89douard_Baldus.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/1/17/Monet_w861.jpg?_=20230407213842",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Mercury_in_color_-_Prockter07_centered.jpg/1280px-Mercury_in_color_-_Prockter07_centered.jpg"
)

@Composable
fun DiscoverEnablePromptModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    onEnableDiscoverClick: () -> Unit = {}
) {
    val context = LocalContext.current
    FeedCtaPromptModule(
        modifier = modifier,
        title = context.getString(wikiSite.languageCode, R.string.home_feed_discover_cta_title),
        description = context.getString(wikiSite.languageCode, R.string.home_feed_discover_cta_description),
        buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_discover_cta_button),
        buttonIcon = painterResource(R.drawable.ic_lightbulb_24dp),
        imageUrls = discoverPromptImageUrls,
        onButtonClick = onEnableDiscoverClick
    )
}

@Preview
@Composable
private fun DiscoverEnablePromptModulePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        DiscoverEnablePromptModule(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColors.Green800)
                .padding(horizontal = 16.dp),
            wikiSite = WikiSite.preview()
        )
    }
}
