package org.wikipedia.yearinreview

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
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
import app.rive.rememberStateMachineResult
import app.rive.rememberViewModelInstanceResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.WikipediaTheme

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
fun YearInReviewRiveSlide(
    riveWorker: RiveWorker,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    accessibilityDescription: String,
    playing: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontResult = spec.font?.let { font ->
        rememberRawResourceBytes(font.resourceId).andThen { bytes ->
            rememberRegisteredFont(riveWorker, font.registrationKey, bytes)
        }
    }
    val riveFileResult = rememberRiveFile(
        source = RiveFileSource.RawRes.from(spec.resourceId),
        riveWorker = riveWorker
    )

    when (val result = fontResult?.andThen { riveFileResult } ?: riveFileResult) {
        Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveErrorView(result.throwable, onRetryClick, modifier)
        is Result.Success -> YearInReviewRiveArtboard(
            riveFile = result.value,
            spec = spec,
            textProperties = textProperties,
            accessibilityDescription = accessibilityDescription,
            playing = playing,
            onRetryClick = onRetryClick,
            modifier = modifier
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
    onRetryClick: () -> Unit,
    modifier: Modifier
) {
    val artboardResult = rememberArtboardResult(file = riveFile, artboardName = spec.artboardName)
    val stateMachineResult = artboardResult.andThen { artboard ->
        rememberStateMachineResult(artboard, spec.stateMachineName)
    }
    val viewModelSource = ViewModelSource.Named(spec.viewModelName)
    val instanceSource = when (spec.instanceType) {
        RiveInstanceType.Default -> viewModelSource.defaultInstance()
        RiveInstanceType.Blank -> viewModelSource.blankInstance()
        RiveInstanceType.Named -> viewModelSource.namedInstance(requireNotNull(spec.instanceName))
    }
    val instanceResult = rememberViewModelInstanceResult(file = riveFile, source = instanceSource)

    when (val result = artboardResult.zip(stateMachineResult).zip(instanceResult)) {
        is Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveErrorView(result.throwable, onRetryClick, modifier)
        is Result.Success -> {
            val (artboardAndStateMachines, instance) = result.value
            val (artboard, stateMachine) = artboardAndStateMachines
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
private fun RiveErrorView(
    throwable: Throwable,
    onRetryClick: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        WikiErrorView(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            caught = throwable,
            errorClickEvents = WikiErrorClickEvents(retryClickListener = onRetryClick),
            retryForGenericError = true
        )
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
