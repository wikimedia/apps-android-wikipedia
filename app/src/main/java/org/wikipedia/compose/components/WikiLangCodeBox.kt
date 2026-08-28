package org.wikipedia.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun WikiLangCodeBox(
    modifier: Modifier = Modifier,
    languageCode: String,
    backgroundColor: Color = WikipediaTheme.colors.paperColor,
    borderColor: Color = WikipediaTheme.colors.primaryColor,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    textColor: Color = WikipediaTheme.colors.primaryColor,
    fontSize: TextUnit = 10.sp,
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = shape)
            .border(width = 1.5.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = languageCode.uppercase(),
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Preview
@Composable
private fun WikiLangCodeBoxRegularPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        WikiLangCodeBox(
            modifier = Modifier
                .height(20.dp)
                .widthIn(min = 20.dp),
            languageCode = "en"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WikiLangCodeBoxDarkPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        WikiLangCodeBox(
            modifier = Modifier
                .height(20.dp)
                .widthIn(min = 20.dp),
            languageCode = "en",
            backgroundColor = WikipediaTheme.colors.primaryColor,
            borderColor = WikipediaTheme.colors.primaryColor,
            textColor = WikipediaTheme.colors.paperColor
        )
    }
}
