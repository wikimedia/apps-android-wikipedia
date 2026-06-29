package org.wikipedia.yir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.BaseTheme

/** A call-to-action on a framing card. The first one renders as the prominent (filled) button. */
data class YirCta(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun YirFramingContent(
    headline: String,
    supportingText: String,
    ctas: List<YirCta>,
    modifier: Modifier = Modifier,
    hint: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headline,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = supportingText,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 40.dp)
        )

        hint?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        ctas.forEachIndexed { index, cta ->
            if (index == 0) {
                Button(
                    onClick = cta.onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(cta.label, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = cta.onClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(cta.label)
                }
            }
        }
    }
}

@Preview
@Composable
private fun YirFramingContentOpeningPreview() {
    BaseTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A3D3A))) {
            YirFramingContent(
                headline = "Your 2025 in Review",
                supportingText = "A look back at what you read, explored and discovered this year.",
                ctas = listOf(YirCta("Begin") {})
            )
        }
    }
}

@Preview
@Composable
private fun YirFramingContentClosingPreview() {
    BaseTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A3D3A))) {
            YirFramingContent(
                headline = "That's a wrap!",
                supportingText = "Thanks for reading with us this year.",
                ctas = listOf(
                    YirCta("Share") {},
                    YirCta("Explore Wikipedia") {},
                    YirCta("Set up recommended reading") {}
                )
            )
        }
    }
}
