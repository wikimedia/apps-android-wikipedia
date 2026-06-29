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

        val orientation = intent.getStringExtra(EXTRA_ORIENTATION)
            ?.let { runCatching { YirPagerOrientation.valueOf(it) }.getOrNull() }
            ?: YirPagerOrientation.VERTICAL

        setContent {
            BaseTheme {
                YirStoryScaffold(
                    pages = demoPages(),
                    orientation = orientation,
                    onClose = { finish() },
                    onDonate = { /* TODO: wire to donate flow; placeholder for the spike */ }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ORIENTATION = "extra_orientation"

        fun newIntent(context: Context, orientation: YirPagerOrientation = YirPagerOrientation.VERTICAL): Intent {
            return Intent(context, YirActivity::class.java)
                .putExtra(EXTRA_ORIENTATION, orientation.name)
        }
    }
}

/**
 * The Year in Review background, matching the production header (yearInReviewHeaderBackground):
 * near-black at the top, teal -> green through the middle, then solid WHITE from ~65% to the bottom.
 * Same color stops as the live YiR so the immersive version reads the same.
 */
private val yirGreenGradient = listOf(
    0.125f to Color(0xFF171717),
    0.225f to Color(0xFF003F45),
    0.285f to Color(0xFF00807A),
    0.440f to Color(0xFF2AECA6),
    0.525f to Color(0xFF86FFAC),
    0.650f to Color(0xFFFFFFFF)
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
                    headline = "Your 2026 in Review",
                    supportingText = "A look back at what you read, explored and discovered this year.",
                    ctas = emptyList(),
                    hint = "Swipe to begin"
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
        // 3b. Interactive image-guess card (images added later; rounded placeholders for now).
        YirPage(
            background = YirBackground.Gradient(yirGreenGradient),
            content = {
                YirImageGuessContent(
                    prompt = "Which of these images appeared in the article you visited most?",
                    options = listOf(
                        YirImageOption(
                            imageSrc = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/Six_weeks_old_cat_%28aka%29.jpg/960px-Six_weeks_old_cat_%28aka%29.jpg",
                            isCorrect = true),
                        YirImageOption(
                            imageSrc = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Callie_the_golden_retriever_puppy.jpg/960px-Callie_the_golden_retriever_puppy.jpg"
                        ),
                        YirImageOption(
                            imageSrc = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Juvenile_Female_Mesocricetus_auratus_in_Pet_Store_enclosure%2C_Illinois%2C_USA.jpg/960px-Juvenile_Female_Mesocricetus_auratus_in_Pet_Store_enclosure%2C_Illinois%2C_USA.jpg"
                        )
                    ),
                    resultHeadline = "It was The Birth of Venus",
                    resultSupportingText = "You visited it more than any other article this year.",
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
