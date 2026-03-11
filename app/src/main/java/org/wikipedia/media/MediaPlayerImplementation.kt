package org.wikipedia.media

import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import androidx.core.net.toUri
import org.wikipedia.WikipediaApp
import org.wikipedia.util.log.L
import java.io.IOException

class MediaPlayerImplementation {
    private val player = MediaPlayer()
    val isPlaying get() = player.isPlaying

    fun deinit() {
        player.release()
    }

    fun load(path: String, callback: AvPlayer.Callback) {
        val wrapper = CallbackWrapper(callback)
        player.reset()
        player.setOnPreparedListener(wrapper)
        player.setOnErrorListener(wrapper)
        if (setDataSource(path)) {
            player.prepareAsync()
        } else {
            wrapper.onError(player, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        }
    }

    fun play(callback: AvPlayer.Callback) {
        val wrapper = CallbackWrapper(callback)
        player.setOnCompletionListener(wrapper)
        player.setOnErrorListener(wrapper)
        player.start()
    }

    fun pause() {
        player.pause()
    }

    fun stop() {
        player.stop()
    }

    private fun setDataSource(path: String): Boolean {
        return try {
            player.setDataSource(
                WikipediaApp.instance,
                path.toUri(),
                mapOf("User-Agent" to WikipediaApp.instance.userAgent)
            )
            true
        } catch (e: IOException) {
            L.e(e)
            false
        }
    }

    private class CallbackWrapper(val callback: AvPlayer.Callback) : OnPreparedListener, OnCompletionListener, MediaPlayer.OnErrorListener {
        override fun onCompletion(mp: MediaPlayer) {
            callback.onSuccess()
        }

        override fun onPrepared(mp: MediaPlayer) {
            callback.onSuccess()
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            callback.onError(what, extra)
            return true
        }
    }
}
