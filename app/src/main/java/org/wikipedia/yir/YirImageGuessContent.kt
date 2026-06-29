package org.wikipedia.yir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme

data class YirImageOption(
    val imageSrc: String? = null,
    val isCorrect: Boolean = false
)

@Composable
fun YirImageGuessContent(
    prompt: String,
    options: List<YirImageOption>,
    resultHeadline: String,
    resultSupportingText: String,
    onSaveArticle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }

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

        if (!revealed) {
            options.forEachIndexed { index, option ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    ImageOptionTile(
                        option = option,
                        onClick = { revealed = true },
                        modifier = Modifier.align(
                            if (index % 2 == 1) Alignment.CenterEnd else Alignment.CenterStart
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
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
private fun ImageOptionTile(
    option: YirImageOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .size(140.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.25f))
            .border(3.dp, Color.Black, shape)
            .clickable(onClick = onClick)
    ) {
        option.imageSrc?.let {
            AsyncImage(
                model = it,
                placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview
@Composable
private fun YirImageGuessContentPreview() {
    BaseTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2A9D67))) {
            YirImageGuessContent(
                prompt = "Which of these images appeared in the article you visited most?",
                options = listOf(
                    YirImageOption(isCorrect = true),
                    YirImageOption(),
                    YirImageOption()
                ),
                resultHeadline = "It was The Birth of Venus",
                resultSupportingText = "You visited it more than any other article this year.",
                onSaveArticle = {}
            )
        }
    }
}
