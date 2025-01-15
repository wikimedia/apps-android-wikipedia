package org.wikipedia.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.OutlinedCircularButton

data class ThemeChooser(
    val name: String,
    val theme: Theme,
    val defaultBackgroundColor: Color,
    val selectedBackgroundColor: Color,
    val pressedColor: Color,
    val textColor: Color
)

@Composable
fun ThemeChooserButtons(
    modifier: Modifier = Modifier,
    currentTheme: Theme = WikipediaApp.instance.currentTheme,
    onThemeClick: (selectedTheme: ThemeChooser) -> Unit
) {
    val buttons = remember {
        mutableStateListOf(
            ThemeChooser(
                name = "Aa",
                theme = Theme.LIGHT,
                defaultBackgroundColor = ComposeColors.White,
                selectedBackgroundColor = ComposeColors.White,
                pressedColor = ComposeColors.Gray300,
                textColor = ComposeColors.Gray700
            ),
            ThemeChooser(
                name = "Aa",
                theme = Theme.SEPIA,
                defaultBackgroundColor = ComposeColors.Beige300,
                selectedBackgroundColor = ComposeColors.Beige300,
                pressedColor = ComposeColors.Beige300,
                textColor = ComposeColors.Gray700
            ),
            ThemeChooser(
                name = "Aa",
                theme = Theme.DARK,
                defaultBackgroundColor = ComposeColors.Gray700,
                selectedBackgroundColor = ComposeColors.Gray700,
                pressedColor = ComposeColors.Gray600,
                textColor = ComposeColors.Gray100
            ),
            ThemeChooser(
                name = "Aa",
                theme = Theme.BLACK,
                defaultBackgroundColor = ComposeColors.Black,
                selectedBackgroundColor = ComposeColors.Black,
                pressedColor = ComposeColors.Gray700,
                textColor = ComposeColors.Gray100
            )
        )
    }
    var currentSelectedTheme by remember { mutableStateOf(buttons.find { it.theme == currentTheme }) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        buttons.forEach { themeChooser ->
            OutlinedCircularButton(
                onClick = {
                    currentSelectedTheme = themeChooser
                    onThemeClick(themeChooser)
                },
                defaultBgColor = themeChooser.defaultBackgroundColor,
                selectedBgColor = themeChooser.selectedBackgroundColor,
                isSelected = currentSelectedTheme?.theme == themeChooser.theme,
                pressedColor = themeChooser.pressedColor,
                content = {
                    Text(
                        text = themeChooser.name,
                        color = themeChooser.textColor,
                        fontSize = 16.sp
                    )
                }
            )
        }
    }
}
