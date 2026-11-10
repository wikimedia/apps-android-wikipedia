package org.wikipedia.yearinreview.presentation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.wikipedia.util.log.L

class YearInReviewAudioPlayer(
    private val context: Context,
    private val onPlaybackEnded: (resourceId: Int) -> Unit = {}
) {
    private var player: ExoPlayer? = null

    @RawRes
    private var currentResourceId: Int? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                currentResourceId?.let(onPlaybackEnded)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            L.e(error)
        }
    }

    /**
     * Plays [resourceId] from the start, or, if it's the sound already loaded, carries on from where
     * it was paused.
     */
    fun play(@RawRes resourceId: Int, loop: Boolean) {
        val player = player ?: buildPlayer().also { player = it }
        if (resourceId != currentResourceId) {
            player.setMediaItem(MediaItem.fromUri(rawResourceUri(resourceId)))
            currentResourceId = resourceId
        }
        player.prepare()
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.play()
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        player?.let {
            it.removeListener(playerListener)
            it.release()
        }
        player = null
        currentResourceId = null
    }

    private fun buildPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
                addListener(playerListener)
            }
    }

    private fun rawResourceUri(@RawRes resourceId: Int): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .path(resourceId.toString())
            .build()
    }
}
