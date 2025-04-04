package org.wikipedia.compose.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme

@Composable
fun WikiCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = WikipediaApp.instance.currentTheme.isDark,
    elevation: Dp = 8.dp,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = WikipediaTheme.colors.paperColor,
        contentColor = WikipediaTheme.colors.paperColor
    ),
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val cardElevation = remember(elevation, isDarkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isDarkTheme) {
            0.dp
        } else {
            elevation
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = colors,
        border = border,
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String,
    imageRes: Int? = null,
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    onPositiveButtonClick: (() -> Unit)? = null,
    onNegativeButtonClick: (() -> Unit)? = null,
    onContainerClick: (() -> Unit)? = null,
    isNegativeButtonVisible: Boolean = true
) {

    WikiCard(modifier = modifier) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onContainerClick != null) {
                onContainerClick?.invoke()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                if (title != null) {
                    Text(
                        text = title,
                        style = WikipediaTheme.typography.h2,
                        color = WikipediaTheme.colors.primaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                HtmlText(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = message,
                    style = TextStyle(
                        color = WikipediaTheme.colors.secondaryColor,
                        fontSize = 16.sp
                    ),
                    linkStyle = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = 16.sp,
                        )
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (positiveButtonText != null) {
                        AppButton(
                            onClick = { onPositiveButtonClick?.invoke() },
                            modifier = Modifier.padding(end = 8.dp),
                            backgroundColor = WikipediaTheme.colors.backgroundColor,
                            contentColor = WikipediaTheme.colors.progressiveColor
                        ) {
                            Text(
                                text = positiveButtonText,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (negativeButtonText != null && isNegativeButtonVisible) {
                        AppTextButton(
                            onClick = { onNegativeButtonClick?.invoke() }
                        ) {
                            Text(
                                text = negativeButtonText,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WikiCardSimpleWikiTextPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiCard(
            modifier = Modifier
                .padding(20.dp),
            isDarkTheme = false
        ) {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = "Text example in a WikiCard",
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BorderAndElevationWikiTextPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        WikiCard(
            modifier = Modifier
                .padding(20.dp),
            border = BorderStroke(width = 0.5.dp, color = WikipediaTheme.colors.progressiveColor),
            elevation = 4.dp,
            isDarkTheme = true
        ) {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = "Text example in a WikiCard",
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}
