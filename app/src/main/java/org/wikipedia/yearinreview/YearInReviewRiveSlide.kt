package org.wikipedia.yearinreview

import android.icu.text.NumberFormat
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.sp
import app.rive.ExperimentalRiveGlobalViewModels
import app.rive.Fit
import app.rive.GetBitmapFun
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.RivePointerInputMode
import app.rive.ViewModelInstance
import app.rive.ViewModelSource
import app.rive.core.RiveWorker
import app.rive.rememberArtboardResult
import app.rive.rememberRegisteredFont
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberStateMachineResult
import app.rive.rememberViewModelInstanceResult
import app.rive.sequence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.log.L

data class RiveSlideSpec(
    @param:RawRes val resourceId: Int,
    val artboardName: String,
    val stateMachineName: String,
    val viewModelName: String,
    val instanceType: RiveInstanceType = RiveInstanceType.Default,
    val globalViewModel: RiveGlobalViewModel? = null,
    val fit: RiveSlideFit = RiveSlideFit.Layout
)

// A view model whose values every artboard in the file can read; left null when the app doesn't set them
data class RiveGlobalViewModel(
    val name: String,
    val instanceType: RiveInstanceType = RiveInstanceType.Default,
    // Number property name to its size at the default text scale
    val textSizes: Map<String, Float> = emptyMap()
)

enum class RiveSlideFit {
    // Resizes the artboard to the slide and lets its auto layout arrange the content, with 1 artboard unit = 1dp
    Layout,
    // Keeps the authored artboard size and scales it down to fit, leaving empty gaps on mismatched screens
    Contain
}

sealed interface RiveInstanceType {
    data object Default : RiveInstanceType
    data object Blank : RiveInstanceType
    data class Named(val name: String) : RiveInstanceType
}

private fun ViewModelSource.instanceSource(instanceType: RiveInstanceType) = when (instanceType) {
    RiveInstanceType.Default -> defaultInstance()
    RiveInstanceType.Blank -> blankInstance()
    is RiveInstanceType.Named -> namedInstance(instanceType.name)
}

data class RiveSlideFont(
    @param:RawRes val resourceId: Int,
    val registrationKey: String
)

// Registered once for the whole screen; a slide should never register its own, otherwise the pager unregisters them for other slides
val YearInReviewRiveFonts = listOf(
    RiveSlideFont(resourceId = R.raw.san_serif_font_6815482, registrationKey = "SanSerifFont-6815482"),
    RiveSlideFont(resourceId = R.raw.san_serif_font_regular_6847910, registrationKey = "SanSerifFont-Regular-6847910"),
    RiveSlideFont(resourceId = R.raw.serif_font_6815481, registrationKey = "SerifFont-6815481")
)

// Shared by every slide in the Year in Review Rive file; sizes are in sp at the default text scale
val YearInReviewRiveGlobalProperties = RiveGlobalViewModel(
    name = "GlobalProperties",
    instanceType = RiveInstanceType.Named("Instance"),
    textSizes = mapOf(
        "headlineFontSize" to 24f,
        "headlineLineHeight" to 32f,
        "bodyCopyFontSize" to 16f,
        "bodyCopyLineHeight" to 24f
    )
)

private const val MAX_RIVE_TEXT_SCALE = 1.5f

// Scales like a Compose Text would (including Android 14+ non-linear scaling), capped so text still fits the frame
@Composable
private fun rememberScaledRiveTextSizes(baseTextSizes: Map<String, Float>): Map<String, Float> {
    val density = LocalDensity.current
    return remember(density, baseTextSizes) {
        baseTextSizes.mapValues { (_, size) ->
            with(density) { size.sp.toDp().value }.coerceAtMost(size * MAX_RIVE_TEXT_SCALE)
        }
    }
}

// Uses the locale's own digits and separators, e.g. 1,234 in English and १,२३४ in Nepali
@Composable
fun rememberLocalizedNumber(number: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, number) { NumberFormat.getInstance(locale).format(number) }
}

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
fun rememberYearInReviewRiveFonts(riveWorker: RiveWorker?, fonts: List<RiveSlideFont>): Result<Unit> {
    if (riveWorker == null) {
        return Result.Loading
    }
    return fonts.map { font ->
        key(font.registrationKey) {
            rememberRawResourceBytes(font.resourceId).andThen { bytes ->
                rememberRegisteredFont(riveWorker, font.registrationKey, bytes)
            }
        }
    }.sequence().map { }
}

