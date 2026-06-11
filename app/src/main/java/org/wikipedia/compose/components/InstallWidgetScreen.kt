package org.wikipedia.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
fun InstallWidgetScreen(
    title: String,
    message: String,
    onCloseClick: () -> Unit,
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Text(
                modifier = Modifier
                    .weight(1f),
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = WikipediaTheme.colors.primaryColor
            )

            IconButton(
                modifier = Modifier
                    .offset(x = 12.dp, y = (-6).dp),
                onClick = onCloseClick
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_close_black_24dp),
                    contentDescription = stringResource(R.string.dialog_close_description),
                    tint = WikipediaTheme.colors.primaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = WikipediaTheme.colors.secondaryColor
        )

        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .height(190.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            content = previewContent
        )

        bottomContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun InstallWidgetScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        InstallWidgetScreen(
            title = "A Faster way to Search",
            message = "Install the Wikipedia Search widget for instant access to knowledge from your home screen.",
            onCloseClick = {},
            previewContent = {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.reading_challenge_blur_background),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = null
                )
            },
            bottomContent = {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Text(text = "Add")
                }
            }
        )
    }
}
