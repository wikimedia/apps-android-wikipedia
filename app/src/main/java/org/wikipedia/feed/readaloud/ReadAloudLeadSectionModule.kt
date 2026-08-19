package org.wikipedia.feed.readaloud

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.components.FadeInAsyncImage
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.ForYouCardDropdownMenu
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.ReadAloudLeadSectionCard
import org.wikipedia.feed.noImageCardBackgroundColors
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.imageservice.ImageService
import kotlin.math.abs
import kotlin.math.hypot

private const val TRANSCRIPT_WORDS_PER_CHUNK = 16
private const val GRADIENT_FADE_DURATION_MILLIS = 500
private val THUMBNAIL_SIZE = 220.dp

/**
 * A radial gradient that reaches black only at the card's corners, rather than at the midpoint of
 * its shortest edge as [Brush.radialGradient] does by default. The thumbnail covers the middle of
 * the card, so a gradient sized to the short edge would be almost entirely hidden behind it.
 */
private fun radialBackdrop(color: Color) = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = RadialGradientShader(
        center = Offset(size.width / 2f, size.height / 2f),
        radius = hypot(size.width, size.height) / 2f,
        colors = listOf(color, Color.Black)
    )
}

/**
 * Pulls the dominant color out of the article thumbnail, to tint the backdrop behind it. This goes
 * through the very same request that renders the image, so it is served from the image cache
 * instead of fetching the thumbnail a second time.
 */
private suspend fun dominantColorOf(context: Context, thumbnailUrl: String): Color? = withContext(Dispatchers.Default) {
    try {
        val request = ImageService.getRequest(context, url = thumbnailUrl) as? ImageRequest ?: return@withContext null
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return@withContext null
        val palette = Palette.from(result.image.toBitmap()).generate()
        (palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch)?.let { Color(it.rgb) }
    } catch (e: Exception) {
        L.e(e)
        null
    }
}

@Composable
fun ReadAloudLeadSectionModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.ReadAloudLeadSection,
    resolveSavedState: suspend (PageTitle) -> Boolean = { false },
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeClick: (card: Card) -> Unit = {},
    onKeepListeningClick: (card: Card) -> Unit = {},
) {
    val context = LocalContext.current
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { pageIndex ->
        val card = module.cards[pageIndex] as ReadAloudLeadSectionCard
        val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_FEED_READ_ALOUD)
        val topic = ArticleTopics.all.find { it.topicId == card.interestTopic.topicId }

        ReadAloudCardContent(
            wikiSite = wikiSite,
            title = card.title,
            resolveSavedState = resolveSavedState,
            backgroundColorIndex = backgroundColorIndex + pageIndex,
            module = module,
            card = card,
            footerText = topic?.let {
                context.getString(
                    wikiSite.languageCode,
                    R.string.explore_feed_because_of_interest,
                    context.getString(wikiSite.languageCode, it.msgKey)
                )
            },
            onPageClick = { onPageClick(card, historyEntry) },
            onShareClick = { onPageShareClick(card, historyEntry) },
            onSaveClick = { onPageBookmarkClick(card, historyEntry) },
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeClick = { onCustomizeClick(card) },
            onKeepListeningClick = { onKeepListeningClick(card) }
        )
    }
}

/**
 * Deliberately a sibling of `ForYouCardContent` rather than a caller of it: this card needs room for
 * the playback controls between the text and the footer, and it swaps the full-bleed thumbnail for a
 * small centered one over a backdrop tinted with the thumbnail's own dominant color.
 */
