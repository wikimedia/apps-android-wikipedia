package org.wikipedia.yearinreview.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import app.rive.Artboard
import app.rive.ExperimentalRiveGlobalViewModels
import app.rive.Fit
import app.rive.ImageAsset
import app.rive.RiveFile
import app.rive.SoftwareRenderBuffer
import app.rive.StateMachine
import app.rive.ViewModelInstance
import app.rive.ViewModelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias RenderShareImage = suspend () -> Bitmap

private const val SHARE_IMAGE_WIDTH = 1080
private const val SHARE_IMAGE_HEIGHT = 1920
private const val SHARE_HEADER_HEIGHT = 180
private const val SHARE_FOOTER_HEIGHT = 150
private const val SHARE_SLIDE_HEIGHT = SHARE_IMAGE_HEIGHT - SHARE_HEADER_HEIGHT - SHARE_FOOTER_HEIGHT
// Lays the artboard out 800 units tall, as tall as the 360×800 phone the slides are designed on,
// so text wraps the same for everyone regardless of their screen
private const val SHARE_SLIDE_SCALE_FACTOR = SHARE_SLIDE_HEIGHT / 800f
private const val SHARE_WORDMARK_WIDTH = 360
private const val SHARE_FOOTER_TEXT_SIZE = 36f
private const val SHARE_FOOTER_HORIZONTAL_MARGIN = 64
private const val SHARE_FOOTER_MAX_LINES = 2
// Advancing in frame-sized steps rather than one jump, so timed transitions along the way aren't skipped
private val SHARE_FRAME_STEP = 16.milliseconds

/**
 * Renders the slide off screen, at its spec's shareFrameTime, so the shared image is the same pose every time
 * and contains only the slide, without the status bar or the screen's own controls.
 * The slide sits between a Wikipedia wordmark header and a "Created with the Wikipedia app" footer.
 * Uses its own artboard, state machine and view model instances, so the slide on screen isn't affected,
 * and the base text sizes rather than the user's font scale.
 */
suspend fun renderRiveShareImage(
    context: Context,
    riveFile: RiveFile,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    numberProperties: Map<String, Int>,
    images: Map<String, ImageAsset>
): Bitmap {
    val slide = renderRiveSlide(riveFile, spec, textProperties, numberProperties, images)
    return withContext(Dispatchers.Default) {
        addShareHeaderAndFooter(context, slide).also { slide.recycle() }
    }
}

@OptIn(ExperimentalRiveGlobalViewModels::class)
private suspend fun renderRiveSlide(
    riveFile: RiveFile,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    numberProperties: Map<String, Int>,
    images: Map<String, ImageAsset>
): Bitmap {
    val riveWorker = riveFile.riveWorker
    return SoftwareRenderBuffer(SHARE_IMAGE_WIDTH, SHARE_SLIDE_HEIGHT, riveWorker).use { buffer ->
        Artboard.create(riveFile, spec.artboardName).use { artboard ->
            StateMachine.create(artboard).use { stateMachine ->
                val instanceSource = ViewModelSource.Named(spec.viewModelName).instanceSource(spec.instanceType)
                ViewModelInstance.create(riveFile, instanceSource).use { instance ->
                    textProperties.forEach { (property, value) -> instance.setString(property, value) }
                    numberProperties.forEach { (property, value) -> instance.setNumber(property, value.toFloat()) }
                    images.forEach { (property, image) -> instance.setImage(property, image) }
                    riveWorker.setMainViewModelInstance(stateMachine.stateMachineHandle, instance.instanceHandle)

                    val globalInstance = spec.globalViewModel?.let { globalViewModel ->
                        val globalSource = ViewModelSource.Named(globalViewModel.name).instanceSource(globalViewModel.instanceType)
                        ViewModelInstance.create(riveFile, globalSource).also { globalInstance ->
                            globalViewModel.textSizes.forEach { (property, size) -> globalInstance.setNumber(property, size) }
                            riveWorker.setGlobalViewModelInstance(stateMachine.stateMachineHandle, globalViewModel.name, globalInstance.instanceHandle)
                        }
                    }
                    try {
                        riveWorker.bind(stateMachine.stateMachineHandle)
                        val fit = when (spec.fit) {
                            RiveSlideFit.Layout -> Fit.Layout(scaleFactor = SHARE_SLIDE_SCALE_FACTOR).also {
                                artboard.resizeArtboard(buffer.surface, SHARE_SLIDE_SCALE_FACTOR)
                            }
                            RiveSlideFit.Contain -> Fit.Contain()
                        }
                        advanceTo(stateMachine, spec.shareFrameTime)
                        withContext(Dispatchers.Default) {
                            buffer.renderInto(
                                bitmap = createBitmap(SHARE_IMAGE_WIDTH, SHARE_SLIDE_HEIGHT),
                                artboard = artboard,
                                stateMachine = stateMachine,
                                fit = fit,
                                clearColor = Color.BLACK
                            )
                        }
                    } finally {
                        globalInstance?.close()
                    }
                }
            }
        }
    }
}

