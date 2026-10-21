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

typealias RiveSlideScreenshotFun = suspend () -> Bitmap

// Everything needed to rebuild a slide offscreen with the same content as the one on screen
data class RiveSlideSnapshotSource(
    val riveFile: RiveFile,
    val spec: RiveSlideSpec,
    val textProperties: Map<String, String>,
    val numberProperties: Map<String, Int>,
    val images: Map<String, ImageAsset?>,
    val textSizes: Map<String, Float>,
    val isRtl: Boolean,
    val fit: Fit
)

// Advancing in display-sized steps lets the state machine take each transition the way it does on screen
private val SNAPSHOT_FRAME_DURATION = 16.milliseconds

/**
 * Renders the slide as it looks [time] into its animation, using its own artboard, state machine and view model
 * instances so the slide on screen keeps playing undisturbed.
 */
@OptIn(ExperimentalRiveGlobalViewModels::class)
suspend fun renderRiveSlideSnapshot(source: RiveSlideSnapshotSource, width: Int, height: Int, time: Duration): Bitmap {
    val riveFile = source.riveFile
    val riveWorker = riveFile.riveWorker
    val spec = source.spec
    return Artboard.create(riveFile, spec.artboardName).use { artboard ->
        StateMachine.create(artboard).use { stateMachine ->
            ViewModelInstance.create(riveFile, ViewModelSource.Named(spec.viewModelName).instanceSource(spec.instanceType)).use { instance ->
                val globalInstance = spec.globalViewModel?.let {
                    ViewModelInstance.create(riveFile, ViewModelSource.Named(it.name).instanceSource(it.instanceType))
                }
                try {
                    source.textProperties.forEach { (property, value) -> instance.setString(property, value) }
                    source.numberProperties.forEach { (property, value) -> instance.setNumber(property, value.toFloat()) }
                    source.images.forEach { (property, image) -> instance.setImage(property, image) }
                    spec.layoutDirectionEnum?.let {
                        instance.setEnum(it, if (source.isRtl) "Right to Left" else "Left to Right")
                    }
                    source.textSizes.forEach { (property, value) -> globalInstance?.setNumber(property, value) }

                    riveWorker.setMainViewModelInstance(stateMachine.stateMachineHandle, instance.instanceHandle)
                    if (spec.globalViewModel != null && globalInstance != null) {
                        riveWorker.setGlobalViewModelInstance(stateMachine.stateMachineHandle, spec.globalViewModel.name, globalInstance.instanceHandle)
                    }
                    riveWorker.bind(stateMachine.stateMachineHandle)

                    SoftwareRenderBuffer(width, height, riveWorker).use { buffer ->
                        if (source.fit is Fit.Layout) {
                            artboard.resizeArtboard(buffer.surface, source.fit.scaleFactor)
                        }
                        stateMachine.advance(Duration.ZERO)
                        var elapsed = Duration.ZERO
                        while (elapsed < time) {
                            val step = minOf(SNAPSHOT_FRAME_DURATION, time - elapsed)
                            stateMachine.advance(step)
                            elapsed += step
                        }
                        // Advances are queued without confirmation, and drawing isn't documented to wait for them,
                        // so a confirmed round trip through the same queue makes sure they've all been applied.
                        artboard.getStateMachineNames()
                        withContext(Dispatchers.Default) {
                            buffer.renderInto(createBitmap(width, height), artboard, stateMachine, source.fit, Color.BLACK)
                        }
                    }
                } finally {
                    globalInstance?.close()
                }
            }
        }
    }
}
