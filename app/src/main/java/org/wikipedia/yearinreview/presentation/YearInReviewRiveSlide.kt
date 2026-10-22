package org.wikipedia.yearinreview.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.NumberFormat
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.startup.AppInitializer
import app.rive.ExperimentalRiveGlobalViewModels
import app.rive.Fit
import app.rive.GetBitmapFun
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.RiveLog
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
import app.rive.runtime.kotlin.RiveInitializer
import app.rive.runtime.kotlin.fonts.FontHelper
import app.rive.runtime.kotlin.fonts.Fonts
import app.rive.sequence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.log.L
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RiveSlideSpec(
    @param:RawRes val resourceId: Int,
    val artboardName: String,
    val viewModelName: String,
    val instanceType: RiveInstanceType = RiveInstanceType.Default,
    val globalViewModel: RiveGlobalViewModel? = null,
    val fit: RiveSlideFit = RiveSlideFit.Layout,
    val isTopBarUiIconsWhite: String? = null,
    val layoutDirectionEnum: String? = null,
    val frameCompleteTrigger: String? = null,
    val shareFrameTime: Duration? = 7.seconds
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

fun ViewModelSource.instanceSource(instanceType: RiveInstanceType) = when (instanceType) {
    RiveInstanceType.Default -> defaultInstance()
    RiveInstanceType.Blank -> blankInstance()
    is RiveInstanceType.Named -> namedInstance(instanceType.name)
}

sealed interface RiveFontSource {
    data class RawResource(@param:RawRes val resourceId: Int) : RiveFontSource
    // A family name from the device's fonts.xml, e.g. "sans-serif" or "serif", at its regular weight. Other weights can't be requested:
    // on recent devices every weight of a family is one variable font file, and Rive can only draw such a font at its default weight.
    data class SystemFamily(val familyName: String) : RiveFontSource
}

// The registration key is the referenced font's file name in the Rive export zip, without the extension: "<asset name>-<asset id>"
data class RiveSlideFont(
    val source: RiveFontSource,
    val registrationKey: String
)

// Registered once for the whole screen; a slide should never register its own, otherwise the pager unregisters them for other slides
@SuppressLint("ResourceType")
val YearInReviewRiveFonts = listOf(
    // If we want to use the system serif font:
    // RiveSlideFont(RiveFontSource.SystemFamily("serif"), registrationKey = "SerifFont-6815481"),
    RiveSlideFont(RiveFontSource.RawResource(R.font.ebgaramond_regular), registrationKey = "SerifFont-6815481"),
    // Static instances of the variable Roboto Flex at weights 400 and 700, since Rive can't pick a weight from a variable font
    RiveSlideFont(RiveFontSource.RawResource(R.font.robotoflex_regular), registrationKey = "SanSerifFont-Regular-6847910"),
    RiveSlideFont(RiveFontSource.RawResource(R.font.robotoflex_bold), registrationKey = "SanSerifFont-6815482")
)

private const val MAX_RIVE_TEXT_SCALE = 1.5f

// Must run before any Rive composable is shown; initializing again is a no-op
fun initializeYearInReviewRive(context: Context) {
    if (BuildConfig.DEBUG) {
        RiveLog.logger = RiveLog.LogcatLogger()
    }
    AppInitializer.getInstance(context).initializeComponent(RiveInitializer::class.java)
}

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
            rememberFontBytes(font.source).andThen { bytes ->
                rememberRegisteredFont(riveWorker, font.registrationKey, bytes)
            }
        }
    }.sequence().map { }
}

