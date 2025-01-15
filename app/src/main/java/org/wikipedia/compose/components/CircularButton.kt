package org.wikipedia.compose.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun CircularButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    defaultBgColor: Color = WikipediaTheme.colors.backgroundColor,
    selectedBgColor: Color = WikipediaTheme.colors.progressiveColor,
    size: Dp = 40.dp,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isSelected) selectedBgColor else defaultBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = {
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview
@Composable
private fun CircularButtonPreview() {
    BaseTheme {
        CircularButton(
            onClick = {},
            content = {}
        )
    }
}
