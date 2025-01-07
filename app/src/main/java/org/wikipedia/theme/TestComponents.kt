package org.wikipedia.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.DialogProperties

data class TestButton(
    val title: String,
    val themeType: WikipediaThemeType,
    var isSelected: Boolean = false
)

@Composable
fun ThemeButtons(
    modifier: Modifier = Modifier,
    onLightThemeClick: () -> Unit,
    onDarkThemeClick: () -> Unit,
    onBlackThemeClick: () -> Unit,
    onSepiaThemeClick: () -> Unit
) {
    val buttons = remember {
        mutableStateListOf(
            TestButton("Light", themeType = WikipediaThemeType.LIGHT, isSelected = true),
            TestButton("Dark", themeType = WikipediaThemeType.DARK),
            TestButton("Black", themeType = WikipediaThemeType.BLACK),
            TestButton("Sepia", themeType = WikipediaThemeType.SEPIA)
        )
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        buttons.forEach { currentButton ->
            ThemedButton(
                onClick = {
                    when (currentButton.themeType) {
                        WikipediaThemeType.SYSTEM -> {}
                        WikipediaThemeType.LIGHT -> onLightThemeClick()
                        WikipediaThemeType.DARK -> onDarkThemeClick()
                        WikipediaThemeType.BLACK -> onBlackThemeClick()
                        WikipediaThemeType.SEPIA -> onSepiaThemeClick()
                    }
                    buttons.forEach {
                        it.isSelected = false
                    }
                    currentButton.isSelected = true
                },
                content = {
                    Text(
                        currentButton.title,
                        color = if (currentButton.isSelected) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.paperColor,

                        )
                },
                isSelected = currentButton.isSelected
            )
        }
    }
}

@Composable
fun ThemedButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
    isSelected: Boolean = false,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) WikipediaTheme.colors.progressiveColor else WikipediaTheme.colors.warningColor,
            contentColor = WikipediaTheme.colors.primaryColor
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
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.primaryColor
        ),
        content = {
            content()
        }
    )
}

@Composable
fun ThemedText(
    text: String,
    color: Color = WikipediaTheme.colors.primaryColor,
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
    datePickerState: DatePickerState = rememberDatePickerState(),
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(
        containerColor = WikipediaTheme.colors.paperColor,
        titleContentColor = WikipediaTheme.colors.destructiveColor,
        headlineContentColor = WikipediaTheme.colors.warningColor,
        dayContentColor = WikipediaTheme.colors.progressiveColor,
        weekdayContentColor = WikipediaTheme.colors.progressiveColor,
    ),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
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
        content = {
            DatePicker(
                state = datePickerState,
                colors = colors
            )
        }
    )
}