// Loads each .riv file once for the whole screen, so slides that share a file share one loaded copy.
// A slide can also use its own .riv file: give its spec a different resourceId and that file is loaded separately.
// Specs with the same resourceId but a different artboard still share the one loaded file.
// riveFontsResult can be null if the app doesn't use any fonts, otherwise it should be the result of rememberYearInReviewRiveFonts().
@Composable
fun rememberYearInReviewRiveFiles(
    riveWorker: RiveWorker?,
    resourceIds: List<Int>,
    riveFontsResult: Result<Unit>? = null
): Map<Int, Result<RiveFile>> {
    return resourceIds.distinct().associateWith { resourceId ->
        key(resourceId) {
            if (riveWorker == null) {
                Result.Loading
            } else {
                (riveFontsResult ?: Result.Success(Unit)).andThen {
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
    content: RiveSlideContent,
    playing: Boolean,
    modifier: Modifier = Modifier,
    slideId: String? = null,
    screenshotGetters: MutableMap<String, RiveSlideScreenshotFun>? = null,
    onUseWhiteTopBarIconsChange: (Boolean) -> Unit = {},
    onFrameComplete: () -> Unit = {},
    onRiveLoaded: () -> Unit = {},
    onRiveError: (Throwable) -> Unit
) {
    when (riveFileResult) {
        Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Error -> RiveFailure(riveFileResult.throwable, onRiveError)
        is Result.Success -> YearInReviewRiveArtboard(
            riveFile = riveFileResult.value,
            spec = content.spec,
            textProperties = content.textProperties,
            numberProperties = content.numberProperties,
            booleanProperties = content.booleanProperties,
            imageUrls = content.imageUrls,
            accessibilityDescription = content.accessibilityDescription,
            playing = playing,
            modifier = modifier,
            onRiveError = onRiveError,
            slideId = slideId,
            screenshotGetters = screenshotGetters,
            onUseWhiteTopBarIconsChange = onUseWhiteTopBarIconsChange,
            onFrameComplete = onFrameComplete,
            onRiveLoaded = onRiveLoaded
        )
    }
}

@Composable
private fun YearInReviewRiveArtboard(
    riveFile: RiveFile,
    spec: RiveSlideSpec,
    textProperties: Map<String, String>,
    numberProperties: Map<String, Int>,
    booleanProperties: Map<String, Boolean>,
    imageUrls: Map<String, String?>,
    accessibilityDescription: String,
    playing: Boolean,
    modifier: Modifier,
    onRiveError: (Throwable) -> Unit,
    slideId: String?,
    screenshotGetters: MutableMap<String, RiveSlideScreenshotFun>?,
    onUseWhiteTopBarIconsChange: (Boolean) -> Unit,
    onFrameComplete: () -> Unit,
    onRiveLoaded: () -> Unit
) {
    // loading the artboard and state machine from the rive file
    val artboardResult = rememberArtboardResult(file = riveFile, artboardName = spec.artboardName)
    val stateMachineResult = artboardResult.andThen { artboard ->
        // Each slide artboard has one state machine, so its default is used rather than looking it up by name
        rememberStateMachineResult(artboard)
    }
    // creating ViewModel instance based on the spec
    val viewModelSource = ViewModelSource.Named(spec.viewModelName)
    val instanceSource = viewModelSource.instanceSource(spec.instanceType)
    val instanceResult = rememberViewModelInstanceResult(file = riveFile, source = instanceSource)
    val globalInstanceResult = rememberGlobalViewModelInstanceResult(riveFile, spec.globalViewModel)
    val imageResults = rememberRiveImages(riveFile.riveWorker, imageUrls)
    // waits for the images too, so the slide's animation never starts with the designer's sample pictures
    val imagesLoading = imageResults.values.any { it is Result.Loading }
    val layoutDirection = LocalLayoutDirection.current

    when (val result = artboardResult.zip(stateMachineResult).zip(instanceResult).zip(globalInstanceResult)) {
        is Result.Loading -> RiveLoadingIndicator(modifier)
        is Result.Success if imagesLoading -> RiveLoadingIndicator(modifier)
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
            val textSizes = rememberScaledRiveTextSizes(spec.globalViewModel?.textSizes.orEmpty())
            val density = LocalDensity.current.density
            val fit = remember(spec.fit, density) {
                when (spec.fit) {
                    RiveSlideFit.Layout -> Fit.Layout(scaleFactor = density)
                    RiveSlideFit.Contain -> Fit.Contain()
                }
            }
            val snapshotSource by rememberUpdatedState(RiveSlideSnapshotSource(
                riveFile = riveFile,
                spec = spec,
                textProperties = textProperties,
                numberProperties = numberProperties,
                images = imageResults.filterValues { it is Result.Success }.mapValues { (it.value as Result.Success).value },
                textSizes = textSizes,
                isRtl = layoutDirection == LayoutDirection.Rtl,
                fit = fit
            ))
            var slideSize by remember { mutableStateOf(IntSize.Zero) }
            val onBitmapAvailable: ((GetBitmapFun) -> Unit)? = if (slideId != null && screenshotGetters != null) {
                { getBitmap ->
                    screenshotGetters[slideId] = spec.shareFrameTime?.let { time ->
                        { renderRiveSlideSnapshot(snapshotSource, slideSize.width, slideSize.height, time) }
                    } ?: { getBitmap() }
                }
            } else {
                null
            }
            DisposableEffect(slideId, screenshotGetters, riveFile, artboard, stateMachine, instance) {
                onDispose {
                    if (slideId != null) {
                        screenshotGetters?.remove(slideId)
                    }
                }
            }
            // setting the provided text properties on the ViewModel instance
            LaunchedEffect(instance, textProperties) {
                textProperties.forEach { (property, value) ->
                    instance.setString(property, value)
                }
            }
            // Rive numbers are always floats
            LaunchedEffect(instance, numberProperties) {
                numberProperties.forEach { (property, value) ->
                    instance.setNumber(property, value.toFloat())
                }
            }
            LaunchedEffect(instance, booleanProperties) {
                booleanProperties.forEach { (property, value) ->
                    instance.setBoolean(property, value)
                }
            }
            LaunchedEffect(instance, spec.layoutDirectionEnum, layoutDirection) {
                val propertyName = spec.layoutDirectionEnum ?: return@LaunchedEffect
                instance.setEnum(propertyName, if (layoutDirection == LayoutDirection.Rtl) "Right to Left" else "Left to Right")
            }
            val currentOnRiveLoaded by rememberUpdatedState(onRiveLoaded)
            LaunchedEffect(artboard, instance) {
                currentOnRiveLoaded()
            }
            val currentOnUseWhiteTopBarIconsChange by rememberUpdatedState(onUseWhiteTopBarIconsChange)
            LaunchedEffect(instance, spec.isTopBarUiIconsWhite) {
                val propertyName = spec.isTopBarUiIconsWhite ?: return@LaunchedEffect
                instance.getBooleanFlow(propertyName).collect { currentOnUseWhiteTopBarIconsChange(it) }
            }
            val currentOnFrameComplete by rememberUpdatedState(onFrameComplete)
            LaunchedEffect(instance, spec.frameCompleteTrigger) {
                val propertyName = spec.frameCompleteTrigger ?: return@LaunchedEffect
                instance.getTriggerFlow(propertyName).collect { currentOnFrameComplete() }
            }
            LaunchedEffect(instance, imageResults) {
                imageResults.forEach { (property, imageResult) ->
                    when (imageResult) {
                        is Result.Success -> instance.setImage(property, imageResult.value)
                        is Result.Error -> L.w(imageResult.throwable)
                        Result.Loading -> Unit
                    }
                }
            }
            LaunchedEffect(globalInstance, textSizes) {
                textSizes.forEach { (property, value) ->
                    globalInstance?.setNumber(property, value)
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
                onBitmapAvailable = onBitmapAvailable,
                modifier = modifier
                    .fillMaxSize()
                    .onSizeChanged { slideSize = it }
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
private fun rememberFontBytes(source: RiveFontSource): Result<ByteArray> {
    val resources = LocalResources.current
    return produceState<Result<ByteArray>>(
        initialValue = Result.Loading,
        key1 = resources,
        key2 = source
    ) {
        value = try {
            val bytes = withContext(Dispatchers.IO) {
                when (source) {
                    is RiveFontSource.RawResource -> resources.openRawResource(source.resourceId).use { it.readBytes() }
                    is RiveFontSource.SystemFamily -> FontHelper.getFallbackFontBytes(Fonts.FontOpts(familyName = source.familyName))
                        ?: throw IllegalStateException("No system font found for ${source.familyName}")
                }
            }
            Result.Success(bytes)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }.value
}
