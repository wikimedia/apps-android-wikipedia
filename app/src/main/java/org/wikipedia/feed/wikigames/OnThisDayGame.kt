package org.wikipedia.feed.wikigames

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.getString
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.games.onthisday.OnThisDayGameResultFragment
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService
import java.util.Locale

@Composable
fun OnThisDayGameCardPreview(
    modifier: Modifier = Modifier,
    state: OnThisDayCardGameState.Preview,
    titleText: String,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    WikiCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = painterResource(R.drawable.ic_events_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
                Text(
                    text = titleText,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.15.sp
                    )
                )
            }

            OnThisDayGameFirstEventView(event = state.event1)

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(),
                color = WikipediaTheme.colors.borderColor
            )

            OnThisDayGameFirstEventView(event = state.event2)

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                FilledTonalButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WikipediaTheme.colors.backgroundColor,
                        contentColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onPlayClick
                ) {
                    Text(
                        text = context.getString(state.langCode, R.string.on_this_day_game_play_today_btn_text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun OnThisDayGameCardSimple(
    modifier: Modifier = Modifier,
    iconRes: Int,
    iconTint: Color,
    titleText: String,
    onPlayClick: () -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp).clickable {
                onPlayClick()
            }
        ) {
            Icon(
                modifier = Modifier
                    .size(44.dp),
                painter = painterResource(iconRes),
                tint = iconTint,
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .padding(top = 16.dp),
                text = titleText,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp
                )
            )
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
fun OnThisDayGameCardProgress(
    modifier: Modifier = Modifier,
    isArchiveGame: Boolean = false,
    state: OnThisDayCardGameState.InProgress,
    titleText: String,
    onContinueClick: () -> Unit
) {
    val context = LocalContext.current
    WikiCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        val columnModifier = Modifier.padding(16.dp).also {
            if (isArchiveGame) {
                it.clickable {
                    onContinueClick()
                }
            }
        }
        Column(
            modifier = columnModifier
        ) {
            Icon(
                modifier = Modifier
                    .size(44.dp),
                painter = painterResource(R.drawable.ic_events_24dp),
                tint = WikipediaTheme.colors.progressiveColor,
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .padding(top = 16.dp),
                text = titleText,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp
                )
            )

            val descriptionText = if (isArchiveGame) {
                stringResource(R.string.on_this_day_game_card_progress_short_label, state.currentQuestion + 1)
            } else {
                stringResource(R.string.on_this_day_game_card_progress_label, state.currentQuestion + 1)
            }

            Text(
                modifier = Modifier,
                text = descriptionText,
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp
                )
            )

            Spacer(Modifier.weight(1f))

            if (!isArchiveGame) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 112.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier
                            .align(Alignment.CenterEnd),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WikipediaTheme.colors.backgroundColor,
                            contentColor = WikipediaTheme.colors.progressiveColor
                        ),
                        onClick = onContinueClick
                    ) {
                        Text(
                            text = context.getString(state.langCode, R.string.on_this_day_game_continue_btn_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnThisDayGameCardCompleted(
    modifier: Modifier = Modifier,
    isArchiveGame: Boolean = false,
    state: OnThisDayCardGameState.Completed,
    titleText: String,
    onReviewResult: () -> Unit,
    onPlayTheArchive: () -> Unit,
    onCountDownFinished: () -> Unit
) {
    val context = LocalContext.current
    WikiCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        val columnModifier = Modifier.padding(16.dp).also {
            if (isArchiveGame) {
                it.clickable {
                    onReviewResult()
                }
            }
        }
        Column(
            modifier = columnModifier
        ) {
            Icon(
                modifier = Modifier
                    .size(44.dp),
                painter = painterResource(R.drawable.ic_event_available),
                tint = WikipediaTheme.colors.successColor,
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .padding(top = 16.dp),
                text = titleText,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp
                )
            )

            OnThisDayGameCountdown(
                state = state,
                isArchiveGame = isArchiveGame,
                onCountDownFinished = onCountDownFinished
            )
            Spacer(Modifier.weight(1f))

            if (!isArchiveGame) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier
                            .weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WikipediaTheme.colors.backgroundColor,
                            contentColor = WikipediaTheme.colors.progressiveColor
                        ),
                        onClick = onReviewResult
                    ) {
                        Text(
                            text = context.getString(state.langCode, R.string.on_this_day_game_review_results_btn_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .weight(1f),
                        onClick = onPlayTheArchive
                    ) {
                        Text(
                            text = context.getString(state.langCode, R.string.on_this_day_game_archive_btn_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = WikipediaTheme.colors.progressiveColor
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnThisDayGameFirstEventView(
    event: OnThisDay.Event
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = event.text,
            color = WikipediaTheme.colors.secondaryColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        AsyncImage(
            model = ImageService.getRequest(
                LocalContext.current,
                url = OnThisDayGameProvider.getThumbnailUrlForEvent(event)
            ),
            error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

/**
 * Counts down the time remaining until the next day by recalculating the duration every second.
 * To avoid false triggers (e.g. user completes the game early in the morning when duration is already > 23hrs),
 * onCountDownFinished only fires after the countdown has dropped below 1 hour.
 * This ensures that an actual countdown cycle occurred.
 * Once below 1 hour, it then watches for the duration to jump back above 23 hours (3600 * 23 seconds),
 * which indicates the date has flipped to the next day.
 */
@Composable
fun OnThisDayGameCountdown(
    state: OnThisDayCardGameState.Completed,
    isArchiveGame: Boolean,
    onCountDownFinished: () -> Unit
) {
    var hasCountedDown = false
    var duration by remember { mutableStateOf(OnThisDayGameResultFragment.timeUntilNextDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            duration = OnThisDayGameResultFragment.timeUntilNextDay()
            if (duration.seconds < 3600) {
                hasCountedDown = true
            }
            if (hasCountedDown && duration.seconds > 3600 * 23) {
                onCountDownFinished()
                break
            }
        }
    }

    val countdownText = if (isArchiveGame) {
        stringResource(R.string.on_this_day_game_explore_feed_card_score_short_message, state.score, state.totalQuestions)
    } else {
        stringResource(R.string.on_this_day_game_explore_feed_card_score_message, state.score, state.totalQuestions, String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            duration.toHoursPart(),
            duration.toMinutesPart(),
            duration.toSecondsPart()
        ))
    }

    Text(
        modifier = Modifier,
        text = countdownText,
        color = WikipediaTheme.colors.secondaryColor,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun OnThisDayGameCardPreviewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnThisDayGameCardPreview(
            state = OnThisDayCardGameState.Preview(
                langCode = "en",
                event1 = OnThisDay.Event(
                    pages = emptyList(),
                    text = "Event 1: Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                    year = 1990
                ),
                event2 = OnThisDay.Event(
                    pages = emptyList(),
                    text = "Event 2: Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    year = 2000
                )
            ),
            titleText = "November 3",
            onPlayClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnThisDayGameCardProgressPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnThisDayGameCardProgress(
            isArchiveGame = false,
            state = OnThisDayCardGameState.InProgress("en", 3),
            titleText = "November 2",
            onContinueClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnThisDayGameCardCompletedPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnThisDayGameCardCompleted(
            isArchiveGame = false,
            state = OnThisDayCardGameState.Completed(
                langCode = "en",
                score = 4,
                totalQuestions = 5
            ),
            titleText = "November 1",
            onReviewResult = {},
            onPlayTheArchive = {},
            onCountDownFinished = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnThisDayGameCardSimplePreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnThisDayGameCardSimple(
            titleText = "Archive",
            iconRes = R.drawable.event_repeat_24dp,
            iconTint = WikipediaTheme.colors.primaryColor,
            onPlayClick = {}
        )
    }
}