// Loads each .riv file once for the whole screen, so slides that share a file share one loaded copy.
// A slide can also use its own .riv file: give its spec a different resourceId and that file is loaded separately.
// Specs with the same resourceId but a different artboard still share the one loaded file.
@Composable
fun rememberYearInReviewRiveFiles(
    riveWorker: RiveWorker?,
    riveFontsResult: Result<Unit>,
    resourceIds: List<Int>
): Map<Int, Result<RiveFile>> {
    return resourceIds.distinct().associateWith { resourceId ->
        key(resourceId) {
            if (riveWorker == null) {
                Result.Loading
            } else {
                riveFontsResult.andThen {
                    rememberRiveFile(
                        source = RiveFileSource.RawRes.from(resourceId),
                        riveWorker = riveWorker
                    )
                }
            }
        }
    }
}

@Composable
fun YearInReviewRiveSlide(
    riveFileResult: Result<RiveFile>,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    accessibilityDescription: String,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onRiveError: (Throwable) -> Unit
) {
    when (riveFileResult) {
        Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveFailure(riveFileResult.throwable, onRiveError)
        is Result.Success -> YearInReviewRiveArtboard(
            riveFile = riveFileResult.value,
            spec = spec,
            textProperties = textProperties,
            accessibilityDescription = accessibilityDescription,
            playing = playing,
            modifier = modifier,
            onRiveError = onRiveError,
            slideId = slideId,
            screenshotGetters = screenshotGetters
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
    onRiveError: (Throwable) -> Unit,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>
) {
    // loading the artboard and state machine from the rive file
    val artboardResult = rememberArtboardResult(file = riveFile, artboardName = spec.artboardName)
    val stateMachineResult = artboardResult.andThen { artboard ->
        rememberStateMachineResult(artboard, spec.stateMachineName)
    }
    // creating ViewModel instance based on the spec
    val viewModelSource = ViewModelSource.Named(spec.viewModelName)
    val instanceSource = viewModelSource.instanceSource(spec.instanceType)
    val instanceResult = rememberViewModelInstanceResult(file = riveFile, source = instanceSource)
    val globalInstanceResult = rememberGlobalViewModelInstanceResult(riveFile, spec.globalViewModel)

    when (val result = artboardResult.zip(stateMachineResult).zip(instanceResult).zip(globalInstanceResult)) {
        is Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveFailure(result.throwable, onRiveError)
        is Result.Success -> {
            val (artboardStateMachineAndInstance, globalInstance) = result.value
            val (artboardAndStateMachines, instance) = artboardStateMachineAndInstance
            val (artboard, stateMachine) = artboardAndStateMachines
            val globalViewModelInstances = remember(spec.globalViewModel, globalInstance) {
                if (spec.globalViewModel != null && globalInstance != null) {
                    mapOf(spec.globalViewModel.name to globalInstance)
                } else {
                    emptyMap()
                }
            }
            DisposableEffect(slideId, screenshotGetters, riveFile, artboard, stateMachine, instance) {
                onDispose { screenshotGetters.remove(slideId) }
            }
            // setting the provided text properties on the ViewModel instance
            LaunchedEffect(instance, textProperties) {
                textProperties.forEach { (property, value) ->
                    instance.setString(property, value)
                }
            }
            val textSizes = rememberScaledRiveTextSizes(spec.globalViewModel?.textSizes.orEmpty())
            LaunchedEffect(globalInstance, textSizes) {
                textSizes.forEach { (property, value) ->
                    globalInstance?.setNumber(property, value)
                }
            }
            val density = LocalDensity.current.density
            val fit = remember(spec.fit, density) {
                when (spec.fit) {
                    RiveSlideFit.Layout -> Fit.Layout(scaleFactor = density)
                    RiveSlideFit.Contain -> Fit.Contain()
                }
            }
            @OptIn(ExperimentalRiveGlobalViewModels::class)
            Rive(
                file = riveFile,
                playing = playing,
                artboard = artboard,
                stateMachine = stateMachine,
                viewModelInstance = instance,
                fit = fit,
                pointerInputMode = RivePointerInputMode.Observe,
                onBitmapAvailable = { screenshotGetters[slideId] = it },
                modifier = modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = accessibilityDescription
                    },
                globalViewModelInstances = globalViewModelInstances
            )
        }
    }
}

@Composable
private fun rememberGlobalViewModelInstanceResult(
    riveFile: RiveFile,
    globalViewModel: RiveGlobalViewModel?
): Result<ViewModelInstance?> {
    if (globalViewModel == null) {
        return Result.Success(null)
    }
    return key(globalViewModel.name, globalViewModel.instanceType) {
        rememberViewModelInstanceResult(
            file = riveFile,
            source = ViewModelSource.Named(globalViewModel.name).instanceSource(globalViewModel.instanceType)
        )
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
