package org.wikipedia.yearinreview.presentation

import android.graphics.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme

private val EntryCardGradient = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val center = Offset(size.width * 0.5f, size.height * 1.22f)
        val radiusX = size.width * 4f
        val radiusY = size.height * 1.6f
        return RadialGradientShader(
            center = center,
            radius = radiusX,
            colors = listOf(
                Color(0xFFF2F2F2),
                Color(0xFF44D3F0),
                Color(0xFF0099DA),
                Color(0xFF01427A),
                Color(0xFF0E2651),
                Color(0xFF061737)
            ),
            colorStops = listOf(0.149f, 0.2548f, 0.3462f, 0.5337f, 0.6298f, 0.7452f)
        ).apply {
            setLocalMatrix(Matrix().apply { setScale(1f, radiusY / radiusX, center.x, center.y) })
        }
    }
}

private val RobotoFlexFontFamily = FontFamily(
    Font(
        R.font.robotoflex,
        weight = FontWeight.W500,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(150f)
        )
    )
)

@Composable
fun YearInReviewEntryCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onGetStartedClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EntryCardGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                color = ComposeColors.White,
                fontFamily = RobotoFlexFontFamily,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = ComposeColors.White
            )
            Button(
                modifier = Modifier.padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeColors.Blue600,
                    contentColor = ComposeColors.White
                ),
                onClick = onGetStartedClick
            ) {
                Text(
                    text = stringResource(R.string.year_in_review_get_started),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview
@Composable
private fun YearInReviewEntryCardPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewEntryCard(
            modifier = Modifier.padding(16.dp),
            title = stringResource(R.string.year_in_review_entry_card_title_collective),
            subtitle = stringResource(R.string.year_in_review_entry_card_subtitle),
            onGetStartedClick = {}
        )
    }
}
