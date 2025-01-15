package org.wikipedia.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun OutlinedCircularButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    isSelected: Boolean = false,
    defaultBgColor: Color = WikipediaTheme.colors.secondaryColor,
    selectedBgColor: Color = WikipediaTheme.colors.paperColor,
    outlineColor: Color = WikipediaTheme.colors.progressiveColor,
    pressedColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    CircularButton(
        modifier = modifier
            .size(size)
            .border(
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isSelected) outlineColor else Color.Transparent
                ),
                shape = CircleShape
            ),
        defaultBgColor = defaultBgColor,
        selectedBgColor = selectedBgColor,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = pressedColor),
        onClick = onClick,
        content = { content() }
    )
}

@Preview
@Composable
private fun OutlinedCircularButtonPreview() {
    BaseTheme {
        OutlinedCircularButton(
            size = 45.dp,
            onClick = {},
            isSelected = true,
            content = {
                Text("Aa")
            },
        )
    }
}
