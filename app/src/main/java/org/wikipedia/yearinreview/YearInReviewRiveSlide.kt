package org.wikipedia.yearinreview

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.RivePointerInputMode
import app.rive.ViewModelSource
import app.rive.core.RiveWorker
import app.rive.rememberArtboardResult
import app.rive.rememberRegisteredFont
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberStateMachineResult
import app.rive.rememberViewModelInstanceResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.log.L

data class RiveSlideSpec(
    @param:RawRes val resourceId: Int,
    val artboardName: String,
    val stateMachineName: String,
    val viewModelName: String,
    val instanceType: RiveInstanceType = RiveInstanceType.Default,
    val instanceName: String? = null,
    val font: RiveSlideFont? = null
)

enum class RiveInstanceType {
    Default,
    Blank,
    Named
}

data class RiveSlideFont(
    @param:RawRes val resourceId: Int,
    val registrationKey: String
)

@Composable
fun rememberYearInReviewRiveWorker(onRiveError: (Throwable) -> Unit): RiveWorker? {
    val riveWorkerError = remember { mutableStateOf<Throwable?>(null) }
    val riveWorker = rememberRiveWorkerOrNull(errorState = riveWorkerError)
    LaunchedEffect(riveWorkerError.value) {
        riveWorkerError.value?.let {
            L.e(it)
            onRiveError(it)
        }
    }
    return riveWorker
}

@Composable
fun YearInReviewRiveSlide(
    riveWorker: RiveWorker?,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    accessibilityDescription: String,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onRiveError: (Throwable) -> Unit
) {
    if (riveWorker == null) {
        // TODO: what will user see if riveWorker is null which means Rive failed to initialize for unknown reasons
        return
    }
    // loading font asset if available and registering with RiveWorker before loading the Rive file
    val fontResult = spec.font?.let { font ->
        rememberRawResourceBytes(font.resourceId).andThen { bytes ->
            rememberRegisteredFont(riveWorker, font.registrationKey, bytes)
        }
    }
    // loading rive file
    val riveFileResult = rememberRiveFile(
        source = RiveFileSource.RawRes.from(spec.resourceId),
        riveWorker = riveWorker
    )

    when (val result = fontResult?.andThen { riveFileResult } ?: riveFileResult) {
        Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveFailure(result.throwable, onRiveError)
        is Result.Success -> YearInReviewRiveArtboard(
            riveFile = result.value,
            spec = spec,
            textProperties = textProperties,
            accessibilityDescription = accessibilityDescription,
            playing = playing,
            modifier = modifier,
            onRiveError = onRiveError
        )
    }
}

@Composable
private fun YearInReviewRiveArtboard(
    riveFile: RiveFile,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    accessibilityDescription: String,
    playing: Boolean,
    modifier: Modifier,
    onRiveError: (Throwable) -> Unit
) {
    // loading the artboard and state machine from the rive file
    val artboardResult = rememberArtboardResult(file = riveFile, artboardName = spec.artboardName)
    val stateMachineResult = artboardResult.andThen { artboard ->
        rememberStateMachineResult(artboard, spec.stateMachineName)
    }
    // creating ViewModel instance based on the spec
    val viewModelSource = ViewModelSource.Named(spec.viewModelName)
    val instanceSource = when (spec.instanceType) {
        RiveInstanceType.Default -> viewModelSource.defaultInstance()
        RiveInstanceType.Blank -> viewModelSource.blankInstance()
        RiveInstanceType.Named -> viewModelSource.namedInstance(requireNotNull(spec.instanceName))
    }
    val instanceResult = rememberViewModelInstanceResult(file = riveFile, source = instanceSource)

    when (val result = artboardResult.zip(stateMachineResult).zip(instanceResult)) {
        is Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveFailure(result.throwable, onRiveError)
        is Result.Success -> {
            val (artboardAndStateMachines, instance) = result.value
            val (artboard, stateMachine) = artboardAndStateMachines
            // setting the provided text properties on the ViewModel instance
            LaunchedEffect(instance, textProperties) {
                textProperties.forEach { (property, value) ->
                    instance.setString(property, value)
                }
            }
            Rive(
                file = riveFile,
                playing = playing,
                artboard = artboard,
                stateMachine = stateMachine,
                viewModelInstance = instance,
                pointerInputMode = RivePointerInputMode.Observe,
                modifier = modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = accessibilityDescription
                    }
            )
        }
    }
}

@Composable
private fun RiveLoadingIndicator(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = WikipediaTheme.colors.progressiveColor
        )
    }
}

@Composable
private fun RiveFailure(
    throwable: Throwable,
    onRiveError: (Throwable) -> Unit
) {
    LaunchedEffect(throwable) {
        L.e(throwable)
        onRiveError(throwable)
    }
}

@Composable
private fun rememberRawResourceBytes(@RawRes resourceId: Int): Result<ByteArray> {
    val resources = LocalResources.current
    return produceState<Result<ByteArray>>(
        initialValue = Result.Loading,
        key1 = resources,
        key2 = resourceId
    ) {
        value = try {
            val bytes = withContext(Dispatchers.IO) {
                resources.openRawResource(resourceId).use { it.readBytes() }
            }
            Result.Success(bytes)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }.value
}
