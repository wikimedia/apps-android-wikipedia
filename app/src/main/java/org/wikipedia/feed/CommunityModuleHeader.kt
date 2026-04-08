package org.wikipedia.feed

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun CommunityModuleHeader(
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int,
    @StringRes subTitleResId: Int,
    @DrawableRes contextIconResId: Int? = null,
    onOverflowClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                text = stringResource(titleResId),
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
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    onOverflowClick()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            text = stringResource(subTitleResId),
            color = WikipediaTheme.colors.secondaryColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
fun CommunityModuleHeaderPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CommunityModuleHeader(
            titleResId = R.string.view_featured_image_card_title,
            subTitleResId = R.string.explore_feed_potd_subtitle,
            contextIconResId = R.drawable.ic_commons_logo
        )
    }
}
