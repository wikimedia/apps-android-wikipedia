package org.wikipedia.feed

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil

@Composable
fun CommunityModuleContainer(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    @StringRes titleResId: Int,
    @StringRes subTitleResId: Int,
    @DrawableRes contextIconResId: Int? = null,
    backgroundColor: Color = WikipediaTheme.colors.paperColor,
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {},
    onCardInView: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val minHeightForImpression = if (context is Activity) DimenUtil.dpToPx(150f) else 0f
    var expanded by remember { mutableStateOf(false) }
    var wasFullyOnScreen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .onGloballyPositioned { coordinates ->
                val ownBounds = coordinates.boundsInRoot()
                val isEnoughOnScreen = ownBounds.height > minHeightForImpression
                if (isEnoughOnScreen && !wasFullyOnScreen) {
                    onCardInView()
                }
                wasFullyOnScreen = isEnoughOnScreen
            }
    ) {
        Column {
            Row(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    text = context.getString(wikiSite.languageCode, titleResId),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.W500
                )
                contextIconResId?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box {
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = {
                            expanded = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                            contentDescription = context.getString(wikiSite.languageCode, R.string.menu_feed_overflow_label),
                            tint = WikipediaTheme.colors.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        offset = DpOffset(x = (-16).dp, y = 0.dp),
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = WikipediaTheme.colors.paperColor
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_no_sim_24dp),
                                    contentDescription = null,
                                    tint = WikipediaTheme.colors.secondaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_feed_card_dismiss),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WikipediaTheme.colors.primaryColor
                                )
                            },
                            onClick = {
                                onHideCardClick()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_visibility_off_24dp),
                                    contentDescription = null,
                                    tint = WikipediaTheme.colors.secondaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.explore_feed_header_overflow_hide_module_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WikipediaTheme.colors.primaryColor
                                )
                            },
                            onClick = {
                                onHideModuleClick()
                                expanded = false
                            }
                        )
                    }
                }
            }
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                text = context.getString(wikiSite.languageCode, subTitleResId),
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        content()
    }
}

@Preview
@Composable
fun CommunityModuleHeaderPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CommunityModuleContainer(
            wikiSite = WikiSite.preview(),
            titleResId = R.string.view_featured_image_card_title,
            subTitleResId = R.string.explore_feed_potd_subtitle,
            contextIconResId = R.drawable.ic_commons_logo
        ) { }
    }
}
