package org.wikipedia.yearinreview.presentation

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias RenderShareImage = suspend () -> Bitmap

private const val SHARE_IMAGE_WIDTH = 1080
private const val SHARE_IMAGE_HEIGHT = 1920
// Lays the artboard out at 450×800 units, as tall as the 360×800 phone the slides are designed on,
// so text wraps the same for everyone regardless of their screen
private const val SHARE_IMAGE_SCALE_FACTOR = 2.4f
// Advancing in frame-sized steps rather than one jump, so timed transitions along the way aren't skipped
private val SHARE_FRAME_STEP = 16.milliseconds

/**
 * Renders the slide off screen, at its spec's shareFrameTime, so the shared image is the same pose every time
 * and contains only the slide, without the status bar or the screen's own controls.
 * Uses its own artboard, state machine and view model instances, so the slide on screen isn't affected,
 * and the base text sizes rather than the user's font scale.
 */
@OptIn(ExperimentalRiveGlobalViewModels::class)
suspend fun renderRiveShareImage(
    riveFile: RiveFile,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    numberProperties: Map<String, Int>,
    images: Map<String, ImageAsset>
): Bitmap {
    val riveWorker = riveFile.riveWorker
    return SoftwareRenderBuffer(SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT, riveWorker).use { buffer ->
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
                            RiveSlideFit.Layout -> Fit.Layout(scaleFactor = SHARE_IMAGE_SCALE_FACTOR).also {
                                artboard.resizeArtboard(buffer.surface, SHARE_IMAGE_SCALE_FACTOR)
                            }
                            RiveSlideFit.Contain -> Fit.Contain()
                        }
                        advanceTo(stateMachine, spec.shareFrameTime)
                        withContext(Dispatchers.Default) {
                            buffer.renderInto(
                                bitmap = createBitmap(SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT),
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
