package org.wikipedia.yir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.BaseTheme

/** One guess option. [isCorrect] is the local stand-in for the real answer (no backend in the spike). */
data class YirGuessOption(
    val title: String,
    val description: String,
    val isCorrect: Boolean
)

/**
 * Interactive card: the user guesses, submits, then sees the personalized result.
 *
 * States: pre-selection (tap to highlight, can change) -> after Submit, the result. On submit the
 * correct option is highlighted; if the user was wrong their pick is also marked, but they are
 * never blocked from continuing. The result (headline + supporting copy + optional save CTA) stays
 * visible until the user swipes to the next card.
 *
 * Wireframe note: the spec separates a "submitted feedback" state from a "reveal" state; here they
 * are shown together after Submit to keep the spike simple. (See NOTES / caveats.)
 */
@Composable
fun YirInteractiveContent(
    prompt: String,
    options: List<YirGuessOption>,
    resultHeadline: String,
    resultSupportingText: String,
    onSaveArticle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var submitted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 72.dp, bottom = 32.dp)
    ) {
        Text(
            text = prompt,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Spacer(Modifier.height(24.dp))

        options.forEachIndexed { index, option ->
            GuessOptionRow(
                option = option,
                selected = index == selectedIndex,
                submitted = submitted,
                onClick = { if (!submitted) selectedIndex = index }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))

        if (!submitted) {
            Button(
                onClick = { submitted = true },
                enabled = selectedIndex >= 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit", fontWeight = FontWeight.Bold)
            }
        } else {
            ResultBlock(
                headline = resultHeadline,
                supportingText = resultSupportingText,
                onSaveArticle = onSaveArticle
            )
        }
    }
}

@Composable
private fun GuessOptionRow(
    option: YirGuessOption,
    selected: Boolean,
    submitted: Boolean,
    onClick: () -> Unit
) {
    // Resolve the visual state. Pre-submit: selection just shows a highlight border. Post-submit:
    // the correct answer is greened, a wrong pick is reddened, everything else is dimmed.
    val correctColor = Color(0xFF1E8E5A)
    val wrongColor = Color(0xFFB3261E)

    val containerColor: Color
    val borderColor: Color
    when {
        submitted && option.isCorrect -> {
            containerColor = correctColor
            borderColor = Color.White
        }
        submitted && selected && !option.isCorrect -> {
            containerColor = wrongColor
            borderColor = Color.White
        }
        submitted -> {
            containerColor = Color.White.copy(alpha = 0.25f)
            borderColor = Color.Transparent
        }
        selected -> {
            containerColor = Color.White
            borderColor = Color(0xFF1B5FB0)
        }
        else -> {
            containerColor = Color.White.copy(alpha = 0.92f)
            borderColor = Color.Transparent
        }
    }

    val onContainer = if (containerColor == correctColor || containerColor == wrongColor) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !submitted, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                color = onContainer,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 17.sp
            )
            Text(
                text = option.description,
                color = onContainer.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        // Placeholder thumbnail.
        Spacer(Modifier.size(12.dp))
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(onContainer.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun ResultBlock(
    headline: String,
    supportingText: String,
    onSaveArticle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = headline,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = supportingText,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        Button(
            onClick = onSaveArticle,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save article", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Swipe up to continue",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private val sampleGuessOptions = listOf(
    YirGuessOption("The Birth of Venus", "Painting by Sandro Botticelli", isCorrect = true),
    YirGuessOption("Etel Adnan", "Lebanese-American writer and artist (1925–2021)", isCorrect = false),
    YirGuessOption("Camille Paglia", "American feminist academic and critic (born 1947)", isCorrect = false),
    YirGuessOption("Impression, Sunrise", "1872 painting by Claude Monet", isCorrect = false)
)

@Preview
@Composable
private fun YirInteractiveContentPreview() {
    BaseTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF12B36B))) {
            YirInteractiveContent(
                prompt = "Guess your most-read article this year",
                options = sampleGuessOptions,
                resultHeadline = "Your most-read article was The Birth of Venus",
                resultSupportingText = "You opened it 27 times this year.",
                onSaveArticle = {}
            )
        }
    }
}
