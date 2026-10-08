package org.wikipedia.feed.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme
import kotlin.math.min

// All coordinates below are in scene units, which are scaled to fit the available space.
private val SceneSize = Size(329f, 252f)
private val CardSize = Size(89f, 40f)
private val PhoneSize = Size(122f, 206f)
private val ShadowSize = Size(153f, 26f)
private val PhoneCenter = Offset(164.5f, 125f)
private val ShadowCenter = Offset(164.5f, 223.75f)

private const val CARD_START_SCALE = 1.2f
private const val HOP_HEIGHT = 10f
private const val HOP_SHADOW_SHRINK = 0.2f
private const val TEXT_BLOCK_SIZE = 4.19f

private val InkColor = Color.Black
private val HighlightColor = Color(0xFFFB7866)
private val CardColor = Color(0xFFE8E7C3)
private val OutlineStroke = Stroke(width = 0.8f)

private val CardTextLines = listOf(
    13.81f to listOf(InkColor, InkColor, InkColor, HighlightColor, HighlightColor, HighlightColor, HighlightColor, InkColor, HighlightColor, InkColor, InkColor),
    22f to listOf(HighlightColor, HighlightColor, HighlightColor, InkColor, InkColor, InkColor, InkColor, HighlightColor, InkColor)
)

private val EnterEasing = CubicBezierEasing(0.55f, 0.06f, 0.36f, 1f)
private val ShrinkEasing = CubicBezierEasing(0.25f, 0f, 0.75f, 1f)

private class FlyingCard(
    @DrawableRes val thumbnailRes: Int,
    startCenter: Offset,
    private val restingCenter: Offset,
    private val centerAnimationSpec: AnimationSpec<Offset>,
    private val scaleAnimationSpec: AnimationSpec<Float>
) {
    val center = Animatable(startCenter, Offset.VectorConverter)
    val scale = Animatable(CARD_START_SCALE)
    val alpha = Animatable(0f)

    suspend fun flyIn() {
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMillis = 270, easing = EnterEasing)) }
            launch { center.animateTo(restingCenter, centerAnimationSpec) }
            launch { scale.animateTo(1f, scaleAnimationSpec) }
        }
    }
}

// Ordered from back to front.
private fun createFlyingCards() = listOf(
    // Slides in from the right into the middle of the stack.
    FlyingCard(
        thumbnailRes = R.drawable.explore_feed_building_thumbnail_clouds,
        startCenter = Offset(264.5f, 133f),
        restingCenter = Offset(164.5f, 133f),
        centerAnimationSpec = tween(durationMillis = 440, delayMillis = 330, easing = EaseInOutCubic),
        scaleAnimationSpec = tween(durationMillis = 440, delayMillis = 330, easing = ShrinkEasing)
    ),
    // Drifts in from the upper left onto the top of the stack.
    FlyingCard(
        thumbnailRes = R.drawable.explore_feed_building_thumbnail_blossoms,
        startCenter = Offset(57f, 44.63f),
        restingCenter = Offset(164.5f, 84f),
        centerAnimationSpec = keyframes {
            durationMillis = 630
            Offset(57f, 44.63f) at 0 using EnterEasing
            Offset(79.26f, 52.78f) at 200 using LinearEasing
        },
        scaleAnimationSpec = tween(durationMillis = 630, easing = ShrinkEasing)
    ),
    // Fades in at the lower left, then moves onto the bottom of the stack.
    FlyingCard(
        thumbnailRes = R.drawable.explore_feed_building_thumbnail_astronomy,
        startCenter = Offset(65.75f, 178.88f),
        restingCenter = Offset(164.5f, 182f),
        centerAnimationSpec = tween(durationMillis = 570, delayMillis = 430, easing = EnterEasing),
        scaleAnimationSpec = tween(durationMillis = 570, delayMillis = 430, easing = EnterEasing)
    )
)

