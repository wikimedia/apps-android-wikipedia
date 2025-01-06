package org.wikipedia.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.DialogProperties

@Composable
fun ThemeButtons(
    modifier: Modifier = Modifier,
    onLightThemeClick: () -> Unit,
    onDarkThemeClick: () -> Unit,
    onBlackThemeClick: () -> Unit,
    onSepiaThemeClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ThemedButton(
            onClick = onLightThemeClick
        ) {
            Text("Light")
        }
        ThemedButton(
            onClick = onDarkThemeClick
        ) {
            Text("Dark")
        }
        ThemedButton(
            onClick = onBlackThemeClick
        ) {
            Text("Black")
        }
        ThemedButton(
            onClick = onSepiaThemeClick
        ) {
            Text("Sepia")
        }
    }
}

@Composable
fun ThemedButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = NewTheme.colors.progressiveColor,
            contentColor = NewTheme.colors.primaryColor
        )
    ) {
        content()
    }
}

@Composable
fun NewWikiCardView(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ThemedElevatedCard(
        modifier = modifier,
        onClick = onClick,
        content = { content() }
    )
}

@Composable
fun ThemedElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = NewTheme.colors.paperColor,
            contentColor = NewTheme.colors.primaryColor
        ),
        content = {
            content()
        }
    )
}

@Composable
fun ThemedText(
    text: String,
    color: Color = NewTheme.colors.primaryColor,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemedDatePicker(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(
        containerColor = NewTheme.colors.progressiveColor
    ),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        shape = shape,
        tonalElevation = tonalElevation,
        colors = colors,
        properties = properties,
        content = content
    )
}