@Composable
private fun ReadAloudCardContent(
    wikiSite: WikiSite,
    title: PageTitle,
    resolveSavedState: suspend (PageTitle) -> Boolean = { false },
    backgroundColorIndex: Int = 0,
    module: ForYouModule? = null,
    card: ForYouCard? = null,
    footerText: String? = null,
    onPageClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCustomizeClick: () -> Unit = {},
    onKeepListeningClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    // Resolved on demand when the overflow button is tapped, so we never query the whole feed up front.
    var isInReadingList by remember { mutableStateOf(false) }
    val showSpaceForPagerDots = (module?.cards?.size ?: 0) > 1
    val playerState = rememberReadAloudPlayerState(
        audioUrl = ReadAloudArticlesRepository.audioUrlFor(title),
        captionsUrl = ReadAloudArticlesRepository.captionsUrlFor(title)
    )

    val thumbnailUrl = title.thumbUrl?.takeIf { it.isNotEmpty() }
        ?.let { ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE) }
    val fallbackColor = colorResource(noImageCardBackgroundColors[backgroundColorIndex % noImageCardBackgroundColors.size])
    var thumbnailColor by remember(thumbnailUrl) { mutableStateOf(fallbackColor) }
    // Fades from the fallback to the thumbnail's own color, so the backdrop doesn't pop once the
    // palette comes back.
    val gradientColor by animateColorAsState(targetValue = thumbnailColor, animationSpec = tween(GRADIENT_FADE_DURATION_MILLIS))

    LaunchedEffect(thumbnailUrl) {
        thumbnailUrl?.let { thumbnailColor = dominantColorOf(context, it) ?: fallbackColor }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onPageClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(remember(gradientColor) { radialBackdrop(gradientColor) })
        )

        thumbnailUrl?.let {
            FadeInAsyncImage(
                model = ImageService.getRequest(context, url = it),
                placeholder = ColorPainter(Color.Transparent),
                error = ColorPainter(Color.Transparent),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.align(Alignment.Center).size(THUMBNAIL_SIZE).clip(RoundedCornerShape(16.dp))
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
            Column(
                modifier = Modifier.background(color = Color.Black.copy(alpha = 0.80f))
                    .padding(bottom = if (showSpaceForPagerDots) 40.dp else 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HtmlText(
                        modifier = Modifier.weight(1f).padding(start = 16.dp, top = 8.dp),
                        text = title.displayText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 2
                    )
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = {
                            scope.launch {
                                isInReadingList = resolveSavedState(title)
                                overflowMenuExpanded = true
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                            contentDescription = context.getString(wikiSite.languageCode, R.string.menu_feed_overflow_label),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    ForYouCardDropdownMenu(
                        expanded = overflowMenuExpanded,
                        wikiSite = wikiSite,
                        isInReadingList = isInReadingList,
                        onDismiss = { overflowMenuExpanded = false },
                        onShareClick = { onShareClick() },
                        onSaveClick = { onSaveClick() },
                        onHideCardClick = {
                            if (module != null && card != null) {
                                onHideCardClick(module, card)
                            }
                        },
                        onHideModuleClick = onHideModuleClick,
                        onCustomizeClick = onCustomizeClick
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                ) {
                    if (playerState.cues.isEmpty()) {
                        (title.extract ?: title.description)?.let {
                            HtmlText(
                                text = it,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        ReadAloudTranscript(
                            cues = playerState.cues,
                            currentCueIndex = playerState.currentCueIndex
                        )
                    }
                }

                ReadAloudPlaybackControls(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp),
                    wikiSite = wikiSite,
                    playerState = playerState
                )

                if (playerState.hasFinishedPlayback) {
                    OutlinedButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = onKeepListeningClick,
                        shape = RoundedCornerShape(percent = 50),
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = context.getString(wikiSite.languageCode, R.string.read_aloud_card_keep_listening),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                footerText?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.volume_up_24dp),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 4.dp).size(16.dp)
                        )
                        HtmlText(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shows the narration a chunk of [TRANSCRIPT_WORDS_PER_CHUNK] words at a time, with the word
 * currently being spoken in white and the rest in gray. Advancing a chunk at a time rather than
 * sliding a window word by word keeps the text still between chunks, so only the highlight moves.
 */
@Composable
private fun ReadAloudTranscript(
    modifier: Modifier = Modifier,
    cues: List<ReadAloudCue>,
    currentCueIndex: Int
) {
    val chunkStart = currentCueIndex.coerceAtLeast(0) / TRANSCRIPT_WORDS_PER_CHUNK * TRANSCRIPT_WORDS_PER_CHUNK
    val chunkEnd = (chunkStart + TRANSCRIPT_WORDS_PER_CHUNK).coerceAtMost(cues.size)
    val transcript = buildAnnotatedString {
        for (index in chunkStart until chunkEnd) {
            if (index > chunkStart) {
                append(' ')
            }
            withStyle(SpanStyle(color = if (index == currentCueIndex) Color.White else Color.White.copy(alpha = 0.5f))) {
                append(cues[index].word)
            }
        }
    }
    Text(
        modifier = modifier,
        text = transcript,
        style = MaterialTheme.typography.headlineSmall,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ReadAloudPlaybackControls(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    playerState: ReadAloudPlayerState
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = { playerState.playOrPause() }
            ) {
                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    val iconRes = when {
                        playerState.hasError -> R.drawable.ic_replay_black_24dp
                        playerState.isPlaying -> R.drawable.ic_pause_black_24dp
                        else -> R.drawable.ic_play_arrow_black_24dp
                    }
                    val labelRes = when {
                        playerState.hasError -> R.string.read_aloud_card_retry
                        playerState.isPlaying -> R.string.read_aloud_card_pause
                        else -> R.string.read_aloud_card_play
                    }
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = context.getString(wikiSite.languageCode, labelRes),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(playerState) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Nothing to seek to until the recording's length is known; let the tap
                            // fall through to opening the article instead.
                            if (playerState.durationMillis <= 0) {
                                return@awaitEachGesture
                            }
                            // Consuming keeps the tap away from the card's own click handler, and the
                            // drag away from the pager that would otherwise swipe to the next card.
                            down.consume()
                            playerState.scrubToFraction(down.position.x / size.width)
                            drag(down.id) { change ->
                                change.consume()
                                playerState.scrubToFraction(change.position.x / size.width)
                            }
                            playerState.commitScrub()
                        }
                    }
                    // Padded after the gesture modifier, so the touch target is taller than the 4dp bar.
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    // Read in the draw phase rather than in composition, so the bar advances without
                    // recomposing this row on every playback position tick.
                    progress = {
                        val duration = playerState.durationMillis
                        if (duration > 0) (playerState.displayPositionMillis.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    drawStopIndicator = {}
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${formatPlaybackTime(playerState.displayPositionMillis)} / ${formatPlaybackTime(playerState.durationMillis)}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (playerState.hasError) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = context.getString(wikiSite.languageCode, R.string.read_aloud_card_error),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }

        playerState.generatedDate?.let {
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                text = context.getString(wikiSite.languageCode, R.string.read_aloud_card_audio_date, DateUtil.getShortDateString(it)),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview
@Composable
fun ReadAloudLeadSectionCardPreviewWithImage() {
    val card = ReadAloudLeadSectionCard(PageTitle.preview(), InterestTopic("architecture"))
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadAloudLeadSectionModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.ReadAloudLeadSection(0, 0, listOf(card, card, card, card))
        )
    }
}

@Preview
@Composable
fun ReadAloudLeadSectionCardPreviewNoImage() {
    val card = ReadAloudLeadSectionCard(PageTitle.preview(withThumbnail = false), InterestTopic("music"))
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadAloudLeadSectionModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.ReadAloudLeadSection(0, 0, listOf(card))
        )
    }
}
