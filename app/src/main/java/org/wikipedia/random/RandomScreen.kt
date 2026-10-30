package org.wikipedia.random

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.FadeInAsyncImage
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.instrument
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.views.imageservice.ImageService
import kotlin.math.sqrt

@Composable
fun RandomScreen(
    viewModel: RandomViewModel,
    onBackPressed: () -> Unit,
    onArticleClick: (PageTitle) -> Unit,
    onSaveClick: (PageTitle) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = RandomViewModel.FIRST_PAGE, pageCount = { Int.MAX_VALUE })

    val currentTitle = viewModel.itemAt(pagerState.currentPage)

    LaunchedEffect(pagerState.currentPage, currentTitle) {
        viewModel.updateSaveState(currentTitle)
        if (currentTitle != null) {
            context.instrument?.submitInteraction("impression", pageData = TestKitchenAdapter.getPageData(pageTitle = currentTitle))
        }
    }

    val goToNext: () -> Unit = {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }
    val goToPrevious: () -> Unit = {
        if (pagerState.currentPage > RandomViewModel.FIRST_PAGE) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    ShakeToAdvance(onShake = {
        context.instrument?.submitInteraction("shake")
        goToNext()
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                LaunchedEffect(page) {
                    viewModel.prefetchIfNeeded(page)
                }
                RandomItemPage(
                    state = viewModel.stateFor(page),
                    onClick = onArticleClick,
                    onRetry = { viewModel.retry() },
                    onBackClick = onBackPressed
                )
            }

            RandomControls(
                isSaved = viewModel.saveButtonState,
                saveEnabled = currentTitle != null,
                backEnabled = pagerState.currentPage > RandomViewModel.FIRST_PAGE,
                onBackClick = {
                    context.instrument?.submitInteraction("click", elementId = "previous_button")
                    goToPrevious()
                },
                onNextClick = {
                    context.instrument?.submitInteraction("click", elementId = "next_button")
                    goToNext()
                },
                onSaveClick = { currentTitle?.let(onSaveClick) }
            )
        }

        Column(modifier = Modifier.align(Alignment.TopStart)) {
            RandomTopBar(
                modifier = Modifier.background(Color.Black.copy(alpha = 0.8f)),
                onBackPressed = onBackPressed
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.80f),
                                0.18f to Color.Black.copy(alpha = 0.7f),
                                0.38f to Color.Black.copy(alpha = 0.50f),
                                0.58f to Color.Black.copy(alpha = 0.30f),
                                0.76f to Color.Black.copy(alpha = 0.15f),
                                0.90f to Color.Black.copy(alpha = 0.05f),
                                1.0f to Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun RandomItemPage(
    state: Resource<PageTitle>?,
    onClick: (PageTitle) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.backgroundColor)
    ) {
        when (state) {
            is Resource.Success -> {
                val title = state.data
                val layoutDirection = if (L10nUtil.isLangRTL(title.wikiSite.languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onClick(title) }
                    ) {
                        if (title.thumbUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WikipediaTheme.colors.backgroundColor)
                            )
                        } else {
                            FadeInAsyncImage(
                                model = ImageService.getRequest(
                                    context,
                                    url = ImageUrlUtil.getUrlForPreferredSize(title.thumbUrl!!, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)
                                ),
                                placeholder = ColorPainter(Color.Black),
                                error = ColorPainter(Color.DarkGray),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Transparent,
                                                0.18f to Color.Black.copy(alpha = 0.05f),
                                                0.38f to Color.Black.copy(alpha = 0.15f),
                                                0.58f to Color.Black.copy(alpha = 0.30f),
                                                0.76f to Color.Black.copy(alpha = 0.50f),
                                                0.90f to Color.Black.copy(alpha = 0.7f),
                                                1.0f to Color.Black.copy(alpha = 0.80f)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = Color.Black.copy(alpha = 0.8f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                                    HtmlText(
                                        text = title.displayText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = FontFamily.Serif
                                        ),
                                        maxLines = 3
                                    )
                                    if (!title.description.isNullOrEmpty()) {
                                        HtmlText(
                                            modifier = Modifier.padding(top = 4.dp),
                                            text = title.description!!,
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .width(48.dp),
                                        thickness = 1.dp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    if (!title.extract.isNullOrEmpty()) {
                                        HtmlText(
                                            text = title.extract!!,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 8
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is Resource.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.throwable.localizedMessage ?: stringResource(R.string.error_message_generic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = stringResource(R.string.error_back),
                            style = MaterialTheme.typography.titleMedium,
                            color = WikipediaTheme.colors.progressiveColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onBackClick() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.page_error_retry),
                            style = MaterialTheme.typography.titleMedium,
                            color = WikipediaTheme.colors.progressiveColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onRetry() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            else -> {
                CircularProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun RandomTopBar(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                tint = Color.White,
                contentDescription = stringResource(R.string.search_back_button_content_description)
            )
        }
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.view_random_card_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
    }
}

@Composable
private fun RandomControls(
    isSaved: Boolean,
    saveEnabled: Boolean,
    backEnabled: Boolean,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    var diceClickCount by remember { mutableIntStateOf(0) }
    val diceRotation by animateFloatAsState(targetValue = diceClickCount * 360f, label = "diceRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WikipediaTheme.colors.paperColor)
            .navigationBarsPadding()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            enabled = backEnabled
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_replay_black_24dp),
                contentDescription = null,
                tint = WikipediaTheme.colors.progressiveColor.copy(alpha = if (backEnabled) 1f else 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(32.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WikipediaTheme.colors.progressiveColor)
                .clickable {
                    diceClickCount++
                    onNextClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dice_24),
                contentDescription = stringResource(R.string.view_next_random_article),
                tint = WikipediaTheme.colors.paperColor,
                modifier = Modifier.rotate(diceRotation)
            )
        }
        Spacer(modifier = Modifier.width(32.dp))
        IconButton(
            onClick = onSaveClick,
            enabled = saveEnabled
        ) {
            Icon(
                painter = painterResource(
                    if (isSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
                ),
                contentDescription = stringResource(R.string.button_add_to_reading_list),
                tint = WikipediaTheme.colors.progressiveColor.copy(alpha = if (saveEnabled) 1f else 0.5f)
            )
        }
    }
}

@Composable
private fun ShakeToAdvance(onShake: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService<SensorManager>()
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L
        var firstImpactTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
                if (acceleration <= SHAKE_THRESHOLD) {
                    return
                }
                val now = System.currentTimeMillis()
                if (now - lastShakeTime < SHAKE_COOLDOWN_MS) {
                    return
                }
                if (firstImpactTime == 0L) {
                    firstImpactTime = now
                } else if (now - firstImpactTime <= SHAKE_DOUBLE_WINDOW_MS) {
                    firstImpactTime = 0L
                    lastShakeTime = now
                    currentOnShake()
                    val vibrator = context.getSystemService(Vibrator::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(SHAKE_VIBRATE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(SHAKE_VIBRATE_MS)
                    }
                } else {
                    firstImpactTime = now
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    firstImpactTime = 0L
                    sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                }
                Lifecycle.Event.ON_PAUSE -> sensorManager?.unregisterListener(listener)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
        }
    }
}

private const val SHAKE_THRESHOLD = 12f
private const val SHAKE_COOLDOWN_MS = 1000L
private const val SHAKE_DOUBLE_WINDOW_MS = 500L
private const val SHAKE_VIBRATE_MS = 80L

@Preview
@Composable
private fun RandomItemPagePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                RandomItemPage(
                    state = Resource.Success(PageTitle.preview()),
                    onClick = {},
                    onRetry = {},
                    onBackClick = {}
                )
            }
            RandomControls(
                isSaved = false,
                saveEnabled = true,
                backEnabled = true,
                onBackClick = {},
                onNextClick = {},
                onSaveClick = {}
            )
        }
    }
}
