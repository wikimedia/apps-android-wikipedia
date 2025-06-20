package org.wikipedia.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WikipediaTheme.colors.progressiveColor,
    contentColor: Color = WikipediaTheme.colors.paperColor,
    content: @Composable (() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        content()
    }
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = WikipediaTheme.colors.progressiveColor,
    content: @Composable (() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(0.dp)
    ) {
        content()
    }
}

@Composable
fun OutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WikipediaTheme.colors.paperColor,
    contentColor: Color = WikipediaTheme.colors.progressiveColor,
    borderColor: Color = WikipediaTheme.colors.borderColor,
    cornerRadius: Int = 8,
    strokeWidth: Int = 1,
    content: @Composable (() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(
            width = strokeWidth.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius.dp)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        content()
    }
}

@Composable
fun SmallOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WikipediaTheme.colors.paperColor,
    contentColor: Color = WikipediaTheme.colors.progressiveColor,
    borderColor: Color = WikipediaTheme.colors.borderColor,
    cornerRadius: Int = 16,
    content: @Composable (() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius.dp)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        content()
    }
}

@Composable
fun ThemeColorCircularButton(
    onClick: () -> Unit,
    text: String = "Aa",
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    defaultBackgroundColor: Color = WikipediaTheme.colors.paperColor,
    selectedBackgroundColor: Color = WikipediaTheme.colors.backgroundColor,
    borderColor: Color = WikipediaTheme.colors.progressiveColor,
    textColor: Color = WikipediaTheme.colors.primaryColor,
    rippleColor: Color = Color.Transparent,
    isSelected: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .border(
                width = 2.dp,
                color = if (isSelected) borderColor else Color.Transparent,
                shape = CircleShape
            )
            .size(size),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) defaultBackgroundColor else selectedBackgroundColor,
            contentColor = rippleColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Preview
@Composable
private fun SepiaThemeColorButton() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ThemeColorCircularButton(
                isSelected = true,
                defaultBackgroundColor = ComposeColors.Beige300,
                selectedBackgroundColor = ComposeColors.Beige300,
                rippleColor = ComposeColors.Beige300,
                textColor = ComposeColors.Gray700,
                onClick = {}
            )
        }
    }
}