@Composable
fun ExploreFeedBuildingAnimation(
    modifier: Modifier = Modifier
) {
    val cards = remember { createFlyingCards() }
    val hopProgress = remember { Animatable(0f) }
    val thumbnails = cards.map { ImageBitmap.imageResource(it.thumbnailRes) }
    val phoneScreen = ImageBitmap.imageResource(R.drawable.explore_feed_building_phone_screen)

    LaunchedEffect(Unit) {
        cards.forEach { launch { it.flyIn() } }
        launch {
            hopProgress.animateTo(0f, keyframes {
                durationMillis = 1370
                0f at 1170 using EnterEasing
                1f at 1270 using EaseInOutCubic
            })
        }
    }

    Canvas(modifier = modifier) {
        val sceneScale = min(size.width / SceneSize.width, size.height / SceneSize.height)
        translate(
            left = (size.width - SceneSize.width * sceneScale) / 2,
            top = (size.height - SceneSize.height * sceneScale) / 2
        ) {
            scale(sceneScale, pivot = Offset.Zero) {
                val hopOffset = Offset(0f, -HOP_HEIGHT * hopProgress.value)
                drawLayer(ShadowCenter, ShadowSize, scale = 1f - HOP_SHADOW_SHRINK * hopProgress.value) {
                    drawOval(InkColor, size = Size(152.36f, 25.06f))
                }
                drawLayer(PhoneCenter + hopOffset, PhoneSize) {
                    drawPhone(phoneScreen)
                }
                cards.forEachIndexed { index, card ->
                    drawLayer(card.center.value + hopOffset, CardSize, card.scale.value) {
                        drawCard(thumbnails[index], card.alpha.value)
                    }
                }
            }
        }
    }
}

private inline fun DrawScope.drawLayer(
    position: Offset,
    layerSize: Size,
    scale: Float = 1f,
    block: DrawScope.() -> Unit
) {
    withTransform({
        translate(left = position.x - layerSize.width / 2, top = position.y - layerSize.height / 2)
        scale(scale, pivot = layerSize.center)
    }, block)
}

private fun DrawScope.drawImageInBounds(image: ImageBitmap, bounds: Rect, alpha: Float = 1f) {
    translate(left = bounds.left, top = bounds.top) {
        scale(bounds.width / image.width, bounds.height / image.height, pivot = Offset.Zero) {
            drawImage(image, alpha = alpha)
        }
    }
}

private fun DrawScope.drawPhone(screen: ImageBitmap) {
    val bodyTopLeft = Offset(0.4f, 0.4f)
    val bodySize = Size(121f, 204.63f)
    val bodyCornerRadius = CornerRadius(16f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFEDEDED),
            0.79f to Color(0xFFBDD1DB),
            1f to Color(0xFF4F87A6),
            startY = bodyTopLeft.y,
            endY = bodyTopLeft.y + bodySize.height
        ),
        topLeft = bodyTopLeft,
        size = bodySize,
        cornerRadius = bodyCornerRadius
    )
    drawRoundRect(InkColor, bodyTopLeft, bodySize, bodyCornerRadius, style = OutlineStroke)
    drawCircle(InkColor, radius = 3.5f, center = Offset(60.9f, 11.58f))
    drawImageInBounds(screen, Rect(10.53f, 25.51f, 111.47f, 195.35f))
    drawRoundRect(InkColor, Offset(10.4f, 25.53f), Size(101f, 170f), CornerRadius(8f), style = OutlineStroke)
}

private fun DrawScope.drawCard(thumbnail: ImageBitmap, alpha: Float) {
    val cardTopLeft = Offset(0.4f, 0.4f)
    val cardSize = Size(88.2f, 39.2f)
    val cardCornerRadius = CornerRadius(7.6f)
    drawRoundRect(CardColor, cardTopLeft, cardSize, cardCornerRadius, alpha = alpha)
    drawRoundRect(InkColor, cardTopLeft, cardSize, cardCornerRadius, style = OutlineStroke, alpha = alpha)
    drawImageInBounds(thumbnail, Rect(4.12f, 8.12f, 27.88f, 31.88f), alpha)
    drawRoundRect(InkColor, Offset(4.4f, 8.4f), Size(23.2f, 23.2f), CornerRadius(3.6f), style = OutlineStroke, alpha = alpha)
    CardTextLines.forEach { (top, blockColors) ->
        blockColors.forEachIndexed { index, color ->
            drawRect(
                color = color,
                topLeft = Offset(32f + index * TEXT_BLOCK_SIZE, top),
                size = Size(TEXT_BLOCK_SIZE, TEXT_BLOCK_SIZE),
                alpha = alpha
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreFeedBuildingAnimationPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ExploreFeedBuildingAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
        )
    }
}
