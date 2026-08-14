package org.wikipedia.feed.readaloud

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.delay
import org.wikipedia.WikipediaApp
import org.wikipedia.util.log.L
import java.util.Locale

// Fine enough that the highlighted word keeps up with the narration; individual words are often
// only a couple of hundred milliseconds long.
private const val POSITION_POLL_INTERVAL_MILLIS = 50L

/**
 * Playback state of one article's lead-section audio, backed by an [ExoPlayer] that lives exactly as
 * long as the composable that remembers it. Scrolling the card out of the feed therefore stops
 * playback and frees the player.
 */
@Stable
class ReadAloudPlayerState internal constructor(private val player: ExoPlayer?) {
    var isPlaying by mutableStateOf(false)
        internal set
    var isBuffering by mutableStateOf(false)
        internal set
    var hasError by mutableStateOf(false)
        internal set
    var durationMillis by mutableLongStateOf(0L)
        internal set
    var positionMillis by mutableLongStateOf(0L)
        internal set

    var cues by mutableStateOf<List<ReadAloudCue>>(emptyList())
        internal set

    // Flipped by the first tap on Play, which is what triggers the captions download: like the audio
    // itself, nothing is fetched for a card the user never listens to.
    var hasStartedPlayback by mutableStateOf(false)
        private set

    // Stays set once the recording runs to its end, until playback is started again.
    var hasFinishedPlayback by mutableStateOf(false)
        private set

    // While the user drags the progress bar we show the dragged position rather than the playhead, so
    // the bar doesn't snap back to the playhead between drag events.
    private var scrubPositionMillis by mutableStateOf<Long?>(null)

    private var isPrepared = false

    val displayPositionMillis get() = scrubPositionMillis ?: positionMillis

    /**
     * Index into [cues] of the word being spoken, or -1 before the first one starts. Derived rather
     * than polled, so the transcript only recomposes when the word actually changes and not on every
     * playback position tick.
     */
    val currentCueIndex by derivedStateOf { cues.indexOfCueStartedAt(displayPositionMillis) }

    fun playOrPause() {
        val player = player ?: return
        hasStartedPlayback = true
        when {
            hasError -> retry()
            player.isPlaying -> player.pause()
            else -> {
                hasFinishedPlayback = false
                prepareIfNeeded()
                player.play()
            }
        }
    }

    /**
     * Moves the scrub position to a fraction of the recording, without seeking yet: the seek happens
     * once the finger lifts, in [commitScrub], so dragging doesn't thrash the player.
     */
    fun scrubToFraction(fraction: Float) {
        scrubPositionMillis = (fraction.coerceIn(0f, 1f) * durationMillis).toLong()
    }

    fun commitScrub() {
        scrubPositionMillis?.let {
            player?.seekTo(it)
            positionMillis = it
        }
        scrubPositionMillis = null
    }

    /**
     * Nothing is fetched until the user actually starts playback, so merely scrolling past the card
     * never pulls down an audio file.
     */
    private fun prepareIfNeeded() {
        if (!isPrepared) {
            player?.prepare()
            isPrepared = true
        }
    }

    private fun retry() {
        hasError = false
        hasFinishedPlayback = false
        isPrepared = false
        prepareIfNeeded()
        player?.play()
    }

    internal fun onPlaybackEnded() {
        player?.pause()
        player?.seekTo(0)
        positionMillis = 0
        hasFinishedPlayback = true
    }

    internal fun currentPlayerPosition() = player?.currentPosition ?: 0L

    internal fun currentPlayerDuration() = player?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
}

/**
 * Index of the last cue that has started by [millis], which keeps the previous word lit through the
 * silences between words instead of leaving nothing highlighted.
 */
private fun List<ReadAloudCue>.indexOfCueStartedAt(millis: Long): Int {
    var low = 0
    var high = size - 1
    var index = -1
    while (low <= high) {
        val mid = (low + high) / 2
        if (this[mid].startMillis <= millis) {
            index = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return index
}

@Composable
fun rememberReadAloudPlayerState(audioUrl: String, captionsUrl: String): ReadAloudPlayerState {
    val context = LocalContext.current
    // Previews render without a real player, so @Preview functions don't try to reach the network.
    val isPreview = LocalInspectionMode.current
    val player = remember(audioUrl, isPreview) { if (isPreview) null else buildPlayer(context, audioUrl) }
    val state = remember(player, isPreview) { ReadAloudPlayerState(player) }

    if (player == null) {
        return state
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                state.isBuffering = playbackState == Player.STATE_BUFFERING
                when (playbackState) {
                    Player.STATE_READY -> state.durationMillis = state.currentPlayerDuration()
                    Player.STATE_ENDED -> state.onPlaybackEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                L.e(error)
                state.hasError = true
                state.isBuffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            state.positionMillis = state.currentPlayerPosition()
            delay(POSITION_POLL_INTERVAL_MILLIS)
        }
    }

    LaunchedEffect(captionsUrl, state.hasStartedPlayback) {
        if (state.hasStartedPlayback && state.cues.isEmpty()) {
            state.cues = ReadAloudCaptions.fetch(captionsUrl)
        }
    }

    return state
}

fun formatPlaybackTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

@OptIn(UnstableApi::class)
private fun buildPlayer(context: Context, audioUrl: String): ExoPlayer {
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(WikipediaApp.instance.userAgent)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
        }
}
