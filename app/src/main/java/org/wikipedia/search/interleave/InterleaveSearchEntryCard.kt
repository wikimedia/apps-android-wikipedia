package org.wikipedia.search.interleave

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.getString
import org.wikipedia.theme.Theme

@Composable
fun InterleaveSearchEntryCard(
    languageCode: String,
    onInfoButtonClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .wrapContentHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = WikipediaTheme.colors.borderColor,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    modifier = Modifier,
                    painter = painterResource(R.drawable.ic_experiment_24dp),
                    tint = WikipediaTheme.colors.secondaryColor,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = context.getString(languageCode, R.string.semantic_search_beta_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
            }

            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = { onInfoButtonClick() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                    tint = WikipediaTheme.colors.placeholderColor,
                    contentDescription = context.getString(languageCode, R.string.semantic_search_info_btn_content_description)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InterleaveSearchEntryCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InterleaveSearchEntryCard(
            languageCode = "en",
            onInfoButtonClick = {}
        )
    }
}
