package org.wikipedia.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun TabsBox(
    modifier: Modifier = Modifier,
    count: Int,
    backgroundColor: Color = WikipediaTheme.colors.paperColor,
    borderColor: Color = WikipediaTheme.colors.primaryColor,
    shape: RoundedCornerShape = RoundedCornerShape(5.dp),
    textColor: Color = WikipediaTheme.colors.primaryColor,
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = shape)
            .border(width = 1.5.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(start = 3.dp, end = 4.dp),
            text = count.toString(),
            autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = 10.sp, stepSize = 1.sp),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            lineHeight = 12.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun TabsBoxSmallNumberPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        IconButton(
            modifier = Modifier
                .statusBarsPadding(),
            onClick = { }
        ) {
            TabsBox(
                modifier = Modifier
                    .size(21.dp, 20.dp),
                count = 1
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TabsBoxSmallLargePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        IconButton(
            modifier = Modifier
                .statusBarsPadding(),
            onClick = { }
        ) {
            TabsBox(
                modifier = Modifier
                    .size(21.dp, 20.dp),
                count = 99
            )
        }
    }
}