private fun advanceTo(stateMachine: StateMachine, frameTime: Duration) {
    // Advancing by zero first leaves the entry state and applies the bound values
    stateMachine.advance(Duration.ZERO)
    var elapsed = Duration.ZERO
    while (elapsed < frameTime) {
        val step = minOf(SHARE_FRAME_STEP, frameTime - elapsed)
        stateMachine.advance(step)
        elapsed += step
    }
}

private fun addShareHeaderAndFooter(context: Context, slide: Bitmap): Bitmap {
    val image = createBitmap(SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT)
    val canvas = Canvas(image)
    val headerBounds = Rect(0, 0, SHARE_IMAGE_WIDTH, SHARE_HEADER_HEIGHT)
    val footerBounds = Rect(0, SHARE_IMAGE_HEIGHT - SHARE_FOOTER_HEIGHT, SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT)
    val topRow = Rect(0, 0, slide.width, 1)
    val bottomRow = Rect(0, slide.height - 1, slide.width, slide.height)

    // Stretches the slide's top and bottom rows of pixels into the header and footer, so its background carries on behind them
    canvas.drawBitmap(slide, topRow, headerBounds, null)
    canvas.drawBitmap(slide, bottomRow, footerBounds, null)
    canvas.drawBitmap(slide, 0f, SHARE_HEADER_HEIGHT.toFloat(), null)

    drawWordmark(context, canvas, headerBounds, contentColorOn(slide, topRow))
    drawFooterText(context, canvas, footerBounds, contentColorOn(slide, bottomRow))
    return image
}

private fun drawWordmark(context: Context, canvas: Canvas, bounds: Rect, color: Int) {
    val wordmark = AppCompatResources.getDrawable(context, R.drawable.feed_header_wordmark)?.mutate() ?: return
    val height = SHARE_WORDMARK_WIDTH * wordmark.intrinsicHeight / wordmark.intrinsicWidth
    val left = bounds.centerX() - SHARE_WORDMARK_WIDTH / 2
    val top = bounds.centerY() - height / 2
    wordmark.setBounds(left, top, left + SHARE_WORDMARK_WIDTH, top + height)
    wordmark.setTint(color)
    wordmark.draw(canvas)
}

private fun drawFooterText(context: Context, canvas: Canvas, bounds: Rect, color: Int) {
    val text = context.getString(R.string.year_in_review_share_created_with)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = SHARE_FOOTER_TEXT_SIZE
        typeface = Typeface.SANS_SERIF
        this.color = color
    }
    val width = bounds.width() - SHARE_FOOTER_HORIZONTAL_MARGIN * 2
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(SHARE_FOOTER_MAX_LINES)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    canvas.withTranslation(
        (bounds.left + SHARE_FOOTER_HORIZONTAL_MARGIN).toFloat(),
        (bounds.top + (bounds.height() - layout.height) / 2).toFloat()
    ) {
        layout.draw(this)
    }
}

// Black or white, whichever stands out more against the average color of the given row of the slide
private fun contentColorOn(slide: Bitmap, row: Rect): Int {
    val pixels = IntArray(row.width())
    slide.getPixels(pixels, 0, row.width(), row.left, row.top, row.width(), 1)
    val averageColor = Color.rgb(
        pixels.map { Color.red(it) }.average().toInt(),
        pixels.map { Color.green(it) }.average().toInt(),
        pixels.map { Color.blue(it) }.average().toInt()
    )
    return if (ColorUtils.calculateContrast(Color.BLACK, averageColor) >= ColorUtils.calculateContrast(Color.WHITE, averageColor)) {
        Color.BLACK
    } else {
        Color.WHITE
    }
}
