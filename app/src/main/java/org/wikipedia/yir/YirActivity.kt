package org.wikipedia.yir

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.util.DeviceUtil
import android.graphics.Color as AndroidColor

/**
 * Hosts the new immersive Year in Review story (the [org.wikipedia.yir] spike).
 *
 * For now it feeds the scaffold placeholder content so the shell (paging, background, top bar) is
 * runnable from Developer Settings. Real content/interactive slides arrive in later steps.
 */
class YirActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // YiR is always a dark, full-bleed experience, so force transparent system bars with light
        // (white) icons regardless of the app theme. SystemBarStyle.dark => light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        // BaseActivity.onCreate sets the system-bar icons by app theme (dark icons in light theme).
        // YiR is always dark and full-bleed, so override to light (white) icons after super runs.
        DeviceUtil.setLightSystemUiVisibility(this, light = false)
        setContent {
            BaseTheme {
                YirStoryScaffold(
                    pages = demoPages(),
                    onClose = { finish() },
                    onDonate = { /* TODO: wire to donate flow; placeholder for the spike */ }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YirActivity::class.java)
        }
    }
}

/** The teal -> green -> pale gradient from the current Year in Review. */
private val yirGreenGradient = listOf(
    Color(0xFF0A3D3A),
    Color(0xFF12B36B),
    Color(0xFFEAF7EE)
)

/**
 * The four-slide wireframe set: Opening (framing) -> Standard (content) -> Interactive -> Closing
 * (framing). Backgrounds use the gradient as a placeholder where a Lottie would go; swap a card's
 * background to YirBackground.Animation("lottie/...") once Design provides assets.
 */
private fun demoPages(): List<YirPage> {
    return listOf(
        // 1. Opening framing card.
        YirPage(
            background = YirBackground.Animation(
                assetPath = "lottie/bg_shooting_star.lottie",
                loop = true
            ),
            content = {
                YirFramingContent(
                    headline = "Your 2025 in Review",
                    supportingText = "A look back at what you read, explored and discovered this year.",
                    ctas = emptyList(),
                    hint = "Swipe up to begin"
                )
            }
        ),
        // 2. Standard content card: one-shot animation background that plays then holds its last
        // frame; the text fades in on top.
        YirPage(
            background = YirBackground.Animation("lottie/bg3.lottie"),
            content = { phase ->
                YirStandardContent(
                    headline = "924 minutes reading",
                    supportingText = "Not all screen time is created equal. Your longest session: March 7, 43 minutes.",
                    phase = phase
                )
            }
        ),
        // 3. Interactive card.
        YirPage(
            background = YirBackground.Gradient(yirGreenGradient),
            content = {
                YirInteractiveContent(
                    prompt = "Guess your most-read article this year",
                    options = listOf(
                        YirGuessOption("The Birth of Venus", "Painting by Sandro Botticelli", isCorrect = true),
                        YirGuessOption("Etel Adnan", "Lebanese-American writer and artist (1925–2021)", isCorrect = false),
                        YirGuessOption("Camille Paglia", "American feminist academic and critic (born 1947)", isCorrect = false),
                        YirGuessOption("Impression, Sunrise", "1872 painting by Claude Monet", isCorrect = false)
                    ),
                    resultHeadline = "Your most-read article was The Birth of Venus",
                    resultSupportingText = "You opened it 27 times this year.",
                    onSaveArticle = { /* TODO: wire to save flow */ }
                )
            }
        ),
        // 4. Closing framing card.
        YirPage(
            background = YirBackground.Animation(
                assetPath = "lottie/bg_shooting_star.lottie",
                loop = true
            ),
            content = {
                YirFramingContent(
                    headline = "That's a wrap!",
                    supportingText = "Thanks for reading with us this year.",
                    ctas = listOf(
                        YirCta("Share") { },
                        YirCta("Explore Wikipedia") { },
                        YirCta("Set up recommended reading") { }
                    )
                )
            }
        )
    )
}

@Preview
@Composable
private fun YirStoryScaffoldPreview() {
    BaseTheme {
        YirStoryScaffold(pages = demoPages(), onClose = {}, onDonate = {})
    }
}
