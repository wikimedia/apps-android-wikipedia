package org.wikipedia.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.WikipediaTheme

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
    backgroundColor: Color,
    borderColor: Color
) {
    Button(
        onClick = onClick,
        modifier = modifier.border(
            width = 2.dp,
            color = borderColor,
            shape = CircleShape
        ).size(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            style = WikipediaTheme.typography.h3.copy(
                color = WikipediaTheme.colors.primaryColor,
                letterSpacing = 0.sp
            )
        )
    }
}
