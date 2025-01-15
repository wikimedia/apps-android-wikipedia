package org.wikipedia.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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

class WikiButtons {

    @Composable
    fun Button(
        text: String,
        backgroundColor: Color = WikipediaTheme.colors.progressiveColor,
        contentColor: Color = WikipediaTheme.colors.paperColor,
        icon: @Composable (() -> Unit)? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
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
            icon?.invoke()
            Text(
                text = text,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
            )
        }
    }

    @Composable
    fun TextButton(
        text: String,
        contentColor: Color = WikipediaTheme.colors.progressiveColor,
        icon: @Composable (() -> Unit)? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
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
            icon?.invoke()
            Text(
                text = text,
                fontSize = 16.sp
            )
        }
    }

    @Composable
    fun OutlineButton(
        text: String,
        backgroundColor: Color = WikipediaTheme.colors.paperColor,
        contentColor: Color = WikipediaTheme.colors.progressiveColor,
        borderColor: Color = WikipediaTheme.colors.borderColor,
        cornerRadius: Int = 8,
        strokeWidth: Int = 1,
        icon: @Composable (() -> Unit)? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
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
            icon?.invoke()
            Text(
                text = text,
                fontSize = 16.sp
            )
        }
    }

    @Composable
    fun SmallOutlineButton(
        text: String,
        backgroundColor: Color = WikipediaTheme.colors.paperColor,
        contentColor: Color = WikipediaTheme.colors.progressiveColor,
        borderColor: Color = WikipediaTheme.colors.borderColor,
        cornerRadius: Int = 16,
        icon: @Composable (() -> Unit)? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
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
            icon?.invoke()
            Text(
                text = text,
                fontSize = 16.sp
            )
        }
    }

    @Composable
    fun ThemeColorCircularButton(
        text: String = "Aa",
        backgroundColor: Color,
        contentColor: Color,
        borderColor: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Button(
            onClick = onClick,
            modifier = modifier.size(40.dp).border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor
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
}
