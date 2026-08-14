package org.wikipedia.feed.readaloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
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
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.views.imageservice.ImageService
import kotlin.math.abs

private const val TRANSCRIPT_WORDS_PER_CHUNK = 16

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
 * Deliberately a sibling of `ForYouCardContent` rather than a caller of it: the layout is the same
 * image-with-scrim treatment, but the playback controls need to sit between the extract and the
 * footer, and the extract is clamped shorter to make room for them.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onPageClick() }
    ) {
        if (title.thumbUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(noImageCardBackgroundColors[backgroundColorIndex % noImageCardBackgroundColors.size]))
            )
        } else {
            FadeInAsyncImage(
                model = title.thumbUrl?.let {
                    ImageService.getRequest(context, url = ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE))
                },
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
                    AppButton(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        onClick = onKeepListeningClick
                    ) {
                        Text(
                            text = context.getString(wikiSite.languageCode, R.string.read_aloud_card_keep_listening),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
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
                        modifier = Modifier.size(24.dp),
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
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Slider(
                modifier = Modifier.weight(1f),
                value = playerState.displayPositionMillis.toFloat(),
                valueRange = 0f..playerState.durationMillis.coerceAtLeast(1L).toFloat(),
                enabled = playerState.canSeek,
                onValueChange = { playerState.scrubTo(it.toLong()) },
                onValueChangeFinished = { playerState.commitScrub() },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    disabledThumbColor = Color.White.copy(alpha = 0.5f),
                    disabledActiveTrackColor = Color.White.copy(alpha = 0.5f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

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
